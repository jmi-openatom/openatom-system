"""Exercise the destructive reset with a fake Docker executable only."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("reset-data.sh")
FAKE_DOCKER = '''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
args = sys.argv[1:]
with open(os.environ["RESET_COMMANDS"], "a") as stream:
    stream.write(json.dumps(args) + "\\n")
if args[0] == "compose":
    if "--images" in args:
        print("openatom-mail-mail-api:test")
elif args[:2] == ["volume", "inspect"]:
    logical = args[-1].removeprefix("openatom-mail_")
    project = "main-site" if os.environ.get("WRONG_OWNER") == logical else "openatom-mail"
    print(project + "|" + logical)
elif args[:2] == ["image", "inspect"]:
    if os.environ.get("MISSING_IMAGE"):
        sys.exit(1)
elif args[:2] != ["volume", "rm"]:
    sys.exit(99)
'''


class ResetDataTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        docker = self.root / "docker"
        docker.write_text(FAKE_DOCKER)
        docker.chmod(0o755)
        self.env_file = self.root / "mail.env"
        self.original = (
            "MAIL_DOMAIN=jmi-openatom.cn\n"
            "MAIL_OAUTH_CLIENT_SECRET=keep-oauth-secret\n"
            "MAIL_DB_PASSWORD=keep-db-password\n"
            "STALWART_CONFIG_TOKEN=old-config\n"
            "export STALWART_API_TOKEN=old-api\n"
            "STALWART_DOMAIN_ID=old-domain\n"
            "STALWART_RECOVERY_ADMIN=old-recovery\n"
            "STALWART_RECOVERY_MODE=1\n"
        )
        self.env_file.write_text(self.original)
        self.commands_file = self.root / "commands"
        self.environment = dict(os.environ, PATH=str(self.root) + ":" + os.environ["PATH"],
                                RESET_COMMANDS=str(self.commands_file))

    def run_reset(self, confirm=True, **extra):
        return subprocess.run(
            ["sh", str(SCRIPT), str(self.env_file)] +
            (["--confirm-delete-mail-data"] if confirm else []),
            env=dict(self.environment, **extra), capture_output=True, text=True,
        )

    def commands(self):
        if not self.commands_file.exists():
            return []
        return [json.loads(line) for line in self.commands_file.read_text().splitlines()]

    def assert_no_mutation(self):
        self.assertEqual(self.env_file.read_text(), self.original)
        for command in self.commands():
            self.assertNotIn("rm", command)
            self.assertNotIn("stop", command)

    def test_requires_explicit_confirmation(self):
        self.assertNotEqual(self.run_reset(confirm=False).returncode, 0)
        self.assert_no_mutation()

    def test_refuses_volume_owned_by_another_project_before_stopping_services(self):
        self.assertNotEqual(self.run_reset(WRONG_OWNER="stalwart_data").returncode, 0)
        self.assert_no_mutation()

    def test_missing_release_image_aborts_before_stopping_services(self):
        self.assertNotEqual(self.run_reset(MISSING_IMAGE="1").returncode, 0)
        self.assert_no_mutation()

    def test_deletes_only_mail_volumes_and_preserves_shared_credentials(self):
        result = self.run_reset()
        self.assertEqual(result.returncode, 0, result.stderr)
        commands = self.commands()
        removals = [command[-1] for command in commands if command[:2] == ["volume", "rm"]]
        self.assertEqual(removals, ["openatom-mail_" + name for name in
                                  ["mail_db_data", "mail_redis_data", "stalwart_etc", "stalwart_data"]])
        stop = next(i for i, command in enumerate(commands) if "stop" in command)
        remove = next(i for i, command in enumerate(commands) if command[:2] == ["volume", "rm"])
        self.assertLess(stop, remove)
        contents = self.env_file.read_text()
        self.assertIn("MAIL_DOMAIN=mailer.jmi-openatom.cn\n", contents)
        self.assertIn("MAIL_OAUTH_CLIENT_SECRET=keep-oauth-secret\n", contents)
        self.assertIn("MAIL_DB_PASSWORD=keep-db-password\n", contents)
        self.assertNotIn("old-", contents)
        self.assertIn("STALWART_RECOVERY_MODE=0\n", contents)


if __name__ == "__main__":
    unittest.main()
