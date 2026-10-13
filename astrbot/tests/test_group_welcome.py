"""Run with python3 -m unittest discover -s astrbot/tests -v (no AstrBot required)."""

import asyncio
import importlib.util
import json
import sys
import types
import unittest
from pathlib import Path
from unittest.mock import AsyncMock, Mock, patch


PLUGIN_DIR = Path(__file__).resolve().parents[1] / "data/plugins/astrbot_plugin_openatom_api"


def decorator(*args, **kwargs):
    def wrap(func):
        func.command = decorator
        return func
    return wrap


def load_plugin():
    # Only the external runtime imports are stubbed; tests run the plugin's handlers.
    api = types.ModuleType("astrbot.api")
    api.AstrBotConfig = dict
    api.logger = Mock()
    event = types.ModuleType("astrbot.api.event")
    event.AstrMessageEvent = object
    event.filter = types.SimpleNamespace(
        command_group=decorator, platform_adapter_type=decorator,
        event_message_type=decorator, llm_tool=decorator,
        PlatformAdapterType=types.SimpleNamespace(AIOCQHTTP="aiocqhttp"),
        EventMessageType=types.SimpleNamespace(ALL="all"),
    )
    star = types.ModuleType("astrbot.api.star")
    star.Context = object
    star.Star = type("Star", (), {"__init__": lambda self, context: None})
    star.register = decorator
    http = types.ModuleType("aiohttp")
    http.web = types.ModuleType("aiohttp.web")
    imports = {
        "astrbot": types.ModuleType("astrbot"), "astrbot.api": api,
        "astrbot.api.event": event, "astrbot.api.star": star,
        "aiohttp": http, "aiohttp.web": http.web,
    }
    spec = importlib.util.spec_from_file_location("openatom_welcome_test", PLUGIN_DIR / "main.py")
    module = importlib.util.module_from_spec(spec)
    with patch.dict(sys.modules, imports):
        spec.loader.exec_module(module)
    return module.OpenAtomApiPlugin


Plugin = load_plugin()


