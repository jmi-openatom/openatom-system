# 统一分组中心

## 数据职责

- `unified_group`、`unified_group_member`、`unified_group_binding` 是后台统一分组中心的读取模型。
- `club_department`、`checkin_group`、`club_alumni_group`、`bot_group` 继续保存各领域专有配置。
- 领域服务写入原业务表后，会在同一事务中更新统一读取模型。
- 往届成员使用 `club_membership.alumni_group_id` 关联分组；`alumni_group` 名称字段暂时保留用于旧接口兼容。

## 类型与来源

| 统一类型 | `source_type` | 兼容业务表 |
|---|---|---|
| `department` | `department` | `club_department` |
| `checkin` | `checkin` | `checkin_group`、`checkin_group_member` |
| `alumni` | `alumni` | `club_alumni_group`、`club_membership` |
| `external` | `qq_group` | `bot_group`、`bot_group_member` |

`source_type + source_id` 是统一投影与兼容表之间的稳定映射，不应使用名称关联。

## 管理接口

- `GET /admin/groups`：统一分组列表。
- `POST /admin/groups`：从统一入口创建部门、签到或往届分组。
- `GET /admin/groups/{groupId}`：详情、成员和依赖摘要。
- `GET /admin/groups/{groupId}/members`：统一成员列表。
- `GET /admin/groups/{groupId}/dependencies`：删除或归档前的业务依赖。
- `POST /admin/groups/sync`：从兼容表全量修复统一投影，并清理已经不存在的来源。

## 部署顺序

1. 备份数据库。
2. 发布后端并由 Flyway 执行 `V48__add_unified_group_center.sql`。
3. 重新登录后台，使新增的 `group:*` 权限进入登录会话。
4. 调用一次 `POST /admin/groups/sync`，确认四类分组数量与旧后台一致。
5. 发布前端，侧边栏将只显示“分组管理”。旧路由在没有分组参数时跳转到统一入口。

## 回滚

前端可独立回滚到旧入口。兼容业务表仍保留完整数据，因此回滚不依赖反向数据迁移。统一表属于读取投影，必要时可以停止投影写入；不要在未备份的情况下删除 `alumni_group_id`，因为它已经成为往届分组的稳定关联键。
