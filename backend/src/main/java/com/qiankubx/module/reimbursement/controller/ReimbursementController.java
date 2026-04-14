package com.qiankubx.module.reimbursement.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.reimbursement.dto.EmailSendDTO;
import com.qiankubx.module.reimbursement.dto.ReimbursementCreateDTO;
import com.qiankubx.module.reimbursement.dto.ReimbursementUpdateDTO;
import com.qiankubx.module.reimbursement.dto.ReimbursementVO;
import com.qiankubx.module.reimbursement.service.ReimbursementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reimbursement")
@RequiredArgsConstructor
public class ReimbursementController {

    private final ReimbursementService reimbursementService;

    @PostMapping("/generate")
    public Result<ReimbursementVO> generate(HttpServletRequest request,
                                            @Valid @RequestBody ReimbursementCreateDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(reimbursementService.generate(userId, dto));
    }

    @GetMapping("/list")
    public Result<List<ReimbursementVO>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(reimbursementService.list(userId));
    }

    @GetMapping("/{id}")
    public Result<ReimbursementVO> getDetail(HttpServletRequest request,
                                             @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(reimbursementService.getDetail(userId, id));
    }

    @PutMapping("/{id}")
    public Result<ReimbursementVO> update(HttpServletRequest request,
                                          @PathVariable Long id,
                                          @Valid @RequestBody ReimbursementUpdateDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        dto.setId(id);
        return Result.ok(reimbursementService.update(userId, dto));
    }

    @GetMapping("/{id}/pdf")
    public Result<String> downloadPdf(HttpServletRequest request,
                                      @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        String url = reimbursementService.getPdfUrl(userId, id);
        return Result.ok(url);
    }

    @GetMapping("/{id}/merged-pdf")
    public Result<String> downloadMergedPdf(HttpServletRequest request,
                                            @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        String url = reimbursementService.getMergedPdfUrl(userId, id);
        return Result.ok(url);
    }

    @GetMapping("/{id}/zip")
    public Result<String> downloadZip(HttpServletRequest request,
                                      @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        String url = reimbursementService.getZipUrl(userId, id);
        return Result.ok(url);
    }

    @PostMapping("/{id}/send-email")
    public Result<Void> sendEmail(HttpServletRequest request,
                                  @PathVariable Long id,
                                  @Valid @RequestBody EmailSendDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        reimbursementService.sendEmail(userId, id, dto);
        return Result.ok();
    }

    @PutMapping("/{id}/received")
    public Result<Void> confirmReceived(HttpServletRequest request,
                                        @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        reimbursementService.confirmReceived(userId, id);
        return Result.ok();
    }

    @PostMapping("/{id}/regenerate")
    public Result<ReimbursementVO> regenerate(HttpServletRequest request,
                                              @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(reimbursementService.regenerate(userId, id));
    }
}