class GroupWelcomeTest(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        schema = json.loads((PLUGIN_DIR / "_conf_schema.json").read_text())
        self.config = {key: value["default"] for key, value in schema.items()}
        self.config.update(welcome_enabled=True, welcome_delay_seconds=0)
        with patch.object(Plugin, "_ensure_callback_server"):
            self.plugin = Plugin(None, self.config)
        self.bot = types.SimpleNamespace(call_action=AsyncMock(side_effect=self.respond))

    async def respond(self, action, **params):
        if action == "get_group_member_info":
            return {"nickname": "新人[CQ:at,qq=all]{qq}", "card": "技术部-张三"}
        if action == "get_group_info":
            return {"group_name": "开放原子"}
        return {"message_id": 1}

    def event(self, **changes):
        raw = dict(post_type="notice", notice_type="group_increase", sub_type="approve",
                   group_id=123, user_id=456, self_id=789, time=1000)
        raw.update(changes)
        return types.SimpleNamespace(bot=self.bot, message_obj=types.SimpleNamespace(raw_message=raw))

    def messages(self):
        return [call.kwargs["message"] for call in self.bot.call_action.call_args_list
                if call.args[0] == "send_group_msg"]

    async def test_default_disabled_and_blank_text(self):
        self.assertFalse(json.loads((PLUGIN_DIR / "_conf_schema.json").read_text())["welcome_enabled"]["default"])
        self.plugin.welcome_enabled = False
        await self.plugin.welcome_new_group_member(self.event())
        self.plugin.welcome_enabled = True
        self.plugin.welcome_text = " \n "
        await self.plugin.welcome_new_group_member(self.event())
        self.bot.call_action.assert_not_awaited()

    async def test_ignores_requests_other_notices_self_and_invalid_ids(self):
        for fields in ({"post_type": "request"}, {"notice_type": "group_decrease"},
                       {"user_id": 789}, {"group_id": ""}, {"user_id": "invalid"}):
            await self.plugin.welcome_new_group_member(self.event(**fields))
        self.bot.call_action.assert_not_awaited()

    async def test_multiline_template_and_structured_at_keep_nickname_literal(self):
        self.plugin.welcome_text = "欢迎 {nickname}\n{qq}/{group_name}/{group_id}/{card_name}/{unknown}"
        await self.plugin.welcome_new_group_member(self.event(sub_type="invite"))
        self.assertEqual(self.messages(), [[
            {"type": "at", "data": {"qq": "456"}},
            {"type": "text", "data": {"text": " 欢迎 新人[CQ:at,qq=all]{qq}\n456/开放原子/123/技术部-张三/{unknown}"}},
        ]])

    async def test_plain_text_does_not_need_lookup_or_backend(self):
        self.plugin.welcome_text = "欢迎入群！\n请阅读公告。"
        self.plugin.welcome_at_new_member = False
        await self.plugin.welcome_new_group_member(self.event())
        self.bot.call_action.assert_awaited_once_with("send_group_msg", group_id=123,
            message=[{"type": "text", "data": {"text": "欢迎入群！\n请阅读公告。"}}])

    async def test_lookup_failure_falls_back_to_ids(self):
        async def unavailable(action, **params):
            if action != "send_group_msg":
                raise RuntimeError("NapCat lookup unavailable")
        self.bot.call_action.side_effect = unavailable
        await self.plugin.welcome_new_group_member(self.event())
        self.assertEqual(self.messages()[0][-1]["data"]["text"], " 欢迎 456 加入 123！")

    async def test_concurrent_duplicates_and_later_rejoin(self):
        await asyncio.gather(*(self.plugin.welcome_new_group_member(self.event()) for _ in range(3)))
        self.assertEqual(len(self.messages()), 1)
        await self.plugin.welcome_new_group_member(self.event(time=1001))
        self.assertEqual(len(self.messages()), 2)

    async def test_send_failure_can_retry(self):
        self.bot.call_action.side_effect = RuntimeError("send unavailable")
        await self.plugin.welcome_new_group_member(self.event())
        self.assertFalse(self.plugin._welcome_notices)
        self.bot.call_action.side_effect = self.respond
        await self.plugin.welcome_new_group_member(self.event())
        self.assertEqual(self.messages()[-1][-1]["data"]["text"], " 欢迎 新人[CQ:at,qq=all]{qq} 加入 开放原子！")

    async def test_delay_before_send(self):
        self.plugin.welcome_delay_seconds = 3
        self.plugin.welcome_text = "欢迎！"
        async def check_delay(seconds):
            self.assertEqual(seconds, 3)
            self.bot.call_action.assert_not_awaited()
        with patch("asyncio.sleep", new=AsyncMock(side_effect=check_delay)) as sleep:
            await self.plugin.welcome_new_group_member(self.event())
        sleep.assert_awaited_once_with(3)
        self.assertEqual(len(self.messages()), 1)

    def configure_groups(self, entries):
        self.config["welcome_group_overrides"] = entries
        with patch.object(Plugin, "_ensure_callback_server"):
            self.plugin = Plugin(None, self.config)

    async def test_distinct_groups_use_their_own_text_and_at_setting(self):
        self.configure_groups([
            {"__template_key": "group_welcome", "group_id": "123", "enabled": True,
             "text": "欢迎加入技术群 {qq}", "at_new_member": False, "delay_seconds": 0},
            {"group_id": "321", "text": "欢迎加入活动群 {group_id}", "at_new_member": True},
        ])
        await asyncio.gather(self.plugin.welcome_new_group_member(self.event()),
                             self.plugin.welcome_new_group_member(self.event(group_id=321)))
        by_group = {call.kwargs["group_id"]: call.kwargs["message"]
                    for call in self.bot.call_action.call_args_list if call.args[0] == "send_group_msg"}
        self.assertEqual(by_group[123], [{"type": "text", "data": {"text": "欢迎加入技术群 456"}}])
        self.assertEqual(by_group[321], [{"type": "at", "data": {"qq": "456"}},
                                        {"type": "text", "data": {"text": " 欢迎加入活动群 321"}}])

    async def test_target_group_can_enable_when_default_is_disabled(self):
        self.config["welcome_enabled"] = False
        self.configure_groups([{"group_id": "123", "enabled": True, "text": "指定群欢迎"}])
        await self.plugin.welcome_new_group_member(self.event(group_id=321))
        self.bot.call_action.assert_not_awaited()
        await self.plugin.welcome_new_group_member(self.event())
        self.assertEqual(len(self.messages()), 1)

    async def test_group_can_disable_or_use_blank_text_with_default_enabled(self):
        self.configure_groups([
            {"group_id": "123", "enabled": False}, {"group_id": "321", "text": " \n "},
        ])
        await self.plugin.welcome_new_group_member(self.event())
        await self.plugin.welcome_new_group_member(self.event(group_id=321))
        self.bot.call_action.assert_not_awaited()
        await self.plugin.welcome_new_group_member(self.event(group_id=654))
        self.assertEqual(len(self.messages()), 1)

    async def test_group_delay_overrides_default(self):
        self.configure_groups([{"group_id": "123", "text": "欢迎", "delay_seconds": 6}])
        with patch("asyncio.sleep", new=AsyncMock()) as sleep:
            await self.plugin.welcome_new_group_member(self.event())
        sleep.assert_awaited_once_with(6)

    async def test_invalid_group_configuration_does_not_break_default(self):
        self.configure_groups([None, {"group_id": ""}, {"group_id": "no-group"},
                               {"group_id": "0"}, {"group_id": "123", "delay_seconds": "bad",
                                                     "enabled": "false", "text": None}])
        await self.plugin.welcome_new_group_member(self.event())
        self.assertEqual(len(self.messages()), 1)
        self.assertEqual(set(self.plugin.welcome_group_overrides), {"123"})
        self.configure_groups("invalid-list")
        self.assertEqual(self.plugin.welcome_group_overrides, {})

    async def test_duplicate_group_uses_first_and_delay_is_clamped(self):
        self.configure_groups([
            {"group_id": " 00123 ", "text": "第一条", "delay_seconds": -5},
            {"group_id": "123", "text": "重复条目", "enabled": False},
            {"group_id": "321", "delay_seconds": 100},
        ])
        await self.plugin.welcome_new_group_member(self.event())
        self.assertEqual(self.messages()[0][-1]["data"]["text"], " 第一条")
        self.assertEqual(self.plugin._welcome_settings("123")["delay_seconds"], 0)
        self.assertEqual(self.plugin._welcome_settings("321")["delay_seconds"], 60)


if __name__ == "__main__":
    unittest.main()
