# OpenAtom API AstrBot 插件

这个插件用于让 AstrBot/NapCat 机器人查询 OpenAtom System 后端数据，并支持用户通过网页登录生成的一次性绑定码绑定 QQ、通过 QQ 号或姓名查系统用户、查询当天晚自习签到概览。

## 配置

随本项目 `docker-compose.yml` 一起部署时，默认 `base_url`：

```text
http://backend:8921/api/v1
```

如果 AstrBot 单独部署在宿主机或其他 Docker 网络，请改成实际后端地址，例如：

```text
http://host.docker.internal:8921/api/v1
```

`auto_approve_friend_requests` 默认开启。NapCat 将 OneBot 好友申请事件发送给 AstrBot 后，插件会立即调用 `set_friend_add_request` 自动同意；如需人工审批，可在插件配置中关闭。

## 自定义入群欢迎

在 AstrBot 面板进入本插件的配置，可使用默认欢迎设置，也可在“按群自定义入群欢迎”（`welcome_group_overrides`）中点击添加，为每个 QQ 群分别配置。保存后重载插件生效。收到实际新成员入群通知（包括手动同意和邀请入群）时发送。

按群设置优先于默认设置，每条填写：

- QQ 群号。
- 本群启用自定义欢迎：单独开启或关闭该群。
- 本群欢迎文案：支持多行和下表中的变量，留空则该群不发送。
- 本群欢迎时 @ 新成员。
- 本群欢迎发送延迟：范围 0～60 秒。

例如，为技术群填写“欢迎 {nickname}！请先阅读技术交流群公告”，为活动群填写“欢迎 {nickname} 参加活动，请关注群内通知”。两个群将各自发送对应文案。

只想对指定群启用时，保持 `welcome_enabled` 关闭，并在按群列表里开启这些群。未单独配置的群继续使用以下默认设置（默认欢迎开关关闭）：

- `welcome_enabled`：未单独配置群的欢迎开关。
- `welcome_text`：支持多行文案，留空则不发送。
- `welcome_at_new_member`：是否 @ 新成员，默认开启。
- `welcome_delay_seconds`：入群后延迟发送的秒数，范围 0～60，默认 1 秒。

同一群号填写多条时使用第一条；群号为空或无效的条目会跳过并记录日志。按群列表使用 AstrBot 的模板列表配置组件，需要 AstrBot v4.10.4 或以上，参见[官方配置文档](https://docs.astrbot.app/dev/star/guides/plugin-config.html)。

文案变量：

| 变量 | 含义 |
| --- | --- |
| `{nickname}` | QQ 昵称，获取失败时使用群名片或 QQ 号 |
| `{qq}` | 新成员 QQ 号 |
| `{group_name}` | 群名称，获取失败时使用群号 |
| `{group_id}` | 群号 |
| `{card_name}` | 群名片，未设置或获取失败时为空 |

例如：

```text
欢迎 {nickname} 加入 {group_name}！🎉
请先阅读群公告，有问题可以在群里交流。
```

机器人自己入群不会触发欢迎，同一通知在 5 分钟内重复推送会去重。自动审批流程保持原样；自动审批原有的“已为你设置群名片”欢迎提示仍会发送，开启本功能后还会发送上述自定义欢迎。网页后台“机器人群管理”的欢迎配置独立生效，同时开启时可能收到两条欢迎。

## AI 工具

插件只暴露一个 AI 工具：

```text
openatom_query(question)
```

AI 必须把用户原始问题传给 `question`，插件内部根据问题自动查询后端 GET 接口并直接生成中文回答。

示例：

```text
当前有什么活动 -> 查询 /site/activities
看一下1号活动 -> 先查 /site/activities，再按第 1 条活动 ID 查询详情
社团有多少人 -> 查询 /site/club-home 的 metrics
主要人员有哪些 -> 查询 /site/club-home 的 people
社团统计/规模 -> 查询 /site/club-home 的 metrics
社团方向/技术栈 -> 查询 /site/club-home 的 focusAreas 和 techStack
社团荣誉/获奖 -> 查询 /site/club-home 的 awards
查人 张三 -> 查询 /bot/users/lookup?keyword=张三
通过QQ号123456查人 -> 查询 /bot/users/lookup?qqOpenid=123456
今晚晚自习签到 -> 查询 /bot/evening-study/today
有哪些部门 -> 查询 /clubs/1/departments
招新开了吗 -> 查询 /site/recruitment
校历怎么安排 -> 查询 /site/school-calendar
注册开了吗 -> 查询 /site/register-enabled
我的申请进度 -> 查询 /site/progress
请假记录 -> 查询 /site/leave-applications
```

不要再使用旧工具名 `openatom_system_overview`、`openatom_public_query`、`openatom_api_get`。如果后台还显示旧工具，说明插件未重载成功。

## 输出形式

项目的 `astrbot/data/cmd_config.json` 已关闭 AstrBot 全局 `t2i`，插件回复、普通模型回复和内置回复都保持文字输出。

## 聊天命令

```text
/oa help
/oa ping
/oa me
/oa config
/oa callback-test
/oa bind-qq 绑定码
/oa 查人 qq 123456
/oa 查人 张三
/oa 晚自习
/群里艾特机器人发送：我要请假，然后在机器人私聊里继续
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

## 查人

机器人支持按 QQ 号或姓名查系统用户：

```text
/oa 查人 qq 123456
/oa 查人 张三
/oa ask 通过QQ号123456查人
/oa ask 查人 张三
```

查人接口读取后端 public 接口 `/bot/users/lookup`，不需要登录，也不需要在插件配置里填写 `access_token`。后端只返回姓名、ID、用户名、学号、院系专业班级、QQ 绑定状态和账号状态，不直接暴露密码、token、手机号、邮箱或 QQ 原始绑定值。

## 晚自习签到

机器人支持查询当天晚自习签到概览：

```text
/oa 晚自习
/oa ask 今晚晚自习签到
```

接口读取后端 public 接口 `/bot/evening-study/today`，按后台创建的实验室分组返回当天生成的晚自习签到场次、应到/已到/未到和已请假剔除人数。

## 机器人请假

用户必须先绑定 QQ。绑定后在群里艾特机器人并发送：

```text
我要请假
```

机器人会在群里提示用户去私聊，并在私聊里依次询问：

1. 请假类型
2. 请假理由
3. 请假时间
4. 图片附件或图片链接

请假时间会优先交给 AstrBot 当前配置的 AI 模型解析，支持“明晚七点到九点”“下周三下午”“今晚去不了”等自然语言；模型不可用时会自动降级为本地解析。

图片链接会直接作为地址提交；直接发送的图片会交给后端落到图床并保存为 `/files/images/...` 地址。提交成功时机器人会把私聊上下文提交给后端；后台通过或驳回后，后端会立即回调插件，机器人会在私聊里告知结果，驳回时会带上驳回理由。

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
