package com.qiankubx.module.agreement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.agreement.dto.AgreementVersionVO;
import com.qiankubx.module.agreement.entity.AgreementVersion;
import com.qiankubx.module.agreement.entity.UserAgreementSign;
import com.qiankubx.module.agreement.mapper.AgreementVersionMapper;
import com.qiankubx.module.agreement.mapper.UserAgreementSignMapper;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/agreement")
@RequiredArgsConstructor
public class AgreementAdminController {

    private final AgreementVersionMapper agreementVersionMapper;
    private final UserAgreementSignMapper userAgreementSignMapper;
    private final UserMapper userMapper;

    @PostMapping("/create")
    public Result<AgreementVersionVO> create(@RequestBody AgreementCreateDTO dto) {
        AgreementVersion version = new AgreementVersion();
        version.setType(dto.getType());
        version.setVersionCode(dto.getVersionCode());
        version.setTitle(dto.getTitle());
        version.setContent(dto.getContent());
        version.setChangeLevel(dto.getChangeLevel());
        version.setChangeSummary(dto.getChangeSummary());
        version.setStatus(0);
        version.setCreatedAt(LocalDateTime.now());
        version.setUpdatedAt(LocalDateTime.now());
        agreementVersionMapper.insert(version);
        return Result.ok(toVO(version));
    }

    @PutMapping("/{id}")
    public Result<AgreementVersionVO> update(@PathVariable Long id, @RequestBody AgreementCreateDTO dto) {
        AgreementVersion version = agreementVersionMapper.selectById(id);
        if (version == null) {
            throw new BizException(404, "协议版本不存在");
        }
        if (version.getStatus() != 0) {
            throw new BizException(400, "只能编辑草稿状态的协议");
        }
        version.setType(dto.getType());
        version.setVersionCode(dto.getVersionCode());
        version.setTitle(dto.getTitle());
        version.setContent(dto.getContent());
        version.setChangeLevel(dto.getChangeLevel());
        version.setChangeSummary(dto.getChangeSummary());
        version.setUpdatedAt(LocalDateTime.now());
        agreementVersionMapper.updateById(version);
        return Result.ok(toVO(version));
    }

    @PostMapping("/{id}/publish")
    public Result<AgreementVersionVO> publish(@PathVariable Long id, @RequestBody AgreementPublishDTO dto) {
        AgreementVersion version = agreementVersionMapper.selectById(id);
        if (version == null) {
            throw new BizException(404, "协议版本不存在");
        }
        if (version.getStatus() != 0) {
            throw new BizException(400, "该协议已发布");
        }
        version.setEffectiveAt(dto.getEffectiveDate());
        version.setStatus(1);
        version.setUpdatedAt(LocalDateTime.now());
        agreementVersionMapper.updateById(version);
        log.info("协议发布: id={}, effectiveDate={}, notifyDaysBefore={}", id, dto.getEffectiveDate(), dto.getNotifyDaysBefore());
        return Result.ok(toVO(version));
    }

    @GetMapping("/list")
    public Result<IPage<AgreementVersionVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<AgreementVersion> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(AgreementVersion::getType, type);
        }
        if (status != null) {
            wrapper.eq(AgreementVersion::getStatus, status);
        }
        wrapper.orderByDesc(AgreementVersion::getCreatedAt);
        IPage<AgreementVersion> result = agreementVersionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        IPage<AgreementVersionVO> voPage = result.convert(this::toVO);
        return Result.ok(voPage);
    }

    @GetMapping("/sign-stats")
    public Result<List<Map<String, Object>>> signStats() {
        List<AgreementVersion> versions = agreementVersionMapper.selectList(
                new LambdaQueryWrapper<AgreementVersion>()
                        .eq(AgreementVersion::getStatus, 1)
                        .orderByDesc(AgreementVersion::getEffectiveAt)
        );
        Long totalUsers = userMapper.selectCount(null);
        List<Map<String, Object>> stats = new ArrayList<>();
        for (AgreementVersion v : versions) {
            Long signed = userAgreementSignMapper.selectCount(
                    new LambdaQueryWrapper<UserAgreementSign>()
                            .eq(UserAgreementSign::getVersionId, v.getId())
            );
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("versionId", v.getId());
            item.put("type", v.getType());
            item.put("versionCode", v.getVersionCode());
            item.put("signed", signed);
            item.put("unsigned", Math.max(0, totalUsers - signed));
            stats.add(item);
        }
        return Result.ok(stats);
    }

    @PostMapping("/{id}/notify")
    public Result<Void> notify(@PathVariable Long id) {
        AgreementVersion version = agreementVersionMapper.selectById(id);
        if (version == null) {
            throw new BizException(404, "协议版本不存在");
        }
        log.info("手动推送协议签署通知: versionId={}, type={}, versionCode={}", id, version.getType(), version.getVersionCode());
        return Result.ok();
    }

    private AgreementVersionVO toVO(AgreementVersion version) {
        AgreementVersionVO vo = new AgreementVersionVO();
        vo.setId(version.getId());
        vo.setType(version.getType());
        vo.setVersionCode(version.getVersionCode());
        vo.setTitle(version.getTitle());
        vo.setContent(version.getContent());
        vo.setChangeSummary(version.getChangeSummary());
        vo.setChangeLevel(version.getChangeLevel());
        vo.setEffectiveAt(version.getEffectiveAt());
        vo.setCreatedAt(version.getCreatedAt());
        return vo;
    }

    @Data
    public static class AgreementCreateDTO {
        private String type;
        private String versionCode;
        private String title;
        private String content;
        private String changeLevel;
        private String changeSummary;
        private String changeDetail;
    }

    @Data
    public static class AgreementPublishDTO {
        private LocalDateTime effectiveDate;
        private Integer notifyDaysBefore;
    }
}
