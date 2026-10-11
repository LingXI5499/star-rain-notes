"""Check tracked public source without printing private values."""
import pathlib
import re
import subprocess
import sys

root = pathlib.Path(__file__).resolve().parents[2]
paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode('utf-8').split('\0')
allowed_root_files = {'README.md', 'LICENSE', '.gitignore', '.gitattributes'}
secret_patterns = [
    re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    re.compile(r'\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,})\b'),
    re.compile(r'\bAKIA[A-Z0-9]{16}\b'),
    re.compile(r'(?<![\w.])[\w.+-]+@(?:qq|163|126|gmail|outlook|hotmail)\.com\b', re.I),
]
errors = []
for name in filter(None, paths):
    path = pathlib.PurePosixPath(name)
    if path.parts[0] not in {'backend', 'frontend', '.github'} and name not in allowed_root_files:
        errors.append((name, 'outside active source tree'))
        continue
    lower = path.name.lower()
    private_config = (lower.startswith('.env') and lower != '.env.example') or (
        re.fullmatch(r'application-.*-secret\.ya?ml', lower) is not None)
    if private_config or lower.endswith(('.pem', '.key', '.p12', '.pfx', '.dump', '.bak')):
        errors.append((name, 'private configuration or key file'))
        continue
    if lower.endswith('.sql') and '/src/main/resources/db/migration/' not in name:
        errors.append((name, 'database export outside migrations'))
        continue
    full = root / name
    if not full.exists():
        errors.append((name, 'tracked file is missing'))
        continue
    try:
        content = full.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        continue
    for number, line in enumerate(content.splitlines(), 1):
        if any(pattern.search(line) for pattern in secret_patterns):
            errors.append((name, f'possible private value at line {number}'))
for name, reason in errors:
    print(f'{name}: {reason}', file=sys.stderr)
if errors:
    sys.exit(1)
print(f'Public source check passed: {len(list(filter(None, paths)))} tracked files')
