#!/usr/bin/env python3
"""OpenAtom 招新压测。仅依赖 Python 3.9+ 标准库，默认只读。"""

import argparse
import concurrent.futures
import csv
import gzip
import http.client
import json
import math
import re
import signal
import ssl
import threading
import time
import uuid
import zlib
from collections import Counter
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urldefrag, urljoin, urlsplit


class PageAssets(HTMLParser):
    """只提取脚本、样式与对应预加载，不下载图片/视频或执行 HTML。"""

    def __init__(self, page_url):
        super().__init__()
        self.base = page_url
        self.references = []
        self.has_base = False

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "base" and attrs.get("href") and not self.has_base:
            self.base = urljoin(self.base, attrs["href"])
            self.has_base = True
        elif tag == "script" and attrs.get("src"):
            self.references.append((attrs["src"], "js"))
        elif tag == "link" and attrs.get("href"):
            rel = set(attrs.get("rel", "").lower().split())
            if "stylesheet" in rel or ("preload" in rel and attrs.get("as") == "style"):
                self.references.append((attrs["href"], "css"))
            elif "modulepreload" in rel or ("preload" in rel and attrs.get("as") == "script"):
                self.references.append((attrs["href"], "js"))


def same_origin_asset(reference, base, page_url, kind):
    url = urldefrag(urljoin(base, reference))[0]
    target, page = urlsplit(url), urlsplit(page_url)
    def origin(parts):
        return parts.scheme, parts.hostname, parts.port or (443 if parts.scheme == "https" else 80)
    if target.scheme not in ("http", "https") or target.username or target.password or origin(target) != origin(page):
        return None
    return url, kind


def discover_assets(body, url, kind, page_url):
    text = body.decode("utf-8", errors="replace")
    references = []
    if kind == "page":
        parser = PageAssets(url)
        parser.feed(text)
        base = parser.base
        references = parser.references
    else:
        base = url
        if kind == "js":
            # 静态 ESM import/export；不执行 JS，也不遍历其他页面的动态 import。
            patterns = [r'\b(?:import|export)\s*(?:\{[^}]*\}|\*\s+as\s+[\w$]+|[\w$]+(?:\s*,\s*\{[^}]*\})?)\s*from\s*["\x27]([^"\x27]+)["\x27]',
                        r'\bimport\s*["\x27]([^"\x27]+)["\x27]']
            for pattern in patterns:
                references.extend((ref, "css" if urlsplit(ref).path.endswith(".css") else "js")
                                  for ref in re.findall(pattern, text) if ref.startswith(("/", "./", "../")))
            # 本项目 Vite 招新路由的懒加载分块及 mapDeps 中的配套 CSS。
            deps = []
            match = re.search(r'\b__vite__mapDeps\s*=.{0,150}?\.f\s*=\s*(\[[^\]]*\])', text, re.S)
            if match:
                try:
                    deps = json.loads(match.group(1))
                except ValueError:
                    pass
            pattern = r'\bimport\(["\x27]([^"\x27]*(?:ApplicationForm|SiteLayout)-[^"\x27]+\.js)["\x27]\)(?:\s*,\s*__vite__mapDeps\(\[([\d,\s]*)\]\))?'
            for ref, indices in re.findall(pattern, text):
                references.append((ref, "js"))
                for index in re.findall(r'\d+', indices):
                    if int(index) < len(deps) and isinstance(deps[int(index)], str):
                        dep = deps[int(index)]
                        # Vite 的 assets/... 相对于站点根目录，不能相对于 /assets/ 再拼接。
                        references.append((urljoin(page_url, "/" + dep.lstrip("/")),
                                           "css" if dep.endswith(".css") else "js"))
        elif kind == "css":
            for match in re.finditer(r'@import\s+(?:url\(\s*)?(?:["\x27]([^"\x27]+)["\x27]|([^\s;)]+))', text):
                references.append((match.group(1) or match.group(2), "css"))
    found = []
    for reference, resource_kind in references:
        asset = same_origin_asset(reference, base, page_url, resource_kind)
        if asset and asset not in found:
            found.append(asset)
    return found


