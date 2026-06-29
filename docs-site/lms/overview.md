# 实验室管理系统（LMS）

## 概述

实验室管理系统（Lab Management System）是一个独立子系统，为算法实验室（ACM/蓝桥杯方向）提供日常训练、考勤及量化考核功能。该系统独立运行，但底层账号和实验室成员资格认证完全依赖社团管理系统（CMS）。

## 系统架构

```
+-------------------------------------------------------------+
|                     社团管理系统 (CMS)                       |
|  [用户主库] -> [OAuth 认证中心] -> [实验室成员打标] -> [积分接口]  |
+-------------------------------------------------------------+
                               | (OAuth / API)
                               v
+-------------------------------------------------------------+
|                     实验室管理系统 (LMS)                       |
|  [独立前端] -> [网关/业务后端] -> [AI 题目生成] -> [沙箱评测机]  |
|  [独立本地库 (用户冗余表、题目表、签到流水、通知表、积分表)]       |
+-------------------------------------------------------------+
```

## 核心功能

### 账号互通与鉴权

- 采用 OAuth2.0，复用社团系统账号
- 点击登录重定向至 CMS OAuth 授权页
- CMS 返回 Access Token，LMS 解析并请求用户信息
- 权限截断：读取 `is_lab_member` 字段，仅实验室成员可登录

### AI 题目生成

- 自动生成符合 ACM 赛制的每日一练题目
- 集成 DeepSeek AI 模型
- 独立沙箱判题（OJ）核心

### 考勤签到

- 实验室签到闭环
- 通知中心
- 内部积分扣减
- 正常签到奖励异步对接社团积分

## 项目结构

### 后端（lab-management-system/）

```
lab-management-system/
├── lab-admin/           # 管理模块
├── lab-common/          # 公共模块
├── lab-framework/       # 框架模块
├── lab-modules/         # 业务模块
│   └── ...
├── backend/             # 后端代码
├── deploy/
│   └── nginx/           # Nginx 配置
├── lab-admin/           # 管理端
├── pom.xml              # Maven 配置
├── Dockerfile
├── docker-compose.yml
└── docker-compose.prod.yml
```

### 前端（lab-ui-web/）

```
lab-ui-web/
├── src/
│   └── ...
├── public/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── Dockerfile
├── nginx.conf
└── .env
```

## 部署方式

LMS 独立部署，使用 Docker Compose：

```bash
cd lab-management-system
docker-compose up -d --build
```

生产环境使用 `docker-compose.prod.yml`。

## 与社团系统的集成

### OAuth2 认证

LMS 作为 OAuth 客户端接入社团系统认证中心：

1. 用户在 LMS 点击"社团账号登录"
2. 重定向到 CMS OAuth 授权页
3. 用户授权后，CMS 返回授权码
4. LMS 用授权码交换 Access Token
5. LMS 请求 CMS UserInfo 接口获取用户信息
6. 检查 `is_lab_member` 字段，决定是否允许登录

### 积分对接

实验室正常签到奖励通过 API 异步同步到社团积分系统。

## 相关文档

- [LMS.md](https://github.com/jmi-openatom/openatom-system/blob/main/LMS.md) - 完整的实验室管理系统需求文档
- [OAuth 使用指南](https://github.com/jmi-openatom/openatom-system/blob/main/docs/oauth-usage.md) - OAuth2 集成说明
