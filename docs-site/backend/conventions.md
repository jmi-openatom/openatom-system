# 后端开发规范

## 代码风格

### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `ClubService` |
| 方法名 | lowerCamelCase | `createClub()` |
| 变量名 | lowerCamelCase | `clubName` |
| 常量 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 包名 | 全小写 | `edu.jmi.openatom.server.openatomsystem` |
| 数据库表名 | snake_case | `club_membership` |
| 数据库字段 | snake_case | `created_at` |

### 类命名约定

| 类型 | 命名模式 | 示例 |
|------|----------|------|
| Controller | `{模块}Controller` | `ClubController` |
| Service 接口 | `{模块}Service` | `ClubService` |
| Service 实现 | `{模块}ServiceImpl` | `ClubServiceImpl` |
| Mapper | `{实体}Mapper` | `ClubMapper` |
| Entity | `{实体名}` | `Club` |
| 请求 DTO | `Request{动作}{对象}DTO` | `RequestCreateClubDTO` |
| 响应 VO | `{对象}VO` | `ClubDetailVO` |
| 枚举 | `{领域}Enum` | `UserStatus` |

## 分层规范

### Controller 层

- **只做**：接收请求、参数校验、调用 Service、返回 Result
- **不做**：业务逻辑、数据库操作
- 必须使用 `@SaCheckPermission` 注解标注权限要求
- 必须使用 `@Valid` 校验请求参数

```java
// ✅ 正确示例
@RestController
@RequestMapping("/clubs")
public class ClubController {

    @SaCheckPermission("club:create")
    @PostMapping
    public Result<Club> create(@Valid @RequestBody RequestCreateClubDTO dto) {
        return Result.success(clubService.create(dto));
    }
}

// ❌ 错误示例 - Controller 中写业务逻辑
@PostMapping
public Result<Club> create(@RequestBody RequestCreateClubDTO dto) {
    // 不要在 Controller 中写业务逻辑
    Club club = new Club();
    club.setName(dto.getName());
    club.setStatus(ClubStatus.ACTIVE);
    clubMapper.insert(club);
    return Result.success(club);
}
```

### Service 层

- **职责**：业务逻辑处理
- 使用 `@Transactional` 管理事务
- 复杂业务逻辑拆分为私有方法
- 异常使用业务异常抛出，由全局异常处理器统一处理

### Mapper 层

- 继承 `BaseMapper<T>`
- 简单 CRUD 使用 MyBatis Plus 内置方法
- 复杂查询使用 XML 或 `@Select` 注解
- 分页使用 `PageRequests` 工具类

## 统一响应

所有 Controller 方法返回 `Result<T>`：

```java
// 成功 - 无数据
return Result.success();

// 成功 - 有数据
return Result.success(data);

// 成功 - 有数据和自定义消息
return Result.success(data, "创建成功");

// 失败
return Result.error("参数错误");
return Result.error(400, "参数错误");
```

## 权限注解

### 接口级权限

```java
@SaCheckPermission("club:create")      // 检查单个权限
@SaCheckPermission({"club:list", "club:detail"})  // 检查多个权限（AND）
@SaCheckPermission(value = "club:list", orRole = "super-admin")  // 权限或角色满足其一
```

### 登录校验

```java
@SaCheckLogin  // 仅检查登录
```

## 新增功能开发流程

### 1. 创建/修改实体类

```java
@TableName("new_feature")
@Data
public class NewFeature {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

### 2. 创建 Flyway 迁移脚本

在 `backend/src/main/resources/db/migration/` 下创建 `V{下一个版本号}__add_new_feature.sql`。

### 3. 创建 Mapper 接口

```java
@Mapper
public interface NewFeatureMapper extends BaseMapper<NewFeature> {
    // 复杂查询可在此定义
}
```

### 4. 创建 Service 接口和实现

```java
public interface NewFeatureService {
    NewFeature create(RequestCreateNewFeatureDTO dto);
    NewFeature detail(Long id);
    Page<NewFeature> list(PageRequest request);
}

@Service
public class NewFeatureServiceImpl implements NewFeatureService {
    @Autowired
    private NewFeatureMapper newFeatureMapper;
    
    @Override
    @Transactional
    public NewFeature create(RequestCreateNewFeatureDTO dto) {
        NewFeature feature = new NewFeature();
        feature.setName(dto.getName());
        newFeatureMapper.insert(feature);
        return feature;
    }
    
    // ...其他方法
}
```

### 5. 创建 Controller

```java
@RestController
@RequestMapping("/new-features")
public class NewFeatureController {
    @Autowired
    private NewFeatureService newFeatureService;
    
    @SaCheckPermission("new-feature:create")
    @PostMapping
    public Result<NewFeature> create(@Valid @RequestBody RequestCreateNewFeatureDTO dto) {
        return Result.success(newFeatureService.create(dto));
    }
    
    @SaCheckPermission("new-feature:list")
    @GetMapping
    public Result<Page<NewFeature>> list(PageRequest request) {
        return Result.success(newFeatureService.list(request));
    }
}
```

### 6. 注册权限点

在 `SystemPermission` 枚举中添加新权限点：

```java
NEW_FEATURE_LIST("查询新功能", "new-feature:list", "api", "/new-features", "GET"),
NEW_FEATURE_CREATE("创建新功能", "new-feature:create", "api", "/new-features", "POST"),
```

## 实体类规范

- 使用 `@TableName` 标注表名
- 使用 `@TableId` 标注主键
- 使用 `@TableField` 标注特殊字段（如自动填充）
- 使用 Lombok `@Data` 或 `@Getter` 简化代码
- 时间字段使用 `LocalDateTime`

## 异常处理

### 业务异常

直接抛出 `RuntimeException` 或其子类，全局异常处理器会捕获并转换为 `Result.error()`：

```java
if (club == null) {
    throw new RuntimeException("社团不存在");
}
```

### 参数校验

使用 `@Valid` + JSR 303 注解：

```java
public class RequestCreateClubDTO {
    @NotBlank(message = "社团名称不能为空")
    @Size(max = 100, message = "社团名称最长100字符")
    private String name;
    
    @NotNull(message = "社团类型不能为空")
    private Integer type;
}
```

## 日志规范

- 使用 SLF4J（Lombok `@Slf4j` 注解）
- 生产环境日志级别：INFO
- 开发环境日志级别：DEBUG
- Sa-Token 日志：开发环境开启，生产环境关闭

```java
@Slf4j
@Service
public class ClubService {
    public void createClub(RequestCreateClubDTO dto) {
        log.info("创建社团: {}", dto.getName());
        // ...
    }
}
```

## 编码约定

- Java 文件编码：UTF-8
- 换行符：LF（Unix）
- 缩进：2 空格（项目使用 google-java-format）
- 导入顺序：标准库 → 第三方库 → 项目内部
- 不使用 `*` 通配符导入
