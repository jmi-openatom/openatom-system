package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateVoteDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSubmitVoteDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateVoteDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestVoteOptionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.entity.VoteCampaign;
import edu.jmi.openatom.server.openatomsystem.entity.VoteOption;
import edu.jmi.openatom.server.openatomsystem.entity.VoteRecord;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.VoteCampaignMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.VoteOptionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.VoteRecordMapper;
import edu.jmi.openatom.server.openatomsystem.service.VoteService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteOptionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteRecordVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteVO;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 投票服务实现 */
@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {
  private static final List<String> STATUSES = List.of("draft", "open", "closed");
  private static final List<String> VOTE_TYPES = List.of("single", "multiple");

  private final VoteCampaignMapper voteCampaignMapper;
  private final VoteOptionMapper voteOptionMapper;
  private final VoteRecordMapper voteRecordMapper;
  private final ClubMapper clubMapper;
  private final UserMapper userMapper;

  @Override
  public Result<List<ResponseVoteVO>> list(Integer clubId, String status) {
    LambdaQueryWrapper<VoteCampaign> wrapper =
        new LambdaQueryWrapper<VoteCampaign>()
            .eq(clubId != null, VoteCampaign::getClubId, clubId)
            .eq(!isBlank(status), VoteCampaign::getStatus, trimToNull(status))
            .orderByDesc(VoteCampaign::getId);
    return Result.success(
        voteCampaignMapper.selectList(wrapper).stream().map(this::toSummary).toList());
  }

  @Override
  public Result<ResponseVoteDetailVO> detail(Integer voteId) {
    VoteCampaign campaign = findCampaign(voteId);
    if (campaign == null) return Result.error(404, "投票活动不存在");
    return Result.success(toDetail(campaign, true));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> create(Integer clubId, RequestCreateVoteDTO request) {
    Result<String> campaignValidation =
        validateCampaignInput(
            clubId,
            request.getTitle(),
            request.getStatus(),
            request.getVoteType(),
            request.getMaxChoices(),
            request.getStartAt(),
            request.getEndAt(),
            request.getOptions());
    if (campaignValidation != null) return campaignValidation;
    VoteCampaign campaign =
        VoteCampaign.builder()
            .clubId(clubId)
            .title(request.getTitle().trim())
            .description(trimToNull(request.getDescription()))
            .status(normalizeStatus(request.getStatus(), "draft"))
            .voteType(normalizeVoteType(request.getVoteType()))
            .maxChoices(normalizeMaxChoices(request.getVoteType(), request.getMaxChoices()))
            .anonymousAllowed(request.getAnonymousAllowed() == null || request.getAnonymousAllowed())
            .resultVisible(request.getResultVisible() == null || request.getResultVisible())
            .startAt(Times.parseTimestamp(request.getStartAt()))
            .endAt(Times.parseTimestamp(request.getEndAt()))
            .createdBy(StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null)
            .build();
    int row = voteCampaignMapper.insert(campaign);
    if (row <= 0) return Result.error("投票活动创建失败");
    Result<String> optionResult = saveOptions(campaign.getId(), request.getOptions());
    if (optionResult != null) return optionResult;
    return Result.success("投票活动创建成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> update(Integer voteId, RequestUpdateVoteDTO request) {
    VoteCampaign campaign = findCampaign(voteId);
    if (campaign == null) return Result.error(404, "投票活动不存在");
    Result<String> campaignValidation =
        validateCampaignInput(
            campaign.getClubId(),
            firstNonBlank(request.getTitle(), campaign.getTitle()),
            request.getStatus(),
            request.getVoteType(),
            request.getMaxChoices(),
            request.getStartAt(),
            request.getEndAt(),
            request.getOptions());
    if (campaignValidation != null) return campaignValidation;
    if (!isBlank(request.getTitle())) campaign.setTitle(request.getTitle().trim());
    if (request.getDescription() != null) campaign.setDescription(trimToNull(request.getDescription()));
    if (request.getStatus() != null) campaign.setStatus(normalizeStatus(request.getStatus(), campaign.getStatus()));
    if (request.getVoteType() != null) campaign.setVoteType(normalizeVoteType(request.getVoteType()));
    if (request.getMaxChoices() != null || request.getVoteType() != null) {
      campaign.setMaxChoices(normalizeMaxChoices(campaign.getVoteType(), request.getMaxChoices()));
    }
    if (request.getAnonymousAllowed() != null) campaign.setAnonymousAllowed(request.getAnonymousAllowed());
    if (request.getResultVisible() != null) campaign.setResultVisible(request.getResultVisible());
    if (request.getStartAt() != null) campaign.setStartAt(Times.parseTimestamp(request.getStartAt()));
    if (request.getEndAt() != null) campaign.setEndAt(Times.parseTimestamp(request.getEndAt()));
    Result<String> timeValidation = validateTimeRange(campaign.getStartAt(), campaign.getEndAt());
    if (timeValidation != null) return timeValidation;
    int row = voteCampaignMapper.updateById(campaign);
    if (row <= 0) return Result.error("投票活动更新失败");
    Result<String> optionResult = saveOptions(voteId, request.getOptions());
    if (optionResult != null) return optionResult;
    return Result.success("投票活动更新成功");
  }

  @Override
  public Result<String> publish(Integer voteId) {
    return updateStatus(voteId, "open", "投票活动已发布");
  }

  @Override
  public Result<String> close(Integer voteId) {
    return updateStatus(voteId, "closed", "投票活动已结束");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> reset(Integer voteId) {
    VoteCampaign campaign = findCampaign(voteId);
    if (campaign == null) return Result.error(404, "投票活动不存在");
    voteRecordMapper.deleteByVoteId(voteId);
    return Result.success("投票记录已清空");
  }

  @Override
  public Result<List<ResponseVoteVO>> siteList(Integer clubId) {
    LambdaQueryWrapper<VoteCampaign> wrapper =
        new LambdaQueryWrapper<VoteCampaign>()
            .eq(clubId != null, VoteCampaign::getClubId, clubId)
            .in(VoteCampaign::getStatus, List.of("open", "closed"))
            .orderByDesc(VoteCampaign::getId);
    return Result.success(
        voteCampaignMapper.selectList(wrapper).stream().map(this::toSummary).toList());
  }

  @Override
  public Result<ResponseVoteDetailVO> siteDetail(Integer voteId) {
    VoteCampaign campaign = findCampaign(voteId);
    if (campaign == null || "draft".equals(campaign.getStatus())) return Result.error(404, "投票活动不存在");
    return Result.success(toPublicDetail(campaign));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseVoteRecordVO> submit(Integer voteId, RequestSubmitVoteDTO request) {
    VoteCampaign campaign = findCampaign(voteId);
    if (campaign == null || "draft".equals(campaign.getStatus())) return Result.error(404, "投票活动不存在");
    Result<String> availability = validateVoteAvailability(campaign);
    if (availability != null) return Result.error(availability.getCode(), availability.getMessage());
    Integer userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
    User user = userId == null ? null : userMapper.selectById(userId);
    if (userId == null && !Boolean.TRUE.equals(campaign.getAnonymousAllowed())) {
      return Result.error(401, "该投票需要登录后参与");
    }
    if (userId == null && isBlank(request.getVoterContact())) return Result.error(400, "请填写联系方式");
    if (userId == null && isBlank(request.getVoterName())) return Result.error(400, "请填写姓名");
    String voterKey = buildVoterKey(userId, request.getVoterContact());
    if (voteRecordMapper.countByVoterKey(voteId, voterKey) > 0) {
      return Result.error(400, "你已经参与过该投票");
    }

    List<Integer> selectedIds = normalizeSelectedOptionIds(request.getOptionIds());
    Result<String> optionValidation = validateSelection(campaign, selectedIds);
    if (optionValidation != null) return Result.error(optionValidation.getCode(), optionValidation.getMessage());
    VoteRecord record =
        VoteRecord.builder()
            .voteId(voteId)
            .userId(userId)
            .voterName(resolveVoterName(request, user))
            .voterContact(resolveVoterContact(request, user))
            .voterKey(voterKey)
            .selectedOptionIds(Jsons.stringify(selectedIds.stream().map(String::valueOf).toList()))
            .remark(trimToNull(request.getRemark()))
            .votedAt(Times.now())
            .build();
    try {
      int row = voteRecordMapper.insert(record);
      if (row <= 0) return Result.error("投票提交失败");
    } catch (DuplicateKeyException e) {
      return Result.error(400, "你已经参与过该投票");
    }
    Map<Integer, VoteOption> optionMap =
        voteOptionMapper.selectByVoteId(voteId).stream()
            .collect(Collectors.toMap(VoteOption::getId, Function.identity()));
    return Result.success(toRecordVO(record, optionMap), "投票成功");
  }

  private Result<String> updateStatus(Integer voteId, String status, String message) {
    VoteCampaign campaign = findCampaign(voteId);
    if (campaign == null) return Result.error(404, "投票活动不存在");
    campaign.setStatus(status);
    int row = voteCampaignMapper.updateById(campaign);
    return row > 0 ? Result.success(message) : Result.error("投票活动状态更新失败");
  }

  private ResponseVoteDetailVO toDetail(VoteCampaign campaign, boolean includeRecords) {
    List<VoteOption> options = voteOptionMapper.selectByVoteId(campaign.getId());
    List<VoteRecord> records = voteRecordMapper.selectByVoteId(campaign.getId());
    Map<Integer, Integer> counts = countSelections(records);
    Map<Integer, VoteOption> optionMap =
        options.stream().collect(Collectors.toMap(VoteOption::getId, Function.identity()));
    return ResponseVoteDetailVO.builder()
        .vote(toSummary(campaign, options, records))
        .options(toOptionVOs(options, counts, records.size(), true))
        .records(includeRecords ? records.stream().map(record -> toRecordVO(record, optionMap)).toList() : List.of())
        .voted(hasCurrentVoted(campaign.getId()))
        .build();
  }

  private ResponseVoteDetailVO toPublicDetail(VoteCampaign campaign) {
    List<VoteOption> options = voteOptionMapper.selectByVoteId(campaign.getId());
    List<VoteRecord> records = voteRecordMapper.selectByVoteId(campaign.getId());
    boolean voted = hasCurrentVoted(campaign.getId());
    boolean showResults =
        Boolean.TRUE.equals(campaign.getResultVisible()) || "closed".equals(campaign.getStatus()) || voted;
    return ResponseVoteDetailVO.builder()
        .vote(toSummary(campaign, options, records))
        .options(toOptionVOs(options, countSelections(records), records.size(), showResults))
        .records(List.of())
        .voted(voted)
        .build();
  }

  private ResponseVoteVO toSummary(VoteCampaign campaign) {
    List<VoteOption> options = voteOptionMapper.selectByVoteId(campaign.getId());
    List<VoteRecord> records = voteRecordMapper.selectByVoteId(campaign.getId());
    return toSummary(campaign, options, records);
  }

  private ResponseVoteVO toSummary(
      VoteCampaign campaign, List<VoteOption> options, List<VoteRecord> records) {
    Club club = clubMapper.selectById(campaign.getClubId());
    int totalVoteCount = countSelections(records).values().stream().mapToInt(Integer::intValue).sum();
    return ResponseVoteVO.builder()
        .id(campaign.getId())
        .clubId(campaign.getClubId())
        .clubName(club == null ? null : club.getName())
        .title(campaign.getTitle())
        .description(campaign.getDescription())
        .status(campaign.getStatus())
        .voteType(campaign.getVoteType())
        .maxChoices(campaign.getMaxChoices())
        .anonymousAllowed(campaign.getAnonymousAllowed())
        .resultVisible(campaign.getResultVisible())
        .optionCount(options.size())
        .voterCount(records.size())
        .totalVoteCount(totalVoteCount)
        .startAt(campaign.getStartAt())
        .endAt(campaign.getEndAt())
        .createdAt(campaign.getCreatedAt())
        .updatedAt(campaign.getUpdatedAt())
        .build();
  }

  private List<ResponseVoteOptionVO> toOptionVOs(
      List<VoteOption> options, Map<Integer, Integer> counts, int voterCount, boolean showResults) {
    return options.stream()
        .map(
            option -> {
              Integer voteCount = showResults ? counts.getOrDefault(option.getId(), 0) : null;
              Double percent =
                  showResults && voterCount > 0
                      ? Math.round(voteCount * 10000.0 / voterCount) / 100.0
                      : null;
              return ResponseVoteOptionVO.builder()
                  .id(option.getId())
                  .voteId(option.getVoteId())
                  .title(option.getTitle())
                  .description(option.getDescription())
                  .sortOrder(option.getSortOrder())
                  .color(option.getColor())
                  .voteCount(voteCount)
                  .votePercent(percent)
                  .build();
            })
        .toList();
  }

  private ResponseVoteRecordVO toRecordVO(VoteRecord record, Map<Integer, VoteOption> optionMap) {
    List<Integer> optionIds = parseOptionIds(record.getSelectedOptionIds());
    return ResponseVoteRecordVO.builder()
        .id(record.getId())
        .voteId(record.getVoteId())
        .userId(record.getUserId())
        .voterName(record.getVoterName())
        .voterContact(record.getVoterContact())
        .optionIds(optionIds)
        .optionTitles(
            optionIds.stream()
                .map(optionMap::get)
                .filter(Objects::nonNull)
                .map(VoteOption::getTitle)
                .toList())
        .remark(record.getRemark())
        .votedAt(record.getVotedAt())
        .build();
  }

  private Result<String> saveOptions(Integer voteId, List<RequestVoteOptionDTO> optionRequests) {
    if (optionRequests == null) return null;
    if (optionRequests.size() < 2) return Result.error(400, "请至少配置两个投票选项");
    List<VoteOption> existingOptions = voteOptionMapper.selectByVoteId(voteId);
    Map<Integer, VoteOption> existingMap =
        existingOptions.stream().collect(Collectors.toMap(VoteOption::getId, Function.identity()));
    Map<Integer, Integer> voteCounts = countSelections(voteRecordMapper.selectByVoteId(voteId));
    Set<Integer> retainedIds = new HashSet<>();
    int defaultSort = 0;
    for (RequestVoteOptionDTO request : optionRequests) {
      defaultSort += 10;
      Result<String> validation = validateOptionRequest(request);
      if (validation != null) return validation;
      if (request.getId() == null) {
        voteOptionMapper.insert(
            VoteOption.builder()
                .voteId(voteId)
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .sortOrder(request.getSortOrder() == null ? defaultSort : request.getSortOrder())
                .color(trimToNull(request.getColor()))
                .build());
        continue;
      }
      VoteOption option = existingMap.get(request.getId());
      if (option == null) return Result.error(400, "存在不属于当前投票活动的选项");
      retainedIds.add(option.getId());
      option.setTitle(request.getTitle().trim());
      option.setDescription(trimToNull(request.getDescription()));
      option.setSortOrder(request.getSortOrder() == null ? defaultSort : request.getSortOrder());
      option.setColor(trimToNull(request.getColor()));
      voteOptionMapper.updateById(option);
    }
    for (VoteOption option : existingOptions) {
      if (retainedIds.contains(option.getId())) continue;
      if (voteCounts.getOrDefault(option.getId(), 0) > 0) return Result.error(400, "已有投票记录的选项不能删除");
      voteOptionMapper.deleteById(option.getId());
    }
    return null;
  }

  private Result<String> validateCampaignInput(
      Integer clubId,
      String title,
      String status,
      String voteType,
      Integer maxChoices,
      String startAt,
      String endAt,
      List<RequestVoteOptionDTO> options) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    if (clubMapper.selectById(clubId) == null) return Result.error(404, "社团不存在");
    if (isBlank(title)) return Result.error(400, "投票标题不能为空");
    if (status != null && !STATUSES.contains(status.trim())) return Result.error(400, "投票状态不合法");
    if (voteType != null && !VOTE_TYPES.contains(voteType.trim())) return Result.error(400, "投票类型不合法");
    if (maxChoices != null && maxChoices <= 0) return Result.error(400, "最多可选数量必须大于0");
    if (options != null && options.size() < 2) return Result.error(400, "请至少配置两个投票选项");
    Timestamp parsedStart = startAt == null ? null : Times.parseTimestamp(startAt);
    Timestamp parsedEnd = endAt == null ? null : Times.parseTimestamp(endAt);
    return validateTimeRange(parsedStart, parsedEnd);
  }

  private Result<String> validateOptionRequest(RequestVoteOptionDTO request) {
    if (request == null) return Result.error(400, "投票选项不能为空");
    if (isBlank(request.getTitle())) return Result.error(400, "选项标题不能为空");
    return null;
  }

  private Result<String> validateTimeRange(Timestamp startAt, Timestamp endAt) {
    if (startAt != null && endAt != null && !startAt.before(endAt)) {
      return Result.error(400, "投票开始时间必须早于结束时间");
    }
    return null;
  }

  private Result<String> validateVoteAvailability(VoteCampaign campaign) {
    if (!"open".equals(campaign.getStatus())) return Result.error(400, "当前投票未开放");
    Timestamp now = Times.now();
    if (campaign.getStartAt() != null && now.before(campaign.getStartAt())) {
      return Result.error(400, "投票尚未开始");
    }
    if (campaign.getEndAt() != null && now.after(campaign.getEndAt())) {
      return Result.error(400, "投票已结束");
    }
    return null;
  }

  private Result<String> validateSelection(VoteCampaign campaign, List<Integer> selectedIds) {
    if (selectedIds.isEmpty()) return Result.error(400, "请选择投票选项");
    int maxChoices = safeMaxChoices(campaign);
    if ("single".equals(campaign.getVoteType()) && selectedIds.size() != 1) {
      return Result.error(400, "单选投票只能选择一个选项");
    }
    if (selectedIds.size() > maxChoices) return Result.error(400, "最多可选择" + maxChoices + "个选项");
    Set<Integer> optionIds =
        voteOptionMapper.selectByVoteId(campaign.getId()).stream()
            .map(VoteOption::getId)
            .collect(Collectors.toSet());
    if (!optionIds.containsAll(selectedIds)) return Result.error(400, "存在无效的投票选项");
    return null;
  }

  private Map<Integer, Integer> countSelections(List<VoteRecord> records) {
    Map<Integer, Integer> counts = new java.util.HashMap<>();
    for (VoteRecord record : records) {
      for (Integer optionId : parseOptionIds(record.getSelectedOptionIds())) {
        counts.merge(optionId, 1, Integer::sum);
      }
    }
    return counts;
  }

  private List<Integer> parseOptionIds(String value) {
    if (value == null || value.isBlank()) return List.of();
    List<Integer> ids = new ArrayList<>();
    for (String item : Jsons.parseStringList(value)) {
      Integer id = parseInteger(item);
      if (id != null) ids.add(id);
    }
    return ids;
  }

  private List<Integer> normalizeSelectedOptionIds(List<Integer> optionIds) {
    if (optionIds == null || optionIds.isEmpty()) return List.of();
    return optionIds.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .toList();
  }

  private boolean hasCurrentVoted(Integer voteId) {
    String voterKey = currentVoterKey();
    return voterKey != null && voteRecordMapper.countByVoterKey(voteId, voterKey) > 0;
  }

  private String currentVoterKey() {
    return StpUtil.isLogin() ? buildVoterKey(StpUtil.getLoginIdAsInt(), null) : null;
  }

  private String buildVoterKey(Integer userId, String contact) {
    if (userId != null) return "user:" + userId;
    return "anon:" + contact.trim().toLowerCase();
  }

  private String resolveVoterName(RequestSubmitVoteDTO request, User user) {
    return firstNonBlank(
        request.getVoterName(), user == null ? null : user.getRealName(), user == null ? null : user.getUserName());
  }

  private String resolveVoterContact(RequestSubmitVoteDTO request, User user) {
    return firstNonBlank(
        request.getVoterContact(), user == null ? null : user.getPhone(), user == null ? null : user.getEmail());
  }

  private VoteCampaign findCampaign(Integer voteId) {
    return voteId == null ? null : voteCampaignMapper.selectById(voteId);
  }

  private String normalizeStatus(String status, String fallback) {
    return isBlank(status) ? fallback : status.trim();
  }

  private String normalizeVoteType(String voteType) {
    return isBlank(voteType) ? "single" : voteType.trim();
  }

  private Integer normalizeMaxChoices(String voteType, Integer maxChoices) {
    if ("multiple".equals(normalizeVoteType(voteType))) return maxChoices == null ? 2 : maxChoices;
    return 1;
  }

  private int safeMaxChoices(VoteCampaign campaign) {
    return "multiple".equals(campaign.getVoteType()) ? Math.max(1, campaign.getMaxChoices()) : 1;
  }

  private Integer parseInteger(String value) {
    if (isBlank(value)) return null;
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String trimToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) return value.trim();
    }
    return null;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
