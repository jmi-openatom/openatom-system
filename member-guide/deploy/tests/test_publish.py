import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('guide_publish', Path(__file__).parents[1] / 'publish.py')
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)
COMMIT = 'a' * 40
DOMAIN = 'guide.jmi-openatom.cn'


class PublishTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.base = Path(self.tmp.name) / 'guide-site'

    def prepare(self, number=1, commit=COMMIT):
        release_id = f'{commit}-{number}-1'
        incoming = self.base / 'incoming' / release_id
        public = incoming / 'public'
        public.mkdir(parents=True)
        files = {}
        for name in ['index.html', 'app.js', 'content.js', 'walkthroughs.js', 'styles.css', 'assets/logo.svg', 'release.json']:
            path = public / name
            path.parent.mkdir(parents=True, exist_ok=True)
            payload = json.dumps({'commit': commit, 'runNumber': number}) if name == 'release.json' else f'{name} release {number}'
            path.write_text(payload)
            files[name] = hashlib.sha256(path.read_bytes()).hexdigest()
        (incoming / 'manifest.json').write_text(json.dumps({'commit': commit, 'files': files}))
        return release_id, public

    def publish(self, release_id, commit=COMMIT, domain=DOMAIN):
        with contextlib.redirect_stdout(io.StringIO()):
            publisher.publish(str(self.base), release_id, commit, domain)

    def test_verified_release_switches_current_and_preserves_old(self):
        first, _ = self.prepare(1)
        self.publish(first)
        second, _ = self.prepare(2)
        self.publish(second)
        self.assertEqual((self.base / 'current').readlink().as_posix(), f'releases/{second}')
        self.assertTrue((self.base / 'releases' / first / 'index.html').is_file())
        self.assertEqual(json.loads((self.base / 'current/release.json').read_text())['runNumber'], 2)

    def test_checksum_failure_keeps_current_release(self):
        first, _ = self.prepare(1)
        self.publish(first)
        second, public = self.prepare(2)
        (public / 'index.html').write_text('modified after packaging')
        with self.assertRaisesRegex(ValueError, 'checksum mismatch'):
            self.publish(second)
        self.assertEqual((self.base / 'current').readlink().as_posix(), f'releases/{first}')

    def test_extra_file_is_rejected(self):
        release_id, public = self.prepare()
        (public / 'unexpected.txt').write_text('not in manifest')
        with self.assertRaisesRegex(ValueError, 'complete public directory'):
            self.publish(release_id)

    def test_traversal_in_manifest_is_rejected(self):
        release_id, _ = self.prepare()
        path = self.base / 'incoming' / release_id / 'manifest.json'
        manifest = json.loads(path.read_text())
        manifest['files']['../outside'] = 'a' * 64
        path.write_text(json.dumps(manifest))
        with self.assertRaisesRegex(ValueError, 'Unsafe manifest path'):
            self.publish(release_id)

    def test_symlink_in_public_is_rejected(self):
        release_id, public = self.prepare()
        (public / 'app.js').unlink()
        (public / 'app.js').symlink_to(public / 'content.js')
        with self.assertRaisesRegex(ValueError, 'unsafe release file'):
            self.publish(release_id)

    def test_newer_release_cannot_be_replaced_by_older_run(self):
        second, _ = self.prepare(2)
        self.publish(second)
        first, _ = self.prepare(1)
        with self.assertRaisesRegex(ValueError, 'newer workflow release'):
            self.publish(first)
        self.assertEqual((self.base / 'current').readlink().as_posix(), f'releases/{second}')

    def test_other_website_directory_is_not_modified(self):
        release_id, _ = self.prepare()
        (self.base / 'index.html').write_text('existing unrelated website')
        with self.assertRaisesRegex(ValueError, 'unrecognized site directory'):
            self.publish(release_id)
        self.assertFalse((self.base / 'current').exists())
        self.assertEqual((self.base / 'index.html').read_text(), 'existing unrelated website')

    def test_domain_mismatch_is_rejected(self):
        first, _ = self.prepare(1)
        self.publish(first)
        second, _ = self.prepare(2)
        with self.assertRaisesRegex(ValueError, 'another website'):
            self.publish(second, domain='other.example.com')


if __name__ == '__main__':
    unittest.main()
