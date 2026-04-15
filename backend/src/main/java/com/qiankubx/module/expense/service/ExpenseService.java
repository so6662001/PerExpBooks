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
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
    private final OfdParseService ofdParseService;
    private final OcrApiService ocrApiService;
    private final UserMapper userMapper;

    public InvoiceUploadVO uploadInvoice(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!Set.of("pdf", "ofd", "jpg", "jpeg", "png").contains(ext)) {
            throw new BizException(400, "不支持的文件格式，请上传PDF/OFD/JPG/PNG文件");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BizException(400, "文件大小不能超过10MB");
        }

        checkInvoiceQuota(userId);

        String suffix = "." + ext;
        String objectKey = ossConfig.getDirs().getInvoice() + UUID.randomUUID() + suffix;

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "文件读取失败");
        }

        ossClient.putObject(ossConfig.getBucketName(), objectKey, new ByteArrayInputStream(fileBytes));

        String fileUrl;
        if (ossConfig.getCdnDomain() != null && !ossConfig.getCdnDomain().isBlank()) {
            fileUrl = ossConfig.getCdnDomain() + "/" + objectKey;
        } else {
            fileUrl = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectKey;
        }

        incrementInvoiceUsed(userId);

        InvoiceUploadVO result;
        if ("pdf".equals(ext)) {
            result = invoiceParseEngine.parseFromPdf(fileBytes);
            result.setParseSuccess(result.getParsedSuccess() != null && result.getParsedSuccess());
            result.setParseMessage(result.getParseSuccess() ? "PDF发票解析成功" : "PDF发票解析未能提取完整信息，请手动补充");
        } else if ("ofd".equals(ext)) {
            result = ofdParseService.parseFromOfd(fileBytes);
        } else if (Set.of("jpg", "jpeg", "png").contains(ext)) {
            result = ocrApiService.recognizeInvoice(fileBytes);
        } else {
            result = new InvoiceUploadVO();
            result.setParsedSuccess(false);
            result.setParseSuccess(false);
            result.setParseMessage("不支持的文件格式");
        }
        result.setFileUrl(fileUrl);
        result.setFileName(originalFilename);

        return result;
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
        if (expense.getReimburseStatus() != null && expense.getReimburseStatus() > 0) {
            throw new BizException(400, "已提交报销的费用不允许编辑");
        }

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
        if (expense.getReimburseStatus() != null && expense.getReimburseStatus() > 0) {
            throw new BizException(400, "已提交报销的费用不允许删除");
        }
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

    private void checkInvoiceQuota(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && (user.getMemberStatus() == null || user.getMemberStatus() == 0)) {
            if (user.getMonthlyInvoiceUsed() != null && user.getMonthlyInvoiceUsed() >= 5) {
                throw new BizException(ResultCode.QUOTA_EXCEEDED.getCode(),
                        "本月发票上传额度已用完(5/5)，升级会员无限使用");
            }
        }
    }

    private void incrementInvoiceUsed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setMonthlyInvoiceUsed(
                    (user.getMonthlyInvoiceUsed() == null ? 0 : user.getMonthlyInvoiceUsed()) + 1);
            userMapper.updateById(user);
        }
    }

    public String buildSignPayload(Expense expense) {
        return expense.getId() + "|"
                + expense.getUserId() + "|"
                + expense.getAmount() + "|"
                + expense.getReimburseStatus() + "|"
                + expense.getCreatedAt();
    }
}
