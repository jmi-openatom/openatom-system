# Lab Management System

这是独立于 CMS 的实验室管理系统实现骨架。CMS 只承担 OAuth 认证中心、实验室成员资格分发和社团积分 API 提供方；LMS 自己运行前端、后端和本地业务数据。

## 目录

```text
lab-management-system/
├── backend/       # 独立 Spring Boot API
└── lab-ui-web/    # 独立实验室网站
```

## 系统边界

- 登录：LMS 跳转到 CMS OAuth，回调后由 LMS 后端读取 CMS 用户信息中的 `isLabMember`。
- 准入：非实验室成员被 LMS 拦截，不发放 LMS token。
- 数据：题目、提交、签到、信誉分和通知属于 LMS 本地数据。
- 对接：社团积分同步只记录待同步事件，真实环境接 CMS `add_club_score` API 或 MQ。
- 判题：当前后端保留 ACM 状态机和安全模式评测，真实环境把 `LabStore#judge` 替换为 Docker/Isolate 沙箱客户端。

## 本地运行

后端：

```bash
cd lab-management-system/backend
mvn spring-boot:run
```

前端：

```bash
cd lab-management-system/lab-ui-web
open index.html
```

默认 API 地址为 `http://localhost:8090/api`。如需改地址，在浏览器控制台设置：

```js
localStorage.setItem('lab_api_base', 'https://lab-api.example.com/api')
```
