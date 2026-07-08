#!/usr/bin/env python3
"""
OpenAtom Backend MCP Server
把整个社团系统后端的公开 API 暴露为 MCP 工具，供 LLM 自由调用。
LLM 拿到原始 JSON 数据后自行组织回答，不再受预格式化限制。
"""
from __future__ import annotations

import json
import os
from typing import Any
from urllib.parse import quote

import aiohttp
from mcp.server.fastmcp import FastMCP

BACKEND_URL = os.environ.get("OPENATOM_BACKEND_URL", "http://backend:8921/api/v1").rstrip("/")
ACCESS_TOKEN = os.environ.get("OPENATOM_ACCESS_TOKEN", "").strip()
TIMEOUT = int(os.environ.get("OPENATOM_TIMEOUT", "15"))

mcp = FastMCP("openatom-backend")


# ── 内部工具 ──────────────────────────────────────────────────────

async def _api_get(path: str, params: dict[str, str] | None = None) -> str:
    """向后端发起 GET 请求，返回 JSON 字符串（已解包 Result.data）。"""
    # 拼接 URL，对 path 段做 URL encode
    clean_path = "/" + path.strip().lstrip("/")
    segments = [quote(s, safe="") for s in clean_path.split("/") if s]
    url = f"{BACKEND_URL}/{'/'.join(segments)}"

    headers = {"Accept": "application/json"}
    if ACCESS_TOKEN:
        headers["jmiopenatom"] = ACCESS_TOKEN
        headers["Authorization"] = f"Bearer {ACCESS_TOKEN}"

    timeout = aiohttp.ClientTimeout(total=TIMEOUT)
    try:
        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.get(url, params=params, headers=headers) as resp:
                text = await resp.text()
                try:
                    data = json.loads(text)
                    # 解包标准 Result 信封 {code, message, data}
                    # 后端 Result.SUCCESS_CODE = 0（不是200），code 为 None 或 0 表示成功
                    if isinstance(data, dict) and "data" in data and "code" in data:
                        code = data.get("code")
                        if code not in (None, 0):
                            return json.dumps({"error": data.get("message", "unknown error"), "code": code}, ensure_ascii=False)
                        data = data["data"]
                    return json.dumps(data, ensure_ascii=False)
                except (json.JSONDecodeError, ValueError):
                    return text
    except Exception as exc:
        return json.dumps({"error": str(exc)}, ensure_ascii=False)


# ── MCP 工具：每个对应一个后端公开接口 ──────────────────────────
# 所有工具返回原始 JSON，LLM 自行理解、筛选、组织回答。


@mcp.tool()
async def get_club_home() -> str:
    """获取社团首页完整信息，包括社团名称、简介、统计指标（在册成员、年度活动、竞赛获奖等）、
    主要人员列表（含姓名、角色、方向）、往届管理人员列表（含姓名、角色、往届分组）、技术方向和获奖记录。

    当用户问"社团概况""社团怎么样""有多少人""主要人员""往届管理人员""前任社长""历届负责人"
    "技术方向""获奖"等关于社团整体信息时使用。返回的 people 字段是现任主要人员，
    alumniManagers 字段是往届管理人员（每个人包含 name、role、focus、alumniGroup 等信息）。
    """
    return await _api_get("/site/club-home")


@mcp.tool()
async def get_activities() -> str:
    """获取社团全部活动列表，返回每个活动的完整信息（标题、时间、地点、摘要等）。

    当用户问"有什么活动""活动安排""最近活动""活动列表"等关于活动的问题时使用。
    如果用户问某个具体活动的详情，可以先调用此工具获取列表，找到对应活动后再调用 get_activity_detail。
    """
    return await _api_get("/site/activities")


@mcp.tool()
async def get_activity_detail(activity_id: str) -> str:
    """获取指定活动的详细信息，包括完整描述。

    Args:
        activity_id: 活动ID（数字字符串），例如"1""3"
    """
    return await _api_get(f"/site/activities/{activity_id}")


@mcp.tool()
async def get_recruitment() -> str:
    """获取社团招新/纳新信息，包括社团招新状态和招新计划列表。

    当用户问"招新""纳新""怎么报名""怎么加入""招新开了吗""报名"等关于招新的问题时使用。
    """
    return await _api_get("/site/recruitment")


@mcp.tool()
async def get_recruitment_detail(campaign_id: str) -> str:
    """获取指定招新计划的详细信息。

    Args:
        campaign_id: 招新计划ID（数字字符串）
    """
    return await _api_get(f"/site/recruitment/{campaign_id}")


@mcp.tool()
async def get_departments(club_id: str = "1") -> str:
    """获取社团部门列表，返回每个部门的完整信息（名称、完整描述、负责人ID、副部长ID等）。

    当用户问"有哪些部门""部门信息""项目部是什么""详细介绍下某个部门""组织架构"等关于部门的问题时使用。
    返回的 description 字段是完整描述，没有截断，你可以根据用户问题自由摘取和详细展开。

    Args:
        club_id: 社团ID，默认"1"
    """
    return await _api_get(f"/clubs/{club_id}/departments")


