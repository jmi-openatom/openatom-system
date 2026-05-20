import json
import re
from typing import Any
from urllib.parse import quote

import aiohttp
from astrbot.api import AstrBotConfig, logger
from astrbot.api.event import AstrMessageEvent, filter
from astrbot.api.star import Context, Star, register


SAFE_METHODS = {"GET"}
BIND_WRITE_PATHS = {
    ("POST", "/auth/qq-bind/confirm"),
}
SENSITIVE_KEYS = {"password", "accessToken", "refreshToken", "token", "authorization", "jmiopenatom"}
MAX_TEXT_CHARS = 90
MAX_DETAIL_CHARS = 220
PLUGIN_VERSION = "1.1.1"


@register(
    "astrbot_plugin_openatom_api",
    "ariven",
    "让 AstrBot/NapCat 机器人查询 OpenAtom System 后端公开数据并绑定 QQ",
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

    @filter.command_group("oa", alias={"openatom", "开放原子"})
    def oa(self):
        pass

    @oa.command("help", alias={"帮助"})
    async def help(self, event: AstrMessageEvent):
        """查看 OpenAtom API 插件用法"""
        yield event.plain_result(
            "\n".join(
                [
                    "OpenAtom API 指令：",
                    "/oa ping",
                    "/oa me",
                    "/oa bind-qq 绑定码",
                    "/oa ask 社团有多少人",
                    "/oa ask 主要人员有哪些",
                    "/oa ask 看一下1号活动",
                    "/oa get /site/club-home",
                    "/oa config",
                    "",
                    f"插件版本：{PLUGIN_VERSION}",
                    "普通查询只允许 GET；QQ 绑定命令只会消费网页登录生成的一次性绑定码。",
                ]
            )
        )

    @oa.command("config", alias={"配置"})
    async def show_config(self, event: AstrMessageEvent):
        """查看当前 API 配置"""
        token_status = "已配置" if self.access_token else "未配置"
        yield event.plain_result(
            f"base_url: {self.base_url}\n"
            f"plugin_version: {PLUGIN_VERSION}\n"
            f"token: {token_status}\n"
            f"timeout: {self.timeout}s\n"
            f"max_reply_chars: {self.max_reply_chars}"
        )

    @oa.command("ping")
    async def ping(self, event: AstrMessageEvent):
        """测试后端连接"""
        data, error = await self._get_data("/site/register-enabled")
        yield event.plain_result("后端连接正常" if error is None else error)

    @oa.command("me")
    async def me(self, event: AstrMessageEvent):
        """查看当前登录用户"""
        data, error = await self._get_data("/auth/me")
        yield event.plain_result(error or self._format_generic(data))

    @oa.command("bind-qq", alias={"绑定qq", "qq绑定"})
    async def bind_qq(self, event: AstrMessageEvent, token: str):
        """使用网页登录生成的一次性绑定码绑定当前 QQ"""
        qq_openid = self._event_sender_id(event)
        if not qq_openid:
            yield event.plain_result("没有获取到当前 QQ 号，无法绑定。")
            return
        result = await self._request(
            "POST",
            "/auth/qq-bind/confirm",
            body={"token": token, "qqOpenid": qq_openid},
        )
        yield event.plain_result(self._success_message(result, f"已将当前 QQ（{qq_openid}）绑定到系统账号。"))

    @oa.command("ask", alias={"问"})
    async def ask(self, event: AstrMessageEvent, *question_parts: str):
        """按自然语言问题查询公开数据"""
        question = " ".join(question_parts).strip()
        yield event.plain_result(await self._answer_question(question))

    @oa.command("get")
    async def get(self, event: AstrMessageEvent, path: str, *params: str):
        """调用 GET API，参数格式 key=value"""
        query = self._parse_query(params)
        data, error = await self._get_data(path, query)
        yield event.plain_result(error or self._format_generic(data))

    @filter.llm_tool(name="openatom_query")
    async def openatom_query(self, event: AstrMessageEvent, question: str):
        '''根据用户原话查询社团系统后端公开数据并直接生成简短回答。

        这是唯一应该给 AI 使用的 OpenAtom 社团系统工具。
        直接把用户原始问题传入 question，例如“当前有什么活动”“社团有多少人”“主要人员有哪些”“看一下1号活动”“有哪些部门”“招新开了吗”。
        工具内部会判断要查哪个后端公开 GET 接口，并返回最终可发送给用户的中文答案。
        不要再自行选择接口，不要展示 JSON、字段名、URL、ID、状态码或工具原始数据。

        Args:
            question(string): 用户原始问题
        '''
        yield event.plain_result(await self._answer_question(question))

    async def _answer_question(self, question: str) -> str:
        text = (question or "").strip().lower()
        if not text:
            return "你想查询社团系统的哪类信息？可以问活动、招新、部门、人数、主要人员或校历。"

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

        return await self._answer_club_home()

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
