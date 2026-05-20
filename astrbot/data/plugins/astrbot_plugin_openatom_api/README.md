# OpenAtom API AstrBot 插件

这个插件用于让 AstrBot/NapCat 机器人查询 OpenAtom System 后端公开数据，并支持用户通过网页登录生成的一次性绑定码绑定 QQ。

## 配置

随本项目 `docker-compose.yml` 一起部署时，默认 `base_url`：

```text
http://backend:8921/api/v1
```

如果 AstrBot 单独部署在宿主机或其他 Docker 网络，请改成实际后端地址，例如：

```text
http://host.docker.internal:8921/api/v1
```

## AI 工具

插件只暴露一个 AI 工具：

```text
openatom_query(question)
```

AI 必须把用户原始问题传给 `question`，插件内部根据问题自动查询后端公开 GET 接口并直接生成中文回答。

示例：

```text
当前有什么活动 -> 查询 /site/activities
看一下1号活动 -> 先查 /site/activities，再按第 1 条活动 ID 查询详情
社团有多少人 -> 查询 /site/club-home 的 metrics
主要人员有哪些 -> 查询 /site/club-home 的 people
社团统计/规模 -> 查询 /site/club-home 的 metrics
社团方向/技术栈 -> 查询 /site/club-home 的 focusAreas 和 techStack
社团荣誉/获奖 -> 查询 /site/club-home 的 awards
有哪些部门 -> 查询 /clubs/1/departments
招新开了吗 -> 查询 /site/recruitment
校历怎么安排 -> 查询 /site/school-calendar
注册开了吗 -> 查询 /site/register-enabled
我的申请进度 -> 查询 /site/progress
请假记录 -> 查询 /site/leave-applications
```

不要再使用旧工具名 `openatom_system_overview`、`openatom_public_query`、`openatom_api_get`。如果后台还显示旧工具，说明插件未重载成功。

## 聊天命令

```text
/oa help
/oa ping
/oa me
/oa bind-qq 绑定码
/oa ask 社团有多少人
/oa ask 主要人员有哪些
/oa ask 看一下1号活动
/oa get /site/club-home
/oa config
```

## QQ 绑定流程

1. 用户先登录 Web 端个人中心。
2. 在“安全控制”里点击“生成绑定码”。
3. 复制页面显示的命令，例如：

```text
/oa bind-qq ABCD1234
```

4. 到 QQ 里发给机器人，机器人会把当前发送人的 QQ 号和该 Web 账号绑定。

绑定码 10 分钟有效，成功绑定后立即失效。

## 写入限制

普通查询代码层面只允许 `GET`。插件唯一允许的写入是 QQ 绑定确认接口：

```python
BIND_WRITE_PATHS = {("POST", "/auth/qq-bind/confirm")}
```

插件不提供登录、创建、修改、删除业务数据的指令。
