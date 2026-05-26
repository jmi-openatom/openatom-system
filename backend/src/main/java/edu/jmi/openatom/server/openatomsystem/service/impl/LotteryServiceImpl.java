package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateLotteryDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestDrawLotteryDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestLotteryPrizeDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateLotteryDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.FormSubmission;
import edu.jmi.openatom.server.openatomsystem.entity.LotteryCampaign;
import edu.jmi.openatom.server.openatomsystem.entity.LotteryPrize;
import edu.jmi.openatom.server.openatomsystem.entity.LotteryWinner;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.FormSubmissionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.LotteryCampaignMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.LotteryPrizeMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.LotteryWinnerMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.SiteFormMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.LotteryService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryPrizeVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryScreenVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryWinnerVO;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 抽奖系统实现类
 *
 * <p>以站点表单提交记录作为参与名单, 按奖品库存抽取并生成公开大屏数据
 */
@Service
@RequiredArgsConstructor
public class LotteryServiceImpl implements LotteryService {
  private static final List<String> STATUSES = List.of("draft", "open", "closed");

  private final LotteryCampaignMapper lotteryCampaignMapper;
  private final LotteryPrizeMapper lotteryPrizeMapper;
  private final LotteryWinnerMapper lotteryWinnerMapper;
  private final ClubMapper clubMapper;
  private final SiteFormMapper siteFormMapper;
  private final FormSubmissionMapper formSubmissionMapper;
  private final UserMapper userMapper;

  @Override
  public Result<List<ResponseLotteryVO>> list(Integer clubId, String status) {
    LambdaQueryWrapper<LotteryCampaign> wrapper =
        new LambdaQueryWrapper<LotteryCampaign>()
            .eq(clubId != null, LotteryCampaign::getClubId, clubId)
            .eq(!isBlank(status), LotteryCampaign::getStatus, trimToNull(status))
            .orderByDesc(LotteryCampaign::getId);
    return Result.success(
        lotteryCampaignMapper.selectList(wrapper).stream().map(this::toSummary).toList());
  }