@mcp.tool()
async def get_register_status() -> str:
    """查询社团当前是否开放注册。

    当用户问"能注册吗""开放注册了吗""注册开关"等关于注册的问题时使用。
    """
    return await _api_get("/site/register-enabled")


@mcp.tool()
async def get_school_calendar() -> str:
    """获取校历信息，包括放假、调休、教学周等。

    当用户问"校历""放假""假期""调休""教学周"等关于校历的问题时使用。
    """
    return await _api_get("/site/school-calendar")


@mcp.tool()
async def get_evening_study() -> str:
    """获取今天晚自习签到概览，包括各分组签到情况。

    当用户问"晚自习""自习签到""今晚签到""实验室签到"等关于晚自习的问题时使用。
    """
    return await _api_get("/bot/evening-study/today")


@mcp.tool()
async def lookup_user(keyword: str = "", qq: str = "", limit: str = "5") -> str:
    """按姓名或QQ号查询社团系统中的用户，返回用户公开信息（姓名、学号、学院、专业、班级、QQ绑定状态等）。

    当用户问"查人 张三""查一下QQ号123456""张三是谁""帮我找一下李四"等查别人的信息时使用。
    也可以用于查自己：如果知道当前用户的QQ号，传入 qq 参数即可查到自己的信息。

    Args:
        keyword: 姓名关键词（与 qq 二选一）
        qq: QQ号/OpenID（与 keyword 二选一）
        limit: 最多返回几条记录，默认"5"，最大"10"
    """
    params = {"limit": limit}
    if qq:
        params["qqOpenid"] = qq
    elif keyword:
        params["keyword"] = keyword
    else:
        return json.dumps({"error": "请提供 keyword（姓名）或 qq（QQ号）参数"}, ensure_ascii=False)
    return await _api_get("/bot/users/lookup", params)


@mcp.tool()
async def get_qq_bind_status(qq: str) -> str:
    """查询指定QQ号是否已绑定系统账号。

    Args:
        qq: QQ号/OpenID
    """
    return await _api_get("/auth/qq-bind/status", {"qqOpenid": qq})


@mcp.tool()
async def get_form(form_id: str) -> str:
    """获取指定表单/问卷的详细信息。

    Args:
        form_id: 表单ID（数字字符串）
    """
    return await _api_get(f"/site/forms/{form_id}")


@mcp.tool()
async def get_blog_articles(keyword: str = "", category: str = "", tag: str = "", featured: str = "", page: str = "1", pageSize: str = "10") -> str:
    """获取博客文章列表，支持按关键词、分类、标签、精选筛选。返回文章标题、摘要、作者、分类、标签等。

    当用户问"博客""文章""有什么文章""技术文章""精选文章""最近文章"等关于博客的问题时使用。

    Args:
        keyword: 搜索关键词，匹配标题和内容
        category: 分类筛选，例如"技术分享""活动总结"
        tag: 标签筛选
        featured: 是否只看精选，传"true"或"false"
        page: 页码，默认"1"
        pageSize: 每页条数，默认"10"
    """
    params = {"page": page, "pageSize": pageSize}
    if keyword:
        params["keyword"] = keyword
    if category:
        params["category"] = category
    if tag:
        params["tag"] = tag
    if featured:
        params["featured"] = featured
    return await _api_get("/site/blog/articles", params)


@mcp.tool()
async def get_blog_article_detail(article_id: str) -> str:
    """获取指定博客文章的完整详情，包括正文内容、作者信息、阅读量、点赞数等。

    当用户想看某篇文章的完整内容时使用。可以先调用 get_blog_articles 获取列表，找到文章ID后再调用此工具。

    Args:
        article_id: 文章ID（数字字符串）
    """
    return await _api_get(f"/site/blog/articles/{article_id}")


@mcp.tool()
async def get_blog_categories() -> str:
    """获取博客所有分类列表。

    当用户问"博客有哪些分类""文章分类"等时使用。
    """
    return await _api_get("/site/blog/categories")


@mcp.tool()
async def get_blog_comments(article_id: str) -> str:
    """获取指定博客文章的公开评论列表。

    当用户问"这篇文章有什么评论""大家怎么看这篇文章"等时使用。

    Args:
        article_id: 文章ID（数字字符串）
    """
    return await _api_get(f"/site/blog/articles/{article_id}/comments")


@mcp.tool()
async def get_regulations() -> str:
    """获取社团规章/制度列表。

    当用户问"社团规章""制度""章程""规定"等关于社团规章的问题时使用。
    """
    return await _api_get("/site/regulations")


@mcp.tool()
async def get_regulation_detail(regulation_id: str) -> str:
    """获取指定规章/制度的完整详情。

    Args:
        regulation_id: 规章ID（数字字符串）
    """
    return await _api_get(f"/site/regulations/{regulation_id}")


