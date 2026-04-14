package com.qiankubx.module.team.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiankubx.common.exception.BizException;
import com.qiankubx.module.team.dto.*;
import com.qiankubx.module.team.entity.Team;
import com.qiankubx.module.team.entity.TeamMember;
import com.qiankubx.module.team.mapper.TeamMapper;
import com.qiankubx.module.team.mapper.TeamMemberMapper;
import com.qiankubx.module.user.entity.User;
import com.qiankubx.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public TeamVO createTeam(Long userId, String name) {
        Long existingCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, 0)
        );
        if (existingCount > 0) {
            throw new BizException(400, "您已在一个团队中，无法创建新团队");
        }

        Team team = new Team();
        team.setName(name);
        team.setOwnerId(userId);
        team.setInviteCode(generateInviteCode());
        team.setMemberCount(1);
        team.setMaxMember(50);
        team.setStatus(0);
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());
        teamMapper.insert(team);

        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(1);
        member.setStatus(0);
        member.setJoinedAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());
        teamMemberMapper.insert(member);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getTeamId, team.getId())
        );

        log.info("团队创建成功: teamId={}, userId={}", team.getId(), userId);
        return getTeamInfo(userId);
    }

    public TeamVO getTeamInfo(Long userId) {
        TeamMember membership = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, 0)
        );
        if (membership == null) {
            return null;
        }

        Team team = teamMapper.selectById(membership.getTeamId());
        if (team == null || team.getStatus() != 0) {
            return null;
        }

        User owner = userMapper.selectById(team.getOwnerId());

        TeamVO vo = new TeamVO();
        vo.setId(team.getId());
        vo.setName(team.getName());
        vo.setOwnerId(team.getOwnerId());
        vo.setOwnerNickname(owner != null ? owner.getNickname() : null);
        vo.setInviteCode(team.getInviteCode());
        vo.setMemberCount(team.getMemberCount());
        vo.setMaxMember(team.getMaxMember());
        vo.setMyRole(membership.getRole());
        vo.setCreatedAt(team.getCreatedAt());
        return vo;
    }

    public InviteLinkVO generateInviteLink(Long userId) {
        TeamMember membership = getAdminMembership(userId);
        Team team = teamMapper.selectById(membership.getTeamId());

        InviteLinkVO vo = new InviteLinkVO();
        vo.setInviteCode(team.getInviteCode());
        vo.setInviteLink("https://qianku.com/join?code=" + team.getInviteCode());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void joinTeam(Long userId, String inviteCode) {
        Long existingCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, 0)
        );
        if (existingCount > 0) {
            throw new BizException(400, "您已在一个团队中");
        }

        Team team = teamMapper.selectOne(
                new LambdaQueryWrapper<Team>()
                        .eq(Team::getInviteCode, inviteCode)
                        .eq(Team::getStatus, 0)
        );
        if (team == null) {
            throw new BizException(400, "邀请码无效或团队已解散");
        }

        if (team.getMemberCount() >= team.getMaxMember()) {
            throw new BizException(400, "团队人数已满");
        }

        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(2);
        member.setStatus(0);
        member.setJoinedAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());
        teamMemberMapper.insert(member);

        teamMapper.update(null, new LambdaUpdateWrapper<Team>()
                .eq(Team::getId, team.getId())
                .setSql("member_count = member_count + 1")
                .set(Team::getUpdatedAt, LocalDateTime.now())
        );

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getTeamId, team.getId())
        );

        log.info("成员加入团队: teamId={}, userId={}", team.getId(), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long userId, Long targetUserId) {
        TeamMember adminMember = getAdminMembership(userId);

        if (userId.equals(targetUserId)) {
            throw new BizException(400, "不能移除自己");
        }

        TeamMember targetMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, adminMember.getTeamId())
                        .eq(TeamMember::getUserId, targetUserId)
                        .eq(TeamMember::getStatus, 0)
        );
        if (targetMember == null) {
            throw new BizException(400, "该成员不在团队中");
        }

        targetMember.setStatus(1);
        teamMemberMapper.updateById(targetMember);

        teamMapper.update(null, new LambdaUpdateWrapper<Team>()
                .eq(Team::getId, adminMember.getTeamId())
                .setSql("member_count = member_count - 1")
                .set(Team::getUpdatedAt, LocalDateTime.now())
        );

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, targetUserId)
                .set(User::getTeamId, null)
        );

        log.info("移除团队成员: teamId={}, targetUserId={}", adminMember.getTeamId(), targetUserId);
    }

    public List<TeamMemberVO> listMembers(Long userId) {
        TeamMember membership = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, 0)
        );
        if (membership == null) {
            throw new BizException(400, "您未加入任何团队");
        }

        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, membership.getTeamId())
                        .eq(TeamMember::getStatus, 0)
                        .orderByAsc(TeamMember::getRole)
                        .orderByAsc(TeamMember::getJoinedAt)
        );

        return members.stream().map(m -> {
            User user = userMapper.selectById(m.getUserId());
            TeamMemberVO vo = new TeamMemberVO();
            vo.setUserId(m.getUserId());
            vo.setNickname(user != null ? user.getNickname() : null);
            vo.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
            vo.setRole(m.getRole());
            vo.setJoinedAt(m.getJoinedAt());
            return vo;
        }).toList();
    }

    private TeamMember getAdminMembership(Long userId) {
        TeamMember membership = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, 0)
                        .eq(TeamMember::getRole, 1)
        );
        if (membership == null) {
            throw new BizException(403, "无管理员权限");
        }
        return membership;
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
