package com.qiankubx.module.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.module.coupon.dto.UserCouponVO;
import com.qiankubx.module.coupon.entity.CouponTemplate;
import com.qiankubx.module.coupon.entity.UserCoupon;
import com.qiankubx.module.coupon.mapper.CouponTemplateMapper;
import com.qiankubx.module.coupon.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;

    public List<UserCouponVO> listMyCoupons(Long userId, Integer useStatus) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getCreatedAt);
        if (useStatus != null) {
            if (useStatus == 0) {
                wrapper.eq(UserCoupon::getUseStatus, 0)
                        .gt(UserCoupon::getExpireAt, LocalDateTime.now());
            } else if (useStatus == 2) {
                wrapper.and(w -> w.eq(UserCoupon::getUseStatus, 2)
                        .or()
                        .eq(UserCoupon::getUseStatus, 0).le(UserCoupon::getExpireAt, LocalDateTime.now()));
            } else {
                wrapper.eq(UserCoupon::getUseStatus, useStatus);
            }
        }
        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        return coupons.stream().map(this::toVO).toList();
    }

    public List<UserCouponVO> getAvailableCoupons(Long userId, Integer planType) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getUseStatus, 0)
                .gt(UserCoupon::getExpireAt, LocalDateTime.now())
                .and(w -> w.eq(UserCoupon::getApplicablePlanType, 0)
                        .or()
                        .eq(UserCoupon::getApplicablePlanType, planType))
                .orderByDesc(UserCoupon::getDiscountValue);
        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        return coupons.stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long userId, Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || template.getStatus() != 1) {
            throw new BizException(400, "优惠券不存在或已下架");
        }

        if (template.getTotalCount() != null && template.getIssuedCount() >= template.getTotalCount()) {
            throw new BizException(400, "优惠券已领完");
        }

        Long existing = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getTemplateId, templateId)
        );
        if (existing > 0) {
            throw new BizException(400, "已领取过该优惠券");
        }

        issueCouponFromTemplate(userId, template);

        couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .setSql("issued_count = issued_count + 1")
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long userId, Long couponId, Long orderId) {
        UserCoupon coupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getId, couponId)
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getUseStatus, 0)
        );
        if (coupon == null) {
            throw new BizException(400, "优惠券不可用");
        }
        if (coupon.getExpireAt() != null && coupon.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(400, "优惠券已过期");
        }

        coupon.setUseStatus(1);
        coupon.setOrderId(orderId);
        coupon.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(coupon);
    }

    public BigDecimal calculateDiscount(Long userId, Long couponId, BigDecimal orderAmount) {
        UserCoupon coupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getId, couponId)
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getUseStatus, 0)
        );
        if (coupon == null) {
            return BigDecimal.ZERO;
        }
        if (coupon.getExpireAt() != null && coupon.getExpireAt().isBefore(LocalDateTime.now())) {
            return BigDecimal.ZERO;
        }
        if (coupon.getMinAmount() != null && orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        return switch (coupon.getType()) {
            case 1 -> coupon.getDiscountValue();
            case 2 -> {
                BigDecimal discount = orderAmount.multiply(
                        BigDecimal.ONE.subtract(coupon.getDiscountValue().divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP))
                );
                yield discount.setScale(2, RoundingMode.HALF_UP);
            }
            case 3 -> coupon.getDiscountValue();
            default -> BigDecimal.ZERO;
        };
    }

    @Transactional(rollbackFor = Exception.class)
    public void issueNewUserCoupon(Long userId) {
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getIssueType, 1)
                        .eq(CouponTemplate::getStatus, 1)
        );
        for (CouponTemplate template : templates) {
            issueCouponFromTemplate(userId, template);
        }
        log.info("新人优惠券发放成功: userId={}, count={}", userId, templates.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void issueInviteCoupon(Long userId) {
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getIssueType, 2)
                        .eq(CouponTemplate::getStatus, 1)
        );
        for (CouponTemplate template : templates) {
            issueCouponFromTemplate(userId, template);
        }
        log.info("邀请优惠券发放成功: userId={}, count={}", userId, templates.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void issueRedeemCoupon(Long userId, BigDecimal amount) {
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setName("积分兑换¥" + amount.stripTrailingZeros().toPlainString() + "优惠券");
        coupon.setType(3);
        coupon.setDiscountValue(amount);
        coupon.setMinAmount(BigDecimal.ZERO);
        coupon.setApplicablePlanType(0);
        coupon.setUseStatus(0);
        coupon.setExpireAt(LocalDateTime.now().plusDays(30));
        coupon.setCreatedAt(LocalDateTime.now());
        userCouponMapper.insert(coupon);
        log.info("积分兑换优惠券发放: userId={}, amount={}", userId, amount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void issueRecallCoupon(Long userId) {
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setName("会员唤回¥20优惠券");
        coupon.setType(3);
        coupon.setDiscountValue(new BigDecimal("20"));
        coupon.setMinAmount(BigDecimal.ZERO);
        coupon.setApplicablePlanType(0);
        coupon.setUseStatus(0);
        coupon.setExpireAt(LocalDateTime.now().plusDays(7));
        coupon.setCreatedAt(LocalDateTime.now());
        userCouponMapper.insert(coupon);
        log.info("会员唤回优惠券发放: userId={}", userId);
    }

    private void issueCouponFromTemplate(Long userId, CouponTemplate template) {
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setTemplateId(template.getId());
        coupon.setName(template.getName());
        coupon.setType(template.getType());
        coupon.setDiscountValue(template.getDiscountValue());
        coupon.setMinAmount(template.getMinAmount());
        coupon.setApplicablePlanType(template.getApplicablePlanType());
        coupon.setUseStatus(0);
        coupon.setExpireAt(LocalDateTime.now().plusDays(template.getValidDays()));
        coupon.setCreatedAt(LocalDateTime.now());
        userCouponMapper.insert(coupon);
    }

    private UserCouponVO toVO(UserCoupon coupon) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setType(coupon.getType());
        vo.setTypeName(switch (coupon.getType()) {
            case 1 -> "满减券";
            case 2 -> "折扣券";
            case 3 -> "代金券";
            default -> "未知";
        });
        vo.setDiscountValue(coupon.getDiscountValue());
        vo.setMinAmount(coupon.getMinAmount());
        vo.setApplicablePlanType(coupon.getApplicablePlanType());
        if (coupon.getUseStatus() == 0 && coupon.getExpireAt() != null
                && coupon.getExpireAt().isBefore(LocalDateTime.now())) {
            vo.setUseStatus(2);
        } else {
            vo.setUseStatus(coupon.getUseStatus());
        }
        vo.setExpireAt(coupon.getExpireAt());
        vo.setCreatedAt(coupon.getCreatedAt());
        return vo;
    }
}