@mcp.tool()
async def get_showcase_apps(keyword: str = "", open_source: str = "") -> str:
    """获取社团展示应用/项目列表，包括项目名称、简介、技术栈等。

    当用户问"社团项目""展示应用""开源项目""做了什么项目"等关于社团项目成果的问题时使用。

    Args:
        keyword: 搜索关键词
        open_source: 是否只看开源项目，传"true"或"false"
    """
    params: dict[str, str] = {}
    if keyword:
        params["keyword"] = keyword
    if open_source:
        params["openSource"] = open_source
    return await _api_get("/site/apps", params or None)


@mcp.tool()
async def get_showcase_app_detail(app_id: str) -> str:
    """获取指定展示应用/项目的详细信息。

    Args:
        app_id: 应用ID（数字字符串）
    """
    return await _api_get(f"/site/apps/{app_id}")


@mcp.tool()
async def get_points_leaderboard(limit: str = "20") -> str:
    """获取积分排行榜，返回成员积分排名列表。

    当用户问"积分排行""排行榜""谁积分最高""积分排名"等关于积分排行的问题时使用。

    Args:
        limit: 返回前几名，默认"20"
    """
    return await _api_get("/site/points/leaderboard", {"limit": limit} if limit else None)


@mcp.tool()
async def get_points_items() -> str:
    """获取积分兑换商品列表。

    当用户问"积分兑换""积分商城""能兑换什么""积分有什么用"等关于积分兑换的问题时使用。
    """
    return await _api_get("/site/points/items")


@mcp.tool()
async def get_votes() -> str:
    """获取社团投票列表，返回进行中和已结束的投票。

    当用户问"投票""正在投票""有什么投票"等关于投票的问题时使用。
    """
    return await _api_get("/site/votes")


@mcp.tool()
async def get_vote_detail(vote_id: str) -> str:
    """获取指定投票的详细信息，包括选项、票数等。

    Args:
        vote_id: 投票ID（数字字符串）
    """
    return await _api_get(f"/site/votes/{vote_id}")


@mcp.tool()
async def get_lottery_screen(lottery_id: str) -> str:
    """获取指定抽奖活动的大屏展示数据，包括抽奖信息和中奖名单。

    当用户问"抽奖结果""中奖名单""抽奖大屏"等关于抽奖的问题时使用。

    Args:
        lottery_id: 抽奖活动ID（数字字符串）
    """
    return await _api_get(f"/site/lotteries/{lottery_id}/screen")


@mcp.tool()
async def get_public_clubs() -> str:
    """获取公开展示的活跃社团列表。

    当用户问"有哪些社团""社团列表"等关于社团列表的问题时使用。
    """
    return await _api_get("/site/clubs")


@mcp.tool()
async def api_get(path: str, params: str = "") -> str:
    """通用 GET 接口——当其他专用工具不满足时，可以用此工具调用任意公开 GET 接口。

    可用路径示例：
    - /site/club-home  社团首页
    - /site/activities  活动列表
    - /site/activities/{id}  活动详情
    - /site/recruitment  招新信息
    - /site/recruitment/{id}  招新详情
    - /clubs/{clubId}/departments  部门列表
    - /site/register-enabled  注册开关
    - /site/school-calendar  校历
    - /site/regulations  规章列表
    - /site/regulations/{id}  规章详情
    - /site/blog/articles  博客文章列表
    - /site/blog/articles/{id}  博客文章详情
    - /site/blog/categories  博客分类
    - /site/blog/articles/{id}/comments  博客评论
    - /site/apps  展示应用列表
    - /site/apps/{id}  应用详情
    - /site/points/leaderboard  积分排行
    - /site/points/items  积分兑换商品
    - /site/votes  投票列表
    - /site/votes/{id}  投票详情
    - /site/clubs  公开社团列表
    - /site/lotteries/{id}/screen  抽奖大屏
    - /bot/users/lookup  查人（需 params: keyword=xxx 或 qqOpenid=xxx）
    - /bot/evening-study/today  晚自习签到
    - /site/forms/{id}  表单详情
    - /auth/qq-bind/status  QQ绑定状态（需 params: qqOpenid=xxx）
    # 注意：/site/progress、/site/leave-applications、/site/activation、/site/points/me 需要登录，MCP 无 token 无法调用

    Args:
        path: API 路径，以 / 开头，例如 "/site/club-home"
        params: 查询参数，格式 "key1=value1&key2=value2"，可为空
    """
    query: dict[str, str] | None = None
    if params and "=" in params:
        query = {}
        for pair in params.split("&"):
            if "=" in pair:
                k, v = pair.split("=", 1)
                k = k.strip()
                if k:
                    query[k] = v.strip()
    return await _api_get(path, query)


if __name__ == "__main__":
    mcp.run()
