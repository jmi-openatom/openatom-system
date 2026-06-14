package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCacheEvict;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCached;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAward;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubAwardMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.service.AwardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 获奖经历管理实现类
 *
 * <p>负责社团获奖记录的创建, 更新, 删除和查询等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class AwardServiceImpl implements AwardService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";

  private final ClubMapper clubMapper;
  private final ClubAwardMapper clubAwardMapper;

  @Override
  @RedisCached(cacheName = "site", key = "'admin-awards'", ttlSeconds = 300)
  public Result<List<ClubAward>> list() {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    return Result.success(clubAwardMapper.selectByClubIdOrdered(club.getId()));
  }

  @Override
  @RedisCacheEvict(cacheNames = {"site"})
  public Result<String> create(RequestCreateAwardDTO request) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    int rows = clubAwardMapper.insert(ClubAward.builder().clubId(club.getId())
        .title(request.getTitle()).competitionName(request.getCompetitionName())
        .awardLevel(request.getAwardLevel()).awardYear(request.getAwardYear())
        .teamName(request.getTeamName()).description(request.getDescription())
        .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder()).build());
    return rows > 0 ? Result.success("获奖经历创建成功") : Result.error("获奖经历创建失败");
  }

  @Override
  @RedisCacheEvict(cacheNames = {"site"})
  public Result<String> update(Integer awardId, RequestUpdateAwardDTO request) {
    ClubAward award = findAward(awardId);
    if (award == null) return Result.error(404, "获奖经历不存在");
    if (request.getTitle() != null) award.setTitle(request.getTitle());
    if (request.getCompetitionName() != null) award.setCompetitionName(request.getCompetitionName());
    if (request.getAwardLevel() != null) award.setAwardLevel(request.getAwardLevel());
    if (request.getAwardYear() != null) award.setAwardYear(request.getAwardYear());
    if (request.getTeamName() != null) award.setTeamName(request.getTeamName());
    if (request.getDescription() != null) award.setDescription(request.getDescription());
    if (request.getSortOrder() != null) award.setSortOrder(request.getSortOrder());
    int rows = clubAwardMapper.updateById(award);
    return rows > 0 ? Result.success("获奖经历更新成功") : Result.error("获奖经历更新失败");
  }

  @Override
  @RedisCacheEvict(cacheNames = {"site"})
  public Result<String> delete(Integer awardId) {
    ClubAward award = findAward(awardId);
    if (award == null) return Result.error(404, "获奖经历不存在");
    int rows = clubAwardMapper.deleteById(awardId);
    return rows > 0 ? Result.success("获奖经历已删除") : Result.error("获奖经历删除失败");
  }

  private ClubAward findAward(Integer awardId) {
    Club club = defaultClub();
    if (awardId == null || club == null) return null;
    return clubAwardMapper.selectOneByIdAndClubId(awardId, club.getId());
  }

  private Club defaultClub() {
    return clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
  }
}
