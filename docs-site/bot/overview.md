# QQ 机器人系统

## 概述

系统集成了一套基于 AstrBot + NapCat + OneBot v11 的 QQ 群机器人系统，实现群消息收发、群管理、请假审批通知等功能。

## 系统架构

```
QQ / QQ群
   ↓
NapCat (OneBot v11 协议端)
   ↓ WebSocket / HTTP API
AstrBot (机器人核心)
   ↓ 自定义插件 / HTTP 回调
后端服务 (BotManagementController)
   ↓
Web 管理后台 (BotGroups.vue)
```

## 组件说明

### AstrBot

[AstrBot](https://docs.astrbot.app/) 是机器人核心，负责：

- AI 对话（DeepSeek 集成）
- 插件系统
- 群聊消息处理
- 指令处理
- 机器人基础配置
- 与 OneBot v11 适配器通信

### NapCat

NapCat 是基于 NTQQ 的 OneBot v11 实现，作为 QQ 侧消息与群管理能力的接入层。

### 自定义插件

项目开发了自定义 AstrBot 插件 `astrbot_plugin_openatom_api`，位于：

```
astrbot/data/plugins/astrbot_plugin_openatom_api/
└── main.py
```

该插件实现与后端系统的 API 交互，包括：

- 请假审批通知推送
- 群消息转发
- 用户信息查询
- 活动通知

## Docker 配置

### AstrBot 容器

```yaml
astrbot:
    image: soulter/astrbot:latest
    container_name: openatom-astrbot
    ports:
        - "${ASTRBOT_PORT:-6185}:6185"      # 管理面板
        - "127.0.0.1:6198:6198"             # API 回调端口
    volumes:
        - ./astrbot/data:/AstrBot/data      # 数据目录
    depends_on:
        - backend
```

### NapCat 容器

```yaml
napcat:
    image: mlikiowa/napcat-docker:latest
    container_name: openatom-napcat
    environment:
        - MODE=astrbot
    ports:
        - "${NAPCAT_PORT:-6099}:6099"       # WebUI
        - "127.0.0.1:3000:3000"             # OneBot API
    volumes:
        - ./astrbot/data:/AstrBot/data
        - ./astrbot/napcat/config:/app/napcat/config
        - ./astrbot/ntqq:/app/.config/QQ
    depends_on:
        - astrbot
```

## 后端 API

`BotManagementController` 提供 QQ 群管理后台 API：

### 群管理

| 权限 | 路径 | 方法 | 说明 |
|------|------|------|------|
| `bot-management:list` | `/bot-management/groups` | GET | 查看群列表 |
| `bot-management:detail` | `/bot-management/groups/{groupId}` | GET | 查看群详情 |
| `bot-management:members` | `/bot-management/groups/{groupId}/members` | GET | 查看群成员 |
| `bot-management:sync` | `/bot-management/groups/{groupId}/sync` | POST | 同步群与成员 |
| `bot-management:config` | `/bot-management/groups/{groupId}/config` | PATCH | 配置群机器人 |
| `bot-management:mute` | `/bot-management/groups/{groupId}/mute` | POST | 执行禁言 |
| `bot-management:messages` | `/bot-management/groups/{groupId}/messages` | POST | 发送群消息 |
| `bot-management:announcements` | `/bot-management/groups/{groupId}/announcements` | POST | 管理群公告 |
| `bot-management:join-requests` | `/bot-management/groups/{groupId}/join-requests` | POST | 处理入群申请 |
| `bot-management:sensitive-words` | `/bot-management/sensitive-words` | POST | 管理敏感词 |
| `bot-management:auto-review` | `/bot-management/auto-review-rules` | POST | 管理自动审核规则 |
| `bot-management:statistics` | `/bot-management/statistics` | GET | 查看统计 |

### 请假审批通知

请假审批后，后端通过回调 URL 通知 AstrBot 插件，由插件在 QQ 群中推送审批结果通知。

配置项：

```yaml
app:
    bot:
        callback-token: ${APP_BOT_CALLBACK_TOKEN:}
        leave-review-callback-url: ${APP_BOT_LEAVE_REVIEW_CALLBACK_URL:http://127.0.0.1:6198/openatom/leave-review}
    napcat:
        base-url: ${APP_NAPCAT_BASE_URL:http://127.0.0.1:3000}
        access-token: ${APP_NAPCAT_ACCESS_TOKEN:sRtnBQJy-lqPTWjj}
```

## 前端管理

管理后台 `BotGroups.vue` 页面提供 QQ 群管理的可视化界面：

- 查看机器人加入的所有群聊
- 按群配置机器人功能开关
- 群成员、群主、管理员信息展示
- 群管理操作（禁言、公告、入群申请）
- 敏感词管理
- 自动审核规则

## CI/CD

GitHub Actions 中包含机器人插件语法检查：

```yaml
- name: AstrBot Plugin Syntax Check
  run: |
    python -m py_compile astrbot/data/plugins/astrbot_plugin_openatom_api/main.py
```

## 相关文档

- [BOTPRD.md](https://github.com/jmi-openatom/openatom-system/blob/main/BOTPRD.md) - 完整的 QQ 群机器人需求文档
- [AstrBot 文档](https://docs.astrbot.app/) - AstrBot 官方文档
