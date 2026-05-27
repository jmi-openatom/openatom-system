package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointAdjustmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRedeemItemDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRedemptionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRedemptionStatusDTO;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInSession;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.entity.PointAccount;
import edu.jmi.openatom.server.openatomsystem.entity.PointRedeemItem;
import edu.jmi.openatom.server.openatomsystem.entity.PointRedemption;
import edu.jmi.openatom.server.openatomsystem.entity.PointTransaction;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.PointAccountMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.PointRedeemItemMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.PointRedemptionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.PointTransactionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.PointService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointAccountVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointRedeemItemVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointRedemptionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointSummaryVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointTransactionVO;
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

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {
  private static final Set<String> ITEM_STATUSES = Set.of("active", "inactive");
  private static final Set<String> REDEMPTION_STATUSES =
      Set.of("pending", "fulfilled", "cancelled", "rejected");

  private final PointAccountMapper accountMapper;
  private final PointTransactionMapper transactionMapper;
  private final PointRedeemItemMapper itemMapper;
  private final PointRedemptionMapper redemptionMapper;
  private final UserMapper userMapper;

  @Override
  public Result<List<ResponsePointAccountVO>> leaderboard(Integer limit) {
    return Result.success(toAccountVOs(accountMapper.selectOrdered(limit == null ? 50 : limit)));
  }

  @Override
  public Result<ResponsePointSummaryVO> mySummary() {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录后查看积分");
    Integer userId = StpUtil.getLoginIdAsInt();
    PointAccount account = getOrCreateAccount(userId);
    return Result.success(
        ResponsePointSummaryVO.builder()
            .account(toAccountVO(account, userMapper.selectById(userId), rankOf(userId)))
            .recentTransactions(toTransactionVOs(transactionMapper.selectRecent(userId, null, 30)))
            .redemptions(toRedemptionVOs(redemptionMapper.selectByUserId(userId)))
            .build());
  }

  @Override
  public Result<List<ResponsePointRedeemItemVO>> siteItems() {
    return Result.success(itemMapper.selectActive().stream().map(this::toItemVO).toList());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Integer> redeem(Integer itemId, RequestPointRedemptionDTO request) {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录后兑换");
    Integer userId = StpUtil.getLoginIdAsInt();
    PointRedeemItem item = itemMapper.selectById(itemId);
    if (item == null || !"active".equals(item.getStatus())) return Result.error(404, "兑换项不存在或已下架");
    int cost = safe(item.getPointCost());
    if (cost <= 0) return Result.error(400, "兑换积分配置不正确");
    if (!hasStock(item)) return Result.error(400, "库存不足");
    PointAccount account = getOrCreateAccount(userId);
    if (safe(account.getBalance()) < cost) return Result.error(400, "积分余额不足");

    PointRedemption redemption =
        PointRedemption.builder()
            .userId(userId)
            .itemId(itemId)
            .points(cost)
            .status("pending")
            .receiverName(trimToNull(request == null ? null : request.getReceiverName()))
            .receiverContact(trimToNull(request == null ? null : request.getReceiverContact()))
            .remark(trimToNull(request == null ? null : request.getRemark()))
            .build();
    redemptionMapper.insert(redemption);
    item.setExchangedCount(safe(item.getExchangedCount()) + 1);
    itemMapper.updateById(item);
    applyTransaction(
        userId,
        -cost,
        "redemption",
        "point_redemption",
        redemption.getId(),
        "redemption:" + redemption.getId(),
        "兑换：" + item.getName(),
        null);
    return Result.success(redemption.getId(), "兑换申请已提交");
  }

  @Override
  public Result<List<ResponsePointRedemptionVO>> myRedemptions() {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录后查看兑换记录");
    return Result.success(toRedemptionVOs(redemptionMapper.selectByUserId(StpUtil.getLoginIdAsInt())));
  }

  @Override
  public Result<List<ResponsePointAccountVO>> adminAccounts(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return Result.success(toAccountVOs(accountMapper.selectAllOrdered()));
    }
    List<Integer> userIds =
        userMapper.searchByNameKeyword(keyword.trim()).stream().map(User::getId).toList();
    return Result.success(toAccountVOs(accountMapper.selectByUserIds(userIds)));
  }

  @Override
  public Result<List<ResponsePointTransactionVO>> adminTransactions(Integer userId, String type) {
    return Result.success(toTransactionVOs(transactionMapper.selectRecent(userId, type, 300)));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> adjust(RequestPointAdjustmentDTO request) {
    if (request.getDelta() == null || request.getDelta() == 0) return Result.error(400, "调整积分不能为 0");
    if (userMapper.selectById(request.getUserId()) == null) return Result.error(404, "用户不存在");
    applyTransaction(
        request.getUserId(),
        request.getDelta(),
        "manual_adjust",
        "manual",
        null,
        null,
        trimToNull(request.getDescription()) == null ? "后台手动调整" : request.getDescription().trim(),
        currentUserId());
    return Result.success("积分已调整");
  }

  @Override
  public Result<List<ResponsePointRedeemItemVO>> adminItems(Boolean includeInactive) {
    return Result.success(itemMapper.selectAdmin(includeInactive).stream().map(this::toItemVO).toList());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Integer> createItem(RequestPointRedeemItemDTO request) {
    Result<String> validResult = validateItem(request, null);
    if (validResult.getCode() != Result.SUCCESS_CODE) {
      return Result.error(validResult.getCode(), validResult.getMessage());
    }
    PointRedeemItem item =
        PointRedeemItem.builder()
            .name(request.getName().trim())
            .description(trimToNull(request.getDescription()))
            .pointCost(request.getPointCost())
            .stock(normalizeStock(request.getStock()))
            .exchangedCount(0)
            .status(normalizeItemStatus(request.getStatus()))
            .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
            .imageUrl(trimToNull(request.getImageUrl()))
            .createdBy(currentUserId())
            .build();
    itemMapper.insert(item);
    return Result.success(item.getId(), "兑换项已创建");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> updateItem(Integer itemId, RequestPointRedeemItemDTO request) {
    PointRedeemItem item = itemMapper.selectById(itemId);
    if (item == null) return Result.error(404, "兑换项不存在");
    Result<String> validResult = validateItem(request, item);
    if (validResult.getCode() != Result.SUCCESS_CODE) {
      return Result.error(validResult.getCode(), validResult.getMessage());
    }
    item.setName(request.getName().trim());
    item.setDescription(trimToNull(request.getDescription()));
    item.setPointCost(request.getPointCost());
    item.setStock(normalizeStock(request.getStock()));
    item.setStatus(normalizeItemStatus(request.getStatus()));
    item.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    item.setImageUrl(trimToNull(request.getImageUrl()));
    itemMapper.updateById(item);
    return Result.success("兑换项已更新");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deleteItem(Integer itemId) {
    PointRedeemItem item = itemMapper.selectById(itemId);
    if (item == null) return Result.error(404, "兑换项不存在");
    item.setStatus("inactive");
    itemMapper.updateById(item);
    return Result.success("兑换项已下架");
  }

  @Override
  public Result<List<ResponsePointRedemptionVO>> adminRedemptions(String status) {
    return Result.success(toRedemptionVOs(redemptionMapper.selectByStatus(status)));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> updateRedemptionStatus(
      Integer redemptionId, RequestPointRedemptionStatusDTO request) {
    PointRedemption redemption = redemptionMapper.selectById(redemptionId);
    if (redemption == null) return Result.error(404, "兑换记录不存在");
    String nextStatus = request.getStatus().trim();
    if (!REDEMPTION_STATUSES.contains(nextStatus)) return Result.error(400, "兑换状态不合法");
    String oldStatus = redemption.getStatus();
    if (("cancelled".equals(oldStatus) || "rejected".equals(oldStatus)) && !oldStatus.equals(nextStatus)) {
      return Result.error(400, "已结束的兑换记录不能重新流转");
    }
    if (!Objects.equals(oldStatus, nextStatus)) {
      if ("cancelled".equals(nextStatus) || "rejected".equals(nextStatus)) {
        refundRedemption(redemption);
        restoreItemStock(redemption.getItemId());
      }
      redemption.setStatus(nextStatus);
      redemption.setReviewedBy(currentUserId());
      redemption.setReviewedAt(Times.now());
    }
    redemption.setAdminNote(trimToNull(request.getAdminNote()));
    redemptionMapper.updateById(redemption);
    return Result.success("兑换状态已更新");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void grantCheckInPoints(
      Integer userId, CheckInSession session, ClubActivity activity, Integer operatorId) {
    if (userId == null || session == null) return;
    if (safe(session.getCheckinPoints()) > 0) {
      awardBySource(
          userId,
          session.getCheckinPoints(),
          "checkin",
          "checkin_session",
          session.getId(),
          checkInSourceKey(session.getId()),
          "扫码签到：" + session.getTitle(),
          operatorId);
    }
    if (activity != null && safe(activity.getParticipationPoints()) > 0) {
      awardBySource(
          userId,
          activity.getParticipationPoints(),
          "activity",
          "club_activity",
          activity.getId(),
          activitySourceKey(activity.getId()),
          "参加活动：" + activity.getTitle(),
          operatorId);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void revokeCheckInPoints(
      Integer userId, CheckInSession session, ClubActivity activity, Integer operatorId) {
    if (userId == null || session == null) return;
    revokeBySource(
        userId,
        "checkin_revoke",
        "checkin_session",
        session.getId(),
        checkInSourceKey(session.getId()),
        "撤销签到积分：" + session.getTitle(),
        operatorId);
    if (activity != null) {
      revokeBySource(
          userId,
          "activity_revoke",
          "club_activity",
          activity.getId(),
          activitySourceKey(activity.getId()),
          "撤销活动积分：" + activity.getTitle(),
          operatorId);
    }
  }

  private void awardBySource(
      Integer userId,
      Integer points,
      String type,
      String sourceType,
      Integer sourceId,
      String sourceKey,
      String description,
      Integer operatorId) {
    if (points == null || points <= 0) return;
    if (sourceKey != null && safe(transactionMapper.sumDeltaBySourceKey(userId, sourceKey)) > 0) return;
    applyTransaction(userId, points, type, sourceType, sourceId, sourceKey, description, operatorId);
  }

  private void revokeBySource(
      Integer userId,
      String type,
      String sourceType,
      Integer sourceId,
      String sourceKey,
      String description,
      Integer operatorId) {
    if (sourceKey == null) return;
    int net = safe(transactionMapper.sumDeltaBySourceKey(userId, sourceKey));
    if (net <= 0) return;
    applyTransaction(userId, -net, type, sourceType, sourceId, sourceKey, description, operatorId);
  }

  private void refundRedemption(PointRedemption redemption) {
    String sourceKey = "redemption:" + redemption.getId();
    int net = safe(transactionMapper.sumDeltaBySourceKey(redemption.getUserId(), sourceKey));
    if (net >= 0) return;
    applyTransaction(
        redemption.getUserId(),
        -net,
        "redemption_refund",
        "point_redemption",
        redemption.getId(),
        sourceKey,
        "兑换退回：" + redemption.getId(),
        currentUserId());
  }

  private PointTransaction applyTransaction(
      Integer userId,
      Integer delta,
      String type,
      String sourceType,
      Integer sourceId,
      String sourceKey,
      String description,
      Integer operatorId) {
    PointAccount account = getOrCreateAccount(userId);
    int balance = safe(account.getBalance()) + safe(delta);
    account.setBalance(balance);
    updateAccountTotals(account, safe(delta), type);
    accountMapper.updateById(account);
    PointTransaction transaction =
        PointTransaction.builder()
            .userId(userId)
            .delta(delta)
            .balanceAfter(balance)
            .type(type)
            .sourceType(sourceType)
            .sourceId(sourceId)
            .sourceKey(sourceKey)
            .description(description)
            .operatorId(operatorId)
            .build();
    transactionMapper.insert(transaction);
    return transaction;
  }

  private void updateAccountTotals(PointAccount account, int delta, String type) {
    if (delta > 0 && ("checkin".equals(type) || "activity".equals(type) || "manual_adjust".equals(type))) {
      account.setTotalEarned(safe(account.getTotalEarned()) + delta);
      return;
    }
    if (delta < 0 && ("checkin_revoke".equals(type) || "activity_revoke".equals(type))) {
      account.setTotalEarned(Math.max(0, safe(account.getTotalEarned()) + delta));
      return;
    }
    if (delta < 0 && "redemption".equals(type)) {
      account.setTotalSpent(safe(account.getTotalSpent()) - delta);
      return;
    }
    if (delta > 0 && "redemption_refund".equals(type)) {
      account.setTotalSpent(Math.max(0, safe(account.getTotalSpent()) - delta));
    }
  }

  private PointAccount getOrCreateAccount(Integer userId) {
    PointAccount account = accountMapper.selectOneByUserId(userId);
    if (account != null) return account;
    if (userMapper.selectById(userId) == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    try {
      account =
          PointAccount.builder().userId(userId).balance(0).totalEarned(0).totalSpent(0).build();
      accountMapper.insert(account);
    } catch (DuplicateKeyException ignored) {
      account = accountMapper.selectOneByUserId(userId);
    }
    return account == null ? accountMapper.selectOneByUserId(userId) : account;
  }

  private Result<String> validateItem(RequestPointRedeemItemDTO request, PointRedeemItem exists) {
    if (request.getPointCost() == null || request.getPointCost() <= 0) {
      return Result.error(400, "兑换所需积分必须大于 0");
    }
    Integer stock = normalizeStock(request.getStock());
    if (stock != null && stock < safe(exists == null ? 0 : exists.getExchangedCount())) {
      return Result.error(400, "库存不能小于已兑换数量");
    }
    String status = normalizeItemStatus(request.getStatus());
    if (!ITEM_STATUSES.contains(status)) return Result.error(400, "兑换项状态不合法");
    return Result.success();
  }

  private void restoreItemStock(Integer itemId) {
    PointRedeemItem item = itemMapper.selectById(itemId);
    if (item == null) return;
    item.setExchangedCount(Math.max(0, safe(item.getExchangedCount()) - 1));
    itemMapper.updateById(item);
  }

  private boolean hasStock(PointRedeemItem item) {
    return item.getStock() == null || safe(item.getStock()) > safe(item.getExchangedCount());
  }

  private List<ResponsePointAccountVO> toAccountVOs(List<PointAccount> accounts) {
    Map<Integer, User> users =
        loadUsers(accounts.stream().map(PointAccount::getUserId).filter(Objects::nonNull).toList());
    List<PointAccount> ordered =
        accounts.stream()
            .sorted(
                (a, b) -> {
                  int balanceCompare = Integer.compare(safe(b.getBalance()), safe(a.getBalance()));
                  if (balanceCompare != 0) return balanceCompare;
                  int earnedCompare = Integer.compare(safe(b.getTotalEarned()), safe(a.getTotalEarned()));
                  if (earnedCompare != 0) return earnedCompare;
                  return Integer.compare(safe(a.getUserId()), safe(b.getUserId()));
                })
            .toList();
    return java.util.stream.IntStream.range(0, ordered.size())
        .mapToObj(index -> toAccountVO(ordered.get(index), users.get(ordered.get(index).getUserId()), index + 1))
        .toList();
  }

  private ResponsePointAccountVO toAccountVO(PointAccount account, User user, Integer rank) {
    return ResponsePointAccountVO.builder()
        .userId(account.getUserId())
        .userName(user == null ? null : user.getUserName())
        .realName(user == null ? null : user.getRealName())
        .studentId(user == null ? null : user.getStudentId())
        .className(user == null ? null : user.getClassName())
        .balance(safe(account.getBalance()))
        .totalEarned(safe(account.getTotalEarned()))
        .totalSpent(safe(account.getTotalSpent()))
        .rank(rank)
        .updatedAt(account.getUpdatedAt())
        .build();
  }

  private List<ResponsePointTransactionVO> toTransactionVOs(List<PointTransaction> transactions) {
    List<Integer> userIds =
        transactions.stream()
            .flatMap(t -> java.util.stream.Stream.of(t.getUserId(), t.getOperatorId()))
            .filter(Objects::nonNull)
            .toList();
    Map<Integer, User> users = loadUsers(userIds);
    return transactions.stream()
        .map(
            item -> {
              User user = users.get(item.getUserId());
              User operator = users.get(item.getOperatorId());
              return ResponsePointTransactionVO.builder()
                  .id(item.getId())
                  .userId(item.getUserId())
                  .userName(user == null ? null : user.getUserName())
                  .realName(user == null ? null : user.getRealName())
                  .studentId(user == null ? null : user.getStudentId())
                  .delta(item.getDelta())
                  .balanceAfter(item.getBalanceAfter())
                  .type(item.getType())
                  .sourceType(item.getSourceType())
                  .sourceId(item.getSourceId())
                  .sourceKey(item.getSourceKey())
                  .description(item.getDescription())
                  .operatorId(item.getOperatorId())
                  .operatorName(operator == null ? null : operator.getRealName())
                  .createdAt(item.getCreatedAt())
                  .build();
            })
        .toList();
  }

  private ResponsePointRedeemItemVO toItemVO(PointRedeemItem item) {
    Integer available =
        item.getStock() == null ? null : Math.max(0, safe(item.getStock()) - safe(item.getExchangedCount()));
    return ResponsePointRedeemItemVO.builder()
        .id(item.getId())
        .name(item.getName())
        .description(item.getDescription())
        .pointCost(item.getPointCost())
        .stock(item.getStock())
        .exchangedCount(safe(item.getExchangedCount()))
        .availableStock(available)
        .status(item.getStatus())
        .sortOrder(item.getSortOrder())
        .imageUrl(item.getImageUrl())
        .createdAt(item.getCreatedAt())
        .updatedAt(item.getUpdatedAt())
        .build();
  }

  private List<ResponsePointRedemptionVO> toRedemptionVOs(List<PointRedemption> redemptions) {
    Map<Integer, User> users =
        loadUsers(
            redemptions.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getUserId(), r.getReviewedBy()))
                .filter(Objects::nonNull)
                .toList());
    Map<Integer, PointRedeemItem> items =
        loadItems(redemptions.stream().map(PointRedemption::getItemId).filter(Objects::nonNull).toList());
    return redemptions.stream()
        .map(
            item -> {
              User user = users.get(item.getUserId());
              User reviewer = users.get(item.getReviewedBy());
              PointRedeemItem redeemItem = items.get(item.getItemId());
              return ResponsePointRedemptionVO.builder()
                  .id(item.getId())
                  .userId(item.getUserId())
                  .userName(user == null ? null : user.getUserName())
                  .realName(user == null ? null : user.getRealName())
                  .studentId(user == null ? null : user.getStudentId())
                  .itemId(item.getItemId())
                  .itemName(redeemItem == null ? null : redeemItem.getName())
                  .points(item.getPoints())
                  .status(item.getStatus())
                  .receiverName(item.getReceiverName())
                  .receiverContact(item.getReceiverContact())
                  .remark(item.getRemark())
                  .adminNote(item.getAdminNote())
                  .reviewedBy(item.getReviewedBy())
                  .reviewerName(reviewer == null ? null : reviewer.getRealName())
                  .reviewedAt(item.getReviewedAt())
                  .createdAt(item.getCreatedAt())
                  .build();
            })
        .toList();
  }

  private Map<Integer, User> loadUsers(List<Integer> ids) {
    List<Integer> distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new)).stream().toList();
    if (distinct.isEmpty()) return Map.of();
    return userMapper.selectBatchIds(distinct).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
  }

  private Map<Integer, PointRedeemItem> loadItems(List<Integer> ids) {
    List<Integer> distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new)).stream().toList();
    if (distinct.isEmpty()) return Map.of();
    return itemMapper.selectBatchIds(distinct).stream().collect(Collectors.toMap(PointRedeemItem::getId, Function.identity(), (a, b) -> a));
  }

  private Integer rankOf(Integer userId) {
    List<PointAccount> ordered = accountMapper.selectAllOrdered();
    for (int i = 0; i < ordered.size(); i++) {
      if (Objects.equals(ordered.get(i).getUserId(), userId)) return i + 1;
    }
    return ordered.size() + 1;
  }

  private Integer currentUserId() {
    return StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
  }

  private String checkInSourceKey(Integer sessionId) {
    return "checkin:" + sessionId;
  }

  private String activitySourceKey(Integer activityId) {
    return "activity:" + activityId;
  }

  private String normalizeItemStatus(String status) {
    return status == null || status.isBlank() ? "active" : status.trim();
  }

  private Integer normalizeStock(Integer stock) {
    return stock == null || stock < 0 ? null : stock;
  }

  private int safe(Integer value) {
    return value == null ? 0 : value;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
