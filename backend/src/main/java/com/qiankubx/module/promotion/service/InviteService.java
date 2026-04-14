package com.qiankubx.module.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.module.promotion.dto.InviteCodeVO;
import com.qiankubx.module.promotion.dto.InviteRecordVO;
import com.qiankubx.module.promotion.entity.Invitation;
import com.qiankubx.module.promotion.mapper.InvitationMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteService {

    private final InvitationMapper invitationMapper;
    private final UserMapper userMapper;
    private final PromoterLevelService promoterLevelService;

    public InviteCodeVO getInviteCode(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(400, "用户不存在");
        }

        String inviteCode = user.getInviteCode();
        if (inviteCode == null || inviteCode.isBlank()) {
            inviteCode = generateInviteCode();
            user.setInviteCode(inviteCode);
            userMapper.updateById(user);
        }

        InviteCodeVO vo = new InviteCodeVO();
        vo.setInviteCode(inviteCode);
        vo.setInviteLink("https://qianku.com/register?invite=" + inviteCode);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindInviteRelation(Long inviteeId, String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            return;
        }

        User inviter = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getInviteCode, inviteCode)
        );
        if (inviter == null) {
            log.warn("邀请码无效: {}", inviteCode);
            return;
        }

        if (inviter.getId().equals(inviteeId)) {
            log.warn("不能邀请自己: userId={}", inviteeId);
            return;
        }

        Invitation level1 = new Invitation();
        level1.setInviterId(inviter.getId());
        level1.setInviteeId(inviteeId);
        level1.setLevel(1);
        level1.setInviteeStatus(0);
        level1.setCreatedAt(LocalDateTime.now());
        invitationMapper.insert(level1);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, inviteeId)
                .set(User::getInviterId, inviter.getId())
        );

        promoterLevelService.incrementInviteCount(inviter.getId());

        if (inviter.getInviterId() != null) {
            Invitation level2 = new Invitation();
            level2.setInviterId(inviter.getInviterId());
            level2.setInviteeId(inviteeId);
            level2.setLevel(2);
            level2.setInviteeStatus(0);
            level2.setCreatedAt(LocalDateTime.now());
            invitationMapper.insert(level2);

            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, inviteeId)
                    .set(User::getRootInviterId, inviter.getInviterId())
            );
        }

        log.info("邀请关系绑定成功: inviterId={}, inviteeId={}", inviter.getId(), inviteeId);
    }

    public List<InviteRecordVO> listInviteRecords(Long userId) {
        List<Invitation> invitations = invitationMapper.selectList(
                new LambdaQueryWrapper<Invitation>()
                        .eq(Invitation::getInviterId, userId)
                        .orderByDesc(Invitation::getCreatedAt)
        );

        List<InviteRecordVO> list = new ArrayList<>();
        for (Invitation inv : invitations) {
            User invitee = userMapper.selectById(inv.getInviteeId());
            InviteRecordVO vo = new InviteRecordVO();
            vo.setInviteeId(inv.getInviteeId());
            vo.setInviteeNickname(invitee != null ? invitee.getNickname() : null);
            vo.setInviteeAvatarUrl(invitee != null ? invitee.getAvatarUrl() : null);
            vo.setLevel(inv.getLevel());
            vo.setInviteeStatus(inv.getInviteeStatus());
            vo.setCreatedAt(inv.getCreatedAt());
            list.add(vo);
        }
        return list;
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
