package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveMemberProfileDTO;
import edu.jmi.openatom.server.openatomsystem.entity.BlogArticle;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPosition;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfile;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfileModule;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfileSocialLink;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.BlogArticleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMembershipMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubPositionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MemberProfileMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MemberProfileModuleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MemberProfileSocialLinkMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.MemberProfileService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseImageUploadVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberCardVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberFilterVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberProfileVO;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 成员名片、公开主页和主页编辑器业务。公开响应只由专用 VO 组装。 */
@Service
@RequiredArgsConstructor
public class MemberProfileServiceImpl implements MemberProfileService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";
  private static final Set<String> MEMBER_STATUSES = Set.of("probation", "active");
  private static final Set<String> THEMES = Set.of("minimal", "tech", "warm", "editorial");
  private static final Set<String> COLOR_MODES = Set.of("light", "dark", "system");
  private static final Set<String> VISIBILITIES = Set.of("members", "unlisted", "private");
  private static final Set<String> MODULE_VISIBILITIES = Set.of("members", "private");
  private static final Set<String> MODULE_KEYS =
      Set.of(
          "about",
          "hero_statement",
          "now",
          "skills",
          "tech_stack",
          "blog_latest",
          "projects",
          "featured_work",
          "timeline",
          "club_experience",
          "awards",
          "stats",
          "gallery",
          "quote",
          "links",
          "contact_cta",
          "markdown");
  private static final Set<String> SOCIAL_PLATFORMS =
      Set.of("website", "github", "gitee", "bilibili", "zhihu", "weibo", "other");
  private static final int MAX_MODULES = 20;
  private static final int MAX_SOCIAL_LINKS = 10;

  private final MemberProfileMapper memberProfileMapper;
  private final MemberProfileModuleMapper memberProfileModuleMapper;
  private final MemberProfileSocialLinkMapper memberProfileSocialLinkMapper;
  private final UserMapper userMapper;
  private final ClubMapper clubMapper;
  private final ClubMembershipMapper clubMembershipMapper;
  private final ClubDepartmentMapper clubDepartmentMapper;
  private final ClubPositionMapper clubPositionMapper;
  private final BlogArticleMapper blogArticleMapper;
  private final ImageHostingStorageServiceImpl imageHostingStorageService;

  @Override
  public Result<PageDataVO<ResponseMemberCardVO>> members(
      String keyword, Integer departmentId, String skill, Long page, Long pageSize) {
    Result<MemberContext> access = requireMember();
    if (access.getCode() != Result.SUCCESS_CODE) return copyError(access);
    List<Integer> userIds = null;
    if (keyword != null && !keyword.isBlank()) {
      java.util.LinkedHashSet<Integer> matches =
          userMapper.searchByNameKeyword(keyword.trim()).stream()
              .map(User::getId)
              .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
      memberProfileMapper.searchPublished(keyword).stream()
          .map(MemberProfile::getUserId)
          .forEach(matches::add);
      userIds = new ArrayList<>(matches);
    }
    if (skill != null && !skill.isBlank()) {
      Set<Integer> skillUsers =
          memberProfileMapper.searchPublished(skill.trim()).stream()
              .filter(profile -> Jsons.parseStringList(profile.getSkills()).contains(skill.trim()))
              .map(MemberProfile::getUserId)
              .collect(Collectors.toSet());
      if (userIds == null) userIds = new ArrayList<>(skillUsers);
      else userIds = userIds.stream().filter(skillUsers::contains).toList();
    }
    Page<ClubMembership> membershipPage =
        clubMembershipMapper.selectVisibleMemberPage(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            access.getData().club().getId(),
            departmentId,
            userIds);
    List<ClubMembership> memberships = membershipPage.getRecords();
    Map<Integer, User> users = users(memberships.stream().map(ClubMembership::getUserId).toList());
    Map<Integer, MemberProfile> profiles =
        memberProfileMapper.selectByUserIds(new ArrayList<>(users.keySet())).stream()
            .collect(Collectors.toMap(MemberProfile::getUserId, Function.identity()));
    Map<Integer, ClubDepartment> departments = departments(access.getData().club().getId());
    Map<Integer, ClubPosition> positions = positions(access.getData().club().getId());
    List<ResponseMemberCardVO> cards =
        memberships.stream()
            .map(
                membership ->
                    toCard(
                        users.get(membership.getUserId()),
                        membership,
                        profiles.get(membership.getUserId()),
                        departments,
                        positions))
            .filter(Objects::nonNull)
            .toList();
    return Result.success(
        PageDataVO.<ResponseMemberCardVO>builder()
            .list(cards)
            .page(membershipPage.getCurrent())
            .pageSize(membershipPage.getSize())
            .total(membershipPage.getTotal())
            .build());
  }

  @Override
  public Result<ResponseMemberFilterVO> filters() {
    Result<MemberContext> access = requireMember();
    if (access.getCode() != Result.SUCCESS_CODE) return copyError(access);
    List<ResponseMemberFilterVO.DepartmentOption> departmentOptions =
        clubDepartmentMapper.selectByClubIdOrdered(access.getData().club().getId()).stream()
            .map(
                department ->
                    ResponseMemberFilterVO.DepartmentOption.builder()
                        .id(department.getId())
                        .name(department.getName())
                        .build())
            .toList();
    List<String> skills =
        memberProfileMapper.searchPublished(null).stream()
            .flatMap(profile -> Jsons.parseStringList(profile.getSkills()).stream())
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .sorted()
            .limit(100)
            .toList();
    return Result.success(
        ResponseMemberFilterVO.builder().departments(departmentOptions).skills(skills).build());
  }

  @Override
  public Result<ResponseMemberProfileVO> detail(String slug) {
    Result<MemberContext> access = requireMember();
    if (access.getCode() != Result.SUCCESS_CODE) return copyError(access);
    String normalizedSlug = trimToNull(slug);
    if (normalizedSlug == null) return Result.error(404, "成员主页不存在");
    MemberProfile profile = memberProfileMapper.selectBySlug(normalizedSlug);
    Integer targetUserId = profile == null ? parseDefaultSlug(normalizedSlug) : profile.getUserId();
    User user = targetUserId == null ? null : userMapper.selectById(targetUserId);
    if (user == null) return Result.error(404, "成员主页不存在");
    if (profile == null) profile = memberProfileMapper.selectByUserId(user.getId());
    ClubMembership membership =
        clubMembershipMapper.selectActiveMembership(user.getId(), access.getData().club().getId());
    if (!isVisibleMembership(membership)) return Result.error(404, "成员主页不存在");
    boolean owner = Objects.equals(access.getData().userId(), user.getId());
    boolean customized = isProfileVisible(profile, owner, true);
    return Result.success(toProfile(user, membership, customized ? profile : null, owner, customized));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseMemberProfileVO> mine() {
    Result<MemberContext> access = requireMember();
    if (access.getCode() != Result.SUCCESS_CODE) return copyError(access);
    MemberProfile profile = ensureProfile(access.getData().userId());
    ClubMembership membership =
        clubMembershipMapper.selectActiveMembership(
            access.getData().userId(), access.getData().club().getId());
    User user = userMapper.selectById(access.getData().userId());
    return Result.success(toProfile(user, membership, profile, true, true));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseMemberProfileVO> save(RequestSaveMemberProfileDTO request) {
    if (request == null) return Result.error(400, "主页内容不能为空");
    Result<MemberContext> access = requireMember();
    if (access.getCode() != Result.SUCCESS_CODE) return copyError(access);
    MemberProfile profile = ensureProfile(access.getData().userId());
    Integer expectedVersion = request.getVersion();
    if (expectedVersion == null || !Objects.equals(expectedVersion, profile.getVersion())) {
      return Result.error(409, "主页已在其他窗口更新，请刷新后重试");
    }
    String validationError = validateRequest(request, profile);
    if (validationError != null) return Result.error(400, validationError);

    profile.setSlug(normalizeSlug(request.getSlug()));
    profile.setDisplayName(limit(trimToNull(request.getDisplayName()), 80));
    profile.setHeadline(limit(trimToNull(request.getHeadline()), 160));
    profile.setBio(limit(trimToNull(request.getBio()), 4000));
    profile.setAvatarUrl(safeImageUrl(request.getAvatarUrl()));
    profile.setBannerUrl(safeImageUrl(request.getBannerUrl()));
    profile.setCardBackgroundUrl(safeImageUrl(request.getCardBackgroundUrl()));
    profile.setCardFocusX(normalizeFocus(request.getCardFocusX()));
    profile.setCardFocusY(normalizeFocus(request.getCardFocusY()));
    profile.setThemeKey(normalizeChoice(request.getThemeKey(), THEMES, "minimal"));
    profile.setColorMode(normalizeChoice(request.getColorMode(), COLOR_MODES, "system"));
    profile.setVisibility(normalizeChoice(request.getVisibility(), VISIBILITIES, "members"));
    profile.setShowDepartment(!Boolean.FALSE.equals(request.getShowDepartment()));
    profile.setShowPosition(!Boolean.FALSE.equals(request.getShowPosition()));
    profile.setSkills(Jsons.stringify(normalizeSkills(request.getSkills())));
    profile.setStatus("draft");
    profile.setVersion(expectedVersion + 1);
    profile.setUpdatedAt(Times.now());
    if (memberProfileMapper.updateWithVersion(profile, expectedVersion) <= 0) {
      return Result.error(409, "主页已在其他窗口更新，请刷新后重试");
    }
    replaceModules(profile.getId(), request.getModules());
    replaceSocialLinks(profile.getId(), request.getSocialLinks());
    return Result.success(currentOwnProfile(access.getData(), profile), "草稿已保存");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseMemberProfileVO> publish() {
    Result<MemberContext> access = requireMember();
    if (access.getCode() != Result.SUCCESS_CODE) return copyError(access);
    MemberProfile profile = ensureProfile(access.getData().userId());
    if (trimToNull(profile.getDisplayName()) == null) return Result.error(400, "请先填写展示名称");
    profile.setStatus("published");
    profile.setPublishedAt(Times.now());
    profile.setVersion(safeVersion(profile) + 1);
    profile.setUpdatedAt(Times.now());
    memberProfileMapper.updateById(profile);
    return Result.success(currentOwnProfile(access.getData(), profile), "主页已发布");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseMemberProfileVO> unpublish() {
    Result<MemberContext> access = requireMember();
    if (access.getCode() != Result.SUCCESS_CODE) return copyError(access);
    MemberProfile profile = ensureProfile(access.getData().userId());
    profile.setStatus("draft");
    profile.setVersion(safeVersion(profile) + 1);
    profile.setUpdatedAt(Times.now());
    memberProfileMapper.updateById(profile);
    return Result.success(currentOwnProfile(access.getData(), profile), "主页已转为草稿");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseImageUploadVO> upload(
      String kind, MultipartFile file, String baseUrl) {
    Result<MemberContext> access = requireMember();
    if (access.getCode() != Result.SUCCESS_CODE) return copyError(access);
    if (!Set.of("avatar", "banner", "card-background").contains(kind)) {
      return Result.error(400, "图片用途不合法");
    }
    if (file == null || file.isEmpty()) return Result.error(400, "请选择图片");
    if (file.getSize() > 8L * 1024 * 1024) return Result.error(400, "主页图片不能超过 8MB");
    try {
      ResponseImageUploadVO uploaded =
          imageHostingStorageService.upload(file, baseUrl, access.getData().userId());
      MemberProfile profile = ensureProfile(access.getData().userId());
      if ("avatar".equals(kind)) profile.setAvatarUrl(uploaded.getUrl());
      if ("banner".equals(kind)) profile.setBannerUrl(uploaded.getUrl());
      if ("card-background".equals(kind)) profile.setCardBackgroundUrl(uploaded.getUrl());
      profile.setStatus("draft");
      profile.setVersion(safeVersion(profile) + 1);
      profile.setUpdatedAt(Times.now());
      memberProfileMapper.updateById(profile);
      return Result.success(uploaded, "图片上传成功，发布后对其他成员可见");
    } catch (IOException | IllegalArgumentException e) {
      return Result.error(400, e.getMessage());
    }
  }

  private ResponseMemberCardVO toCard(
      User user,
      ClubMembership membership,
      MemberProfile profile,
      Map<Integer, ClubDepartment> departments,
      Map<Integer, ClubPosition> positions) {
    if (user == null) return null;
    boolean customized = isProfileVisible(profile, false, false);
    MemberProfile visible = customized ? profile : null;
    return ResponseMemberCardVO.builder()
        .slug(visible == null ? defaultSlug(user.getId()) : visible.getSlug())
        .displayName(displayName(user, visible))
        .headline(visible == null ? "开放原子开源社团成员" : visible.getHeadline())
        .avatarUrl(visible != null && visible.getAvatarUrl() != null ? visible.getAvatarUrl() : user.getAvatar())
        .cardBackgroundUrl(visible == null ? null : visible.getCardBackgroundUrl())
        .cardFocusX(visible == null ? new BigDecimal("50.00") : visible.getCardFocusX())
        .cardFocusY(visible == null ? new BigDecimal("50.00") : visible.getCardFocusY())
        .departmentName(
            visible == null || !Boolean.FALSE.equals(visible.getShowDepartment())
                ? nameOf(departments.get(membership.getDepartmentId()))
                : null)
        .positionName(
            visible == null || !Boolean.FALSE.equals(visible.getShowPosition())
                ? nameOf(positions.get(membership.getPositionId()))
                : null)
        .skills(visible == null ? List.of() : Jsons.parseStringList(visible.getSkills()))
        .articleCount(blogArticleMapper.countPublishedByAuthor(user.getId()))
        .customized(customized)
        .build();
  }

  private ResponseMemberProfileVO toProfile(
      User user, ClubMembership membership, MemberProfile profile, boolean owner, boolean customized) {
    Club club = clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
    Map<Integer, ClubDepartment> departments = departments(club.getId());
    Map<Integer, ClubPosition> positions = positions(club.getId());
    boolean showDepartment = profile == null || !Boolean.FALSE.equals(profile.getShowDepartment());
    boolean showPosition = profile == null || !Boolean.FALSE.equals(profile.getShowPosition());
    return ResponseMemberProfileVO.builder()
        .slug(profile == null ? defaultSlug(user.getId()) : profile.getSlug())
        .displayName(displayName(user, profile))
        .headline(profile == null ? "开放原子开源社团成员" : profile.getHeadline())
        .bio(profile == null ? null : profile.getBio())
        .avatarUrl(profile != null && profile.getAvatarUrl() != null ? profile.getAvatarUrl() : user.getAvatar())
        .bannerUrl(profile == null ? null : profile.getBannerUrl())
        .cardBackgroundUrl(profile == null ? null : profile.getCardBackgroundUrl())
        .cardFocusX(profile == null ? new BigDecimal("50.00") : profile.getCardFocusX())
        .cardFocusY(profile == null ? new BigDecimal("50.00") : profile.getCardFocusY())
        .departmentName(
            showDepartment && membership != null
                ? nameOf(departments.get(membership.getDepartmentId()))
                : null)
        .positionName(
            showPosition && membership != null
                ? nameOf(positions.get(membership.getPositionId()))
                : null)
        .skills(profile == null ? List.of() : Jsons.parseStringList(profile.getSkills()))
        .themeKey(profile == null ? "minimal" : profile.getThemeKey())
        .colorMode(profile == null ? "system" : profile.getColorMode())
        .visibility(profile == null ? "members" : profile.getVisibility())
        .status(profile == null ? "default" : profile.getStatus())
        .showDepartment(showDepartment)
        .showPosition(showPosition)
        .version(profile == null ? 0 : profile.getVersion())
        .owner(owner)
        .customized(customized)
        .publishedAt(profile == null ? null : profile.getPublishedAt())
        .updatedAt(profile == null ? null : profile.getUpdatedAt())
        .modules(profile == null ? defaultModules(user.getId()) : modules(profile, owner))
        .socialLinks(profile == null ? List.of() : socialLinks(profile, owner))
        .build();
  }

  private List<ResponseMemberProfileVO.ModuleVO> modules(MemberProfile profile, boolean owner) {
    return memberProfileModuleMapper.selectByProfileId(profile.getId()).stream()
        .filter(module -> owner || Boolean.TRUE.equals(module.getEnabled()))
        .filter(module -> owner || !"private".equals(module.getVisibility()))
        .map(
            module -> {
              Map<String, Object> data = new LinkedHashMap<>(Jsons.parseObject(module.getConfigJson()));
              if ("blog_latest".equals(module.getModuleKey())) {
                data.put("articles", latestArticles(profile.getUserId()));
              }
              return ResponseMemberProfileVO.ModuleVO.builder()
                  .id(module.getId())
                  .key(module.getModuleKey())
                  .title(module.getTitle())
                  .sortOrder(module.getSortOrder())
                  .columnSpan(module.getColumnSpan())
                  .enabled(module.getEnabled())
                  .visibility(module.getVisibility())
                  .data(data)
                  .build();
            })
        .toList();
  }

  private List<ResponseMemberProfileVO.ModuleVO> defaultModules(Integer userId) {
    Map<String, Object> articleData = new LinkedHashMap<>();
    articleData.put("articles", latestArticles(userId));
    return List.of(
        ResponseMemberProfileVO.ModuleVO.builder()
            .key("about")
            .title("关于我")
            .sortOrder(0)
            .columnSpan(12)
            .enabled(true)
            .visibility("members")
            .data(Map.of())
            .build(),
        ResponseMemberProfileVO.ModuleVO.builder()
            .key("blog_latest")
            .title("最近文章")
            .sortOrder(10)
            .columnSpan(12)
            .enabled(true)
            .visibility("members")
            .data(articleData)
            .build());
  }

  private List<Map<String, Object>> latestArticles(Integer authorId) {
    Page<BlogArticle> page =
        blogArticleMapper.selectPageByConditions(
            new Page<>(1, 3), null, null, null, null, authorId, null, true);
    return page.getRecords().stream()
        .map(
            article -> {
              Map<String, Object> item = new LinkedHashMap<>();
              item.put("id", article.getId());
              item.put("title", article.getTitle());
              item.put("summary", article.getSummary());
              item.put("coverUrl", article.getCoverUrl());
              item.put("publishedAt", article.getPublishedAt());
              return item;
            })
        .toList();
  }

  private List<ResponseMemberProfileVO.SocialLinkVO> socialLinks(
      MemberProfile profile, boolean owner) {
    return memberProfileSocialLinkMapper.selectByProfileId(profile.getId()).stream()
        .filter(link -> owner || Boolean.TRUE.equals(link.getEnabled()))
        .map(
            link ->
                ResponseMemberProfileVO.SocialLinkVO.builder()
                    .id(link.getId())
                    .platform(link.getPlatform())
                    .label(link.getLabel())
                    .url(link.getUrl())
                    .sortOrder(link.getSortOrder())
                    .enabled(link.getEnabled())
                    .build())
        .toList();
  }

  private void replaceModules(
      Long profileId, List<RequestSaveMemberProfileDTO.ModuleItem> requested) {
    memberProfileModuleMapper.deleteByProfileId(profileId);
    if (requested == null) return;
    for (int i = 0; i < requested.size(); i++) {
      RequestSaveMemberProfileDTO.ModuleItem item = requested.get(i);
      memberProfileModuleMapper.insert(
          MemberProfileModule.builder()
              .profileId(profileId)
              .moduleKey(item.getKey())
              .title(limit(trimToNull(item.getTitle()), 80))
              .sortOrder(item.getSortOrder() == null ? i * 10 : item.getSortOrder())
              .columnSpan(normalizeSpan(item.getColumnSpan()))
              .enabled(!Boolean.FALSE.equals(item.getEnabled()))
              .visibility(
                  normalizeChoice(item.getVisibility(), MODULE_VISIBILITIES, "members"))
              .configJson(Jsons.stringify(item.getConfig() == null ? Map.of() : item.getConfig()))
              .build());
    }
  }

  private void replaceSocialLinks(
      Long profileId, List<RequestSaveMemberProfileDTO.SocialLinkItem> requested) {
    memberProfileSocialLinkMapper.deleteByProfileId(profileId);
    if (requested == null) return;
    for (int i = 0; i < requested.size(); i++) {
      RequestSaveMemberProfileDTO.SocialLinkItem item = requested.get(i);
      memberProfileSocialLinkMapper.insert(
          MemberProfileSocialLink.builder()
              .profileId(profileId)
              .platform(normalizeChoice(item.getPlatform(), SOCIAL_PLATFORMS, "other"))
              .label(limit(trimToNull(item.getLabel()), 60))
              .url(item.getUrl().trim())
              .sortOrder(item.getSortOrder() == null ? i * 10 : item.getSortOrder())
              .enabled(!Boolean.FALSE.equals(item.getEnabled()))
              .build());
    }
  }

  private String validateRequest(RequestSaveMemberProfileDTO request, MemberProfile profile) {
    String slug = normalizeSlug(request.getSlug());
    if (slug == null || !slug.matches("[a-z0-9][a-z0-9-]{2,63}")) {
      return "主页地址需为3至64位小写字母、数字或连字符";
    }
    if (memberProfileMapper.countBySlug(slug, profile.getId()) > 0) return "该主页地址已被使用";
    if (trimToNull(request.getDisplayName()) == null) return "展示名称不能为空";
    if (request.getModules() != null && request.getModules().size() > MAX_MODULES) {
      return "主页组件不能超过" + MAX_MODULES + "个";
    }
    if (request.getSocialLinks() != null && request.getSocialLinks().size() > MAX_SOCIAL_LINKS) {
      return "公开链接不能超过" + MAX_SOCIAL_LINKS + "个";
    }
    Set<String> seen = new java.util.HashSet<>();
    if (request.getModules() != null) {
      for (RequestSaveMemberProfileDTO.ModuleItem item : request.getModules()) {
        if (item == null || !MODULE_KEYS.contains(item.getKey())) return "包含不支持的主页组件";
        if (!seen.add(item.getKey())) return "同一种主页组件只能添加一次";
        String serialized = Jsons.stringify(item.getConfig());
        if (serialized != null && serialized.length() > 20000) return "单个组件内容过长";
      }
    }
    if (request.getSocialLinks() != null) {
      for (RequestSaveMemberProfileDTO.SocialLinkItem item : request.getSocialLinks()) {
        if (item == null || !isSafeExternalUrl(item.getUrl())) return "公开链接必须是 http 或 https 地址";
      }
    }
    for (String url : List.of(nullToEmpty(request.getAvatarUrl()), nullToEmpty(request.getBannerUrl()), nullToEmpty(request.getCardBackgroundUrl()))) {
      if (!url.isEmpty() && !isSafeExternalUrl(url) && !url.startsWith("/files/images/")) {
        return "图片地址格式不正确";
      }
    }
    return null;
  }

  private MemberProfile ensureProfile(Integer userId) {
    MemberProfile existing = memberProfileMapper.selectByUserId(userId);
    if (existing != null) return existing;
    User user = userMapper.selectById(userId);
    MemberProfile profile =
        MemberProfile.builder()
            .userId(userId)
            .slug(defaultSlug(userId))
            .displayName(displayName(user, null))
            .headline("开放原子开源社团成员")
            .avatarUrl(user == null ? null : user.getAvatar())
            .cardFocusX(new BigDecimal("50.00"))
            .cardFocusY(new BigDecimal("50.00"))
            .themeKey("minimal")
            .colorMode("system")
            .visibility("members")
            .status("draft")
            .showDepartment(true)
            .showPosition(true)
            .skills("[]")
            .version(0)
            .build();
    memberProfileMapper.insert(profile);
    replaceModules(
        profile.getId(),
        List.of(defaultModule("about", "关于我", 0), defaultModule("blog_latest", "最近文章", 10)));
    return profile;
  }

  private RequestSaveMemberProfileDTO.ModuleItem defaultModule(
      String key, String title, int order) {
    RequestSaveMemberProfileDTO.ModuleItem item = new RequestSaveMemberProfileDTO.ModuleItem();
    item.setKey(key);
    item.setTitle(title);
    item.setSortOrder(order);
    item.setColumnSpan(12);
    item.setEnabled(true);
    item.setVisibility("members");
    item.setConfig(Map.of());
    return item;
  }

  private Result<MemberContext> requireMember() {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    Integer userId = StpUtil.getLoginIdAsInt();
    Club club = clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
    if (club == null) return Result.error(503, "默认社团尚未配置");
    if (!StpUtil.hasRole("super_admin")) {
      ClubMembership membership = clubMembershipMapper.selectActiveMembership(userId, club.getId());
      if (!isVisibleMembership(membership)) return Result.error(403, "仅社团成员可以访问成员主页");
    }
    return Result.success(new MemberContext(userId, club));
  }

  private ResponseMemberProfileVO currentOwnProfile(MemberContext context, MemberProfile profile) {
    User user = userMapper.selectById(context.userId());
    ClubMembership membership =
        clubMembershipMapper.selectActiveMembership(context.userId(), context.club().getId());
    return toProfile(user, membership, profile, true, true);
  }

  private boolean isProfileVisible(MemberProfile profile, boolean owner, boolean direct) {
    if (profile == null) return false;
    if (owner) return true;
    if (!"published".equals(profile.getStatus()) || "private".equals(profile.getVisibility())) {
      return false;
    }
    return direct || "members".equals(profile.getVisibility());
  }

  private boolean isVisibleMembership(ClubMembership membership) {
    return membership != null
        && membership.getLeftAt() == null
        && MEMBER_STATUSES.contains(membership.getStatus());
  }

  private Map<Integer, User> users(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return Map.of();
    return userMapper.selectBatchIds(ids.stream().distinct().toList()).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
  }

  private Map<Integer, ClubDepartment> departments(Integer clubId) {
    return clubDepartmentMapper.selectByClubIdOrdered(clubId).stream()
        .collect(Collectors.toMap(ClubDepartment::getId, Function.identity()));
  }

  private Map<Integer, ClubPosition> positions(Integer clubId) {
    return clubPositionMapper.selectByClubIdOrdered(clubId).stream()
        .collect(Collectors.toMap(ClubPosition::getId, Function.identity()));
  }

  private String displayName(User user, MemberProfile profile) {
    if (profile != null && trimToNull(profile.getDisplayName()) != null) return profile.getDisplayName();
    if (user == null) return "社团成员";
    if (trimToNull(user.getRealName()) != null) return user.getRealName();
    if (trimToNull(user.getUserName()) != null) return user.getUserName();
    return "社团成员";
  }

  private String nameOf(ClubDepartment department) {
    return department == null ? null : department.getName();
  }

  private String nameOf(ClubPosition position) {
    return position == null ? null : position.getName();
  }

  private String defaultSlug(Integer userId) {
    return "member-" + userId;
  }

  private Integer parseDefaultSlug(String slug) {
    if (!slug.matches("member-[1-9][0-9]*")) return null;
    try {
      return Integer.valueOf(slug.substring(7));
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private String normalizeSlug(String value) {
    String normalized = trimToNull(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private List<String> normalizeSkills(List<String> skills) {
    if (skills == null) return List.of();
    return skills.stream()
        .map(this::trimToNull)
        .filter(Objects::nonNull)
        .map(value -> limit(value, 30))
        .distinct()
        .limit(20)
        .toList();
  }

  private BigDecimal normalizeFocus(BigDecimal value) {
    if (value == null) return new BigDecimal("50.00");
    return value.max(BigDecimal.ZERO).min(new BigDecimal("100.00"));
  }

  private int normalizeSpan(Integer span) {
    return span != null && span <= 6 ? 6 : 12;
  }

  private int safeVersion(MemberProfile profile) {
    return profile.getVersion() == null ? 0 : profile.getVersion();
  }

  private String safeImageUrl(String value) {
    return trimToNull(value);
  }

  private boolean isSafeExternalUrl(String value) {
    String normalized = trimToNull(value);
    if (normalized == null) return false;
    try {
      URI uri = new URI(normalized);
      return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          && uri.getHost() != null;
    } catch (URISyntaxException ignored) {
      return false;
    }
  }

  private String normalizeChoice(String value, Set<String> allowed, String fallback) {
    String normalized = trimToNull(value);
    return normalized != null && allowed.contains(normalized) ? normalized : fallback;
  }

  private String limit(String value, int length) {
    if (value == null || value.length() <= length) return value;
    return value.substring(0, length);
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private <T> Result<T> copyError(Result<?> result) {
    return Result.error(result.getCode(), result.getMessage());
  }

  private record MemberContext(Integer userId, Club club) {}
}
