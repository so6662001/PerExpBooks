package com.qiankubx.module.expense.service;

import com.aliyun.oss.OSS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.config.OssConfig;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.expense.dto.ExpenseCreateDTO;
import com.qiankubx.module.expense.dto.ExpenseUpdateDTO;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.expense.mapper.ExpenseCategoryMapper;
import com.qiankubx.module.expense.mapper.ExpenseMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseMapper expenseMapper;
    @Mock private ExpenseCategoryMapper categoryMapper;
    @Mock private OSS ossClient;
    @Mock private OssConfig ossConfig;
    @Mock private DataSignService dataSignService;
    @Mock private InvoiceParseEngine invoiceParseEngine;
    @Mock private OfdParseService ofdParseService;
    @Mock private OcrApiService ocrApiService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private ExpenseService expenseService;

    private User freeUser;

    @BeforeEach
    void setUp() {
        freeUser = new User();
        freeUser.setId(1L);
        freeUser.setMemberStatus(0);
        freeUser.setMonthlyInvoiceUsed(0);
    }

    @Test
    void uploadInvoice_unsupportedFormat_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.doc", "application/msword", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> expenseService.uploadInvoice(1L, file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持的文件格式");
    }

    @Test
    void uploadInvoice_oversized_shouldThrow() {
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", largeContent);

        assertThatThrownBy(() -> expenseService.uploadInvoice(1L, file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("文件大小不能超过10MB");
    }

    @Test
    void uploadInvoice_freeUserExceededQuota_shouldThrow() {
        freeUser.setMonthlyInvoiceUsed(5);
        when(userMapper.selectById(1L)).thenReturn(freeUser);

        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> expenseService.uploadInvoice(1L, file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("发票上传额度已用完");
    }

    @Test
    void createExpense_validData_shouldSucceed() {
        ExpenseCreateDTO dto = new ExpenseCreateDTO();
        dto.setCategoryId(1L);
        dto.setType(1);
        dto.setAmount(new BigDecimal("100.50"));
        dto.setExpenseDate(LocalDate.now());
        dto.setDescription("测试费用");
        dto.setInvoiceNo("12345678");

        when(dataSignService.sign(anyString())).thenReturn("valid-sign");
        when(expenseMapper.insert(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(101L);
            return 1;
        });

        Expense result = expenseService.createExpense(1L, dto);

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(result.getReimburseStatus()).isEqualTo(0);
        assertThat(result.getStatus()).isEqualTo(0);
        assertThat(result.getDataSign()).isEqualTo("valid-sign");
        verify(expenseMapper).insert(any(Expense.class));
    }

    @Test
    void updateExpense_alreadyReimbursed_shouldThrow() {
        Expense expense = buildExpense(1L, 1L);
        expense.setReimburseStatus(1);
        when(expenseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(expense);

        ExpenseUpdateDTO dto = new ExpenseUpdateDTO();
        dto.setId(1L);
        dto.setAmount(new BigDecimal("200.00"));

        assertThatThrownBy(() -> expenseService.updateExpense(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已提交报销的费用不允许编辑");
    }

    @Test
    void deleteExpense_alreadyReimbursed_shouldThrow() {
        Expense expense = buildExpense(1L, 1L);
        expense.setReimburseStatus(1);
        when(expenseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(expense);

        assertThatThrownBy(() -> expenseService.deleteExpense(1L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已提交报销的费用不允许删除");
    }

    @Test
    void deleteExpense_valid_shouldSoftDelete() {
        Expense expense = buildExpense(1L, 1L);
        expense.setReimburseStatus(0);
        when(expenseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(expense);

        expenseService.deleteExpense(1L, 1L);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void updateExpense_validData_shouldSucceed() {
        Expense expense = buildExpense(1L, 1L);
        expense.setReimburseStatus(0);
        when(expenseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(expense);
        when(dataSignService.sign(anyString())).thenReturn("new-sign");

        ExpenseUpdateDTO dto = new ExpenseUpdateDTO();
        dto.setId(1L);
        dto.setCategoryId(2L);
        dto.setType(1);
        dto.setAmount(new BigDecimal("300.00"));
        dto.setExpenseDate(LocalDate.now());

        Expense result = expenseService.updateExpense(1L, dto);

        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(result.getDataSign()).isEqualTo("new-sign");
        verify(expenseMapper).updateById(any(Expense.class));
    }

    @Test
    void getExpense_notFound_shouldThrow() {
        when(expenseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> expenseService.getExpense(1L, 999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("费用记录不存在");
    }

    private Expense buildExpense(Long id, Long userId) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setUserId(userId);
        expense.setCategoryId(1L);
        expense.setType(1);
        expense.setAmount(new BigDecimal("100.00"));
        expense.setReimburseStatus(0);
        expense.setStatus(0);
        expense.setCreatedAt(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());
        return expense;
    }
}