class Client:
    """每个虚拟用户独立连接；使用真实出口 IP，不伪造代理头，不重试 POST。"""

    def __init__(self, timeout, context=None):
        self.timeout = timeout
        self.connections = {}
        self.context = context or ssl.create_default_context()

    def close(self):
        for connection in self.connections.values():
            connection.close()
        self.connections.clear()

    def request(self, url, method="GET", payload=None, api=False, resource_kind=None):
        started = time.perf_counter()
        result = {"method": method, "url": url, "http_status": 0,
                  "business_code": "", "ok": False, "bytes": 0,
                  "cache": "", "application_id": "", "error": "",
                  "resource_kind": resource_kind or ("api" if api else "page"),
                  "content_encoding": "", "decoded_bytes": 0}
        body = b""
        origin = None
        try:
            parts = urlsplit(url)
            origin = (parts.scheme, parts.hostname, parts.port)
            if origin not in self.connections:
                cls = http.client.HTTPSConnection if parts.scheme == "https" else http.client.HTTPConnection
                kwargs = {"timeout": self.timeout}
                if parts.scheme == "https":
                    kwargs["context"] = self.context
                self.connections[origin] = cls(parts.hostname, parts.port, **kwargs)
            headers = {"User-Agent": "OpenAtom-Recruitment-Loadtest/1.0",
                       "Accept": "application/json" if api else ("*/*" if resource_kind else "text/html"),
                       "Accept-Encoding": "gzip, deflate"}
            data = None
            if payload is not None:
                data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
                headers["Content-Type"] = "application/json"
            path = parts.path or "/"
            if parts.query:
                path += "?" + parts.query
            connection = self.connections[origin]
            connection.request(method, path, body=data, headers=headers)
            response = connection.getresponse()
            result["http_status"] = response.status
            result["cache"] = response.getheader("X-Proxy-Cache", "")
            body = response.read()
            result["bytes"] = len(body)
            encoding = response.getheader("Content-Encoding", "identity").lower().strip()
            result["content_encoding"] = encoding
            if encoding == "gzip":
                body = gzip.decompress(body)
            elif encoding == "deflate":
                try:
                    body = zlib.decompress(body)
                except zlib.error:
                    body = zlib.decompress(body, -zlib.MAX_WBITS)
            elif encoding not in ("", "identity"):
                raise ValueError("不支持的 Content-Encoding: " + encoding)
            result["decoded_bytes"] = len(body)
            result["ok"] = 200 <= response.status < 300
            if api:
                parsed = json.loads(body)
                if not isinstance(parsed, dict):
                    raise ValueError("API 返回值不是 JSON 对象")
                result["business_code"] = parsed.get("code", "missing")
                result["ok"] = result["ok"] and parsed.get("code") == 0
                if result["ok"] and method == "POST":
                    application_id = parsed.get("data")
                    if type(application_id) is not int or application_id <= 0:
                        raise ValueError("提交返回成功，但没有有效申请 ID")
                    result["application_id"] = application_id
                elif result["ok"]:
                    detail = parsed.get("data")
                    if not isinstance(detail, dict) or not detail.get("campaign") or not detail.get("club"):
                        raise ValueError("招新详情返回成功，但缺少表单数据")
                if not result["ok"]:
                    result["error"] = str(parsed.get("message", "业务失败"))[:160]
            elif result["ok"] and resource_kind:
                mime = response.getheader("Content-Type", "").split(";", 1)[0].strip().lower()
                allowed = {"text/css"} if resource_kind == "css" else {
                    "text/javascript", "application/javascript", "application/ecmascript",
                    "text/ecmascript", "application/x-javascript", "application/octet-stream"}
                if mime not in allowed or not body or body.lstrip().lower().startswith((b"<!doctype html", b"<html")):
                    raise ValueError("资源未返回有效 " + resource_kind.upper() + "（Content-Type: " + mime + "）")
            elif result["ok"]:
                if "text/html" not in response.getheader("Content-Type", "") or b"<html" not in body.lower():
                    raise ValueError("页面未返回 HTML")
            if not result["ok"] and not result["error"]:
                result["error"] = "HTTP " + str(response.status)
        except Exception as exc:
            result["ok"] = False
            result["error"] = type(exc).__name__ + ": " + str(exc)[:160]
            if origin in self.connections:
                self.connections.pop(origin).close()
        result["latency_ms"] = round((time.perf_counter() - started) * 1000, 3)
        return result, body


