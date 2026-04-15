package com.qiankubx.module.reimbursement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.module.expense.dto.ExpenseVO;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.expense.entity.ExpenseCategory;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.expense.mapper.ExpenseCategoryMapper;
import com.qiankubx.module.expense.mapper.ExpenseMapper;
import com.qiankubx.module.expense.service.ExpenseService;
import com.qiankubx.module.reimbursement.dto.*;
import com.qiankubx.module.reimbursement.entity.Reimbursement;
import com.qiankubx.module.reimbursement.mapper.ReimbursementMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReimbursementService {

    private final ReimbursementMapper reimbursementMapper;
    private final ExpenseMapper expenseMapper;
    private final ExpenseCategoryMapper expenseCategoryMapper;
    private final UserMapper userMapper;
    private final PdfCoverService pdfCoverService;
    private final PdfMergeService pdfMergeService;
    private final ZipPackageService zipPackageService;
    private final EmailSendService emailSendService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final @Lazy DataSignService dataSignService;
    private final @Lazy ExpenseService expenseService;

    @Transactional(rollbackFor = Exception.class)
    public ReimbursementVO generate(Long userId, ReimbursementCreateDTO dto) {
        checkReimburseQuota(userId);

        List<Expense> expenses = validateAndGetExpenses(userId, dto.getExpenseIds());

        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int invoiceCount = (int) expenses.stream()
                .filter(e -> e.getFileUrl() != null && !e.getFileUrl().isEmpty())
                .count();

        String reimburseNo = generateReimburseNo();

        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setUserId(userId);
        reimbursement.setReimburseNo(reimburseNo);
        reimbursement.setTitle(dto.getTitle());
        reimbursement.setTotalAmount(totalAmount);
        reimbursement.setInvoiceCount(invoiceCount);
        reimbursement.setItemCount(expenses.size());
        reimbursement.setRemark(dto.getRemark());
        reimbursement.setTripId(dto.getTripId());
        reimbursement.setReimburseStatus(0);
        reimbursement.setExportCount(0);
        reimbursement.setEmailSent(0);
        reimbursement.setStatus(0);
        reimbursement.setCreatedAt(LocalDateTime.now());
        reimbursement.setUpdatedAt(LocalDateTime.now());

        reimbursementMapper.insert(reimbursement);

        updateExpensesReimburseStatus(dto.getExpenseIds(), reimbursement.getId(), 1);

        User user = userMapper.selectById(userId);
        Map<Long, String> categoryMap = buildCategoryMap();

        String coverUrl = pdfCoverService.generateCoverPdf(reimbursement, user, expenses, categoryMap);
        reimbursement.setPdfUrl(coverUrl);

        String mergedUrl = pdfMergeService.mergePdfs(reimbursement, expenses);
        reimbursement.setMergedPdfUrl(mergedUrl);

        String zipUrl = zipPackageService.generateZip(reimbursement, expenses, mergedUrl);
        reimbursement.setZipUrl(zipUrl);

        reimbursementMapper.updateById(reimbursement);

        incrementReimburseUsed(userId);

        return toVO(reimbursement, expenses);
    }

    public List<ReimbursementVO> list(Long userId, Integer reimburseStatus) {
        LambdaQueryWrapper<Reimbursement> wrapper = new LambdaQueryWrapper<Reimbursement>()
                .eq(Reimbursement::getUserId, userId)
                .eq(Reimbursement::getStatus, 0)
                .orderByDesc(Reimbursement::getCreatedAt);
        if (reimburseStatus != null) {
            wrapper.eq(Reimbursement::getReimburseStatus, reimburseStatus);
        }
        List<Reimbursement> reimbursements = reimbursementMapper.selectList(wrapper);
        return reimbursements.stream().map(r -> toVO(r, null)).toList();
    }

    public ReimbursementVO getDetail(Long userId, Long id) {
        Reimbursement reimbursement = getByIdAndUser(id, userId);
        List<Expense> expenses = getRelatedExpenses(reimbursement.getId());
        return toVO(reimbursement, expenses);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReimbursementVO update(Long userId, ReimbursementUpdateDTO dto) {
        Reimbursement reimbursement = getByIdAndUser(dto.getId(), userId);

        if (dto.getTitle() != null) {
            reimbursement.setTitle(dto.getTitle());
        }
        if (dto.getRemark() != null) {
            reimbursement.setRemark(dto.getRemark());
        }

        if (dto.getExpenseIds() != null && !dto.getExpenseIds().isEmpty()) {
            List<Expense> oldExpenses = getRelatedExpenses(reimbursement.getId());
            List<Long> oldIds = oldExpenses.stream().map(Expense::getId).toList();
            updateExpensesReimburseStatus(oldIds, null, 0);

            List<Expense> newExpenses = validateAndGetExpenses(userId, dto.getExpenseIds());
            updateExpensesReimburseStatus(dto.getExpenseIds(), reimbursement.getId(), 1);

            BigDecimal totalAmount = newExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int invoiceCount = (int) newExpenses.stream()
                    .filter(e -> e.getFileUrl() != null && !e.getFileUrl().isEmpty())
                    .count();

            reimbursement.setTotalAmount(totalAmount);
            reimbursement.setInvoiceCount(invoiceCount);
            reimbursement.setItemCount(newExpenses.size());
        }

        reimbursement.setUpdatedAt(LocalDateTime.now());

        List<Expense> updatedExpenses = getRelatedExpenses(reimbursement.getId());
        User user = userMapper.selectById(userId);
        Map<Long, String> categoryMap = buildCategoryMap();

        reimbursement.setPdfUrl(null);
        reimbursement.setMergedPdfUrl(null);
        reimbursement.setZipUrl(null);

        String pdfUrl = pdfCoverService.generateCoverPdf(reimbursement, user, updatedExpenses, categoryMap);
        reimbursement.setPdfUrl(pdfUrl);

        String mergedPdfUrl = pdfMergeService.mergePdfs(reimbursement, updatedExpenses);
        reimbursement.setMergedPdfUrl(mergedPdfUrl);

        String zipUrl = zipPackageService.generateZip(reimbursement, updatedExpenses, mergedPdfUrl);
        reimbursement.setZipUrl(zipUrl);

        reimbursementMapper.updateById(reimbursement);

        return toVO(reimbursement, updatedExpenses);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmReceived(Long userId, Long id) {
        Reimbursement reimbursement = getByIdAndUser(id, userId);
        reimbursement.setReimburseStatus(2);
        reimbursement.setReceivedAt(LocalDateTime.now());
        reimbursement.setUpdatedAt(LocalDateTime.now());
        reimbursementMapper.updateById(reimbursement);

        List<Expense> relatedExpenses = expenseMapper.selectList(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getReimbursementId, reimbursement.getId())
                        .eq(Expense::getStatus, 0)
        );
        for (Expense expense : relatedExpenses) {
            expense.setReimburseStatus(2);
            expense.setUpdatedAt(LocalDateTime.now());
            expense.setDataSign(dataSignService.sign(expenseService.buildSignPayload(expense)));
            expenseMapper.updateById(expense);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ReimbursementVO regenerate(Long userId, Long id) {
        Reimbursement reimbursement = getByIdAndUser(id, userId);
        List<Expense> expenses = getRelatedExpenses(reimbursement.getId());
        User user = userMapper.selectById(userId);
        Map<Long, String> categoryMap = buildCategoryMap();

        String coverUrl = pdfCoverService.generateCoverPdf(reimbursement, user, expenses, categoryMap);
        reimbursement.setPdfUrl(coverUrl);

        String mergedUrl = pdfMergeService.mergePdfs(reimbursement, expenses);
        reimbursement.setMergedPdfUrl(mergedUrl);

        String zipUrl = zipPackageService.generateZip(reimbursement, expenses, mergedUrl);
        reimbursement.setZipUrl(zipUrl);

        reimbursement.setUpdatedAt(LocalDateTime.now());
        reimbursementMapper.updateById(reimbursement);

        return toVO(reimbursement, expenses);
    }

    public String getPdfUrl(Long userId, Long id) {
        Reimbursement reimbursement = getByIdAndUser(id, userId);
        if (reimbursement.getPdfUrl() == null || reimbursement.getPdfUrl().isBlank()) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "报销单PDF尚未生成");
        }
        markExported(reimbursement);
        return reimbursement.getPdfUrl();
    }

    public String getMergedPdfUrl(Long userId, Long id) {
        Reimbursement reimbursement = getByIdAndUser(id, userId);
        if (reimbursement.getMergedPdfUrl() == null || reimbursement.getMergedPdfUrl().isBlank()) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "合并PDF尚未生成");
        }
        markExported(reimbursement);
        return reimbursement.getMergedPdfUrl();
    }

    @Transactional(rollbackFor = Exception.class)
    public String getZipUrl(Long userId, Long id) {
        User user = userMapper.selectById(userId);
        if (user.getMemberStatus() == null || user.getMemberStatus() == 0) {
            throw new BizException(ResultCode.MEMBER_REQUIRED.getCode(), "ZIP打包导出为会员专属功能，请升级会员");
        }

        Reimbursement reimbursement = getByIdAndUser(id, userId);

        if (reimbursement.getZipUrl() == null || reimbursement.getZipUrl().isBlank()) {
            List<Expense> expenses = getRelatedExpenses(reimbursement.getId());
            String mergedPdfUrl = reimbursement.getMergedPdfUrl();
            String zipUrl = zipPackageService.generateZip(reimbursement, expenses, mergedPdfUrl);
            reimbursement.setZipUrl(zipUrl);
            reimbursement.setUpdatedAt(LocalDateTime.now());
            reimbursementMapper.updateById(reimbursement);
        }

        markExported(reimbursement);
        return reimbursement.getZipUrl();
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendEmail(Long userId, Long id, EmailSendDTO dto) {
        User user = userMapper.selectById(userId);

        if (user.getMemberStatus() == null || user.getMemberStatus() == 0) {
            throw new BizException(ResultCode.MEMBER_REQUIRED.getCode(), "邮件发送为会员专属功能，请升级会员");
        }
        if (user.getMemberType() != null && user.getMemberType() == 1) {
            String key = "email:count:" + userId + ":" + YearMonth.now();
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count > 5) {
                redisTemplate.opsForValue().decrement(key);
                throw new BizException(400, "月度会员每月最多发送5次邮件");
            }
            if (count != null && count == 1) {
                redisTemplate.expire(key, 32, TimeUnit.DAYS);
            }
        }

        Reimbursement reimbursement = getByIdAndUser(id, userId);

        if (dto.getAttachType() != null && dto.getAttachType() == 2) {
            if (reimbursement.getZipUrl() == null || reimbursement.getZipUrl().isBlank()) {
                List<Expense> expenses = getRelatedExpenses(reimbursement.getId());
                String mergedPdfUrl = reimbursement.getMergedPdfUrl();
                String zipUrl = zipPackageService.generateZip(reimbursement, expenses, mergedPdfUrl);
                reimbursement.setZipUrl(zipUrl);
            }
        } else {
            if (reimbursement.getMergedPdfUrl() == null || reimbursement.getMergedPdfUrl().isBlank()) {
                throw new BizException(ResultCode.NOT_FOUND.getCode(), "合并PDF尚未生成，请先重新生成报销单");
            }
        }

        emailSendService.sendReimbursementEmail(reimbursement, user, dto.getEmail(),
                dto.getAttachType() != null ? dto.getAttachType() : 1,
                dto.getMessage());

        reimbursement.setEmailSent(1);
        reimbursement.setEmailAddress(dto.getEmail());
        reimbursement.setEmailSentAt(LocalDateTime.now());
        reimbursement.setUpdatedAt(LocalDateTime.now());
        reimbursementMapper.updateById(reimbursement);
    }

    private void checkReimburseQuota(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && (user.getMemberStatus() == null || user.getMemberStatus() == 0)) {
            if (user.getMonthlyReimburseUsed() != null && user.getMonthlyReimburseUsed() >= 2) {
                throw new BizException(ResultCode.QUOTA_EXCEEDED.getCode(),
                        "本月报销单额度已用完(2/2)，升级会员无限使用");
            }
        }
    }

    private void incrementReimburseUsed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setMonthlyReimburseUsed(
                    (user.getMonthlyReimburseUsed() == null ? 0 : user.getMonthlyReimburseUsed()) + 1);
            userMapper.updateById(user);
        }
    }

    private List<Expense> validateAndGetExpenses(Long userId, List<Long> expenseIds) {
        if (expenseIds == null || expenseIds.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "费用ID列表不能为空");
        }

        List<Expense> expenses = expenseMapper.selectList(
                new LambdaQueryWrapper<Expense>()
                        .in(Expense::getId, expenseIds)
                        .eq(Expense::getUserId, userId));

        if (expenses.size() != expenseIds.size()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "部分费用记录不存在或不属于当前用户");
        }

        for (Expense expense : expenses) {
            if (expense.getReimburseStatus() != null && expense.getReimburseStatus() != 0) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(),
                        "费用[" + expense.getId() + "]已关联其他报销单");
            }
        }

        return expenses;
    }

    private String generateReimburseNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RB" + dateStr;

        Long count = reimbursementMapper.selectCount(
                new LambdaQueryWrapper<Reimbursement>()
                        .likeRight(Reimbursement::getReimburseNo, prefix));

        return prefix + String.format("%04d", count + 1);
    }

    private void updateExpensesReimburseStatus(List<Long> expenseIds, Long reimbursementId, int status) {
        if (expenseIds == null || expenseIds.isEmpty()) {
            return;
        }
        List<Expense> expenses = expenseMapper.selectList(
                new LambdaQueryWrapper<Expense>().in(Expense::getId, expenseIds)
        );
        for (Expense expense : expenses) {
            expense.setReimburseStatus(status);
            expense.setReimbursementId(reimbursementId);
            expense.setUpdatedAt(LocalDateTime.now());
            expense.setDataSign(dataSignService.sign(expenseService.buildSignPayload(expense)));
            expenseMapper.updateById(expense);
        }
    }

    private List<Expense> getRelatedExpenses(Long reimbursementId) {
        return expenseMapper.selectList(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getReimbursementId, reimbursementId)
                        .orderByAsc(Expense::getCreatedAt));
    }

    private Reimbursement getByIdAndUser(Long id, Long userId) {
        Reimbursement reimbursement = reimbursementMapper.selectOne(
                new LambdaQueryWrapper<Reimbursement>()
                        .eq(Reimbursement::getId, id)
                        .eq(Reimbursement::getUserId, userId));
        if (reimbursement == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "报销单不存在");
        }
        return reimbursement;
    }

    private void markExported(Reimbursement reimbursement) {
        if (reimbursement.getReimburseStatus() != null && reimbursement.getReimburseStatus() == 0) {
            reimbursement.setReimburseStatus(1);
        }
        if (reimbursement.getExportedAt() == null) {
            reimbursement.setExportedAt(LocalDateTime.now());
        }
        reimbursement.setExportCount(
                (reimbursement.getExportCount() == null ? 0 : reimbursement.getExportCount()) + 1);
        reimbursement.setUpdatedAt(LocalDateTime.now());
        reimbursementMapper.updateById(reimbursement);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelReimbursement(Long userId, Long id) {
        Reimbursement reimbursement = getByIdAndUser(id, userId);
        if (reimbursement.getReimburseStatus() != null && reimbursement.getReimburseStatus() == 2) {
            throw new BizException(400, "已收款的报销单不能取消");
        }

        List<Expense> expenses = expenseMapper.selectList(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getReimbursementId, id)
                        .eq(Expense::getStatus, 0)
        );
        for (Expense expense : expenses) {
            expense.setReimburseStatus(0);
            expense.setReimbursementId(null);
            expense.setUpdatedAt(LocalDateTime.now());
            expense.setDataSign(dataSignService.sign(expenseService.buildSignPayload(expense)));
            expenseMapper.updateById(expense);
        }

        reimbursement.setStatus(1);
        reimbursement.setUpdatedAt(LocalDateTime.now());
        reimbursementMapper.updateById(reimbursement);
    }

    private Map<Long, String> buildCategoryMap() {
        return expenseCategoryMapper.selectList(null)
                .stream()
                .collect(Collectors.toMap(ExpenseCategory::getId, ExpenseCategory::getName,
                        (a, b) -> a));
    }

    private ReimbursementVO toVO(Reimbursement r, List<Expense> expenses) {
        ReimbursementVO vo = new ReimbursementVO();
        vo.setId(r.getId());
        vo.setUserId(r.getUserId());
        vo.setReimburseNo(r.getReimburseNo());
        vo.setTitle(r.getTitle());
        vo.setTotalAmount(r.getTotalAmount());
        vo.setInvoiceCount(r.getInvoiceCount());
        vo.setItemCount(r.getItemCount());
        vo.setRemark(r.getRemark());
        vo.setPdfUrl(r.getPdfUrl());
        vo.setMergedPdfUrl(r.getMergedPdfUrl());
        vo.setZipUrl(r.getZipUrl());
        vo.setTripId(r.getTripId());
        vo.setReimburseStatus(r.getReimburseStatus());
        vo.setExportCount(r.getExportCount());
        vo.setExportedAt(r.getExportedAt());
        vo.setEmailSent(r.getEmailSent());
        vo.setEmailAddress(r.getEmailAddress());
        vo.setEmailSentAt(r.getEmailSentAt());
        vo.setReceivedAt(r.getReceivedAt());
        vo.setStatus(r.getStatus());
        vo.setCreatedAt(r.getCreatedAt());
        vo.setUpdatedAt(r.getUpdatedAt());

        if (expenses != null) {
            vo.setExpenses(expenses.stream().map(this::toExpenseVO).toList());
        } else {
            vo.setExpenses(Collections.emptyList());
        }

        return vo;
    }

    private ExpenseVO toExpenseVO(Expense e) {
        ExpenseVO vo = new ExpenseVO();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setCategoryId(e.getCategoryId());
        vo.setTripId(e.getTripId());
        vo.setType(e.getType());
        vo.setAmount(e.getAmount());
        vo.setTaxAmount(e.getTaxAmount());
        vo.setInvoiceNo(e.getInvoiceNo());
        vo.setInvoiceCode(e.getInvoiceCode());
        vo.setInvoiceDate(e.getInvoiceDate());
        vo.setInvoiceType(e.getInvoiceType());
        vo.setSellerName(e.getSellerName());
        vo.setBuyerName(e.getBuyerName());
        vo.setFileUrl(e.getFileUrl());
        vo.setFileName(e.getFileName());
        vo.setDescription(e.getDescription());
        vo.setExpenseDate(e.getExpenseDate());
        vo.setReimburseStatus(e.getReimburseStatus());
        vo.setReimbursementId(e.getReimbursementId());
        vo.setDataSign(e.getDataSign());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }
}
