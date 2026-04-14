package com.qiankubx.module.expense.service;

import com.aliyun.oss.OSS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiankubx.common.config.OssConfig;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.expense.dto.*;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.expense.entity.ExpenseCategory;
import com.qiankubx.module.expense.mapper.ExpenseCategoryMapper;
import com.qiankubx.module.expense.mapper.ExpenseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseMapper expenseMapper;
    private final ExpenseCategoryMapper categoryMapper;
    private final OSS ossClient;
    private final OssConfig ossConfig;
    private final DataSignService dataSignService;
    private final InvoiceParseEngine invoiceParseEngine;

    public InvoiceUploadVO uploadInvoice(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = ossConfig.getDirs().getInvoice() + UUID.randomUUID() + suffix;

        byte[] pdfBytes;
        try {
            pdfBytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "文件读取失败");
        }

        ossClient.putObject(ossConfig.getBucketName(), objectKey, new ByteArrayInputStream(pdfBytes));

        String fileUrl;
        if (ossConfig.getCdnDomain() != null && !ossConfig.getCdnDomain().isBlank()) {
            fileUrl = ossConfig.getCdnDomain() + "/" + objectKey;
        } else {
            fileUrl = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectKey;
        }

        InvoiceUploadVO vo = invoiceParseEngine.parseFromPdf(pdfBytes);
        vo.setFileUrl(fileUrl);
        vo.setFileName(originalFilename);

        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Expense createExpense(Long userId, ExpenseCreateDTO dto) {
        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setCategoryId(dto.getCategoryId());
        expense.setTripId(dto.getTripId());
        expense.setType(dto.getType());
        expense.setAmount(dto.getAmount());
        expense.setTaxAmount(dto.getTaxAmount());
        expense.setInvoiceNo(dto.getInvoiceNo());
        expense.setInvoiceCode(dto.getInvoiceCode());
        expense.setInvoiceDate(dto.getInvoiceDate());
        expense.setInvoiceType(dto.getInvoiceType());
        expense.setSellerName(dto.getSellerName());
        expense.setBuyerName(dto.getBuyerName());
        expense.setFileUrl(dto.getFileUrl());
        expense.setFileName(dto.getFileName());
        expense.setDescription(dto.getDescription());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setReimburseStatus(0);
        expense.setStatus(0);
        expense.setCreatedAt(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());

        String signPayload = buildSignPayload(expense);
        expense.setDataSign(dataSignService.sign(signPayload));

        expenseMapper.insert(expense);
        return expense;
    }

    @Transactional(rollbackFor = Exception.class)
    public Expense updateExpense(Long userId, ExpenseUpdateDTO dto) {
        Expense expense = getByIdAndUserId(dto.getId(), userId);

        expense.setCategoryId(dto.getCategoryId());
        expense.setTripId(dto.getTripId());
        expense.setType(dto.getType());
        expense.setAmount(dto.getAmount());
        expense.setTaxAmount(dto.getTaxAmount());
        expense.setInvoiceNo(dto.getInvoiceNo());
        expense.setInvoiceCode(dto.getInvoiceCode());
        expense.setInvoiceDate(dto.getInvoiceDate());
        expense.setInvoiceType(dto.getInvoiceType());
        expense.setSellerName(dto.getSellerName());
        expense.setBuyerName(dto.getBuyerName());
        expense.setFileUrl(dto.getFileUrl());
        expense.setFileName(dto.getFileName());
        expense.setDescription(dto.getDescription());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setUpdatedAt(LocalDateTime.now());

        String signPayload = buildSignPayload(expense);
        expense.setDataSign(dataSignService.sign(signPayload));

        expenseMapper.updateById(expense);
        return expense;
    }

    public void deleteExpense(Long userId, Long expenseId) {
        Expense expense = getByIdAndUserId(expenseId, userId);
        expense.setStatus(1);
        expense.setUpdatedAt(LocalDateTime.now());
        expenseMapper.updateById(expense);
    }

    public IPage<ExpenseVO> listExpenses(Long userId, ExpenseQueryDTO queryDTO) {
        Page<ExpenseVO> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        return expenseMapper.selectByUserIdWithPage(page, userId, queryDTO);
    }

    public ExpenseVO getExpense(Long userId, Long expenseId) {
        Expense expense = getByIdAndUserId(expenseId, userId);

        ExpenseVO vo = new ExpenseVO();
        vo.setId(expense.getId());
        vo.setUserId(expense.getUserId());
        vo.setCategoryId(expense.getCategoryId());
        vo.setTripId(expense.getTripId());
        vo.setType(expense.getType());
        vo.setAmount(expense.getAmount());
        vo.setTaxAmount(expense.getTaxAmount());
        vo.setInvoiceNo(expense.getInvoiceNo());
        vo.setInvoiceCode(expense.getInvoiceCode());
        vo.setInvoiceDate(expense.getInvoiceDate());
        vo.setInvoiceType(expense.getInvoiceType());
        vo.setSellerName(expense.getSellerName());
        vo.setBuyerName(expense.getBuyerName());
        vo.setFileUrl(expense.getFileUrl());
        vo.setFileName(expense.getFileName());
        vo.setDescription(expense.getDescription());
        vo.setExpenseDate(expense.getExpenseDate());
        vo.setReimburseStatus(expense.getReimburseStatus());
        vo.setReimbursementId(expense.getReimbursementId());
        vo.setDataSign(expense.getDataSign());
        vo.setStatus(expense.getStatus());
        vo.setCreatedAt(expense.getCreatedAt());
        vo.setUpdatedAt(expense.getUpdatedAt());

        if (expense.getCategoryId() != null) {
            ExpenseCategory category = categoryMapper.selectById(expense.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        return vo;
    }

    public List<ExpenseVO> listPending(Long userId) {
        return expenseMapper.selectPendingByUserId(userId);
    }

    private Expense getByIdAndUserId(Long expenseId, Long userId) {
        Expense expense = expenseMapper.selectOne(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getId, expenseId)
                        .eq(Expense::getUserId, userId)
                        .eq(Expense::getStatus, 0)
        );
        if (expense == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "费用记录不存在");
        }
        return expense;
    }

    private String buildSignPayload(Expense expense) {
        return expense.getId() + "|"
                + expense.getUserId() + "|"
                + expense.getAmount() + "|"
                + expense.getType() + "|"
                + expense.getExpenseDate() + "|"
                + expense.getInvoiceNo();
    }
}
