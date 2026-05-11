package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.bootstrap.RoleSeedTemplate;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateUserDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestResetPasswordDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateUserStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUserUpdateDTO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMembershipVO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import edu.jmi.openatom.server.openatomsystem.service.UserService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理实现类
 *
 * <p>负责用户的创建, 更新, 删除, 分页查询, Excel导入导出, 密码重置以及用户社团信息查询等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";

  private final UserMapper userMapper;
  private final RoleMapper roleMapper;
  private final UserRoleMapper userRoleMapper;
  private final ClubMapper clubMapper;
  private final ClubDepartmentMapper clubDepartmentMapper;
  private final ClubMembershipMapper clubMembershipMapper;
  private final MembershipApplicationMapper membershipApplicationMapper;
  private final ActivityRegistrationMapper activityRegistrationMapper;
  private final FormSubmissionMapper formSubmissionMapper;
  private final PasswordService passwordService;

  @Override
  public Result<PageDataVO<User>> getUsers(
      String keyword, UserStatus status, Integer clubId, Long page, Long pageSize) {
    long current = PageRequests.page(page);
    long size = PageRequests.pageSize(pageSize);
    List<Integer> userIds = null;
    if (clubId != null) {
      userIds = clubMembershipMapper.selectByClubId(clubId).stream()
          .map(ClubMembership::getUserId).distinct().toList();
      if (userIds.isEmpty()) {
        return Result.success(PageDataVO.<User>builder().list(List.of())
            .page(current).pageSize(size).total(0L).build());
      }
    }
    Page<User> userPage = userMapper.selectPageByConditions(new Page<>(current, size), keyword, status, userIds);
    List<User> users = userPage.getRecords().stream().map(this::buildSafeUser).toList();
    return Result.success(PageDataVO.<User>builder().list(users)
        .page(userPage.getCurrent()).pageSize(userPage.getSize()).total(userPage.getTotal()).build());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> createUser(RequestCreateUserDTO requestCreateUserDTO) {
    if (requestCreateUserDTO == null) return Result.error("请求参数为空");
    if (isBlank(requestCreateUserDTO.getUsername()) || isBlank(requestCreateUserDTO.getPassword())
        || isBlank(requestCreateUserDTO.getRealName())) {
      return Result.error(400, "用户名、密码、真实姓名不能为空");
    }
    String studentId = isBlank(requestCreateUserDTO.getStudentNo())
        ? requestCreateUserDTO.getUsername() : requestCreateUserDTO.getStudentNo();
    if (existsByUsernameOrStudentId(requestCreateUserDTO.getUsername(), studentId)) {
      return Result.error(400, "用户名或学号已存在");
    }
    User user = User.builder().userName(requestCreateUserDTO.getUsername()).studentId(studentId)
        .password(passwordService.encode(requestCreateUserDTO.getPassword()))
        .realName(requestCreateUserDTO.getRealName()).gender(requestCreateUserDTO.getGender())
        .phone(requestCreateUserDTO.getPhone()).email(requestCreateUserDTO.getEmail())
        .college(requestCreateUserDTO.getCollege()).major(requestCreateUserDTO.getMajor())
        .grade(requestCreateUserDTO.getGrade()).className(requestCreateUserDTO.getClassName())
        .avatar(requestCreateUserDTO.getAvatar())
        .userStatus(requestCreateUserDTO.getStatus() == null ? UserStatus.ACTIVE : requestCreateUserDTO.getStatus())
        .build();
    int row = userMapper.insert(user);
    if (row <= 0) return Result.error("用户创建失败");
    bindDefaultRole(user.getId());
    bindDefaultClubMembership(user.getId());
    return Result.success("用户创建成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> importUsers(MultipartFile file) {
    if (file == null || file.isEmpty()) return Result.error("上传文件为空");
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      int rowCount = sheet.getPhysicalNumberOfRows();
      if (rowCount <= 1) return Result.error("Excel 数据为空");
      List<User> userList = new ArrayList<>();
      for (int i = 1; i < rowCount; i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        String userName = getCellValue(row.getCell(0));
        String realName = getCellValue(row.getCell(1));
        String studentId = getCellValue(row.getCell(2));
        String phone = getCellValue(row.getCell(3));
        String email = getCellValue(row.getCell(4));
        String college = getCellValue(row.getCell(5));
        String major = getCellValue(row.getCell(6));
        String grade = getCellValue(row.getCell(7));
        String className = getCellValue(row.getCell(8));
        if (isBlank(userName) || isBlank(realName)) continue;
        if (isBlank(studentId)) studentId = userName;
        if (existsByUsernameOrStudentId(userName, studentId)) continue;
        User user = User.builder().userName(userName).realName(realName).studentId(studentId)
            .phone(phone).email(email).college(college).major(major).grade(grade)
            .className(className).password(passwordService.encode("123456"))
            .userStatus(UserStatus.ACTIVE).build();
        userList.add(user);
      }
      if (!userList.isEmpty()) {
        for (User user : userList) {
          userMapper.insert(user);
          bindDefaultRole(user.getId());
          bindDefaultClubMembership(user.getId());
        }
      }
      return Result.success("成功导入 " + userList.size() + " 条用户数据");
    } catch (Exception e) {
      return Result.error("Excel 导入失败: " + e.getMessage());
    }
  }

  @Override
  public byte[] exportTemplate() {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("用户导入模板");
      Row header = sheet.createRow(0);
      String[] titles = {"用户名*", "姓名*", "学号", "手机号", "邮箱", "学院", "专业", "年级", "班级"};
      for (int i = 0; i < titles.length; i++) header.createCell(i).setCellValue(titles[i]);
      Row example = sheet.createRow(1);
      example.createCell(0).setCellValue("test_user");
      example.createCell(1).setCellValue("张三");
      example.createCell(2).setCellValue("20230001");
      example.createCell(3).setCellValue("13800000000");
      example.createCell(4).setCellValue("test@example.com");
      example.createCell(5).setCellValue("信息工程学院");
      example.createCell(6).setCellValue("计算机应用技术");
      example.createCell(7).setCellValue("2023");
      example.createCell(8).setCellValue("计网2301");
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("模板导出失败", e);
    }
  }

  private String getCellValue(Cell cell) {
    if (cell == null) return "";
    switch (cell.getCellType()) {
      case STRING: return cell.getStringCellValue();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
        return String.valueOf((long) cell.getNumericCellValue());
      case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
      case FORMULA: return cell.getCellFormula();
      default: return "";
    }
  }

  @Override
  public Result<User> infoByUserId(Integer userId) {
    Integer targetUserId = userId == null ? StpUtil.getLoginIdAsInt() : userId;
    User user = userMapper.selectById(targetUserId);
    if (user == null) return Result.error(404, "用户不存在");
    return Result.success(buildSafeUser(user));
  }

  @Override
  public Result<String> updateUserInfo(Integer userId, RequestUserUpdateDTO requestUserUpdate) {
    if (userId == null) return Result.error(400, "userId不能为空");
    if (requestUserUpdate == null) return Result.error("请求参数为空");
    User user = userMapper.selectById(userId);
    if (user == null) return Result.error(404, "用户不存在");
    user.setRealName(requestUserUpdate.getRealName());
    user.setGender(requestUserUpdate.getGender());
    user.setPhone(requestUserUpdate.getPhone());
    user.setEmail(requestUserUpdate.getEmail());
    user.setStudentId(requestUserUpdate.getStudentNo());
    user.setCollege(requestUserUpdate.getCollege());
    user.setMajor(requestUserUpdate.getMajor());
    user.setGrade(requestUserUpdate.getGrade());
    user.setClassName(requestUserUpdate.getClassName());
    user.setAvatar(requestUserUpdate.getAvatar());
    int row = userMapper.updateById(user);
    return row > 0 ? Result.success("用户信息更新成功") : Result.error("用户信息更新失败");
  }

  @Override
  public Result<String> updateUserStatus(Integer userId, RequestUpdateUserStatusDTO requestUpdateUserStatusDTO) {
    if (userId == null) return Result.error(400, "userId不能为空");
    if (requestUpdateUserStatusDTO == null || requestUpdateUserStatusDTO.getStatus() == null)
      return Result.error(400, "用户状态不能为空");
    User user = userMapper.selectById(userId);
    if (user == null) return Result.error(404, "用户不存在");
    user.setUserStatus(requestUpdateUserStatusDTO.getStatus());
    int row = userMapper.updateById(user);
    return row > 0 ? Result.success("用户状态更新成功") : Result.error("用户状态更新失败");
  }

  @Override
  public Result<String> resetPassword(Integer userId, RequestResetPasswordDTO requestResetPasswordDTO) {
    if (userId == null) return Result.error(400, "userId不能为空");
    if (requestResetPasswordDTO == null || isBlank(requestResetPasswordDTO.getNewPassword()))
      return Result.error(400, "新密码不能为空");
    User user = userMapper.selectById(userId);
    if (user == null) return Result.error(404, "用户不存在");
    user.setPassword(passwordService.encode(requestResetPasswordDTO.getNewPassword()));
    int row = userMapper.updateById(user);
    return row > 0 ? Result.success("密码重置成功") : Result.error("密码重置失败");
  }

  @Override
  public Result<List<ResponseMembershipVO>> getUserMemberships(Integer userId) {
    if (userId == null) return Result.error(400, "userId不能为空");
    User user = userMapper.selectById(userId);
    if (user == null) return Result.error(404, "用户不存在");
    List<ResponseMembershipVO> memberships = clubMembershipMapper.selectByUserId(userId).stream()
        .map(this::buildMembershipResponse).toList();
    return Result.success(memberships);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deleteUser(Integer userId) {
    if (userId == null) return Result.error(400, "userId不能为空");
    if (StpUtil.isLogin() && userId.equals(StpUtil.getLoginIdAsInt()))
      return Result.error(400, "不能删除当前登录用户");
    User user = userMapper.selectById(userId);
    if (user == null) return Result.error(404, "用户不存在");
    Long activeMembershipCount = clubMembershipMapper.countActiveByUserId(userId);
    if (activeMembershipCount != null && activeMembershipCount > 0)
      return Result.error(400, "请先将该用户从社团中移出");
    userRoleMapper.deleteByUserId(userId);
    clubMembershipMapper.deleteByUserId(userId);
    activityRegistrationMapper.deleteByUserId(userId);
    membershipApplicationMapper.nullifyUserId(userId);
    formSubmissionMapper.nullifyUserId(userId);
    clubMapper.nullifyPresidentUserId(userId);
    clubDepartmentMapper.nullifyManagerUserId(userId);
    int rows = userMapper.deleteById(userId);
    return rows > 0 ? Result.success("用户已删除") : Result.error("用户删除失败");
  }

  private boolean existsByUsernameOrStudentId(String username, String studentId) {
    return userMapper.countByUsernameOrStudentId(username, studentId) > 0;
  }

  private void bindDefaultRole(Integer userId) {
    String roleCode = RoleSeedTemplate.probationaryMember().code();
    Role role = roleMapper.selectByCode(roleCode);
    if (role == null) throw new IllegalStateException("Default role not initialized: " + roleCode);
    UserRole exists = userRoleMapper.selectOneByUserAndRole(userId, role.getId());
    if (exists == null) {
      userRoleMapper.insert(UserRole.builder().userId(userId).roleId(role.getId()).build());
    }
  }

  private void bindDefaultClubMembership(Integer userId) {
    Club club = clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
    if (club == null) throw new IllegalStateException("Default club not initialized: " + DEFAULT_CLUB_CODE);
    ClubMembership exists = clubMembershipMapper.selectActiveMembership(userId, club.getId());
    if (exists == null) {
      clubMembershipMapper.insert(
          ClubMembership.builder().userId(userId).clubId(club.getId()).status("probation").build());
    }
  }

  private User buildSafeUser(User user) {
    return User.builder().id(user.getId()).userName(user.getUserName()).realName(user.getRealName())
        .gender(user.getGender()).phone(user.getPhone()).email(user.getEmail())
        .studentId(user.getStudentId()).college(user.getCollege()).major(user.getMajor())
        .grade(user.getGrade()).className(user.getClassName()).avatar(user.getAvatar())
        .userStatus(user.getUserStatus()).createTime(user.getCreateTime())
        .lastLoginAt(user.getLastLoginAt()).build();
  }

  private ResponseMembershipVO buildMembershipResponse(ClubMembership membership) {
    return ResponseMembershipVO.builder().id(membership.getId()).userId(membership.getUserId())
        .clubId(membership.getClubId()).departmentId(membership.getDepartmentId())
        .positionId(membership.getPositionId()).status(membership.getStatus())
        .joinedAt(membership.getJoinedAt()).leftAt(membership.getLeftAt()).build();
  }

  private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
