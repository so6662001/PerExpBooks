package com.qiankubx.module.expense.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.common.security.DataSignService;
import com.qiankubx.module.expense.dto.ExpenseVO;
import com.qiankubx.module.expense.dto.TripCreateDTO;
import com.qiankubx.module.expense.dto.TripUpdateDTO;
import com.qiankubx.module.expense.dto.TripVO;
import com.qiankubx.module.expense.entity.BusinessTrip;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.expense.mapper.BusinessTripMapper;
import com.qiankubx.module.expense.mapper.ExpenseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final BusinessTripMapper tripMapper;
    private final ExpenseMapper expenseMapper;
    private final @Lazy DataSignService dataSignService;
    private final @Lazy ExpenseService expenseService;

    @Transactional(rollbackFor = Exception.class)
    public BusinessTrip createTrip(Long userId, TripCreateDTO dto) {
        BusinessTrip trip = new BusinessTrip();
        trip.setUserId(userId);
        trip.setTitle(dto.getTitle());
        trip.setDestination(dto.getDestination());
        trip.setStartDate(dto.getStartDate());
        trip.setEndDate(dto.getEndDate());
        trip.setSubsidyPerDay(dto.getSubsidyPerDay());
        trip.setRemark(dto.getRemark());
        trip.setStatus(0);
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());

        int days = (int) ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        trip.setDays(days);
        trip.setSubsidyTotal(dto.getSubsidyPerDay().multiply(BigDecimal.valueOf(days)));

        tripMapper.insert(trip);

        Expense subsidy = new Expense();
        subsidy.setUserId(userId);
        subsidy.setTripId(trip.getId());
        subsidy.setType(2);
        subsidy.setAmount(trip.getSubsidyTotal());
        subsidy.setDescription("出差补贴 " + trip.getDestination() + " " + trip.getDays() + "天×" + trip.getSubsidyPerDay() + "元");
        subsidy.setExpenseDate(trip.getStartDate());
        subsidy.setReimburseStatus(0);
        subsidy.setStatus(0);
        subsidy.setCreatedAt(LocalDateTime.now());
        subsidy.setUpdatedAt(LocalDateTime.now());
        expenseMapper.insert(subsidy);
        String signPayload = expenseService.buildSignPayload(subsidy);
        subsidy.setDataSign(dataSignService.sign(signPayload));
        expenseMapper.updateById(subsidy);

        return trip;
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessTrip updateTrip(Long userId, TripUpdateDTO dto) {
        BusinessTrip trip = getByIdAndUserId(dto.getId(), userId);

        trip.setTitle(dto.getTitle());
        trip.setDestination(dto.getDestination());
        trip.setStartDate(dto.getStartDate());
        trip.setEndDate(dto.getEndDate());
        trip.setSubsidyPerDay(dto.getSubsidyPerDay());
        trip.setRemark(dto.getRemark());
        trip.setUpdatedAt(LocalDateTime.now());

        int days = (int) ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        trip.setDays(days);
        trip.setSubsidyTotal(dto.getSubsidyPerDay().multiply(BigDecimal.valueOf(days)));

        tripMapper.updateById(trip);

        Expense oldSubsidy = expenseMapper.selectOne(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getTripId, trip.getId())
                        .eq(Expense::getType, 2)
                        .eq(Expense::getStatus, 0)
        );
        if (oldSubsidy != null && oldSubsidy.getReimburseStatus() != null && oldSubsidy.getReimburseStatus() == 0) {
            oldSubsidy.setAmount(trip.getSubsidyTotal());
            oldSubsidy.setDescription("出差补贴 " + trip.getDestination() + " " + trip.getDays() + "天×" + trip.getSubsidyPerDay() + "元");
            oldSubsidy.setExpenseDate(trip.getStartDate());
            oldSubsidy.setUpdatedAt(LocalDateTime.now());
            oldSubsidy.setDataSign(dataSignService.sign(expenseService.buildSignPayload(oldSubsidy)));
            expenseMapper.updateById(oldSubsidy);
        }

        return trip;
    }

    public void deleteTrip(Long userId, Long tripId) {
        BusinessTrip trip = getByIdAndUserId(tripId, userId);
        trip.setStatus(1);
        trip.setUpdatedAt(LocalDateTime.now());
        tripMapper.updateById(trip);

        expenseMapper.update(null, new LambdaUpdateWrapper<Expense>()
                .eq(Expense::getTripId, tripId)
                .eq(Expense::getType, 2)
                .eq(Expense::getReimburseStatus, 0)
                .eq(Expense::getStatus, 0)
                .set(Expense::getStatus, 1)
                .set(Expense::getUpdatedAt, LocalDateTime.now())
        );
    }

    public List<TripVO> listTrips(Long userId) {
        List<BusinessTrip> trips = tripMapper.selectList(
                new LambdaQueryWrapper<BusinessTrip>()
                        .eq(BusinessTrip::getUserId, userId)
                        .eq(BusinessTrip::getStatus, 0)
                        .orderByDesc(BusinessTrip::getCreatedAt)
        );
        return trips.stream().map(this::toTripVO).collect(Collectors.toList());
    }

    public TripVO getTripDetail(Long userId, Long tripId) {
        BusinessTrip trip = getByIdAndUserId(tripId, userId);
        TripVO vo = toTripVO(trip);

        List<Expense> relatedExpenses = expenseMapper.selectList(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getTripId, trip.getId())
                        .eq(Expense::getUserId, trip.getUserId())
                        .eq(Expense::getStatus, 0)
                        .orderByDesc(Expense::getCreatedAt)
        );
        vo.setExpenses(relatedExpenses.stream().map(this::toExpenseVO).collect(Collectors.toList()));
        return vo;
    }

    private BusinessTrip getByIdAndUserId(Long tripId, Long userId) {
        BusinessTrip trip = tripMapper.selectOne(
                new LambdaQueryWrapper<BusinessTrip>()
                        .eq(BusinessTrip::getId, tripId)
                        .eq(BusinessTrip::getUserId, userId)
                        .eq(BusinessTrip::getStatus, 0)
        );
        if (trip == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "出差记录不存在");
        }
        return trip;
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

    private TripVO toTripVO(BusinessTrip trip) {
        TripVO vo = new TripVO();
        vo.setId(trip.getId());
        vo.setUserId(trip.getUserId());
        vo.setTitle(trip.getTitle());
        vo.setDestination(trip.getDestination());
        vo.setStartDate(trip.getStartDate());
        vo.setEndDate(trip.getEndDate());
        vo.setDays(trip.getDays());
        vo.setSubsidyPerDay(trip.getSubsidyPerDay());
        vo.setSubsidyTotal(trip.getSubsidyTotal());
        vo.setRemark(trip.getRemark());
        vo.setStatus(trip.getStatus());
        vo.setCreatedAt(trip.getCreatedAt());
        vo.setUpdatedAt(trip.getUpdatedAt());

        Long count = expenseMapper.selectCount(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getTripId, trip.getId())
                        .eq(Expense::getUserId, trip.getUserId())
                        .eq(Expense::getStatus, 0)
        );
        vo.setExpenseCount(count.intValue());
        return vo;
    }
}
