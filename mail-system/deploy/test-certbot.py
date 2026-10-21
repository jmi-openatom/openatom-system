#!/usr/bin/env python3
"""Run the shell entrypoint with fake commands and isolated system directories."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class CertbotTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="openatom-certbot-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.script = self.root / "deploy/certbot.sh"
        self.script.parent.mkdir()
        source = Path(__file__).with_name("certbot.sh").read_text()
        # Redirect only absolute system paths; all branching and commands run unchanged.
        for directory in ("/etc/letsencrypt", "/etc/cron.d", "/var/lib/letsencrypt", "/var/log/letsencrypt"):
            source = source.replace(directory, str(self.root) + directory)
        self.script.write_text(source)
        self.env_file = self.root / "mail.env"
        self.env_file.write_text("MAIL_ACME_EMAIL=ops@example.com\n")
        self.webroot = self.root / "webroot"
        self.log = self.root / "commands.jsonl"
        self.pending = self.root / "var/lib/letsencrypt/openatom-mail-deploy-pending"
        self.cron = self.root / "etc/cron.d/openatom-mail-certbot"
        # Keep Certbot discovery deterministic even on a machine with it installed.
        for command in ("dirname", "awk", "tail", "install", "mktemp", "chmod", "mv", "rm", "touch"):
            (self.bin / command).symlink_to(shutil.which(command))
        self.env = {**os.environ, "PATH": str(self.bin), "TEST_LOG": str(self.log),
                    "TEST_PENDING": str(self.pending), "OPENATOM_MAIL_PREBUILT": "1"}
        self.mock("nginx")
        self.mock("docker")
        self.mock("crond")
        self.mock("hook", self.root / "deploy/stalwart/certbot-deploy-hook.sh")

    def mock(self, name, path=None):
        path = path or self.bin / name
        path.parent.mkdir(exist_ok=True)
        path.write_text(f"""#!{shutil.which('python3')}
import json, os, pathlib, sys
name = {name!r}
with open(os.environ['TEST_LOG'], 'a') as log:
    log.write(json.dumps([name, *sys.argv[1:]]) + '\\n')
if name == 'docker' and os.environ.get('TEST_RENEWED') == '1':
    pathlib.Path(os.environ['TEST_PENDING']).touch()
sys.exit(int(os.environ.get('FAIL_' + name.upper(), '0')))
""")
        path.chmod(0o755)

    def run_script(self, operation="certonly", expected=0):
        result = subprocess.run(["/bin/sh", str(self.script), str(self.env_file), str(self.webroot), operation],
                                env=self.env, text=True, capture_output=True)
        self.assertEqual(result.returncode, expected, result.stdout + result.stderr)
        return [json.loads(line) for line in self.log.read_text().splitlines()] if self.log.exists() else []

    def test_native_certbot_keeps_host_hook_and_skips_docker(self):
        self.mock("certbot")
        calls = self.run_script()
        self.assertEqual([call[0] for call in calls], ["certbot", "hook", "nginx", "nginx"])
        command = calls[0]
        hook = command[command.index("--deploy-hook") + 1]
        self.assertIn("certbot-deploy-hook.sh", hook)
        self.assertIn("-s reload", hook)
        self.assertFalse(self.cron.exists())

    def test_docker_issue_imported_image_and_idempotent_cron(self):
        calls = self.run_script()
        docker = calls[0]
        self.assertEqual(docker[0:3], ["docker", "run", "--rm"])
        self.assertIn("certbot/certbot:v5.8.0", docker)
        self.assertEqual(docker[docker.index("--pull") + 1], "never")
        self.assertIn(f"{self.webroot}:{self.webroot}", docker)
        self.assertIn("--no-directory-hooks", docker)
        self.assertTrue(docker[docker.index("--deploy-hook") + 1].startswith("touch "))
        self.assertEqual([c[0] for c in calls[1:]], ["hook", "nginx", "nginx"])
        self.assertFalse(self.pending.exists())
        cron = self.cron.read_text()
        self.assertIn("23 3,15 * * * root", cron)
        self.assertIn(f"{self.env_file} {self.webroot} renew", cron)
        self.run_script()
        self.assertEqual(self.cron.read_text(), cron)

    def test_renew_no_change_does_not_reload(self):
        calls = self.run_script("renew")
        self.assertEqual([c[0] for c in calls], ["docker"])
        self.assertIn("renew", calls[0])
        self.assertFalse(self.cron.exists())

    def test_successful_renewal_applies_to_both_consumers(self):
        self.env["TEST_RENEWED"] = "1"
        calls = self.run_script("renew")
        self.assertEqual([c[0] for c in calls], ["docker", "hook", "nginx", "nginx"])
        self.assertFalse(self.pending.exists())

    def test_failed_sync_keeps_pending_and_next_run_retries(self):
        self.env["TEST_RENEWED"] = "1"
        self.env["FAIL_HOOK"] = "7"
        self.run_script("renew", expected=7)
        self.assertTrue(self.pending.exists())
        del self.env["TEST_RENEWED"]
        del self.env["FAIL_HOOK"]
        self.log.unlink()
        calls = self.run_script("renew")
        self.assertEqual([c[0] for c in calls], ["docker", "hook", "nginx", "nginx"])
        self.assertFalse(self.pending.exists())

    def test_failed_acme_does_not_apply_certificates(self):
        self.env["FAIL_DOCKER"] = "1"
        calls = self.run_script(expected=1)
        self.assertEqual([c[0] for c in calls], ["docker"])

    def test_missing_scheduler_stops_before_issuance(self):
        (self.bin / "crond").unlink()
        self.assertEqual(self.run_script(expected=69), [])

    def test_nginx_failure_keeps_pending_for_retry(self):
        self.env["FAIL_NGINX"] = "1"
        self.run_script(expected=1)
        self.assertTrue(self.pending.exists())
        self.assertTrue(self.cron.exists())


if __name__ == "__main__":
    unittest.main()
