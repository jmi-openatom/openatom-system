"""通过本地 HTTP 服务验证压测脚本，不访问线上、不写业务数据库。"""

import csv
import gzip
import json
import signal
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[2] / "scripts/recruitment_loadtest.py"


class LoadtestIntegrationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.posts = []
        self.detail_requests = 0
        self.fail_after_preflight = False
        self.with_assets = False
        self.broken_css = False
        self.asset_requests = []
        self.page_requests = 0
        self.lock = threading.Lock()
        owner = self

        class Handler(BaseHTTPRequestHandler):
            protocol_version = "HTTP/1.1"

            def log_message(self, *_):
                pass

            def send(self, status, body, content_type="application/json", encoding=None):
                if not isinstance(body, bytes):
                    body = json.dumps(body).encode()
                self.send_response(status)
                self.send_header("Content-Type", content_type)
                self.send_header("Content-Length", str(len(body)))
                if encoding:
                    self.send_header("Content-Encoding", encoding)
                self.send_header("X-Proxy-Cache", "HIT")
                self.end_headers()
                self.wfile.write(body)

            def do_GET(self):
                if self.path.startswith("/apply/"):
                    with owner.lock:
                        owner.page_requests += 1
                    html = b'<!DOCTYPE html><html><body>test</body></html>'
                    if owner.with_assets:
                        html = b'''<!DOCTYPE html><html><head>
                        <script src="/assets/entry.js" type="module"></script>
                        <script src="/assets/entry.js#duplicate"></script>
                        <link rel="modulepreload" href="/assets/shared.js">
                        <link rel="stylesheet" href="/assets/root.css?v=1">
                        <script src="https://third-party.invalid/tracker.js"></script>
                        <link rel="preload" as="image" href="/image.png">
                        </head><body>test</body></html>'''
                    self.send(200, html, "text/html")
                    return
                if self.path.startswith("/assets/"):
                    with owner.lock:
                        owner.asset_requests.append(self.path)
                    assets = {
                        "/assets/entry.js": b'''const __vite__mapDeps=(i,m=__vite__mapDeps,d=(m.f||(m.f=["assets/form.css","assets/layout.css"])))=>i.map(i=>d[i]);
                        import {x} from "./shared.js";
                        g(()=>import("./ApplicationForm-test.js"),__vite__mapDeps([0]));
                        g(()=>import("./SiteLayout-test.js"),__vite__mapDeps([1]));
                        g(()=>import("./Admin-test.js"));''',
                        "/assets/shared.js": b'import "./entry.js";export const x=1;',
                        "/assets/root.css?v=1": b'@import url("./base.css"); body {color:black}',
                        "/assets/base.css": b'html {background:white}',
                        "/assets/ApplicationForm-test.js": b'import{x}from"./shared.js"; export const form=x;',
                        "/assets/SiteLayout-test.js": b'export const layout=1;',
                        "/assets/form.css": b'.form {color:black}',
                        "/assets/layout.css": b'.layout {color:black}',
                        "/assets/manual.js": b'export const extra=1;'
                    }
                    if self.path not in assets or (owner.broken_css and self.path == "/assets/form.css"):
                        self.send(200, b"<!doctype html><html>SPA fallback</html>", "text/html")
                    elif self.path.endswith(".js"):
                        self.send(200, gzip.compress(assets[self.path]), "text/javascript", "gzip")
                    else:
                        self.send(200, assets[self.path], "text/css")
                    return
                with owner.lock:
                    owner.detail_requests += 1
                    count = owner.detail_requests
                if owner.fail_after_preflight and count > 1:
                    if count % 3 == 0:
                        self.send(429, {"code": 429, "message": "限流"})
                    elif count % 3 == 1:
                        self.send(200, b"invalid JSON")
                    else:
                        self.send(200, {"code": 400, "message": "业务失败"})
                    return
                campaign_id = int(self.path.rsplit("/", 1)[1])
                self.send(200, {"code": 0, "data": {
                    "campaign": {"id": campaign_id, "status": "open", "loginRequired": False,
                                 "formSchema": '[{"key":"college","required":true}]'},
                    "club": {"id": 1}, "departments": []}})

            def do_POST(self):
                payload = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                with owner.lock:
                    owner.posts.append(payload)
                    application_id = len(owner.posts)
                self.send(200, {"code": 0, "data": application_id})

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.server.daemon_threads = True
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base = "http://127.0.0.1:" + str(self.server.server_port)

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        self.temp.cleanup()

    def run_script(self, *args):
        return subprocess.run([sys.executable, str(SCRIPT),
                               "--page-url", self.base + "/apply/1",
                               "--api-base", self.base + "/api/v1",
                               "--output", self.temp.name, "--timeout", "2", *args],
                              capture_output=True, text=True, timeout=15)

    def report(self):
        path = next(Path(self.temp.name).glob("*/summary.json"))
        summary = json.loads(path.read_text())
        with (path.parent / "requests.csv").open(encoding="utf-8-sig") as file:
            rows = list(csv.DictReader(file))
        return summary, rows

    def test_burst_reads_html_and_detail_once_per_user(self):
        result = self.run_script("--mode", "burst", "--users", "5")
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        report, rows = self.report()
        self.assertEqual(len(rows), 10)
        self.assertEqual(report["stages"][0]["successes"], 10)
        self.assertEqual(self.detail_requests, 6)  # 预检 + 5 个用户
        self.assertFalse(self.posts)

    def test_api_stages_and_business_errors_are_failures(self):
        self.fail_after_preflight = True
        result = self.run_script("--mode", "api", "--stages", "2,3",
                                 "--duration", "0.3", "--think-time", "0.02")
        self.assertEqual(result.returncode, 1, result.stderr + result.stdout)
        report, rows = self.report()
        self.assertEqual(len(report["stages"]), 2)
        self.assertTrue(rows)
        self.assertTrue(all(row["ok"] == "False" for row in rows))
        self.assertGreater(sum(stage["rate_limited"] for stage in report["stages"]), 0)
        self.assertTrue(any(row["business_code"] == "400" for row in rows))
        self.assertTrue(any("JSONDecodeError" in row["error"] for row in rows))
        self.assertFalse(self.posts)

    def test_submit_requires_explicit_write_flags_before_network(self):
        result = self.run_script("--mode", "submit", "--users", "2")
        self.assertEqual(result.returncode, 2)
        self.assertEqual(self.detail_requests, 0)
        self.assertFalse(self.posts)

    def test_bounded_submit_creates_unique_payloads_and_records_ids(self):
        template = {"campaignId": 999, "clubId": 1,
                    "profile": {"applicantName": "压测-{index}",
                                "studentId": "LOADTEST-{run_id}-{index}",
                                "contact": "虚拟资料", "college": "信息工程学院"}}
        path = Path(self.temp.name) / "payload.json"
        path.write_text(json.dumps(template))
        result = self.run_script("--mode", "submit", "--campaign-id", "999",
                                 "--allow-write", "--payload-file", str(path),
                                 "--users", "3", "--count", "7", "--think-time", "0")
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertEqual(len(self.posts), 7)
        self.assertEqual(len({row["profile"]["studentId"] for row in self.posts}), 7)
        report, rows = self.report()
        self.assertEqual({row["application_id"] for row in rows}, {str(i) for i in range(1, 8)})
        self.assertEqual(report["stages"][0]["requests"], 7)

    def test_submit_rejects_missing_required_profile_before_writing(self):
        path = Path(self.temp.name) / "payload.json"
        path.write_text(json.dumps({"campaignId": 999, "clubId": 1,
                                    "profile": {"studentId": "TEST-{run_id}-{index}"}}))
        result = self.run_script("--mode", "submit", "--campaign-id", "999",
                                 "--allow-write", "--payload-file", str(path))
        self.assertEqual(result.returncode, 2)
        self.assertIn("缺少必填", result.stderr)
        self.assertFalse(self.posts)

    def test_interrupt_preserves_partial_report(self):
        process = subprocess.Popen([sys.executable, str(SCRIPT),
                                    "--page-url", self.base + "/apply/1",
                                    "--api-base", self.base + "/api/v1",
                                    "--output", self.temp.name, "--mode", "api",
                                    "--users", "2", "--duration", "30", "--think-time", "0.01"],
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        try:
            deadline = time.monotonic() + 5
            while self.detail_requests < 4 and process.poll() is None and time.monotonic() < deadline:
                time.sleep(0.02)
            self.assertGreaterEqual(self.detail_requests, 4)
            process.send_signal(signal.SIGINT)
            stdout, stderr = process.communicate(timeout=5)
            self.assertEqual(process.returncode, 130, stderr + stdout)
            report, rows = self.report()
            self.assertTrue(report["interrupted"])
            self.assertTrue(rows)
            self.assertEqual(report["stages"][0]["requests"], len(rows))
        finally:
            if process.poll() is None:
                process.kill()
                process.communicate()

    def test_assets_include_dependencies_route_chunks_and_gzip(self):
        self.with_assets = True
        result = self.run_script("--mode", "burst", "--users", "3")
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        report, rows = self.report()
        self.assertEqual(len(rows), 30)  # 每人 HTML + API + 8 个去重资源
        self.assertEqual(len(self.asset_requests), 24)
        self.assertEqual(self.asset_requests.count("/assets/entry.js"), 3)
        self.assertNotIn("/assets/Admin-test.js", self.asset_requests)
        kinds = report["stages"][0]["resource_kinds"]
        self.assertEqual(kinds["js"]["requests"], 12)
        self.assertEqual(kinds["css"]["requests"], 12)
        self.assertEqual(kinds["js"]["content_encodings"], {"gzip": 12})
        self.assertTrue(all(int(row["decoded_bytes"]) > 0 for row in rows))

    def test_assets_download_once_per_user_and_can_be_disabled(self):
        self.with_assets = True
        result = self.run_script("--users", "1", "--duration", "1.0", "--think-time", "0.01")
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertGreater(self.page_requests, 1)
        self.assertEqual(len(self.asset_requests), 8)
        before = len(self.asset_requests)
        result = self.run_script("--mode", "burst", "--users", "2", "--assets", "none")
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertEqual(len(self.asset_requests), before)

    def test_assets_always_download_each_round(self):
        self.with_assets = True
        result = self.run_script("--users", "1", "--duration", "1", "--think-time", "0", "--assets", "always")
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertGreater(self.page_requests, 1)
        self.assertEqual(len(self.asset_requests), self.page_requests * 8)

    def test_html_fallback_for_css_is_reported_as_failure(self):
        self.with_assets = True
        self.broken_css = True
        result = self.run_script("--mode", "burst", "--users", "1")
        self.assertEqual(result.returncode, 1, result.stderr + result.stdout)
        report, rows = self.report()
        failed = [row for row in rows if row["ok"] == "False"]
        self.assertEqual(len(failed), 1)
        self.assertEqual(failed[0]["resource_kind"], "css")
        self.assertEqual(failed[0]["http_status"], "200")
        self.assertEqual(report["stages"][0]["failures"], 1)


if __name__ == "__main__":
    unittest.main()
