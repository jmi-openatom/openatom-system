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
/oa config
/oa bind-qq 绑定码
/艾特机器人发送：我要请假
/oa ask 社团有多少人
/oa ask 主要人员有哪些
/oa ask 看一下1号活动
/oa get /site/club-home
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

## 机器人请假

用户必须先绑定 QQ。绑定后在 QQ 里艾特机器人并发送：

```text
我要请假
```

机器人会依次询问：

1. 请假类型
2. 请假理由
3. 请假时间
4. 图片附件或图片链接

请假时间会优先交给 AstrBot 当前配置的 AI 模型解析，支持“明晚七点到九点”“下周三下午”“今晚去不了”等自然语言；模型不可用时会自动降级为本地解析。

图片附件会先转成可在 Web 后台直接预览的 `data:image/...;base64,...` 内容，转换失败不会提交申请。提交成功时机器人会把本次申请所在的群聊或私聊上下文提交给后端；后台通过或驳回后，后端会立即回调插件，机器人会在原会话 At 申请人并告知结果，驳回时会带上驳回理由。

用户可发送“取消”退出本次请假流程。

## 写入限制

普通查询代码层面只允许 `GET`。插件只允许 QQ 绑定确认和机器人请假两个写入接口：

```python
BIND_WRITE_PATHS = {
    ("POST", "/auth/qq-bind/confirm"),
    ("POST", "/bot/leave-applications"),
}
```

插件不提供登录、创建、修改、删除业务数据的指令。
