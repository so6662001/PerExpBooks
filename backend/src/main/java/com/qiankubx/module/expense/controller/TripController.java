package com.qiankubx.module.expense.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.expense.dto.TripCreateDTO;
import com.qiankubx.module.expense.dto.TripUpdateDTO;
import com.qiankubx.module.expense.dto.TripVO;
import com.qiankubx.module.expense.entity.BusinessTrip;
import com.qiankubx.module.expense.service.TripService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trip")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public Result<BusinessTrip> create(HttpServletRequest request,
                                       @Valid @RequestBody TripCreateDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(tripService.createTrip(userId, dto));
    }

    @PutMapping("/{id}")
    public Result<BusinessTrip> update(HttpServletRequest request,
                                       @PathVariable Long id,
                                       @Valid @RequestBody TripUpdateDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        dto.setId(id);
        return Result.ok(tripService.updateTrip(userId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request,
                               @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        tripService.deleteTrip(userId, id);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<List<TripVO>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(tripService.listTrips(userId));
    }

    @GetMapping("/{id}")
    public Result<TripVO> detail(HttpServletRequest request,
                                 @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(tripService.getTripDetail(userId, id));
    }
}
