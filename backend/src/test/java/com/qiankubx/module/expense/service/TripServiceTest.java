package com.qiankubx.module.expense.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.expense.dto.TripCreateDTO;
import com.qiankubx.module.expense.dto.TripUpdateDTO;
import com.qiankubx.module.expense.entity.BusinessTrip;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.expense.mapper.BusinessTripMapper;
import com.qiankubx.module.expense.mapper.ExpenseMapper;
import com.qiankubx.TestLambdaCacheInitializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock private BusinessTripMapper tripMapper;
    @Mock private ExpenseMapper expenseMapper;
    @Mock private DataSignService dataSignService;
    @Mock private ExpenseService expenseService;

    @InjectMocks
    private TripService tripService;

    @BeforeAll
    static void initCache() {
        TestLambdaCacheInitializer.initAll();
    }

    @Test
    void createTrip_shouldAutoCalculateDaysAndSubsidy() {
        TripCreateDTO dto = new TripCreateDTO();
        dto.setTitle("北京出差");
        dto.setDestination("北京");
        dto.setStartDate(LocalDate.of(2026, 4, 10));
        dto.setEndDate(LocalDate.of(2026, 4, 12));
        dto.setSubsidyPerDay(new BigDecimal("100"));

        when(tripMapper.insert(any(BusinessTrip.class))).thenAnswer(inv -> {
            BusinessTrip t = inv.getArgument(0);
            t.setId(1L);
            return 1;
        });
        when(expenseMapper.insert(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(10L);
            return 1;
        });
        when(expenseService.buildSignPayload(any(Expense.class))).thenReturn("payload");
        when(dataSignService.sign("payload")).thenReturn("sign");

        BusinessTrip result = tripService.createTrip(1L, dto);

        assertThat(result.getDays()).isEqualTo(3);
        assertThat(result.getSubsidyTotal()).isEqualByComparingTo(new BigDecimal("300"));
        verify(tripMapper).insert(any(BusinessTrip.class));
    }

    @Test
    void createTrip_endDateBeforeStartDate_shouldThrow() {
        TripCreateDTO dto = new TripCreateDTO();
        dto.setTitle("出差");
        dto.setDestination("上海");
        dto.setStartDate(LocalDate.of(2026, 4, 15));
        dto.setEndDate(LocalDate.of(2026, 4, 10));
        dto.setSubsidyPerDay(new BigDecimal("100"));

        assertThatThrownBy(() -> tripService.createTrip(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("出差结束日期不能早于开始日期");
    }

    @Test
    void createTrip_shouldAutoCreateSubsidyExpense() {
        TripCreateDTO dto = new TripCreateDTO();
        dto.setTitle("广州出差");
        dto.setDestination("广州");
        dto.setStartDate(LocalDate.of(2026, 5, 1));
        dto.setEndDate(LocalDate.of(2026, 5, 3));
        dto.setSubsidyPerDay(new BigDecimal("150"));

        when(tripMapper.insert(any(BusinessTrip.class))).thenAnswer(inv -> {
            BusinessTrip t = inv.getArgument(0);
            t.setId(2L);
            return 1;
        });
        when(expenseMapper.insert(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(20L);
            return 1;
        });
        when(expenseService.buildSignPayload(any(Expense.class))).thenReturn("payload");
        when(dataSignService.sign("payload")).thenReturn("sign");

        tripService.createTrip(1L, dto);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseMapper).insert(captor.capture());
        Expense subsidy = captor.getValue();
        assertThat(subsidy.getType()).isEqualTo(2);
        assertThat(subsidy.getAmount()).isEqualByComparingTo(new BigDecimal("450"));
        assertThat(subsidy.getTripId()).isEqualTo(2L);
        assertThat(subsidy.getReimburseStatus()).isEqualTo(0);
    }

    @Test
    void updateTrip_shouldSyncSubsidyExpense() {
        BusinessTrip trip = buildTrip(1L, 1L);
        when(tripMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(trip);

        Expense oldSubsidy = new Expense();
        oldSubsidy.setId(10L);
        oldSubsidy.setTripId(1L);
        oldSubsidy.setType(2);
        oldSubsidy.setReimburseStatus(0);
        oldSubsidy.setStatus(0);
        when(expenseMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(oldSubsidy);
        when(expenseService.buildSignPayload(any(Expense.class))).thenReturn("payload");
        when(dataSignService.sign("payload")).thenReturn("sign");

        TripUpdateDTO dto = new TripUpdateDTO();
        dto.setId(1L);
        dto.setTitle("北京出差-更新");
        dto.setDestination("北京");
        dto.setStartDate(LocalDate.of(2026, 4, 10));
        dto.setEndDate(LocalDate.of(2026, 4, 14));
        dto.setSubsidyPerDay(new BigDecimal("200"));

        BusinessTrip result = tripService.updateTrip(1L, dto);

        assertThat(result.getDays()).isEqualTo(5);
        assertThat(result.getSubsidyTotal()).isEqualByComparingTo(new BigDecimal("1000"));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseMapper).updateById(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void deleteTrip_shouldSoftDeleteSubsidyExpense() {
        BusinessTrip trip = buildTrip(1L, 1L);
        when(tripMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(trip);

        tripService.deleteTrip(1L, 1L);

        ArgumentCaptor<BusinessTrip> captor = ArgumentCaptor.forClass(BusinessTrip.class);
        verify(tripMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);

        verify(expenseMapper).update(isNull(), any());
    }

    private BusinessTrip buildTrip(Long id, Long userId) {
        BusinessTrip trip = new BusinessTrip();
        trip.setId(id);
        trip.setUserId(userId);
        trip.setTitle("测试出差");
        trip.setDestination("测试目的地");
        trip.setStartDate(LocalDate.of(2026, 4, 10));
        trip.setEndDate(LocalDate.of(2026, 4, 12));
        trip.setDays(3);
        trip.setSubsidyPerDay(new BigDecimal("100"));
        trip.setSubsidyTotal(new BigDecimal("300"));
        trip.setStatus(0);
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        return trip;
    }
}