def percentile(values, percent):
    if not values:
        return 0
    values = sorted(values)
    position = (len(values) - 1) * percent / 100
    low, high = math.floor(position), math.ceil(position)
    return round(values[low] + (values[high] - values[low]) * (position - low), 2)


def summarize(rows, elapsed):
    latencies = [row["latency_ms"] for row in rows]
    successes = sum(row["ok"] for row in rows)
    return {"requests": len(rows), "successes": successes,
            "failures": len(rows) - successes,
            "success_percent": round(successes * 100 / len(rows), 2) if rows else 0,
            "elapsed_seconds": round(elapsed, 3),
            "requests_per_second": round(len(rows) / elapsed, 2) if elapsed else 0,
            "p50_ms": percentile(latencies, 50), "p95_ms": percentile(latencies, 95),
            "p99_ms": percentile(latencies, 99), "max_ms": max(latencies, default=0),
            "http_statuses": dict(Counter(str(row["http_status"]) for row in rows)),
            "business_codes": dict(Counter(str(row["business_code"]) for row in rows if row["business_code"] != "")),
            "rate_limited": sum(row["http_status"] == 429 or str(row["business_code"]) == "429" for row in rows),
            "response_body_bytes": sum(row["bytes"] for row in rows),
            "decoded_body_bytes": sum(row["decoded_bytes"] for row in rows),
            "response_body_mbps": round(sum(row["bytes"] for row in rows) * 8 / elapsed / 1000000, 3) if elapsed else 0,
            "content_encodings": dict(Counter(row["content_encoding"] for row in rows)),
            "cache_statuses": dict(Counter(row["cache"] for row in rows if row["cache"])),
            "errors": dict(Counter(row["error"] for row in rows if row["error"]))}


def substitute(value, run_id, index):
    if isinstance(value, str):
        return value.replace("{run_id}", run_id).replace("{index}", str(index))
    if isinstance(value, dict):
        return {key: substitute(item, run_id, index) for key, item in value.items()}
    if isinstance(value, list):
        return [substitute(item, run_id, index) for item in value]
    return value


