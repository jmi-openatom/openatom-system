"""Verify a static member-guide release, then atomically switch its current symlink."""
import fcntl
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import sys

KIND = 'openatom-member-guide'


def publish(base_path, release_id, expected_commit, domain):
    base = Path(base_path)
    if not base.is_absolute() or base.is_symlink() or len(base.parts) < 4:
        raise ValueError('Use a dedicated absolute deployment directory, not a shared root or symlink.')
    if not re.fullmatch(r'[a-f0-9]{40}-[0-9]+-[0-9]+', release_id):
        raise ValueError('Invalid release ID.')
    if not re.fullmatch(r'[a-f0-9]{40}', expected_commit) or not release_id.startswith(expected_commit + '-'):
        raise ValueError('Invalid or mismatched commit.')
    if not re.fullmatch(r'[a-zA-Z0-9](?:[a-zA-Z0-9.-]*[a-zA-Z0-9])?', domain):
        raise ValueError('Invalid site domain.')
    base.mkdir(parents=True, exist_ok=True)
    with (base / '.publish.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        marker = base / '.member-guide-site.json'
        if marker.exists():
            identity = json.loads(marker.read_text())
            if identity.get('kind') != KIND or identity.get('domain') != domain:
                raise ValueError('Deployment directory belongs to another website.')
        elif any(p.name not in {'incoming', 'releases', '.publish.lock'} for p in base.iterdir()):
            raise ValueError('Refusing to publish into a nonempty, unrecognized site directory.')

        incoming = base / 'incoming' / release_id
        public = incoming / 'public'
        if incoming.is_symlink() or public.is_symlink() or not public.is_dir():
            raise ValueError('Missing or unsafe incoming public directory.')
        manifest = json.loads((incoming / 'manifest.json').read_text())
        if manifest.get('commit') != expected_commit or not isinstance(manifest.get('files'), dict):
            raise ValueError('Invalid manifest or wrong commit.')
        files = manifest['files']
        required = {'index.html', 'app.js', 'content.js', 'walkthroughs.js', 'styles.css', 'assets/logo.svg', 'release.json'}
        if not required.issubset(files):
            raise ValueError('Release is missing required site files.')
        for name, expected_hash in files.items():
            relative = PurePosixPath(name)
            if relative.is_absolute() or '..' in relative.parts or str(relative) != name:
                raise ValueError('Unsafe manifest path.')
            if not re.fullmatch(r'[a-f0-9]{64}', str(expected_hash)):
                raise ValueError('Invalid file digest.')
            path = public / name
            if path.is_symlink() or not path.is_file():
                raise ValueError('Missing or unsafe release file: ' + name)
            if hashlib.sha256(path.read_bytes()).hexdigest() != expected_hash:
                raise ValueError('Release checksum mismatch: ' + name)
        actual = set()
        for path in public.rglob('*'):
            if path.is_symlink():
                raise ValueError('Symlinks are not allowed in static releases.')
            if path.is_file():
                actual.add(path.relative_to(public).as_posix())
        if actual != set(files):
            raise ValueError('Manifest does not match the complete public directory.')

        metadata = json.loads((public / 'release.json').read_text())
        run_number = metadata.get('runNumber')
        if metadata.get('commit') != expected_commit or type(run_number) is not int or run_number < 1:
            raise ValueError('Invalid release metadata.')
        current = base / 'current'
        if current.exists() and not current.is_symlink():
            raise ValueError('Current site root must be a symlink managed by this publisher.')
        if current.is_symlink():
            if not os.readlink(current).startswith('releases/'):
                raise ValueError('Current symlink points outside managed releases.')
            old = json.loads((current / 'release.json').read_text())
            if old['runNumber'] > run_number:
                raise ValueError('A newer workflow release is already active.')

        releases = base / 'releases'
        releases.mkdir(exist_ok=True)
        if releases.is_symlink():
            raise ValueError('Releases directory must not be a symlink.')
        base.chmod(0o755)
        releases.chmod(0o755)
        release = releases / release_id
        if release.exists():
            raise ValueError('This release ID is already published; rerun the workflow for a new attempt ID.')
        os.replace(public, release)
        # Only static files go into the web root. Previous releases stay available.
        for path in release.rglob('*'):
            path.chmod(0o755 if path.is_dir() else 0o644)
        release.chmod(0o755)
        temporary = base / ('.current-' + release_id)
        marker.write_text(json.dumps({'kind': KIND, 'domain': domain}) + '\n')
        temporary.symlink_to('releases/' + release_id, target_is_directory=True)
        os.replace(temporary, current)
        print(json.dumps({'status': 'published', 'commit': expected_commit, 'root': str(current), 'domain': domain}))


if __name__ == '__main__':
    if len(sys.argv) != 5:
        raise SystemExit('Usage: publish.py DEPLOY_PATH RELEASE_ID COMMIT DOMAIN')
    publish(*sys.argv[1:])