  @Override
  public Result<ResponseLotteryDetailVO> detail(Integer lotteryId) {
    LotteryCampaign campaign = findCampaign(lotteryId);
    if (campaign == null) return Result.error(404, "抽奖活动不存在");
    return Result.success(toDetail(campaign));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> create(Integer clubId, RequestCreateLotteryDTO request) {
    Result<String> relationCheck = validateRelation(clubId, request.getFormId());
    if (relationCheck != null) return relationCheck;
    String status = normalizeStatus(request.getStatus(), "draft");
    if (!STATUSES.contains(status)) return Result.error(400, "抽奖状态不合法");
    LotteryCampaign campaign =
        LotteryCampaign.builder()
            .clubId(clubId)
            .formId(request.getFormId())
            .title(request.getTitle().trim())
            .description(trimToNull(request.getDescription()))
            .status(status)
            .createdBy(StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null)
            .build();
    int row = lotteryCampaignMapper.insert(campaign);
    if (row <= 0) return Result.error("抽奖活动创建失败");
    Result<String> prizeResult = savePrizes(campaign.getId(), request.getPrizes());
    if (prizeResult != null) return prizeResult;
    return Result.success("抽奖活动创建成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> update(Integer lotteryId, RequestUpdateLotteryDTO request) {
    LotteryCampaign campaign = findCampaign(lotteryId);
    if (campaign == null) return Result.error(404, "抽奖活动不存在");
    if (request.getFormId() != null) {
      Result<String> relationCheck = validateRelation(campaign.getClubId(), request.getFormId());
      if (relationCheck != null) return relationCheck;
      if (!Objects.equals(campaign.getFormId(), request.getFormId())
          && lotteryWinnerMapper.countByLotteryId(lotteryId) > 0) {
        return Result.error(400, "已有中奖记录后不能更换关联表单");
      }
      campaign.setFormId(request.getFormId());
    }
    if (!isBlank(request.getTitle())) campaign.setTitle(request.getTitle().trim());
    if (request.getDescription() != null) campaign.setDescription(trimToNull(request.getDescription()));
    if (request.getStatus() != null) {
      String status = normalizeStatus(request.getStatus(), campaign.getStatus());
      if (!STATUSES.contains(status)) return Result.error(400, "抽奖状态不合法");
      campaign.setStatus(status);
    }
    int row = lotteryCampaignMapper.updateById(campaign);
    if (row <= 0) return Result.error("抽奖活动更新失败");
    Result<String> prizeResult = savePrizes(lotteryId, request.getPrizes());
    if (prizeResult != null) return prizeResult;
    return Result.success("抽奖活动更新成功");
  }

  @Override
  public Result<String> publish(Integer lotteryId) {
    return updateStatus(lotteryId, "open", "抽奖活动已开始");
  }

  @Override
  public Result<String> close(Integer lotteryId) {
    return updateStatus(lotteryId, "closed", "抽奖活动已结束");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public synchronized Result<ResponseLotteryWinnerVO> draw(
      Integer lotteryId, RequestDrawLotteryDTO request) {
    LotteryCampaign campaign = findCampaign(lotteryId);
    if (campaign == null) return Result.error(404, "抽奖活动不存在");
    if (!"open".equals(campaign.getStatus())) return Result.error(400, "请先开始抽奖活动");
    List<LotteryPrize> prizes = lotteryPrizeMapper.selectByLotteryId(lotteryId);
    List<LotteryWinner> winners = lotteryWinnerMapper.selectByLotteryId(lotteryId);
    Map<Integer, Long> wonCounts = countWinnersByPrize(winners);
    LotteryPrize prize = selectPrize(prizes, wonCounts, request == null ? null : request.getPrizeId());
    if (prize == null) return Result.error(400, "没有可抽取的奖品");
    if (remainingCount(prize, wonCounts) <= 0) return Result.error(400, "该奖品已抽完");

    List<FormSubmission> eligible = eligibleSubmissions(campaign, winners);
    if (eligible.isEmpty()) return Result.error(400, "当前没有可抽取的表单提交记录");
    Collections.shuffle(eligible, ThreadLocalRandom.current());
    FormSubmission submission = eligible.get(0);
    User user = submission.getUserId() == null ? null : userMapper.selectById(submission.getUserId());
    Map<String, Object> formData = Jsons.parseObject(submission.getFormData());
    Timestamp now = new Timestamp(System.currentTimeMillis());
    LotteryWinner winner =
        LotteryWinner.builder()
            .lotteryId(lotteryId)
            .prizeId(prize.getId())
            .submissionId(submission.getId())
            .userId(submission.getUserId())
            .winnerName(resolveWinnerName(submission, user, formData))
            .winnerContact(resolveWinnerContact(submission, user, formData))
            .winnerAccount(resolveWinnerAccount(user, formData))
            .wonAt(now)
            .build();
    int row = lotteryWinnerMapper.insert(winner);
    if (row <= 0) return Result.error("抽奖失败");
    return Result.success(toWinnerVO(winner, prize, formData), "抽奖成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> reset(Integer lotteryId) {
    LotteryCampaign campaign = findCampaign(lotteryId);
    if (campaign == null) return Result.error(404, "抽奖活动不存在");
    lotteryWinnerMapper.deleteByLotteryId(lotteryId);
    return Result.success("中奖记录已清空");
  }

  @Override
  public Result<ResponseLotteryScreenVO> screen(Integer lotteryId) {
    LotteryCampaign campaign = findCampaign(lotteryId);
    if (campaign == null) return Result.error(404, "抽奖活动不存在");
    ResponseLotteryDetailVO detail = toDetail(campaign);
    List<ResponseLotteryWinnerVO> winners = detail.getWinners();
    return Result.success(
        ResponseLotteryScreenVO.builder()
            .lottery(detail.getLottery())
            .participantNames(participantNames(campaign.getFormId()))
            .prizes(detail.getPrizes())
            .winners(winners)
            .latestWinner(winners.isEmpty() ? null : winners.get(0))
            .build());
  }

  private Result<String> updateStatus(Integer lotteryId, String status, String message) {
    LotteryCampaign campaign = findCampaign(lotteryId);
    if (campaign == null) return Result.error(404, "抽奖活动不存在");
    campaign.setStatus(status);
    int row = lotteryCampaignMapper.updateById(campaign);
    return row > 0 ? Result.success(message) : Result.error("抽奖活动状态更新失败");
  }

  private ResponseLotteryDetailVO toDetail(LotteryCampaign campaign) {
    List<LotteryPrize> prizes = lotteryPrizeMapper.selectByLotteryId(campaign.getId());
    List<LotteryWinner> winners = lotteryWinnerMapper.selectByLotteryId(campaign.getId());
    return ResponseLotteryDetailVO.builder()
        .lottery(toSummary(campaign, prizes, winners))
        .prizes(toPrizeVOs(prizes, countWinnersByPrize(winners)))
        .winners(toWinnerVOs(winners, prizes))
        .build();
  }

  private ResponseLotteryVO toSummary(LotteryCampaign campaign) {
    List<LotteryPrize> prizes = lotteryPrizeMapper.selectByLotteryId(campaign.getId());
    List<LotteryWinner> winners = lotteryWinnerMapper.selectByLotteryId(campaign.getId());
    return toSummary(campaign, prizes, winners);
  }

  private ResponseLotteryVO toSummary(
      LotteryCampaign campaign, List<LotteryPrize> prizes, List<LotteryWinner> winners) {
    Club club = clubMapper.selectById(campaign.getClubId());
    SiteForm form = siteFormMapper.selectById(campaign.getFormId());
    Map<Integer, Long> wonCounts = countWinnersByPrize(winners);
    int totalPrizeCount = prizes.stream().mapToInt(prize -> safeQuantity(prize.getQuantity())).sum();
    int remainingPrizeCount = prizes.stream().mapToInt(prize -> remainingCount(prize, wonCounts)).sum();
    return ResponseLotteryVO.builder()
        .id(campaign.getId())
        .clubId(campaign.getClubId())
        .clubName(club == null ? null : club.getName())
        .formId(campaign.getFormId())
        .formName(form == null ? null : form.getName())
        .title(campaign.getTitle())
        .description(campaign.getDescription())
        .status(campaign.getStatus())
        .participantCount(countParticipants(campaign.getFormId()))
        .winnerCount(winners.size())
        .totalPrizeCount(totalPrizeCount)
        .remainingPrizeCount(remainingPrizeCount)
        .createdAt(campaign.getCreatedAt())
        .updatedAt(campaign.getUpdatedAt())
        .build();
  }

  private List<ResponseLotteryPrizeVO> toPrizeVOs(
      List<LotteryPrize> prizes, Map<Integer, Long> wonCounts) {
    return prizes.stream()
        .map(
            prize -> {
              int wonCount = Math.toIntExact(wonCounts.getOrDefault(prize.getId(), 0L));
              return ResponseLotteryPrizeVO.builder()
                  .id(prize.getId())
                  .lotteryId(prize.getLotteryId())
                  .name(prize.getName())
                  .level(prize.getLevel())
                  .quantity(safeQuantity(prize.getQuantity()))
                  .sortOrder(prize.getSortOrder())
                  .color(prize.getColor())
                  .wonCount(wonCount)
                  .remainingCount(Math.max(0, safeQuantity(prize.getQuantity()) - wonCount))
                  .build();
            })
        .toList();
  }

  private List<ResponseLotteryWinnerVO> toWinnerVOs(
      List<LotteryWinner> winners, List<LotteryPrize> prizes) {
    if (winners.isEmpty()) return List.of();
    Map<Integer, LotteryPrize> prizeMap =
        prizes.stream().collect(Collectors.toMap(LotteryPrize::getId, Function.identity()));
    Set<Integer> submissionIds =
        winners.stream()
            .map(LotteryWinner::getSubmissionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Integer, FormSubmission> submissionMap =
        submissionIds.isEmpty()
            ? Map.of()
            : formSubmissionMapper.selectBatchIds(submissionIds).stream()
                .collect(Collectors.toMap(FormSubmission::getId, Function.identity()));
    return winners.stream()
        .map(
            winner -> {
              FormSubmission submission = submissionMap.get(winner.getSubmissionId());
              Map<String, Object> formData =
                  submission == null ? Map.of() : Jsons.parseObject(submission.getFormData());
              return toWinnerVO(winner, prizeMap.get(winner.getPrizeId()), formData);
            })
        .toList();
  }

  private ResponseLotteryWinnerVO toWinnerVO(
      LotteryWinner winner, LotteryPrize prize, Map<String, Object> formData) {
    return ResponseLotteryWinnerVO.builder()
        .id(winner.getId())
        .lotteryId(winner.getLotteryId())
        .prizeId(winner.getPrizeId())
        .prizeName(prize == null ? null : prize.getName())
        .prizeLevel(prize == null ? null : prize.getLevel())
        .submissionId(winner.getSubmissionId())
        .userId(winner.getUserId())
        .winnerName(winner.getWinnerName())
        .winnerContact(winner.getWinnerContact())
        .winnerAccount(winner.getWinnerAccount())
        .formData(formData)
        .wonAt(winner.getWonAt())
        .build();
  }

  private Result<String> savePrizes(Integer lotteryId, List<RequestLotteryPrizeDTO> prizeRequests) {
    if (prizeRequests == null) return null;
    if (prizeRequests.isEmpty()) return Result.error(400, "请至少配置一个奖品");
    List<LotteryPrize> existingPrizes = lotteryPrizeMapper.selectByLotteryId(lotteryId);
    Map<Integer, LotteryPrize> existingMap =
        existingPrizes.stream().collect(Collectors.toMap(LotteryPrize::getId, Function.identity()));
    Set<Integer> retainedIds = new HashSet<>();
    int defaultSort = 0;
    for (RequestLotteryPrizeDTO request : prizeRequests) {
      defaultSort += 10;
      Result<String> validation = validatePrizeRequest(request);
      if (validation != null) return validation;
      if (request.getId() == null) {
        lotteryPrizeMapper.insert(
            LotteryPrize.builder()
                .lotteryId(lotteryId)
                .name(request.getName().trim())
                .level(defaultString(request.getLevel(), "奖品"))
                .quantity(request.getQuantity())
                .sortOrder(request.getSortOrder() == null ? defaultSort : request.getSortOrder())
                .color(trimToNull(request.getColor()))
                .build());
        continue;
      }
      LotteryPrize prize = existingMap.get(request.getId());
      if (prize == null) return Result.error(400, "存在不属于当前抽奖活动的奖品");
      long wonCount = lotteryWinnerMapper.countByPrizeId(prize.getId());
      if (request.getQuantity() < wonCount) {
        return Result.error(400, "奖品“" + prize.getName() + "”数量不能小于已抽中数量");
      }
      retainedIds.add(prize.getId());
      prize.setName(request.getName().trim());
      prize.setLevel(defaultString(request.getLevel(), "奖品"));
      prize.setQuantity(request.getQuantity());
      prize.setSortOrder(request.getSortOrder() == null ? defaultSort : request.getSortOrder());
      prize.setColor(trimToNull(request.getColor()));
      lotteryPrizeMapper.updateById(prize);
    }
    for (LotteryPrize prize : existingPrizes) {
      if (retainedIds.contains(prize.getId())) continue;
      if (lotteryWinnerMapper.countByPrizeId(prize.getId()) > 0) {
        return Result.error(400, "已有中奖记录的奖品不能删除");
      }
      lotteryPrizeMapper.deleteById(prize.getId());
    }
    return null;
  }

  private Result<String> validatePrizeRequest(RequestLotteryPrizeDTO request) {
    if (request == null) return Result.error(400, "奖品配置不能为空");
    if (isBlank(request.getName())) return Result.error(400, "奖品名称不能为空");
    if (request.getQuantity() == null || request.getQuantity() <= 0)
      return Result.error(400, "奖品数量必须大于0");
    return null;
  }

  private Result<String> validateRelation(Integer clubId, Integer formId) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    if (formId == null) return Result.error(400, "关联表单不能为空");
    Club club = clubMapper.selectById(clubId);
    if (club == null) return Result.error(404, "社团不存在");
    SiteForm form = siteFormMapper.selectById(formId);
    if (form == null) return Result.error(404, "关联表单不存在");
    if (!Objects.equals(form.getClubId(), clubId)) return Result.error(400, "关联表单不属于当前社团");
    return null;
  }

  private LotteryPrize selectPrize(
      List<LotteryPrize> prizes, Map<Integer, Long> wonCounts, Integer prizeId) {
    if (prizeId != null) {
      return prizes.stream().filter(prize -> Objects.equals(prize.getId(), prizeId)).findFirst().orElse(null);
    }
    return prizes.stream().filter(prize -> remainingCount(prize, wonCounts) > 0).findFirst().orElse(null);
  }

  private List<FormSubmission> eligibleSubmissions(
      LotteryCampaign campaign, List<LotteryWinner> winners) {
    Set<Integer> wonSubmissionIds =
        winners.stream()
            .map(LotteryWinner::getSubmissionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    List<FormSubmission> submissions = formSubmissionMapper.selectByFormIdOrdered(campaign.getFormId());
    List<FormSubmission> eligible = new ArrayList<>();
    for (FormSubmission submission : submissions) {
      if (wonSubmissionIds.contains(submission.getId())) continue;
      if (!"submitted".equals(defaultString(submission.getStatus(), "submitted"))) continue;
      eligible.add(submission);
    }
    return eligible;
  }

  private List<String> participantNames(Integer formId) {
    if (formId == null) return List.of();
    List<FormSubmission> submissions =
        formSubmissionMapper.selectByFormIdOrdered(formId).stream()
            .filter(submission -> "submitted".equals(defaultString(submission.getStatus(), "submitted")))
            .toList();
    if (submissions.isEmpty()) return List.of();
    Set<Integer> userIds =
        submissions.stream()
            .map(FormSubmission::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Integer, User> users =
        userIds.isEmpty()
            ? Map.of()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    return submissions.stream()
        .map(
            submission -> {
              User user = users.get(submission.getUserId());
              Map<String, Object> formData = Jsons.parseObject(submission.getFormData());
              return resolveWinnerName(submission, user, formData);
            })
        .filter(name -> !isBlank(name))
        .toList();
  }

  private Map<Integer, Long> countWinnersByPrize(List<LotteryWinner> winners) {
    if (winners == null || winners.isEmpty()) return Map.of();
    return winners.stream()
        .filter(winner -> winner.getPrizeId() != null)
        .collect(Collectors.groupingBy(LotteryWinner::getPrizeId, Collectors.counting()));
  }

  private int countParticipants(Integer formId) {
    if (formId == null) return 0;
    return Math.toIntExact(
        formSubmissionMapper.selectCount(
            new LambdaQueryWrapper<FormSubmission>().eq(FormSubmission::getFormId, formId)));
  }

  private int remainingCount(LotteryPrize prize, Map<Integer, Long> wonCounts) {
    int wonCount = Math.toIntExact(wonCounts.getOrDefault(prize.getId(), 0L));
    return Math.max(0, safeQuantity(prize.getQuantity()) - wonCount);
  }

  private String resolveWinnerName(
      FormSubmission submission, User user, Map<String, Object> formData) {
    return firstNonBlank(
        user == null ? null : user.getRealName(),
        user == null ? null : user.getUserName(),
        submission.getAnonymousName(),
        firstValue(formData, "name", "realName", "applicantName", "anonymousName"),
        "参与者" + submission.getId());
  }

  private String resolveWinnerContact(
      FormSubmission submission, User user, Map<String, Object> formData) {
    return firstNonBlank(
        user == null ? null : user.getPhone(),
        user == null ? null : user.getEmail(),
        submission.getAnonymousContact(),
        firstValue(formData, "contact", "phone", "email", "wechat", "anonymousContact"));
  }

  private String resolveWinnerAccount(User user, Map<String, Object> formData) {
    return firstNonBlank(
        user == null ? null : user.getUserName(),
        user == null ? null : user.getStudentId(),
        firstValue(formData, "studentId", "userName", "account"));
  }

  private String firstValue(Map<String, Object> values, String... keys) {
    if (values == null || values.isEmpty()) return null;
    for (String key : keys) {
      Object value = values.get(key);
      if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value);
    }
    return null;
  }

  private LotteryCampaign findCampaign(Integer lotteryId) {
    return lotteryId == null ? null : lotteryCampaignMapper.selectById(lotteryId);
  }

  private String normalizeStatus(String status, String fallback) {
    return isBlank(status) ? fallback : status.trim();
  }

  private int safeQuantity(Integer quantity) {
    return quantity == null ? 0 : Math.max(0, quantity);
  }

  private String trimToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private String defaultString(String value, String fallback) {
    return isBlank(value) ? fallback : value.trim();
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
