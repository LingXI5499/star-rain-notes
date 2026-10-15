"""Build a whitelisted deployment ZIP from clean, matching frontend/backend builds."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
from datetime import datetime, timezone
import zipfile


def sha256(path):
    with path.open('rb') as stream:
        digest = hashlib.sha256()
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
        return digest.hexdigest()


def git(source, *args):
    return subprocess.check_output(['git', *args], cwd=source, text=True).strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    options = parser.parse_args()
    source = options.source.resolve()
    output = options.output.resolve()
    commit = git(source, 'rev-parse', 'HEAD')
    if git(source, 'status', '--porcelain'):
        raise SystemExit('Refusing to package a dirty source checkout')
    frontend = source / 'frontend/dist'
    frontend_identity = json.loads((frontend / 'build-info.json').read_text(encoding='utf-8'))
    if frontend_identity.get('commit') != commit or frontend_identity.get('dirty') is not False:
        raise SystemExit('Frontend build does not match the clean source commit')
    jar = source / 'backend/star-rain-boot/target/star-rain-boot-2.0.1.jar'
    with zipfile.ZipFile(jar) as archive:
        entries = set(archive.namelist())
        candidates = entries.intersection({'META-INF/build-info.properties',
                                          'BOOT-INF/classes/META-INF/build-info.properties'})
        if len(candidates) != 1 or not any(name.startswith('BOOT-INF/lib/') for name in entries):
            raise SystemExit('Expected an executable Spring Boot JAR with build metadata')
        info = archive.read(candidates.pop()).decode('utf-8')
        backend_identity = dict(line.split('=', 1) for line in info.splitlines()
                                if line and not line.startswith('#') and '=' in line)
    if backend_identity.get('build.commit') != commit:
        raise SystemExit('Backend build does not match the source commit')
    if not (frontend / 'index.html').is_file():
        raise SystemExit('Frontend index.html is missing')
    now = datetime.now(timezone.utc)
    name = f'star-rain-account-v3-{now:%Y%m%d}-{commit[:12]}'
    output.mkdir(parents=True, exist_ok=True)
    stage = output / name
    zip_path = output / (name + '.zip')
    checksum_path = output / (name + '.zip.sha256')
    if stage.exists() or zip_path.exists() or checksum_path.exists():
        raise SystemExit('Release output already exists; choose another output directory')
    # Only explicit build outputs and reviewed public documents enter the package.
    copies = {
        'backend/star-rain-boot-2.0.1.jar': jar,
        'DEPLOY.md': source / 'docs/deployment/account-v3-deploy.md',
        'LICENSE': source / 'LICENSE',
    }
    for name in ('application-local-secret.example.yml', 'nginx-http-log-format.conf',
                 'nginx-site.example.conf', 'star-rain.service'):
        copies[f'deploy/{name}'] = source / 'backend/deploy/account-v3' / name
    for name in ('account-v3-final-audit.md', 'account-v3-test-report.md',
                 'account-v3-release.md', 'account-v3-verification.json'):
        copies[f'docs/account/{name}'] = source / 'docs/account' / name
    for path in frontend.rglob('*'):
        if path.is_symlink():
            raise SystemExit('Symlinks are not accepted in frontend build outputs')
        if path.is_file():
            copies['frontend/dist/' + path.relative_to(frontend).as_posix()] = path
    for relative, original in copies.items():
        if original.is_symlink() or not original.is_file():
            raise SystemExit(f'Missing or invalid package input: {relative}')
    stage.mkdir()
    for relative, original in copies.items():
        target = stage / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(original, target)
    manifest = {
        'applicationVersion': '2.0.1', 'accountStatus': 'Freeze Candidate',
        'repository': 'https://github.com/LingXI5499/star-rain-notes',
        'commit': commit, 'builtAtUtc': now.isoformat(),
        'latestFlywayMigration': '2.047',
        'backend': backend_identity, 'frontend': frontend_identity,
        'verification': 'See docs/account; this packaging run does not repeat the test suite.',
        'productionAcceptance': 'Pending actual HTTPS, SMTP, backup recovery and hardware acceptance.',
    }
    (stage / 'BUILD-MANIFEST.json').write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    files = sorted(path for path in stage.rglob('*') if path.is_file())
    (stage / 'SHA256SUMS').write_text(''.join(
        f'{sha256(path)}  {path.relative_to(stage).as_posix()}\n' for path in files), encoding='utf-8')
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in sorted(stage.rglob('*')):
            if path.is_file():
                archive.write(path, 'star-rain-account-v3/' + path.relative_to(stage).as_posix())
    checksum = sha256(zip_path)
    checksum_path.write_text(f'{checksum}  {zip_path.name}\n', encoding='utf-8')
    print(json.dumps({'zip': str(zip_path), 'sha256': checksum,
                      'bytes': zip_path.stat().st_size, 'commit': commit}, indent=2))


if __name__ == '__main__':
    main()
