package com.qiankubx.module.expense.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.ResultCode;
import com.qiankubx.module.expense.dto.TripCreateDTO;
import com.qiankubx.module.expense.dto.TripUpdateDTO;
import com.qiankubx.module.expense.dto.TripVO;
import com.qiankubx.module.expense.entity.BusinessTrip;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.expense.mapper.BusinessTripMapper;
import com.qiankubx.module.expense.mapper.ExpenseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final BusinessTripMapper tripMapper;
    private final ExpenseMapper expenseMapper;

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
        return trip;
    }

    public void deleteTrip(Long userId, Long tripId) {
        BusinessTrip trip = getByIdAndUserId(tripId, userId);
        trip.setStatus(1);
        trip.setUpdatedAt(LocalDateTime.now());
        tripMapper.updateById(trip);
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
        return toTripVO(trip);
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