def validate_url(parser, url):
    parts = urlsplit(url)
    if parts.scheme not in ("http", "https") or not parts.hostname or parts.username or parts.password or parts.query or parts.fragment:
        parser.error("目标必须是完整 http(s) URL，且不含凭据、查询参数或片段")
    try:
        parts.port
    except ValueError:
        parser.error("目标 URL 端口无效")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument("--page-url", default="https://www.jmi-openatom.cn/apply/1")
    parser.add_argument("--api-base", default="https://api.jmi-openatom.cn/api/v1")
    parser.add_argument("--campaign-id", type=int, help="默认从 page-url 的 /apply/ID 提取；提交模式必须显式指定")
    parser.add_argument("--mode", choices=["browse", "api", "burst", "submit"], default="browse",
                        help="browse 每轮读页面和详情；api 只读详情；burst 每人只读一轮；submit 有限次写入")
    parser.add_argument("--users", type=int, default=50, help="并发虚拟用户数，不是固定 RPS")
    parser.add_argument("--stages", help="逐级并发，例如 10,30,50；每级运行 duration 秒，仅用于 browse/api")
    parser.add_argument("--duration", type=float, default=60, help="每级发起请求的持续秒数；在途请求结束后退出")
    parser.add_argument("--think-time", type=float, default=1, help="每轮请求后等待秒数")
    parser.add_argument("--timeout", type=float, default=10, help="连接/socket 超时秒数")
    parser.add_argument("--assets", choices=["once", "always", "none"], default="once",
                        help="browse/burst 中下载同源 JS/CSS：每人首次一次、每轮一次或不下载")
    parser.add_argument("--asset-url", action="append", default=[], help="额外同源 JS/CSS URL，可重复指定，用于未自动发现的分块")
    parser.add_argument("--count", type=int, default=50, help="submit 的总提交次数")
    parser.add_argument("--payload-file", type=Path, help="提交 JSON 模板；支持 {run_id} 和 {index}")
    parser.add_argument("--allow-write", action="store_true", help="确认允许真实创建申请；建议仅用于独立测试批次")
    parser.add_argument("--output", type=Path, default=Path("output/loadtest"))
    args = parser.parse_args()
    for target in (args.page_url, args.api_base):
        validate_url(parser, target)
    if args.mode not in ("browse", "burst"):
        if args.asset_url or args.assets == "always":
            parser.error("资源下载仅用于 browse/burst 模式")
        args.assets = "none"
    extra_assets = []
    for reference in args.asset_url:
        extension = urlsplit(reference).path.rsplit(".", 1)[-1].lower()
        asset = same_origin_asset(reference, args.page_url, args.page_url, "css" if extension == "css" else "js")
        if extension not in ("js", "mjs", "css") or not asset or args.assets == "none":
            parser.error("asset-url 必须是同源 JS/CSS URL，且 assets 不能为 none")
        extra_assets.append(asset)
    if not 1 <= args.users <= 500 or args.duration <= 0 or not math.isfinite(args.duration):
        parser.error("users 必须为 1～500，duration 必须为有限正数")
    if not math.isfinite(args.think_time) or args.think_time < 0 or not math.isfinite(args.timeout) or args.timeout <= 0:
        parser.error("think-time 必须为有限非负数，timeout 必须为有限正数")
    if args.count < 1:
        parser.error("count 必须为正数")
    stages = [args.users]
    if args.stages:
        if args.mode not in ("browse", "api"):
            parser.error("stages 只支持 browse/api")
        try:
            stages = [int(value) for value in args.stages.split(",")]
        except ValueError:
            parser.error("stages 格式应为 10,30,50")
        if not all(1 <= value <= 500 for value in stages):
            parser.error("每一级并发必须为 1～500")
    explicit_campaign = args.campaign_id is not None
    if not explicit_campaign:
        parts = urlsplit(args.page_url).path.rstrip("/").split("/")
        if len(parts) < 3 or parts[-2] != "apply" or not parts[-1].isdigit():
            parser.error("无法从 page-url 提取批次，请指定 campaign-id")
        args.campaign_id = int(parts[-1])
    if args.campaign_id < 1:
        parser.error("campaign-id 必须为正数")
    if args.mode != "submit" and (args.allow_write or args.payload_file):
        parser.error("allow-write/payload-file 仅用于 submit")
    if args.mode == "submit" and not (args.allow_write and args.payload_file and explicit_campaign):
        parser.error("submit 必须同时指定 --allow-write --payload-file --campaign-id，会实际写入申请")

    run_id = time.strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:8]
    detail_url = args.api_base.rstrip("/") + "/site/recruitment/" + str(args.campaign_id)
    submit_url = args.api_base.rstrip("/") + "/applications"
    print("预检目标:", detail_url, flush=True)
    tls_context = ssl.create_default_context()
    client = Client(args.timeout, tls_context)
    preflight, body = client.request(detail_url, api=True)
    client.close()
    if not preflight["ok"]:
        parser.exit(2, "预检失败，未启动压测: " + preflight["error"] + "\n")
    detail = json.loads(body)["data"]
    if detail["campaign"].get("id") != args.campaign_id:
        parser.exit(2, "预检返回的批次 ID 不匹配，未启动压测\n")
    template = None
    if args.mode == "submit":
        try:
            template = json.loads(args.payload_file.read_text(encoding="utf-8"))
            if not isinstance(template, dict) or template.get("campaignId") != args.campaign_id or template.get("clubId") != detail["club"]["id"]:
                raise ValueError("模板 campaignId/clubId 必须与预检的测试批次一致")
            if detail["campaign"].get("loginRequired") is not False:
                raise ValueError("提交模式仅支持允许匿名报名的测试批次")
            if detail["campaign"].get("status") not in ("open", "published"):
                raise ValueError("测试批次尚未开放报名")
            profile = template.get("profile", {})
            if not isinstance(profile, dict):
                raise ValueError("profile 必须是对象")
            student_id = profile.get("studentId", "")
            if not isinstance(student_id, str) or "{run_id}" not in student_id or "{index}" not in student_id:
                raise ValueError("profile.studentId 必须包含 {run_id} 和 {index}，防止跨请求/运行冲突")
            schema = detail["campaign"].get("formSchema") or []
            if isinstance(schema, str):
                schema = json.loads(schema)
            required = {"applicantName", "contact", "college"}
            required.update(field["key"] for field in schema if field.get("required") and field.get("key"))
            missing = sorted(key for key in required if profile.get(key) is None or not str(profile[key]).strip())
            if missing:
                raise ValueError("缺少必填资料: " + ", ".join(missing))
        except (OSError, ValueError, TypeError, KeyError) as exc:
            parser.exit(2, "提交模板预检失败，未写入申请: " + str(exc) + "\n")
        print(f"写入模式: 批次 {args.campaign_id}，共 {args.count} 次；无自动重试。", flush=True)

    output = args.output / run_id
    output.mkdir(parents=True, exist_ok=False)
    stopped = threading.Event()
    signal.signal(signal.SIGINT, lambda *_: stopped.set())
    signal.signal(signal.SIGTERM, lambda *_: stopped.set())
    summaries = []
    failures = 0
    fields = ["stage", "user", "sequence", "method", "url", "http_status", "business_code", "ok",
              "latency_ms", "bytes", "cache", "application_id", "error", "resource_kind",
              "content_encoding", "decoded_bytes"]
    with (output / "requests.csv").open("w", encoding="utf-8-sig", newline="") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=fields)
        writer.writeheader()
        for stage_index, users in enumerate(stages, 1):
            if stopped.is_set():
                break
            stage = f"{stage_index}-{users}users"
            rows = []
            lock = threading.Lock()
            barrier = threading.Barrier(users + 1)
            timing = {}
            counter = [0]
            active = [0, 0]

            def record(client, user, sequence, url, method="GET", payload=None, api=False, resource_kind=None):
                with lock:
                    active[0] += 1
                    active[1] = max(active[1], active[0])
                row, body = client.request(url, method, payload, api, resource_kind)
                row.update(stage=stage, user=user, sequence=sequence)
                with lock:
                    active[0] -= 1
                    rows.append(row)
                    writer.writerow(row)
                return row, body

            def download_assets(client, user, sequence, html):
                queue = discover_assets(html, args.page_url, "page", args.page_url) + extra_assets
                seen = set()
                while queue and not stopped.is_set():
                    url, kind = queue.pop(0)
                    if url in seen:
                        continue
                    seen.add(url)
                    row, body = record(client, user, sequence, url, resource_kind=kind)
                    if row["ok"]:
                        queue.extend(discover_assets(body, url, kind, args.page_url))

            def worker(user):
                connection = Client(args.timeout, tls_context)
                try:
                    barrier.wait(timeout=30)
                    sequence = 0
                    while not stopped.is_set():
                        if args.mode in ("browse", "api") and time.perf_counter() >= timing["deadline"]:
                            break
                        if args.mode == "submit":
                            with lock:
                                if counter[0] >= args.count:
                                    break
                                counter[0] += 1
                                sequence = counter[0]
                            payload = substitute(template, run_id, sequence)
                            record(connection, user, sequence, submit_url, "POST", payload, True)
                        else:
                            sequence += 1
                            if args.mode != "api":
                                page_row, html = record(connection, user, sequence, args.page_url)
                                if page_row["ok"] and args.assets != "none" and (sequence == 1 or args.assets == "always"):
                                    download_assets(connection, user, sequence, html)
                            if stopped.is_set():
                                break
                            record(connection, user, sequence, detail_url, api=True)
                        if args.mode == "burst":
                            break
                        if stopped.wait(args.think_time):
                            break
                finally:
                    connection.close()

            print(f"开始 {stage}，模式 {args.mode}；Ctrl+C 停止并保留报告。", flush=True)
            with concurrent.futures.ThreadPoolExecutor(max_workers=users) as pool:
                futures = [pool.submit(worker, user + 1) for user in range(users)]
                timing["start"] = time.perf_counter()
                timing["deadline"] = timing["start"] + args.duration
                try:
                    barrier.wait(timeout=30)
                    while any(not future.done() for future in futures):
                        time.sleep(0.2)
                        if stopped.is_set():
                            continue
                    for future in futures:
                        future.result()
                except Exception as exc:
                    stopped.set()
                    barrier.abort()
                    print("执行中断:", type(exc).__name__, str(exc), flush=True)
            elapsed = time.perf_counter() - timing["start"]
            summary = summarize(rows, elapsed)
            summary.update(stage=stage, users=users, max_inflight_requests=active[1])
            summary["endpoints"] = {url: summarize([row for row in rows if row["url"] == url], elapsed)
                                    for url in sorted({row["url"] for row in rows})}
            summary["resource_kinds"] = {kind: summarize([row for row in rows if row["resource_kind"] == kind], elapsed)
                                         for kind in sorted({row["resource_kind"] for row in rows})}
            summaries.append(summary)
            failures += summary["failures"] or (1 if not rows else 0)
            csv_file.flush()
            print(f"完成 {stage}: {summary['requests']} 请求，成功 {summary['success_percent']}%，"
                  f"RPS {summary['requests_per_second']}，P95 {summary['p95_ms']}ms，"
                  f"限流 {summary['rate_limited']} 次，下载 {summary['response_body_bytes'] / 1000000:.2f} MB，"
                  f"平均响应体流量 {summary['response_body_mbps']} Mbps", flush=True)
    report = {"run_id": run_id, "mode": args.mode, "page_url": args.page_url,
              "detail_url": detail_url, "campaign_id": args.campaign_id,
              "assets": args.assets, "extra_assets": [url for url, _ in extra_assets],
              "interrupted": stopped.is_set(), "stages": summaries,
              "notes": ["预检请求不计入统计", "HTTP 与业务 code 同时判定成功", "耗时包含失败请求",
                        "使用连接复用，同源 JS/CSS 顺序下载，不执行 JS/渲染，无固定 RPS 保证",
                        "自动解析 HTML/静态 ESM/CSS import 及 Vite 报名页分块，其他动态资源可能需 asset-url 补充",
                        "使用真实出口 IP，支持 gzip/deflate，bytes 为压缩后响应体字节，不含 TLS/HTTP 头"]}
    (output / "summary.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("报告目录:", output.resolve(), flush=True)
    return 130 if stopped.is_set() else (1 if failures else 0)


if __name__ == "__main__":
    raise SystemExit(main())
