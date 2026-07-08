from __future__ import annotations

import asyncio
import base64
import contextlib
import json
import inspect
import re
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any
from urllib.parse import quote
from zoneinfo import ZoneInfo

import aiohttp
from aiohttp import web
from astrbot.api import AstrBotConfig, logger
from astrbot.api.event import AstrMessageEvent, filter
from astrbot.api.star import Context, Star, register


SAFE_METHODS = {"GET"}
BIND_WRITE_PATHS = {
    ("POST", "/auth/qq-bind/confirm"),
    ("POST", "/bot/leave-applications"),
}
SENSITIVE_KEYS = {"password", "accessToken", "refreshToken", "token", "authorization", "jmiopenatom"}
MAX_TEXT_CHARS = 90
MAX_DETAIL_CHARS = 220
PLUGIN_VERSION = "1.6.0"
MAX_ATTACHMENT_BYTES = 6 * 1024 * 1024


class OutgoingMessageChain:
    def __init__(self, chain: list[Any]):
        self.chain = chain


@register(
    "astrbot_plugin_openatom_api",
    "ariven",
    "让 AstrBot/NapCat 机器人查询 OpenAtom System 后端数据、绑定 QQ 并查人",
    PLUGIN_VERSION,
    "local",
)
class OpenAtomApiPlugin(Star):
    def __init__(self, context: Context, config: AstrBotConfig):
        super().__init__(context)
        self.config = config
        self.base_url = self._normalize_base_url(
            self.config.get("base_url", "http://backend:8921/api/v1")
        )
        self.access_token = str(self.config.get("access_token") or "").strip()
        self.timeout = int(self.config.get("timeout_seconds", 15) or 15)
        self.max_reply_chars = int(self.config.get("max_reply_chars", 0) or 0)
        self.callback_host = str(self.config.get("callback_host", "0.0.0.0") or "0.0.0.0").strip()
        self.callback_port = int(self.config.get("callback_port", 6198) or 6198)
        self.callback_token = str(self.config.get("callback_token", "") or "").strip()
        self.auto_approve_friend_requests = bool(self.config.get("auto_approve_friend_requests", True))
        self.leave_sessions: dict[str, dict[str, Any]] = {}
        self.callback_runner: web.AppRunner | None = None
        self.callback_site: web.TCPSite | None = None
        self.callback_task: asyncio.Task | None = None
        self._ensure_callback_server()

    @filter.command_group("oa", alias={"openatom", "开放原子"})
    def oa(self):
        pass

    @oa.command("help", alias={"帮助"})
    async def help(self, event: AstrMessageEvent):
        """查看 OpenAtom API 插件用法"""
        yield self._plain_result(
            event,
            "\n".join(
                [
                    "OpenAtom 机器人指南：",
                    "",
                    "【直接跟我聊天】",
                    "你可以直接问我问题，我会理解你的意思并查询数据：",
                    "- 社团有多少人 / 社团概况",
                    "- 有什么活动 / 看一下1号活动",
                    "- 有哪些部门 / 主要人员",
                    "- 招新开了吗 / 怎么加入",
                    "- 晚自习签到",
                    "- 我是谁（通过QQ查你在系统里的信息）",
                    "- 查人 张三 / 查人 qq 123456",
                    "- 获奖 / 荣誉",
                    "",
                    "【指令模式】",
                    "/oa ping - 测试后端连接",
                    "/oa me - 查看当前登录用户",
                    "/oa bind-qq 绑定码 - 绑定 QQ",
                    "/oa 查人 qq 123456 / /oa 查人 张三",
                    "/oa 晚自习 - 晚自习签到概览",
                    "/oa ask 社团有多少人 - 自然语言查询",
                    "/oa get /site/club-home - 直接调用 API",
                    "/oa config - 查看配置",
                    "",
                    "【请假】",
                    "群里艾特我发送“我要请假 / 请个假 / 今天去不了”，我会私聊你继续办理",
                    "",
                    f"插件版本：{PLUGIN_VERSION}",
                    "普通查询只允许 GET；查人走独立 public 接口；QQ 绑定命令只会消费网页登录生成的一次性绑定码。",
                ]
            )
        )

    @oa.command("config", alias={"配置"})
    async def show_config(self, event: AstrMessageEvent):
        """查看当前 API 配置"""
        token_status = "已配置" if self.access_token else "未配置"
        yield self._plain_result(
            event,
            f"base_url: {self.base_url}\n"
            f"plugin_version: {PLUGIN_VERSION}\n"
            f"token: {token_status}\n"
            f"timeout: {self.timeout}s\n"
            f"max_reply_chars: {self.max_reply_chars}\n"
            f"auto_approve_friend_requests: {self.auto_approve_friend_requests}\n"
            f"callback: http://{self.callback_host}:{self.callback_port}/openatom/leave-review"
        )

    @oa.command("callback-test", alias={"回调测试"})
    async def callback_test(self, event: AstrMessageEvent):
        """测试后端审批回调通知链路"""
        sender_id = self._event_sender_id(event)
        self._ensure_callback_server()
        sent = await self._send_leave_status_notification(
            {
                "unifiedMsgOrigin": self._private_origin_for_event(event, sender_id),
                "senderId": sender_id,
                "leaveId": "TEST",
                "title": "回调测试",
            },
            {
                "id": "TEST",
                "status": "approved",
                "title": "回调测试",
                "reviewComment": "",
            },
        )
        yield self._plain_result(event, "回调发送测试成功。" if sent else "回调发送测试失败，请查看 AstrBot 日志。")

    @oa.command("ping")
    async def ping(self, event: AstrMessageEvent):
        """测试后端连接"""
        data, error = await self._get_data("/site/register-enabled")
        yield self._plain_result(event, "后端连接正常" if error is None else error)

    @oa.command("me")
    async def me(self, event: AstrMessageEvent):
        """查看当前登录用户"""
        data, error = await self._get_data("/auth/me")
        yield self._plain_result(event, error or self._format_generic(data))

    @oa.command("bind-qq", alias={"绑定qq", "qq绑定"})
    async def bind_qq(self, event: AstrMessageEvent, token: str):
        """使用网页登录生成的一次性绑定码绑定当前 QQ"""
        qq_openid = self._event_sender_id(event)
        if not qq_openid:
            yield self._plain_result(event, "没有获取到当前 QQ 号，无法绑定。")
            return
        result = await self._request(
            "POST",
            "/auth/qq-bind/confirm",
            body={"token": token, "qqOpenid": qq_openid},
        )
        yield self._plain_result(event, self._success_message(result, f"已将当前 QQ（{qq_openid}）绑定到系统账号。"))

    @oa.command("find", alias={"查人", "找人", "user", "用户"})
    async def find_user(self, event: AstrMessageEvent, *parts: str):
        """按 QQ 号或姓名查询系统用户"""
        lookup_type, keyword = self._parse_user_lookup_command(parts)
        yield self._plain_result(event, await self._answer_user_lookup(keyword, lookup_type))

    @oa.command("evening-study", alias={"晚自习", "晚自习签到", "自习签到"})
    async def evening_study(self, event: AstrMessageEvent):
        """查看今天晚自习签到概览"""
        yield self._plain_result(event, await self._answer_evening_study())

    @oa.command("ask", alias={"问"})
    async def ask(self, event: AstrMessageEvent, *question_parts: str):
        """按自然语言问题查询公开数据"""
        question = " ".join(question_parts).strip()
        yield self._plain_result(event, await self._answer_question(question, event=event))

    @oa.command("get")
    async def get(self, event: AstrMessageEvent, path: str, *params: str):
        """调用 GET API，参数格式 key=value"""
        query = self._parse_query(params)
        data, error = await self._get_data(path, query)
        yield self._plain_result(event, error or self._format_generic(data))

    @filter.platform_adapter_type(filter.PlatformAdapterType.AIOCQHTTP)
    async def auto_approve_friend_request(self, event: AstrMessageEvent):
        """自动同意 NapCat/OneBot 好友申请。"""
        if not self.auto_approve_friend_requests:
            return
        raw = self._onebot_raw_event(event)
        if (
            self._raw_event_value(raw, "post_type") != "request"
            or self._raw_event_value(raw, "request_type") != "friend"
        ):
            return
        flag = str(self._raw_event_value(raw, "flag") or "").strip()
        user_id = str(self._raw_event_value(raw, "user_id") or "").strip()
        if not flag:
            logger.warning(f"OpenAtom auto approve friend request skipped: missing flag, userId={user_id}")
            return
        bot = getattr(event, "bot", None)
        if bot is None:
            logger.warning(f"OpenAtom auto approve friend request skipped: missing aiocqhttp bot, userId={user_id}")
            return
        try:
            await bot.call_action("set_friend_add_request", flag=flag, approve=True)
            logger.info(f"OpenAtom friend request auto approved: userId={user_id or 'unknown'}")
        except Exception as exc:
            logger.warning(f"OpenAtom friend request auto approve failed: userId={user_id or 'unknown'}, error={exc}")

    @filter.event_message_type(filter.EventMessageType.ALL)
    async def handle_leave_conversation(self, event: AstrMessageEvent):
        """处理“@机器人 我要请假”的逐步问答。"""
        self._ensure_callback_server()
        sender_id = self._event_sender_id(event)
        if not sender_id:
            return
        is_group = self._is_group_event(event)
        session_key = self._private_leave_session_key(sender_id)
        text = self._event_plain_text(event).strip()
        attachments = self._event_attachments(event)

        session = self.leave_sessions.get(session_key)
        if not session:
            if text.startswith("/") or not self._looks_like_leave_request(text):
                return
            bound, error = await self._is_qq_bound(sender_id)
            if error:
                yield self._plain_result(event, error)
                event.stop_event()
                return
            if not bound:
                yield self._plain_result(event, "当前 QQ 还没有绑定系统账号。请先登录网页个人中心生成绑定码，再发送 /oa bind-qq 绑定码。")
                event.stop_event()
                return
            notify_origin = self._private_origin_for_event(event, sender_id)
            self.leave_sessions[session_key] = {"step": "title", "qqOpenid": sender_id, "notifyOrigin": notify_origin}
            prompt = "开始提交请假申请。请先发送请假类型，例如：例会请假、活动请假。发送“取消”可退出。"
            if is_group:
                sent = await self._send_plain_message(notify_origin, prompt)
                if sent:
                    yield self._plain_result(event, "我已经私聊你了，请在私聊里继续完成请假申请。")
                else:
                    self.leave_sessions.pop(session_key, None)
                    yield self._plain_result(event, "我没能发起私聊，请先给机器人发送一条私聊消息后再试。")
            else:
                yield self._plain_result(event, prompt)
            event.stop_event()
            return

        if is_group:
            if self._looks_like_leave_request(text):
                yield self._plain_result(event, "你有一条请假申请正在私聊里办理，请到私聊继续。")
                event.stop_event()
            return

        if text in {"取消", "退出", "算了", "不请了"}:
            self.leave_sessions.pop(session_key, None)
            yield self._plain_result(event, "已取消本次请假申请。")
            event.stop_event()
            return

        step = session.get("step")
        if step == "title":
            if not text:
                yield self._plain_result(event, "请发送请假类型，例如：例会请假、活动请假。")
            else:
                session["title"] = self._excerpt(text, 80)
                session["step"] = "reason"
                yield self._plain_result(event, "请发送请假理由。")
            event.stop_event()
            return

        if step == "reason":
            if not text:
                yield self._plain_result(event, "请发送请假理由。")
            else:
                session["reason"] = self._excerpt(text, 500)
                session["step"] = "time"
                yield self._plain_result(event, "请发送请假时间，可以随意写，例如：明晚7点到9点、今天下午、5月21日18点到20点、周五晚上。")
            event.stop_event()
            return

        if step == "time":
            start_at, end_at = await self._parse_leave_time_with_ai(event, text)
            if not start_at and not end_at:
                yield self._plain_result(event, "没有识别到时间。你可以这样发：明晚7点到9点、今天下午、5月21日18点到20点、周五晚上。")
            else:
                session["startAt"] = start_at
                session["endAt"] = end_at
                session["step"] = "attachments"
                yield self._plain_result(event, "请发送请假附件图片，或发送图片链接。")
            event.stop_event()
            return

        if step == "attachments":
            if not attachments:
                yield self._plain_result(event, "请发送图片附件或图片链接，附件不能为空。")
                event.stop_event()
                return
            prepared_attachments, attachment_error = await self._prepare_leave_attachments(attachments[:5])
            if attachment_error:
                yield self._plain_result(event, attachment_error)
                event.stop_event()
                return
            session["attachments"] = prepared_attachments
            result = await self._request(
                "POST",
                "/bot/leave-applications",
                body={
                    "qqOpenid": session["qqOpenid"],
                    "title": session["title"],
                    "reason": session["reason"],
                    "startAt": session.get("startAt"),
                    "endAt": session.get("endAt"),
                    "attachments": session["attachments"],
                    "botNotifyOrigin": str(session.get("notifyOrigin") or getattr(event, "unified_msg_origin", "") or ""),
                    "botNotifyUserId": session["qqOpenid"],
                },
            )
            if result["ok"]:
                self.leave_sessions.pop(session_key, None)
                body = result.get("body")
                leave_id = body.get("data") if isinstance(body, dict) else None
                if leave_id:
                    yield self._plain_result(event, f"请假申请已提交，编号 {leave_id}，当前状态：待审批。审批完成后我会在私聊里通知你。")
                else:
                    yield self._plain_result(event, "请假申请已提交，当前状态：待审批。")
            else:
                yield self._plain_result(event, self._format_error(result))
            event.stop_event()
            return

    # ==================================================================
    # LLM 工具：who_am_i —— 需要当前用户 QQ 上下文，MCP 无法获取，所以保留在插件
    # 其他数据查询工具由 MCP Server 提供（astrbot/data/mcp_servers/openatom_backend/）
    # ==================================================================

    @filter.llm_tool(name="openatom_who_am_i")
    async def llm_who_am_i(self, event: AstrMessageEvent):
        '''查询当前发起对话的用户在社团系统中的身份信息。

        当用户问"我是谁""我叫什么""我的信息""你知道我是谁吗"等关于自身身份的问题时使用。
        此工具通过当前 QQ 号自动查询，用户不需要提供任何参数。
        '''
        result = await self._answer_who_am_i(event)
        if result is not None:
            return result
        return "无法获取当前用户的 QQ 信息，可能消息平台未提供发送者 ID。"

    async def _answer_question(self, question: str, event: AstrMessageEvent | None = None) -> str:
        text = (question or "").strip().lower()
        if not text:
            return "你想查询社团系统的哪类信息？可以问活动、招新、部门、人数、主要人员或校历，也可以问“我是谁”。"

        # —— 寒暄 / 问候 ——
        if self._is_greeting(text):
            return self._answer_greeting(text)

        # —— 机器人自我介绍 ——
        if self._has_any(text, ("你是谁", "你叫什么", "你叫啥", "你是啥", "你是什么", "自我介绍", "你能做什么", "你会什么", "你能干啥", "你有什么功能", "你能帮我")):
            return self._answer_bot_intro()

        # —— 查询当前用户身份（我是谁）——
        if self._is_who_am_i_question(text):
            if event is not None:
                result = await self._answer_who_am_i(event)
                if result is not None:
                    return result
            return "我可以通过你的 QQ 查到你在社团系统里的信息，不过需要你先绑定系统账号。请登录网页个人中心生成绑定码，再发送 /oa bind-qq 绑定码。"

        lookup_type, lookup_keyword = self._extract_user_lookup_from_question(question)
        if lookup_keyword:
            return await self._answer_user_lookup(lookup_keyword, lookup_type)

        if self._has_any(text, ("主要人员", "负责人", "骨干", "成员风采", "人员有哪些", "有哪些人", "社长", "会长", "部长", "核心成员", "老师")):
            return await self._answer_people()
        if self._has_any(text, ("多少人", "几个人", "人数", "成员数", "在册成员", "社员数", "多少成员")):
            return await self._answer_member_count()
        if self._has_any(text, ("数据", "统计", "指标", "概况", "规模")):
            return await self._answer_metrics()
        if self._has_any(text, ("技术栈", "技术", "方向", "领域", "做什么", "研究什么", "项目方向", "学习方向")):
            return await self._answer_focus_or_tech()
        if self._has_any(text, ("荣誉", "获奖", "奖项", "比赛成绩", "竞赛")):
            return await self._answer_awards()

        activity_ordinal = self._extract_activity_ordinal(text)
        if activity_ordinal is not None:
            return await self._answer_activity_by_ordinal(activity_ordinal)
        activity_id = self._extract_id(text, ("活动", "activity"))
        if activity_id:
            return await self._answer_activity_detail(activity_id)
        activity_by_title = await self._try_answer_activity_by_title(text)
        if activity_by_title:
            return activity_by_title
        if self._has_any(text, ("活动", "大会", "讲座", "比赛", "赛事", "openpixel", "安排")):
            return await self._answer_activities()

        recruitment_id = self._extract_id(text, ("招新", "recruitment", "campaign"))
        if recruitment_id:
            return await self._answer_recruitment_detail(recruitment_id)
        if self._has_any(text, ("招新", "报名", "加入", "纳新", "申请入社", "入社")):
            return await self._answer_recruitment()

        if self._has_any(text, ("部门", "项目部", "宣传组", "活动部", "外联部", "组织架构", "分组", "组别", "岗位")):
            return await self._answer_departments()
        if self._has_any(text, ("校历", "放假", "假期", "调休", "上课", "教学周")):
            return await self._answer_school_calendar()
        if self._has_any(text, ("注册", "开放注册", "能注册", "可以注册", "注册开关")):
            return await self._answer_register_enabled()
        if self._has_any(text, ("进度", "申请进展", "我的申请", "状态进展")):
            return await self._answer_site_progress()
        if self._has_any(text, ("请假", "假条", "请假记录", "请假申请")):
            return await self._answer_site_leaves()
        if self._has_any(text, ("晚自习", "自习签到", "今晚签到", "实验室签到")):
            return await self._answer_evening_study()
        if self._has_any(text, ("表单", "问卷", "收集")):
            form_id = self._extract_id(text, ("表单", "问卷", "收集", "form"))
            return await self._answer_form(form_id) if form_id else "需要告诉我表单编号，我才能查询表单详情。"
        if self._has_any(text, ("所有公开", "全部公开", "公开接口", "全部信息", "所有信息")):
            return "\n\n".join(
                [
                    await self._answer_club_home(),
                    await self._answer_member_count(),
                    await self._answer_people(),
                    await self._answer_departments(),
                    await self._answer_focus_or_tech(),
                    await self._answer_activities(),
                    await self._answer_recruitment(),
                    await self._answer_awards(),
                    await self._answer_register_enabled(),
                ]
            )

        # 兜底：不再无脑返回社团首页，而是给出引导提示
        return (
            "这个问题我暂时没有直接对应的数据。\n"
            "你可以试试问我：\n"
            "- 社团有多少人 / 社团概况\n"
            "- 有什么活动 / 看一下1号活动\n"
            "- 有哪些部门 / 主要人员\n"
            "- 招新开了吗 / 怎么加入\n"
            "- 晚自习签到\n"
            "- 我是谁\n"
            "- 查人 张三 / 查人 qq 123456\n"
            "或者直接跟我聊天，我会尽力帮你。"
        )

    # ==================================================================
    # 智能问答辅助方法：身份查询 / 寒暄 / 自我介绍
    # ==================================================================

    def _is_who_am_i_question(self, text: str) -> bool:
        """判断是否在问“我是谁 / 我叫什么”类身份问题。"""
        patterns = (
            "我是谁", "我叫什么", "我叫什么名字", "我叫啥", "我是哪个",
            "我的信息", "我的资料", "我的个人信息", "查一下我", "查查我",
            "who am i", "whoami", "tell me who i am",
        )
        return self._has_any(text, patterns)

    def _is_greeting(self, text: str) -> bool:
        """判断是否为寒暄问候 / 感谢。"""
        greetings = (
            "你好", "您好", "hi", "hello", "嗨", "哈喽", "在吗", "在不在",
            "早上好", "下午好", "晚上好", "晚安", "早安", "早啊", "中午好",
            "谢谢", "感谢", "thanks", "thank you", "辛苦了", "多谢",
            "牛逼", "厉害", "干得漂亮", "不错", "点赞", "棒",
        )
        stripped = text.strip()
        # 纯寒暄或短句（≤ 6 字）才匹配，避免“你好我想问活动”被误拦
        if len(stripped) <= 6 and self._has_any(stripped, greetings):
            return True
        # “你好!”“谢谢！”等带标点的短句
        for word in greetings:
            for suffix in ("", "了", "啊", "!", "！", "呀", "呢"):
                if stripped == word + suffix:
                    return True
        return False

    def _answer_greeting(self, text: str) -> str:
        """处理寒暄问候，返回自然回复。"""
        if self._has_any(text, ("谢谢", "感谢", "thanks", "thank you", "辛苦了", "多谢")):
            return "不客气～有其他问题随时问我。"
        if self._has_any(text, ("牛逼", "厉害", "干得漂亮", "不错", "点赞", "棒")):
            return "谢谢夸奖～有什么想查的随时说。"
        if self._has_any(text, ("晚安", "早安", "早啊", "早上好", "中午好", "晚上好")):
            hour = datetime.now(ZoneInfo("Asia/Shanghai")).hour
            if 5 <= hour < 11:
                return "早上好～有什么可以帮你的？可以问活动、招新、人数，也可以问“我是谁”。"
            if 11 <= hour < 14:
                return "中午好～有什么可以帮你的？可以问活动、招新、人数，也可以问“我是谁”。"
            if 18 <= hour < 23:
                return "晚上好～有什么可以帮你的？可以问活动、招新、人数，也可以问“我是谁”。"
            return "夜深了～有什么可以帮你的？可以问活动、招新、人数，也可以问“我是谁”。"
        if self._has_any(text, ("在吗", "在不在")):
            return "在的～有什么可以帮你？可以问社团活动、招新、人数、部门，也可以问“我是谁”。"
        return "你好～我是开放原子开源社团的机器人助手，可以帮你查社团信息。你可以问“社团有多少人”“有什么活动”“我是谁”等。"

    def _answer_bot_intro(self) -> str:
        """机器人自我介绍。"""
        return (
            "我是开放原子开源社团的机器人助手，可以帮你查询社团系统的各类信息：\n"
            "- 社团概况 / 多少人 / 统计数据\n"
            "- 活动列表 / 活动详情\n"
            "- 主要人员 / 部门架构\n"
            "- 招新信息 / 获奖荣誉\n"
            "- 晚自习签到\n"
            "- 查人（按姓名或QQ号）\n"
            "- 你是谁（通过QQ查你在系统里的信息）\n"
            "- 请假（群里艾特我说“我要请假”即可开始）\n"
            "你也可以用 /oa 帮助 查看完整指令列表。"
        )

    async def _answer_who_am_i(self, event: AstrMessageEvent) -> str | None:
        """通过当前用户的 QQ openid 查询其在社团系统中的身份信息。"""
        sender_id = self._event_sender_id(event)
        if not sender_id:
            return None
        params = {"qqOpenid": sender_id, "limit": "1"}
        data, error = await self._get_data("/bot/users/lookup", params)
        if error:
            return error
        records = data.get("list") if isinstance(data, dict) else data
        if not isinstance(records, list) or not records:
            return (
                f"我在系统里还没有查到你的信息。\n"
                f"你的 QQ（{sender_id}）尚未绑定系统账号。\n"
                "请登录网页个人中心生成绑定码，再发送 /oa bind-qq 绑定码 绑定。"
            )
        item = records[0]
        if not isinstance(item, dict):
            return str(item)
        name = item.get("realName") or item.get("userName") or "未命名用户"
        parts = [f"你是 {name}。"]
        user_name = item.get("userName")
        if user_name and user_name != name:
            parts.append(f"用户名：{user_name}")
        student_id = item.get("studentId")
        if student_id:
            parts.append(f"学号：{student_id}")
        org = " / ".join(
            str(v) for v in (item.get("college"), item.get("major"), item.get("className")) if v
        )
        if org:
            parts.append(org)
        grade = item.get("grade")
        if grade:
            parts.append(f"年级：{grade}")
        status = self._format_user_status(item.get("userStatus"))
        if status:
            parts.append(f"状态：{status}")
        return "；".join(parts) + "。"

    async def _answer_user_lookup(self, keyword: str | None, lookup_type: str | None = None) -> str:
        value = self._normalize_user_lookup_keyword(keyword)
        if not value:
            return "请告诉我要查的 QQ 号或姓名，例如：/oa 查人 qq 123456，或 /oa 查人 张三。"

        params = {"limit": "5"}
        if lookup_type == "qq":
            params["qqOpenid"] = value
        else:
            params["keyword"] = value
        data, error = await self._get_data("/bot/users/lookup", params)
        if error:
            return error
        return self._format_user_lookup_result(data, lookup_type, value)

    def _parse_user_lookup_command(self, parts: tuple[str, ...]) -> tuple[str | None, str | None]:
        text = " ".join(str(part) for part in parts if part is not None).strip()
        if not text:
            return None, None
        lower = text.lower()
        qq_match = re.match(r"^(?:qq|qq号|qq号码)\s*[:：=]?\s*(.+)$", text, re.IGNORECASE)
        if qq_match:
            return "qq", qq_match.group(1)
        name_match = re.match(r"^(?:name|姓名|名字)\s*[:：=]?\s*(.+)$", text, re.IGNORECASE)
        if name_match:
            return "name", name_match.group(1)
        if "qq" in lower:
            qq_value = self._extract_qq_lookup(text)
            if qq_value:
                return "qq", qq_value
        return "name", text

    def _extract_user_lookup_from_question(self, question: str) -> tuple[str | None, str | None]:
        raw = (question or "").strip()
        text = raw.lower()
        if not self._has_any(text, ("查人", "找人", "查询用户", "用户查询", "qq号", "qq ", "姓名", "名字")):
            return None, None
        if "qq" in text:
            qq_value = self._extract_qq_lookup(raw)
            if qq_value:
                return "qq", qq_value
        patterns = (
            r"(?:查人|找人|查询用户|用户查询)\s*[:：]?\s*(.+)$",
            r"(?:姓名|名字|name)\s*(?:查|查询)?\s*[:：]?\s*(.+)$",
        )
        for pattern in patterns:
            match = re.search(pattern, raw, re.IGNORECASE)
            if match:
                value = self._normalize_user_lookup_keyword(match.group(1))
                if value:
                    return "name", value
        return None, None

    def _extract_qq_lookup(self, text: str) -> str | None:
        match = re.search(r"(?:qq|qq号|qq号码)\s*[:：=]?\s*([A-Za-z0-9_-]{5,80})", text, re.IGNORECASE)
        if match:
            return match.group(1)
        match = re.search(r"\b(\d{5,20})\b", text)
        return match.group(1) if match else None

    def _normalize_user_lookup_keyword(self, value: Any) -> str:
        text = str(value or "").strip()
        text = re.sub(r"^(?:qq|qq号|qq号码|name|姓名|名字)\s*[:：=]?\s*", "", text, flags=re.IGNORECASE)
        text = re.sub(r"^(?:查|查询|一下|用户|成员|同学)\s*", "", text)
        text = re.sub(r"(?:是谁|的信息|信息|资料|这个人|这个成员)\s*$", "", text)
        return text.strip(" \t\r\n，。；;：:,.")

    def _format_user_lookup_result(self, data: Any, lookup_type: str | None, keyword: str) -> str:
        records = data.get("list") if isinstance(data, dict) else data
        total = data.get("total") if isinstance(data, dict) else None
        if not isinstance(records, list) or not records:
            label = f"QQ {keyword}" if lookup_type == "qq" else keyword
            return f"没有查到 {label} 对应的系统用户。"
        total_count = int(total) if isinstance(total, int) else len(records)
        if total_count > len(records):
            lines = [f"查到 {total_count} 人，先展示前 {len(records)} 个："]
        else:
            lines = [f"查到 {len(records)} 人："]
        for index, item in enumerate(records, start=1):
            lines.append(f"{index}. {self._format_user_record(item)}")
        return self._truncate("\n".join(lines))

    def _format_user_record(self, item: Any) -> str:
        if not isinstance(item, dict):
            return self._excerpt(item, MAX_TEXT_CHARS)
        name = item.get("realName") or item.get("userName") or "未命名用户"
        parts = [str(name)]
        user_id = item.get("id")
        if user_id not in (None, ""):
            parts[0] += f"（ID {user_id}）"
        user_name = item.get("userName")
        if user_name and user_name != name:
            parts.append(f"用户名：{user_name}")
        student_id = item.get("studentId")
        if student_id:
            parts.append(f"学号：{student_id}")
        org = " / ".join(str(value) for value in (item.get("college"), item.get("major"), item.get("className")) if value)
        if org:
            parts.append(org)
        qq_bound = item.get("qqBound") if "qqBound" in item else bool(item.get("qqOpenid"))
        qq_status = "已绑定" if qq_bound else "未绑定"
        parts.append(f"QQ：{qq_status}")
        status = self._format_user_status(item.get("userStatus"))
        if status:
            parts.append(f"状态：{status}")
        return "；".join(parts)

    def _format_user_status(self, value: Any) -> str:
        status_map = {"active": "启用", "disabled": "禁用", "locked": "锁定"}
        return status_map.get(str(value or "").lower(), str(value or "") if value else "")

    async def _answer_club_home(self) -> str:
        data, error = await self._get_data("/site/club-home")
        if error:
            return error
        club = data.get("club") if isinstance(data, dict) and isinstance(data.get("club"), dict) else {}
        name = club.get("name") or "社团"
        desc = self._excerpt(club.get("description"), MAX_TEXT_CHARS)
        metrics = self._format_metrics(data.get("metrics") if isinstance(data, dict) else None)
        if desc and metrics:
            return f"{name}：{desc}\n{metrics}"
        if desc:
            return f"{name}：{desc}"
        return metrics or "暂时没有查询到社团介绍。"

    async def _answer_member_count(self) -> str:
        data, error = await self._get_data("/site/club-home")
        if error:
            return error
        metric = self._find_metric(data, ("成员", "人数"))
        if metric:
            return f"社团当前在册成员约 {metric} 人。"
        return "暂时没有查询到社团人数。"

    async def _answer_metrics(self) -> str:
        data, error = await self._get_data("/site/club-home")
        if error:
            return error
        metrics = self._format_metrics(data.get("metrics") if isinstance(data, dict) else None)
        return f"社团当前数据概况：{metrics}。" if metrics else "暂时没有查询到社团统计数据。"

    async def _answer_people(self) -> str:
        data, error = await self._get_data("/site/club-home")
        if error:
            return error
        people = data.get("people") if isinstance(data, dict) else None
        if not isinstance(people, list) or not people:
            return "首页暂时没有展示主要人员信息。"
        lines = ["目前首页展示的主要人员有："]
        for item in people:
            if not isinstance(item, dict):
                continue
            name = item.get("name") or item.get("realName") or item.get("userName") or "未命名成员"
            role = item.get("role") or item.get("position") or item.get("department")
            focus = item.get("focus")
            suffix = []
            if role:
                suffix.append(str(role))
            if focus:
                suffix.append(self._excerpt(focus, 40))
            lines.append(f"- {name}" + (f"（{'，'.join(suffix)}）" if suffix else ""))
        return "\n".join(lines)

    async def _answer_focus_or_tech(self) -> str:
        data, error = await self._get_data("/site/club-home")
        if error:
            return error
        if not isinstance(data, dict):
            return "暂时没有查询到社团方向信息。"
        lines = []
        focus_areas = data.get("focusAreas")
        if isinstance(focus_areas, list) and focus_areas:
            parts = []
            for item in focus_areas:
                if not isinstance(item, dict):
                    continue
                title = item.get("title")
                desc = self._excerpt(item.get("description"), 50)
                if title and desc:
                    parts.append(f"{title}（{desc}）")
                elif title:
                    parts.append(str(title))
            if parts:
                lines.append("社团主要方向：" + "、".join(parts) + "。")
        tech_stack = data.get("techStack")
        if isinstance(tech_stack, list) and tech_stack:
            lines.append("涉及技术：" + "、".join(str(item) for item in tech_stack if item) + "。")
        return "\n".join(lines) if lines else "暂时没有查询到社团方向或技术栈信息。"

    async def _answer_awards(self) -> str:
        data, error = await self._get_data("/site/club-home")
        if error:
            return error
        awards = data.get("awards") if isinstance(data, dict) else None
        if not isinstance(awards, list) or not awards:
            return "暂时没有查询到社团获奖信息。"
        lines = [f"目前首页展示了 {len(awards)} 条获奖信息："]
        for item in awards:
            if not isinstance(item, dict):
                continue
            title = item.get("title") or item.get("competitionName") or "未命名奖项"
            extra = "，".join(
                str(part)
                for part in (item.get("year"), item.get("awardLevel"), item.get("teamName"))
                if part
            )
            desc = self._excerpt(item.get("description"), 50)
            line = f"- {title}"
            if extra:
                line += f"（{extra}）"
            if desc:
                line += f"：{desc}"
            lines.append(line)
        return "\n".join(lines)

    async def _answer_activities(self) -> str:
        data, error = await self._get_data("/site/activities")
        if error:
            return error
        if not isinstance(data, list) or not data:
            return "目前没有查询到活动。"
        lines = [f"目前有 {len(data)} 个活动："]
        for index, item in enumerate(data, start=1):
            lines.append(f"{index}. {self._format_activity(item, detailed=False)}")
        return "\n".join(lines)

    async def _answer_activity_by_ordinal(self, ordinal: int) -> str:
        data, error = await self._get_data("/site/activities")
        if error:
            return error
        if not isinstance(data, list) or not data:
            return "目前没有查询到活动。"
        if ordinal < 1 or ordinal > len(data):
            return f"目前只查询到 {len(data)} 个活动，没有第 {ordinal} 个。"
        item = data[ordinal - 1]
        activity_id = item.get("id") if isinstance(item, dict) else None
        if activity_id:
            return await self._answer_activity_detail(str(activity_id), fallback=item)
        return self._format_activity(item, detailed=True) if isinstance(item, dict) else str(item)

    async def _answer_activity_detail(self, activity_id: str, fallback: dict[str, Any] | None = None) -> str:
        data, error = await self._get_data(f"/site/activities/{activity_id}")
        if error and fallback is None:
            return error
        item = data if error is None else fallback
        return self._format_activity(item, detailed=True) if isinstance(item, dict) else "没有查询到这个活动详情。"

    async def _try_answer_activity_by_title(self, text: str) -> str | None:
        if not self._has_any(text, ("活动", "openpixel", "微光", "测试", "共创", "大会")):
            return None
        data, error = await self._get_data("/site/activities")
        if error or not isinstance(data, list):
            return None
        normalized = self._normalize_text(text)
        for item in data:
            if not isinstance(item, dict):
                continue
            title = self._normalize_text(item.get("title") or item.get("name") or "")
            if not title:
                continue
            title_parts = [part for part in re.split(r"[-—_\s]+", title) if part]
            if title in normalized or any(part in normalized for part in title_parts):
                activity_id = item.get("id")
                if activity_id:
                    return await self._answer_activity_detail(str(activity_id), fallback=item)
                return self._format_activity(item, detailed=True)
        return None

    async def _answer_recruitment(self) -> str:
        data, error = await self._get_data("/site/recruitment")
        if error:
            return error
        if not isinstance(data, dict):
            return self._format_generic(data)
        club = data.get("club")
        campaigns = data.get("campaigns")
        club_name = club.get("name") if isinstance(club, dict) else "社团"
        if isinstance(campaigns, list) and campaigns:
            lines = [f"{club_name} 当前有 {len(campaigns)} 个招新计划："]
            for item in campaigns:
                lines.append(f"- {self._format_named_item(item)}")
            return "\n".join(lines)
        status = club.get("recruitmentStatus") if isinstance(club, dict) else None
        if status == "open":
            return f"{club_name} 当前处于招新开放状态。"
        if status == "closed":
            return f"{club_name} 当前暂未开放招新。"
        return f"{club_name} 暂时没有查询到正在发布的招新计划。"

    async def _answer_recruitment_detail(self, campaign_id: str) -> str:
        data, error = await self._get_data(f"/site/recruitment/{campaign_id}")
        if error:
            return error
        return self._format_named_item(data) if isinstance(data, dict) else self._format_generic(data)

    async def _answer_departments(self) -> str:
        data, error = await self._get_data("/clubs/1/departments")
        if error:
            return error
        if not isinstance(data, list) or not data:
            return "暂时没有查询到部门信息。"
        parts = []
        for item in data:
            if not isinstance(item, dict):
                continue
            name = item.get("name") or item.get("title")
            desc = self._excerpt(item.get("description"), 40)
            if name and desc:
                parts.append(f"{name}（{desc}）")
            elif name:
                parts.append(str(name))
        return "社团目前有：" + "、".join(parts) + "。" if parts else "暂时没有查询到部门信息。"

    async def _answer_school_calendar(self) -> str:
        data, error = await self._get_data("/site/school-calendar")
        if error:
            return error
        return self._format_generic(data)

    async def _answer_register_enabled(self) -> str:
        data, error = await self._get_data("/site/register-enabled")
        if error:
            return error
        return "当前开放注册。" if bool(data) else "当前暂未开放注册。"

    async def _answer_site_progress(self) -> str:
        data, error = await self._get_data("/site/progress")
        if error:
            return error
        return self._format_generic(data)

    async def _answer_site_leaves(self) -> str:
        data, error = await self._get_data("/site/leave-applications")
        if error:
            return error
        return self._format_generic(data)

    async def _answer_evening_study(self) -> str:
        data, error = await self._get_data("/bot/evening-study/today")
        if error:
            return error
        return self._format_evening_study(data)

    def _format_evening_study(self, data: Any) -> str:
        if not isinstance(data, dict):
            return self._format_generic(data)
        sessions = data.get("sessions") if isinstance(data.get("sessions"), list) else []
        date = data.get("date") or "今天"
        if not sessions:
            return f"{date} 暂未生成晚自习签到。"
        lines = [
            (
                f"{date} 晚自习签到：应到 {data.get('targetCount') or 0} 人，"
                f"已签 {data.get('signedCount') or 0} 人，"
                f"迟到 {data.get('lateCount') or 0} 人，"
                f"旷课 {data.get('absentCount') or 0} 人，"
                f"未签到 {data.get('pendingCount') or 0} 人，"
                f"请假剔除 {data.get('excusedCount') or 0} 人。"
            )
        ]
        for item in sessions[:8]:
            if not isinstance(item, dict):
                continue
            group = item.get("groupName") or "实验室分组"
            status = {"draft": "草稿", "open": "进行中", "closed": "已关闭"}.get(str(item.get("status") or ""), item.get("status") or "-")
            time_range = self._short_time_range(item.get("startAt"), item.get("endAt"))
            location = item.get("location") or "未设置地点"
            lines.append(
                f"- {group}：{status}，已签 {item.get('signedCount') or 0}/{item.get('targetCount') or 0}，"
                f"迟到 {item.get('lateCount') or 0}，旷课 {item.get('absentCount') or 0}，"
                f"请假 {item.get('excusedCount') or 0}，{time_range}，{location}"
            )
        return self._truncate("\n".join(lines))

    def _short_time_range(self, start: Any, end: Any) -> str:
        return f"{self._short_time(start)}-{self._short_time(end)}"

    def _short_time(self, value: Any) -> str:
        text = str(value or "").strip()
        if not text:
            return "不限"
        match = re.search(r"(\d{2}:\d{2})", text)
        if match:
            return match.group(1)
        return self._excerpt(text, 16) or "不限"

    async def _answer_form(self, form_id: str) -> str:
        data, error = await self._get_data(f"/site/forms/{form_id}")
        if error:
            return error
        return self._format_named_item(data) if isinstance(data, dict) else self._format_generic(data)

    async def _get_data(self, path: str, params: dict[str, str] | None = None) -> tuple[Any, str | None]:
        result = await self._request("GET", path, params=params)
        if not result["ok"]:
            return None, self._format_error(result)
        body = self._redact(result.get("body"))
        data = body.get("data") if isinstance(body, dict) and "data" in body else body
        return data, None

    async def _request(
        self,
        method: str,
        path: str,
        body: dict[str, Any] | None = None,
        params: dict[str, str] | None = None,
    ) -> dict[str, Any]:
        method = method.upper()
        normalized_path = self._normalize_path(path)
        if method not in SAFE_METHODS and (method, normalized_path) not in BIND_WRITE_PATHS:
            return {"ok": False, "status": 400, "body": {"message": "插件只允许读取数据，不允许写入或修改数据"}}

        url = self._join_url(normalized_path)
        headers = {"Accept": "application/json"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        if self.access_token:
            headers["jmiopenatom"] = self.access_token
            headers["Authorization"] = f"Bearer {self.access_token}"

        timeout = aiohttp.ClientTimeout(total=self.timeout)
        try:
            logger.info(f"OpenAtom API request: {method} {url}")
            async with aiohttp.ClientSession(timeout=timeout) as session:
                async with session.request(method, url, json=body, params=params, headers=headers) as response:
                    text = await response.text()
                    parsed = self._parse_response_text(text)
                    ok = 200 <= response.status < 300
                    if isinstance(parsed, dict) and parsed.get("code") not in (None, 0):
                        ok = False
                    return {"ok": ok, "status": response.status, "body": parsed}
        except Exception as exc:
            logger.warning(f"OpenAtom API request failed: {method} {url}: {exc}")
            return {"ok": False, "status": 0, "body": {"message": str(exc)}}

    async def _prepare_leave_attachments(self, attachments: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], str | None]:
        prepared: list[dict[str, Any]] = []
        for index, item in enumerate(attachments, start=1):
            content = str(item.get("content") or "").strip()
            if not content:
                continue
            if content.startswith("data:image/"):
                prepared.append(self._normalized_attachment(item, content))
                continue
            data_url = self._local_image_as_data_url(content)
            if data_url:
                prepared.append(self._normalized_attachment(item, data_url))
                continue
            data_url = self._raw_base64_image_as_data_url(content)
            if data_url:
                prepared.append(self._normalized_attachment(item, data_url))
                continue
            if content.startswith(("http://", "https://")):
                prepared.append(self._normalized_attachment(item, content))
                continue
            return [], "没有拿到可上传的图片内容。请直接发送图片原图，或发送可公开访问的图片链接。"
        if not prepared:
            return [], "没有拿到可上传的图片内容。请直接发送图片原图，或发送可公开访问的图片链接。"
        return prepared[:5], None

    async def _download_image_as_data_url(self, url: str) -> tuple[str | None, str | None]:
        timeout = aiohttp.ClientTimeout(total=self.timeout)
        try:
            async with aiohttp.ClientSession(timeout=timeout) as session:
                async with session.get(url) as response:
                    if response.status < 200 or response.status >= 300:
                        return None, f"图片下载失败 HTTP {response.status}"
                    content_length = response.headers.get("Content-Length")
                    if content_length and int(content_length) > MAX_ATTACHMENT_BYTES:
                        return None, "图片超过 6MB"
                    data = await response.read()
                    if len(data) > MAX_ATTACHMENT_BYTES:
                        return None, "图片超过 6MB"
                    content_type = str(response.headers.get("Content-Type") or "").split(";", 1)[0].strip().lower()
                    detected_type = self._detect_image_type(data)
                    if not content_type.startswith("image/"):
                        content_type = detected_type
                    if not content_type.startswith("image/"):
                        return None, "链接内容不是图片"
                    encoded = base64.b64encode(data).decode("ascii")
                    return f"data:{content_type};base64,{encoded}", None
        except Exception as exc:
            logger.warning(f"OpenAtom leave attachment download failed: {url}: {exc}")
            return None, str(exc)

    def _normalized_attachment(self, item: dict[str, Any], content: str) -> dict[str, Any]:
        return {
            "name": str(item.get("name") or "leave-attachment"),
            "type": self._guess_attachment_type(content, str(item.get("type") or "image")),
            "size": item.get("size") or len(content),
            "content": content,
        }

    def _local_image_as_data_url(self, value: str) -> str | None:
        if not value or value.startswith(("http://", "https://")):
            return None
        try:
            path = Path(value).expanduser()
            if not path.is_file():
                return None
            data = path.read_bytes()
            if len(data) > MAX_ATTACHMENT_BYTES:
                return None
            content_type = self._detect_image_type(data)
            if not content_type.startswith("image/"):
                return None
            encoded = base64.b64encode(data).decode("ascii")
            return f"data:{content_type};base64,{encoded}"
        except Exception:
            return None

    def _raw_base64_image_as_data_url(self, value: str) -> str | None:
        compact = value.strip()
        if not compact or len(compact) < 64 or not re.fullmatch(r"[A-Za-z0-9+/=\s]+", compact):
            return None
        try:
            data = base64.b64decode(re.sub(r"\s+", "", compact), validate=True)
        except Exception:
            return None
        if len(data) > MAX_ATTACHMENT_BYTES:
            return None
        content_type = self._detect_image_type(data)
        if not content_type.startswith("image/"):
            return None
        return f"data:{content_type};base64,{base64.b64encode(data).decode('ascii')}"

    def _detect_image_type(self, data: bytes) -> str:
        if data.startswith(b"\x89PNG\r\n\x1a\n"):
            return "image/png"
        if data.startswith(b"\xff\xd8\xff"):
            return "image/jpeg"
        if data.startswith((b"GIF87a", b"GIF89a")):
            return "image/gif"
        if data.startswith(b"RIFF") and data[8:12] == b"WEBP":
            return "image/webp"
        return ""

    def _ensure_callback_server(self):
        if self.callback_task is not None and not self.callback_task.done():
            return
        if self.callback_runner is not None:
            return
        try:
            self.callback_task = asyncio.create_task(self._start_callback_server())
        except RuntimeError:
            self.callback_task = None

    async def _start_callback_server(self):
        try:
            app = web.Application()
            app.router.add_get("/openatom/health", self._handle_callback_health)
            app.router.add_post("/openatom/leave-review", self._handle_leave_review_callback)
            runner = web.AppRunner(app)
            await runner.setup()
            site = web.TCPSite(runner, self.callback_host, self.callback_port)
            await site.start()
            self.callback_runner = runner
            self.callback_site = site
            logger.info(f"OpenAtom leave review callback server started: {self.callback_host}:{self.callback_port}")
        except Exception as exc:
            logger.warning(f"OpenAtom leave review callback server failed to start: {exc}")
            self.callback_runner = None
            self.callback_site = None

    async def _handle_callback_health(self, request: web.Request) -> web.Response:
        return web.json_response({"ok": True, "pluginVersion": PLUGIN_VERSION})

    async def _handle_leave_review_callback(self, request: web.Request) -> web.Response:
        if self.callback_token:
            token = request.headers.get("X-OpenAtom-Bot-Token", "")
            if token != self.callback_token:
                return web.json_response({"ok": False, "message": "invalid token"}, status=403)
        try:
            payload = await request.json()
        except Exception:
            return web.json_response({"ok": False, "message": "invalid json"}, status=400)
        if not isinstance(payload, dict):
            return web.json_response({"ok": False, "message": "invalid payload"}, status=400)
        item = {
            "unifiedMsgOrigin": payload.get("botNotifyOrigin"),
            "senderId": payload.get("botNotifyUserId"),
            "leaveId": payload.get("leaveId"),
            "title": payload.get("title"),
        }
        data = {
            "id": payload.get("leaveId"),
            "status": payload.get("status"),
            "title": payload.get("title"),
            "reviewComment": payload.get("reviewComment"),
        }
        sent = await self._send_leave_status_notification(item, data)
        if not sent:
            return web.json_response({"ok": False, "message": "send failed"}, status=500)
        logger.info(
            f"OpenAtom leave review callback sent: leaveId={payload.get('leaveId')}, "
            f"status={payload.get('status')}, origin={payload.get('botNotifyOrigin')}"
        )
        return web.json_response({"ok": True})

    async def _send_leave_status_notification(self, item: dict[str, Any], data: dict[str, Any]) -> bool:
        origin = str(item.get("unifiedMsgOrigin") or "")
        sender_id = str(item.get("senderId") or "")
        if not origin:
            logger.warning(f"OpenAtom leave status notification skipped: empty origin, leaveId={data.get('id') or item.get('leaveId')}")
            return True
        leave_id = data.get("id") or item.get("leaveId")
        title = data.get("title") or item.get("title") or "请假申请"
        status = str(data.get("status") or "")
        if status == "approved":
            text = f" 你的请假申请“{title}”（编号 {leave_id}）已通过。"
        elif status == "rejected":
            reason = data.get("reviewComment") or "未填写驳回理由"
            text = f" 你的请假申请“{title}”（编号 {leave_id}）未通过，原因：{reason}"
        else:
            return False
        return await self._send_plain_message(origin, text.strip(), sender_id if "FriendMessage" not in origin else None)

    async def _send_plain_message(self, origin: str, text: str, at_user: str | None = None) -> bool:
        if not origin:
            return False
        try:
            import astrbot.api.message_components as Comp

            chain_items = self._build_plain_chain(Comp, text, at_user)
            chain = OutgoingMessageChain(chain_items)
            await self.context.send_message(origin, chain)
            return True
        except Exception as exc:
            logger.warning(f"OpenAtom active plain message failed: origin={origin}, error={exc}")
            return False

    def _plain_result(self, event: AstrMessageEvent, text: str):
        message = str(text or "")
        try:
            import astrbot.api.message_components as Comp

            chain = self._build_plain_chain(Comp, message)
            for method_name in ("chain_result", "message_result"):
                method = getattr(event, method_name, None)
                if callable(method):
                    return method(chain)
            return OutgoingMessageChain(chain)
        except Exception as exc:
            logger.warning(f"OpenAtom plain reply fallback to plain_result: {exc}")
            return event.plain_result(message)

    def _build_plain_chain(self, Comp: Any, text: str, at_user: str | None = None) -> list[Any]:
        chain: list[Any] = []
        if at_user:
            chain.append(Comp.At(qq=at_user))
        chain.append(Comp.Plain(str(text or "")))
        return chain

    async def terminate(self):
        if self.callback_task is not None:
            self.callback_task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self.callback_task
        if self.callback_runner is not None:
            await self.callback_runner.cleanup()

    def _success_message(self, result: dict[str, Any], fallback: str) -> str:
        if not result["ok"]:
            return self._format_error(result)
        body = result.get("body")
        message = body.get("message") if isinstance(body, dict) else None
        return str(message or fallback)

    def _event_sender_id(self, event: AstrMessageEvent) -> str:
        try:
            return str(event.get_sender_id() or "").strip()
        except Exception:
            return ""

    def _onebot_raw_event(self, event: AstrMessageEvent) -> Any:
        message_obj = getattr(event, "message_obj", None)
        return getattr(message_obj, "raw_message", None) if message_obj is not None else None

    def _raw_event_value(self, raw: Any, key: str) -> Any:
        if raw is None:
            return None
        getter = getattr(raw, "get", None)
        if callable(getter):
            return getter(key)
        return getattr(raw, key, None)

    def _leave_session_key(self, event: AstrMessageEvent, sender_id: str) -> str:
        message_obj = getattr(event, "message_obj", None)
        session_id = getattr(message_obj, "session_id", "") if message_obj is not None else ""
        group_id = getattr(message_obj, "group_id", "") if message_obj is not None else ""
        return f"{group_id or session_id or 'private'}:{sender_id}"

    def _private_leave_session_key(self, sender_id: str) -> str:
        return f"private:{sender_id}"

    def _is_group_event(self, event: AstrMessageEvent) -> bool:
        message_obj = getattr(event, "message_obj", None)
        group_id = getattr(message_obj, "group_id", "") if message_obj is not None else ""
        origin = str(getattr(event, "unified_msg_origin", "") or "")
        return bool(group_id) or "GroupMessage" in origin

    def _private_origin_for_event(self, event: AstrMessageEvent, sender_id: str) -> str:
        origin = str(getattr(event, "unified_msg_origin", "") or "")
        parts = origin.split(":")
        platform = parts[0] if parts and parts[0] else "default"
        return f"{platform}:FriendMessage:{sender_id}"

    def _event_plain_text(self, event: AstrMessageEvent) -> str:
        value = getattr(event, "message_str", None)
        if value is None:
            message_obj = getattr(event, "message_obj", None)
            value = getattr(message_obj, "message_str", "") if message_obj is not None else ""
        return str(value or "").strip()

    def _event_attachments(self, event: AstrMessageEvent) -> list[dict[str, Any]]:
        attachments: list[dict[str, Any]] = []
        message_obj = getattr(event, "message_obj", None)
        chain = getattr(message_obj, "message", []) if message_obj is not None else []
        for index, item in enumerate(chain or []):
            type_name = item.__class__.__name__.lower()
            if "image" not in type_name and "file" not in type_name:
                continue
            content = self._first_attr(item, ("url", "file", "path", "content"))
            if not content:
                continue
            attachments.append(
                {
                    "name": self._first_attr(item, ("name", "file_name", "filename")) or f"bot-attachment-{index + 1}",
                    "type": self._guess_attachment_type(content, type_name),
                    "size": self._first_attr(item, ("size", "file_size")) or 0,
                    "content": content,
                }
            )
        text = self._event_plain_text(event)
        for index, url in enumerate(re.findall(r"https?://\\S+", text)):
            if any(item.get("content") == url for item in attachments):
                continue
            attachments.append(
                {
                    "name": f"bot-url-attachment-{index + 1}",
                    "type": self._guess_attachment_type(url, "image"),
                    "size": 0,
                    "content": url.rstrip("，。；;"),
                }
            )
        return attachments

    def _first_attr(self, item: Any, names: tuple[str, ...]) -> Any:
        for name in names:
            value = getattr(item, name, None)
            if value:
                return str(value)
        return None

    def _guess_attachment_type(self, content: Any, fallback: str) -> str:
        text = str(content or "").split("?", 1)[0].lower()
        if text.startswith("data:image/"):
            return text.split(";", 1)[0].replace("data:", "")
        if any(text.endswith(ext) for ext in (".jpg", ".jpeg")):
            return "image/jpeg"
        if text.endswith(".png"):
            return "image/png"
        if text.endswith(".gif"):
            return "image/gif"
        return "image/*" if "image" in fallback else ""

    def _looks_like_leave_request(self, text: str) -> bool:
        value = (text or "").strip().lower()
        if not value:
            return False
        keywords = (
            "我要请假",
            "我想请假",
            "想请假",
            "请个假",
            "请假",
            "假条",
            "请假条",
            "请假申请",
            "要请假",
            "请一天假",
            "请半天假",
            "去不了",
            "来不了",
            "到不了",
            "不能去",
            "没法去",
            "没办法去",
            "不参加",
            "不能参加",
            "缺席",
            "请假一下",
            "帮我请假",
        )
        if any(keyword in value for keyword in keywords):
            return True
        return bool(re.search(r"(今天|明天|后天|今晚|明晚|周[一二三四五六日天]|星期[一二三四五六日天]).*(去不了|来不了|缺席|请假)", value))

    async def _parse_leave_time_with_ai(self, event: AstrMessageEvent, text: str) -> tuple[str | None, str | None]:
        now = datetime.now(ZoneInfo("Asia/Shanghai"))
        prompt = (
            "你是请假时间解析器。请把用户输入解析成北京时间的请假开始和结束时间，只输出 JSON，不要解释。\n"
            f"当前时间：{now.strftime('%Y-%m-%d %H:%M:%S')}，时区：Asia/Shanghai。\n"
            "输出格式：{\"startAt\":\"YYYY-MM-DD HH:mm:ss 或 null\",\"endAt\":\"YYYY-MM-DD HH:mm:ss 或 null\"}\n"
            "规则：\n"
            "1. 理解中文相对时间，例如今天、明天、后天、今晚、明晚、本周五、下周三。\n"
            "2. 理解模糊时段：上午默认 08:00-12:00，下午默认 14:00-18:00，晚上默认 18:00-22:00。\n"
            "3. 例如“明晚七点到九点”应解析为明天 19:00 到 21:00；有上下文时不要把晚上九点解析成 09:00。\n"
            "4. 如果只有一个明确时间，endAt 可以为 null；如果无法判断，两个字段都为 null。\n"
            f"用户输入：{text}"
        )
        try:
            ai_text = await self._call_llm_text(event, prompt)
            parsed = self._parse_ai_time_json(ai_text)
            if parsed:
                return parsed
        except Exception as exc:
            logger.warning(f"OpenAtom leave time AI parse failed, fallback to local parser: {exc}")
        return self._parse_leave_time(text)

    async def _call_llm_text(self, event: AstrMessageEvent, prompt: str) -> str:
        context = self.context
        origin = getattr(event, "unified_msg_origin", None)
        provider = None

        for method_name in ("get_using_provider", "get_default_provider"):
            method = getattr(context, method_name, None)
            if not callable(method):
                continue
            try:
                provider = await self._maybe_await_call(method, umo=origin)
            except TypeError:
                provider = None
            if provider is None:
                try:
                    provider = await self._maybe_await_call(method)
                except TypeError:
                    provider = None
            if provider is not None:
                break

        if provider is not None:
            for method_name in ("text_chat", "chat", "generate", "ask"):
                method = getattr(provider, method_name, None)
                if not callable(method):
                    continue
                result = await self._try_llm_call(method, prompt, origin)
                text = self._llm_result_to_text(result)
                if text:
                    return text

        for method_name in ("llm_generate", "text_chat", "chat", "generate", "ask"):
            method = getattr(context, method_name, None)
            if not callable(method):
                continue
            result = await self._try_llm_call(method, prompt, origin)
            text = self._llm_result_to_text(result)
            if text:
                return text

        return ""

    async def _try_llm_call(self, method: Any, prompt: str, origin: Any) -> Any:
        attempts = (
            {"prompt": prompt, "session_id": origin},
            {"prompt": prompt},
            {"message": prompt, "session_id": origin},
            {"message": prompt},
            {},
        )
        last_error: Exception | None = None
        for kwargs in attempts:
            try:
                if kwargs:
                    return await self._maybe_await_call(method, **kwargs)
                return await self._maybe_await_call(method, prompt)
            except TypeError as exc:
                last_error = exc
                continue
        if last_error:
            raise last_error
        return None

    async def _maybe_await_call(self, method: Any, *args: Any, **kwargs: Any) -> Any:
        value = method(*args, **kwargs)
        if inspect.isawaitable(value):
            return await value
        return value

    def _llm_result_to_text(self, result: Any) -> str:
        if result is None:
            return ""
        if isinstance(result, str):
            return result.strip()
        for name in ("completion_text", "text", "content", "result", "message"):
            value = getattr(result, name, None)
            if value:
                return str(value).strip()
        if isinstance(result, dict):
            for key in ("completion_text", "text", "content", "result", "message"):
                value = result.get(key)
                if value:
                    return str(value).strip()
        return str(result).strip()

    def _parse_ai_time_json(self, text: str) -> tuple[str | None, str | None] | None:
        if not text:
            return None
        content = str(text).strip()
        fence = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", content, re.S)
        if fence:
            content = fence.group(1)
        else:
            match = re.search(r"\{.*\}", content, re.S)
            if match:
                content = match.group(0)
        try:
            data = json.loads(content)
        except Exception:
            return None
        if not isinstance(data, dict):
            return None
        start_at = self._normalize_ai_datetime(data.get("startAt") or data.get("start_at"))
        end_at = self._normalize_ai_datetime(data.get("endAt") or data.get("end_at"))
        if not start_at and not end_at:
            return None
        return start_at, end_at

    def _normalize_ai_datetime(self, value: Any) -> str | None:
        if value in (None, "", "null"):
            return None
        normalized = self._normalize_datetime(str(value))
        if normalized and re.match(r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$", normalized):
            return normalized
        return None

    def _parse_leave_time(self, text: str) -> tuple[str | None, str | None]:
        raw = self._normalize_chinese_time_text((text or "").strip())
        if not raw:
            return None, None
        now = datetime.now(ZoneInfo("Asia/Shanghai"))
        base_date = self._parse_leave_date(raw, now)
        explicit_values = self._parse_explicit_datetimes(raw, now)
        if len(explicit_values) >= 2:
            return explicit_values[0], explicit_values[1]
        if len(explicit_values) == 1:
            first = explicit_values[0]
            date_part = first[:10]
            time_range = self._parse_time_range(raw)
            if time_range and len(time_range) >= 2:
                return f"{date_part} {time_range[0]}", f"{date_part} {time_range[1]}"
            return first, None

        if base_date is None:
            return None, None
        time_range = self._parse_time_range(raw)
        if time_range and len(time_range) >= 2:
            return self._combine_date_time(base_date, time_range[0]), self._combine_date_time(base_date, time_range[1])
        if time_range and len(time_range) == 1:
            return self._combine_date_time(base_date, time_range[0]), None

        default_range = self._default_time_range(raw)
        return self._combine_date_time(base_date, default_range[0]), self._combine_date_time(base_date, default_range[1])

    def _parse_explicit_datetimes(self, text: str, now: datetime) -> list[str]:
        values: list[str] = []
        full_pattern = r"(?:(\d{4})[-/年])?(\d{1,2})[-/月](\d{1,2})日?(?:\s*(上午|中午|下午|晚上|晚|早上|傍晚)?\s*(\d{1,2})(?:[:：点时](\d{1,2}))?)?"
        for match in re.finditer(full_pattern, text):
            year, month, day, period, hour, minute = match.groups()
            if hour is None and not re.search(r"[-/年月日]", match.group(0)):
                continue
            date_value = datetime(int(year or now.year), int(month), int(day), tzinfo=now.tzinfo)
            if hour is None:
                values.append(self._combine_date_time(date_value, "00:00:00"))
            else:
                values.append(self._combine_date_time(date_value, self._normalize_time(hour, minute, period)))
        return values

    def _parse_leave_date(self, text: str, now: datetime) -> datetime | None:
        compact = text.replace(" ", "")
        if any(word in compact for word in ("今天", "今日", "今晚")):
            return now
        if any(word in compact for word in ("明天", "明日", "明晚")):
            return now + timedelta(days=1)
        if "后天" in compact:
            return now + timedelta(days=2)
        month_day = re.search(r"(?:(\d{4})年)?(\d{1,2})月(\d{1,2})[日号]?", compact)
        if month_day:
            year, month, day = month_day.groups()
            return datetime(int(year or now.year), int(month), int(day), tzinfo=now.tzinfo)
        slash_day = re.search(r"(?:(\d{4})[-/])?(\d{1,2})[-/](\d{1,2})", compact)
        if slash_day:
            year, month, day = slash_day.groups()
            return datetime(int(year or now.year), int(month), int(day), tzinfo=now.tzinfo)
        day_only = re.search(r"(\d{1,2})[号日]", compact)
        if day_only:
            candidate = datetime(now.year, now.month, int(day_only.group(1)), tzinfo=now.tzinfo)
            if candidate.date() < now.date():
                month = now.month + 1
                year = now.year + (1 if month > 12 else 0)
                month = 1 if month > 12 else month
                candidate = datetime(year, month, int(day_only.group(1)), tzinfo=now.tzinfo)
            return candidate
        week_map = {"一": 0, "二": 1, "三": 2, "四": 3, "五": 4, "六": 5, "日": 6, "天": 6}
        week_match = re.search(r"(?:下?周|星期)([一二三四五六日天])", compact)
        if week_match:
            target = week_map[week_match.group(1)]
            delta = (target - now.weekday()) % 7
            if delta == 0 or "下周" in compact:
                delta += 7
            return now + timedelta(days=delta)
        if re.search(r"\d{1,2}(?:[:：点时]\d{0,2})", compact):
            return now
        return None

    def _parse_time_range(self, text: str) -> list[str]:
        matches: list[tuple[str, str, str | None]] = []
        time_pattern = r"(?<![月/\-])(?:(上午|中午|下午|晚上|晚|早上|傍晚)\s*)?(\d{1,2})(?:[:：](\d{2})|[点时](\d{1,2})?)"
        last_period = ""
        for period, hour, minute1, minute2 in re.findall(time_pattern, text):
            if period:
                last_period = period
            elif last_period in {"下午", "晚上", "晚", "傍晚", "上午", "早上", "中午"}:
                period = last_period
            matches.append((period, hour, minute1 or minute2 or "0"))
        if not matches:
            range_match = re.search(r"(上午|中午|下午|晚上|晚|早上|傍晚)?\s*(\d{1,2})\s*(?:到|至|-|~)\s*(\d{1,2})", text)
            if range_match:
                period, start_hour, end_hour = range_match.groups()
                matches.extend([(period or "", start_hour, "0"), (period or "", end_hour, "0")])
        period_only = re.search(r"(上午|中午|下午|晚上|晚|早上|傍晚)", text)
        if not matches and period_only:
            return list(self._default_time_range(period_only.group(1)))
        times = [self._normalize_time(hour, minute, period) for period, hour, minute in matches]
        return [time for time in times if time]

    def _normalize_chinese_time_text(self, text: str) -> str:
        numbers = {
            "零": 0, "一": 1, "二": 2, "两": 2, "三": 3, "四": 4, "五": 5,
            "六": 6, "七": 7, "八": 8, "九": 9, "十": 10,
        }

        def convert(value: str) -> str:
            if value == "十":
                return "10"
            if value.startswith("十"):
                return str(10 + numbers.get(value[-1], 0))
            if value.endswith("十"):
                return str(numbers.get(value[0], 0) * 10)
            if "十" in value:
                left, right = value.split("十", 1)
                return str(numbers.get(left, 1) * 10 + numbers.get(right, 0))
            return str(numbers.get(value, value))

        text = re.sub(r"([一二两三四五六七八九十]{1,3})(?=点|时)", lambda m: convert(m.group(1)), text)
        text = re.sub(r"(\d{1,2})[点时]半", r"\1点30", text)
        return text

    def _normalize_time(self, hour: str, minute: str | None, period: str | None) -> str:
        h = int(hour)
        m = int(minute or 0)
        p = period or ""
        if p in {"下午", "晚上", "晚", "傍晚"} and h < 12:
            h += 12
        if p == "中午" and h < 11:
            h += 12
        if h > 23 or m > 59:
            return ""
        return f"{h:02d}:{m:02d}:00"

    def _default_time_range(self, text: str) -> tuple[str, str]:
        compact = text.replace(" ", "")
        if any(word in compact for word in ("早上", "上午")):
            return "08:00:00", "12:00:00"
        if "中午" in compact:
            return "12:00:00", "14:00:00"
        if "下午" in compact:
            return "14:00:00", "18:00:00"
        if any(word in compact for word in ("晚上", "晚", "今晚", "明晚")):
            return "18:00:00", "22:00:00"
        return "00:00:00", "23:59:00"

    def _combine_date_time(self, date_value: datetime, time_value: str) -> str:
        return f"{date_value.year:04d}-{date_value.month:02d}-{date_value.day:02d} {time_value}"

    def _normalize_datetime(self, value: str) -> str | None:
        value = (value or "").strip().replace("/", "-").replace("T", " ")
        match = re.match(r"(\d{4})-(\d{1,2})-(\d{1,2})(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?", value)
        if not match:
            return None
        year, month, day, hour, minute, second = match.groups()
        if hour is None:
            return f"{year}-{int(month):02d}-{int(day):02d} 00:00:00"
        return f"{year}-{int(month):02d}-{int(day):02d} {int(hour):02d}:{minute}:{second or '00'}"

    async def _is_qq_bound(self, qq_openid: str) -> tuple[bool, str | None]:
        data, error = await self._get_data("/auth/qq-bind/status", {"qqOpenid": qq_openid})
        if error:
            return False, error
        return bool(data), None

    def _format_activity(self, item: dict[str, Any], detailed: bool) -> str:
        title = item.get("title") or item.get("name") or "未命名活动"
        summary = item.get("summary") or item.get("description") or item.get("descriptionMarkdown")
        time_value = item.get("activityAt") or item.get("date") or item.get("startAt") or item.get("createdAt")
        location = item.get("location") or item.get("address")
        parts = [str(title)]
        if time_value:
            parts.append(f"时间：{time_value}")
        if location:
            parts.append(f"地点：{location}")
        if summary:
            parts.append(self._excerpt(summary, MAX_DETAIL_CHARS if detailed else MAX_TEXT_CHARS))
        if detailed:
            detail = item.get("descriptionMarkdown") or item.get("contentMarkdown")
            if detail and detail != summary:
                parts.append(f"详情：{self._excerpt(detail, MAX_DETAIL_CHARS)}")
        return "；".join(parts)

    def _format_named_item(self, item: dict[str, Any]) -> str:
        title = item.get("title") or item.get("name") or item.get("clubName") or "未命名"
        summary = item.get("summary") or item.get("description") or item.get("content") or item.get("descriptionMarkdown")
        start = item.get("startAt") or item.get("applyStartAt") or item.get("createdAt")
        end = item.get("endAt") or item.get("applyEndAt")
        location = item.get("location") or item.get("address")
        parts = [str(title)]
        if start and end:
            parts.append(f"时间：{start} 至 {end}")
        elif start:
            parts.append(f"时间：{start}")
        if location:
            parts.append(f"地点：{location}")
        if summary:
            parts.append(self._excerpt(summary, MAX_TEXT_CHARS))
        return "；".join(parts)

    def _format_metrics(self, metrics: Any) -> str:
        if not isinstance(metrics, list):
            return ""
        parts = []
        for metric in metrics:
            if not isinstance(metric, dict):
                continue
            label = metric.get("label")
            value = metric.get("value")
            if label and value not in (None, ""):
                parts.append(f"{label} {value}")
        return "，".join(parts)

    def _find_metric(self, data: Any, keywords: tuple[str, ...]) -> str | None:
        metrics = data.get("metrics") if isinstance(data, dict) else None
        if not isinstance(metrics, list):
            return None
        for metric in metrics:
            if not isinstance(metric, dict):
                continue
            label = str(metric.get("label") or "")
            if any(keyword in label for keyword in keywords):
                value = metric.get("value")
                return str(value) if value not in (None, "") else None
        return None

    def _format_generic(self, value: Any) -> str:
        if isinstance(value, list):
            if not value:
                return "没有查询到相关数据。"
            lines = [f"查询到 {len(value)} 条数据："]
            for index, item in enumerate(value, start=1):
                lines.append(f"{index}. {self._format_generic_record(item)}")
            return self._truncate("\n".join(lines))
        if isinstance(value, dict):
            return self._truncate(self._format_generic_record(value))
        return self._truncate(self._excerpt(value, MAX_DETAIL_CHARS))

    def _format_generic_record(self, item: Any) -> str:
        if not isinstance(item, dict):
            return self._excerpt(item, MAX_TEXT_CHARS)
        if item.get("title") or item.get("name"):
            return self._format_named_item(item)
        readable = []
        for key, value in item.items():
            if key in SENSITIVE_KEYS or value in (None, ""):
                continue
            if isinstance(value, (dict, list)):
                continue
            readable.append(f"{key}：{self._excerpt(value, 40)}")
            if len(readable) >= 6:
                break
        return "；".join(readable) if readable else "已查询到数据。"

    def _join_url(self, path: str) -> str:
        path = self._normalize_path(path)
        parts = [quote(part, safe="") for part in path.split("/") if part]
        return f"{self.base_url}/{'/'.join(parts)}"

    def _normalize_path(self, path: str) -> str:
        path = (path or "").strip()
        if not path.startswith("/"):
            path = "/" + path
        return path

    def _parse_query(self, params: tuple[str, ...]) -> dict[str, str]:
        query = {}
        for item in params:
            if "=" not in item:
                continue
            key, value = item.split("=", 1)
            key = key.strip()
            if key:
                query[key] = value.strip()
        return query

    def _format_response(self, result: dict[str, Any]) -> str:
        if not result["ok"]:
            return self._format_error(result)
        body = self._redact(result["body"])
        data = body.get("data") if isinstance(body, dict) and "data" in body else body
        return self._format_generic(data)

    def _format_error(self, result: dict[str, Any]) -> str:
        body = result.get("body")
        message = body.get("message") if isinstance(body, dict) else str(body)
        status = result.get("status", 0)
        if status == 405:
            return self._truncate(
                f"请求失败 HTTP 405\n{message}\n"
                "请检查后端是否已部署支持 QQ 绑定的新版本，或在 /oa config 查看 base_url 是否指向了正确后端。"
            )
        return self._truncate(f"请求失败 HTTP {status}\n{message}")

    def _parse_response_text(self, text: str) -> Any:
        if not text:
            return {}
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return text

    def _redact(self, value: Any) -> Any:
        if isinstance(value, dict):
            return {
                key: "***" if str(key) in SENSITIVE_KEYS else self._redact(item)
                for key, item in value.items()
            }
        if isinstance(value, list):
            return [self._redact(item) for item in value]
        return value

    def _has_any(self, text: str, words: tuple[str, ...]) -> bool:
        return any(word in text for word in words)

    def _normalize_text(self, value: Any) -> str:
        return re.sub(r"\s+", "", str(value or "").lower())

    def _extract_id(self, text: str, words: tuple[str, ...]) -> str | None:
        if not self._has_any(text, words):
            return None
        match = re.search(r"(?:id|编号)\s*[:：]?\s*(\d+)", text, re.IGNORECASE)
        return match.group(1) if match else None

    def _extract_activity_ordinal(self, text: str) -> int | None:
        match = re.search(r"(?:第\s*)?(\d+)\s*(?:个|号)?\s*活动", text)
        if match:
            return int(match.group(1))
        chinese = {"一": 1, "二": 2, "三": 3, "四": 4, "五": 5, "六": 6, "七": 7, "八": 8, "九": 9, "十": 10}
        match = re.search(r"(?:第\s*)?([一二三四五六七八九十])\s*(?:个|号)?\s*活动", text)
        return chinese.get(match.group(1)) if match else None

    def _excerpt(self, value: Any, limit: int) -> str:
        if value is None:
            return ""
        text = str(value).replace("\r\n", "\n").replace("\r", "\n")
        text = re.sub(r"[#>*_`~-]+", "", text)
        text = " ".join(part.strip() for part in text.splitlines() if part.strip())
        if len(text) <= limit:
            return text
        return text[:limit].rstrip() + "..."

    def _truncate(self, text: str) -> str:
        if self.max_reply_chars <= 0 or len(text) <= self.max_reply_chars:
            return text
        return text[: self.max_reply_chars] + "\n...（响应过长，已截断）"

    def _normalize_base_url(self, value: Any) -> str:
        url = str(value or "").strip() or "http://host.docker.internal:8921/api/v1"
        return url.rstrip("/")
