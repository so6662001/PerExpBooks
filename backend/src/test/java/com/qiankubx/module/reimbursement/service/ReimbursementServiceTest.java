package com.qiankubx.module.reimbursement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.expense.mapper.ExpenseCategoryMapper;
import com.qiankubx.module.expense.mapper.ExpenseMapper;
import com.qiankubx.module.expense.service.ExpenseService;
import com.qiankubx.module.reimbursement.dto.ReimbursementCreateDTO;
import com.qiankubx.module.reimbursement.dto.ReimbursementVO;
import com.qiankubx.module.reimbursement.entity.Reimbursement;
import com.qiankubx.module.reimbursement.mapper.ReimbursementMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReimbursementServiceTest {

    @Mock private ReimbursementMapper reimbursementMapper;
    @Mock private ExpenseMapper expenseMapper;
    @Mock private ExpenseCategoryMapper expenseCategoryMapper;
    @Mock private UserMapper userMapper;
    @Mock private PdfCoverService pdfCoverService;
    @Mock private PdfMergeService pdfMergeService;
    @Mock private ZipPackageService zipPackageService;
    @Mock private EmailSendService emailSendService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private DataSignService dataSignService;
    @Mock private ExpenseService expenseService;

    @InjectMocks
    private ReimbursementService reimbursementService;

    private User freeUser;

    @BeforeEach
    void setUp() {
        freeUser = new User();
        freeUser.setId(1L);
        freeUser.setMemberStatus(0);
        freeUser.setMonthlyReimburseUsed(0);
        freeUser.setNickname("免费用户");
    }

    @Test
    void generate_withValidExpenses_shouldSucceed() {
        when(userMapper.selectById(1L)).thenReturn(freeUser);

        Expense e1 = buildExpense(10L, 1L, new BigDecimal("100"), 0);
        Expense e2 = buildExpense(11L, 1L, new BigDecimal("200"), 0);
        e1.setFileUrl("http://example.com/invoice.pdf");
        when(expenseMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(e1, e2))
                .thenReturn(List.of(e1, e2));

        when(reimbursementMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(reimbursementMapper.insert(any(Reimbursement.class))).thenAnswer(inv -> {
            Reimbursement r = inv.getArgument(0);
            r.setId(100L);
            return 1;
        });
        when(expenseCategoryMapper.selectList(isNull())).thenReturn(Collections.emptyList());
        when(pdfCoverService.generateCoverPdf(any(), any(), any(), any())).thenReturn("http://pdf.url");
        when(pdfMergeService.mergePdfs(any(), any())).thenReturn("http://merged.url");
        when(zipPackageService.generateZip(any(), any(), any())).thenReturn("http://zip.url");
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(expenseService.buildSignPayload(any())).thenReturn("payload");

        ReimbursementCreateDTO dto = new ReimbursementCreateDTO();
        dto.setTitle("测试报销单");
        dto.setExpenseIds(List.of(10L, 11L));

        ReimbursementVO result = reimbursementService.generate(1L, dto);

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(result.getItemCount()).isEqualTo(2);
        assertThat(result.getInvoiceCount()).isEqualTo(1);
        verify(reimbursementMapper).insert(any(Reimbursement.class));
    }

    @Test
    void generate_withAlreadyReimbursedExpense_shouldThrow() {
        when(userMapper.selectById(1L)).thenReturn(freeUser);

        Expense reimbursedExpense = buildExpense(10L, 1L, new BigDecimal("100"), 1);
        when(expenseMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(reimbursedExpense));

        ReimbursementCreateDTO dto = new ReimbursementCreateDTO();
        dto.setTitle("测试");
        dto.setExpenseIds(List.of(10L));

        assertThatThrownBy(() -> reimbursementService.generate(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已关联其他报销单");
    }

    @Test
    void generate_expenseBelongsToOtherUser_shouldThrow() {
        when(userMapper.selectById(1L)).thenReturn(freeUser);

        when(expenseMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        ReimbursementCreateDTO dto = new ReimbursementCreateDTO();
        dto.setTitle("测试");
        dto.setExpenseIds(List.of(10L, 11L));

        assertThatThrownBy(() -> reimbursementService.generate(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("部分费用记录不存在或不属于当前用户");
    }

    @Test
    void generate_freeUserExceededQuota_shouldThrow() {
        freeUser.setMonthlyReimburseUsed(2);
        when(userMapper.selectById(1L)).thenReturn(freeUser);

        ReimbursementCreateDTO dto = new ReimbursementCreateDTO();
        dto.setTitle("测试");
        dto.setExpenseIds(List.of(10L));

        assertThatThrownBy(() -> reimbursementService.generate(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("报销单额度已用完");
    }

    @Test
    void confirmReceived_shouldUpdateExpensesToReceived() {
        Reimbursement reimbursement = buildReimbursement(100L, 1L);
        reimbursement.setReimburseStatus(1);
        when(reimbursementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reimbursement);

        Expense e1 = buildExpense(10L, 1L, new BigDecimal("100"), 1);
        when(expenseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(e1));
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(expenseService.buildSignPayload(any())).thenReturn("payload");

        reimbursementService.confirmReceived(1L, 100L);

        ArgumentCaptor<Reimbursement> rCaptor = ArgumentCaptor.forClass(Reimbursement.class);
        verify(reimbursementMapper).updateById(rCaptor.capture());
        assertThat(rCaptor.getValue().getReimburseStatus()).isEqualTo(2);

        ArgumentCaptor<Expense> eCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseMapper).updateById(eCaptor.capture());
        assertThat(eCaptor.getValue().getReimburseStatus()).isEqualTo(2);
    }

    @Test
    void cancelReimbursement_shouldRestoreExpenseStatus() {
        Reimbursement reimbursement = buildReimbursement(100L, 1L);
        reimbursement.setReimburseStatus(1);
        when(reimbursementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reimbursement);

        Expense e1 = buildExpense(10L, 1L, new BigDecimal("100"), 1);
        when(expenseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(e1));
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(expenseService.buildSignPayload(any())).thenReturn("payload");

        reimbursementService.cancelReimbursement(1L, 100L);

        ArgumentCaptor<Reimbursement> rCaptor = ArgumentCaptor.forClass(Reimbursement.class);
        verify(reimbursementMapper).updateById(rCaptor.capture());
        assertThat(rCaptor.getValue().getStatus()).isEqualTo(1);

        ArgumentCaptor<Expense> eCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseMapper).updateById(eCaptor.capture());
        assertThat(eCaptor.getValue().getReimburseStatus()).isEqualTo(0);
        assertThat(eCaptor.getValue().getReimbursementId()).isNull();
    }

    @Test
    void cancelReimbursement_alreadyReceived_shouldThrow() {
        Reimbursement reimbursement = buildReimbursement(100L, 1L);
        reimbursement.setReimburseStatus(2);
        when(reimbursementMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reimbursement);

        assertThatThrownBy(() -> reimbursementService.cancelReimbursement(1L, 100L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已收款的报销单不能取消");
    }

    @Test
    void getZipUrl_freeUser_shouldThrow() {
        when(userMapper.selectById(1L)).thenReturn(freeUser);

        assertThatThrownBy(() -> reimbursementService.getZipUrl(1L, 100L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ZIP打包导出为会员专属功能");
    }

    @Test
    void generateReimburseNo_shouldBeUnique() {
        when(userMapper.selectById(1L)).thenReturn(freeUser);

        Expense e1 = buildExpense(10L, 1L, new BigDecimal("100"), 0);
        when(expenseMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(e1))
                .thenReturn(List.of(e1));

        when(reimbursementMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(reimbursementMapper.insert(any(Reimbursement.class))).thenAnswer(inv -> {
            Reimbursement r = inv.getArgument(0);
            r.setId(101L);
            return 1;
        });
        when(expenseCategoryMapper.selectList(isNull())).thenReturn(Collections.emptyList());
        when(pdfCoverService.generateCoverPdf(any(), any(), any(), any())).thenReturn("http://pdf.url");
        when(pdfMergeService.mergePdfs(any(), any())).thenReturn("http://merged.url");
        when(zipPackageService.generateZip(any(), any(), any())).thenReturn("http://zip.url");
        when(dataSignService.sign(anyString())).thenReturn("sign");
        when(expenseService.buildSignPayload(any())).thenReturn("payload");

        ReimbursementCreateDTO dto = new ReimbursementCreateDTO();
        dto.setTitle("报销单号测试");
        dto.setExpenseIds(List.of(10L));

        reimbursementService.generate(1L, dto);

        ArgumentCaptor<Reimbursement> captor = ArgumentCaptor.forClass(Reimbursement.class);
        verify(reimbursementMapper).insert(captor.capture());
        assertThat(captor.getValue().getReimburseNo()).startsWith("RB");
    }

    private Expense buildExpense(Long id, Long userId, BigDecimal amount, int reimburseStatus) {
        Expense e = new Expense();
        e.setId(id);
        e.setUserId(userId);
        e.setAmount(amount);
        e.setReimburseStatus(reimburseStatus);
        e.setStatus(0);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    private Reimbursement buildReimbursement(Long id, Long userId) {
        Reimbursement r = new Reimbursement();
        r.setId(id);
        r.setUserId(userId);
        r.setReimburseNo("RB202604150001");
        r.setTitle("测试报销单");
        r.setTotalAmount(new BigDecimal("300"));
        r.setReimburseStatus(0);
        r.setStatus(0);
        r.setExportCount(0);
        r.setEmailSent(0);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }
}
