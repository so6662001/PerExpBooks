package com.qiankubx.module.team.controller;

import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.response.Result;
import com.qiankubx.module.team.dto.*;
import com.qiankubx.module.team.service.TeamService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/create")
    public Result<TeamVO> create(HttpServletRequest request,
                                  @Valid @RequestBody CreateTeamDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(teamService.createTeam(userId, dto.getName()));
    }

    @GetMapping("/info")
    public Result<TeamVO> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(teamService.getTeamInfo(userId));
    }

    @PostMapping("/invite")
    public Result<InviteLinkVO> invite(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(teamService.generateInviteLink(userId));
    }

    @PostMapping("/join")
    public Result<Void> join(HttpServletRequest request,
                              @Valid @RequestBody JoinTeamDTO dto) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        teamService.joinTeam(userId, dto.getInviteCode());
        return Result.ok();
    }

    @DeleteMapping("/member/{targetUserId}")
    public Result<Void> removeMember(HttpServletRequest request,
                                      @PathVariable Long targetUserId) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        teamService.removeMember(userId, targetUserId);
        return Result.ok();
    }

    @GetMapping("/members")
    public Result<List<TeamMemberVO>> members(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return Result.ok(teamService.listMembers(userId));
    }
}
