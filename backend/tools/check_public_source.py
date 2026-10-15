"""Check tracked public source without printing private values."""
# 只读取 Git 跟踪的当前源码，忽略本机运行数据；发现问题时不打印匹配到的隐私值。
# PurePosixPath 统一处理 Git 返回的路径，避免 Windows 分隔符影响目录白名单。
import pathlib
import hashlib
import re
import subprocess
import sys

# 脚本位于 backend/tools；向上两级取得仓库根目录，避免依赖 shell 当前目录。
root = pathlib.Path(__file__).resolve().parents[2]
# NUL 分隔能保留带空格或中文的文件名；Git 出错时立即失败，不把空清单视为通过。
paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode('utf-8').split('\0')
# 根目录只允许项目说明、开源协议和 Git 规则，其余公共内容应放在前后端、自建 skills 或 CI 目录。
allowed_root_files = {'README.md', 'LICENSE', '.gitignore', '.gitattributes'}
# 扫描私钥头、常见平台令牌、云访问标识及个人邮箱特征；命中仅返回文件和行号。
# 这是项目范围的辅助检查，不尝试解密文件或访问任何外部服务。
secret_patterns = [
    re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    re.compile(r'\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,})\b'),
    re.compile(r'\bAKIA[A-Z0-9]{16}\b'),
    re.compile(r'(?<![\w.])[\w.+-]+@(?:qq|163|126|gmail|outlook|hotmail)\.com\b', re.I),
]
# Word export bundles this exact upstream license snapshot. Its copyright includes
# a publicly distributed author email; exempt only the email heuristic, never keys/tokens.
# A changed snapshot must be reviewed and pinned again, not admitted by path alone.
public_license_snapshots = {
    'frontend/public/legal/editor-word-third-party-notices.txt':
        '058f7c18088e046e163ec1c423775c00cedeffef55061d6187fa3a4599cbc8a2',
}
# 累积所有问题再统一报告，方便一次修正多个文件。
errors = []
# 逐文件验证目录范围、私有配置类型和内容特征；路径或类型不允许时不继续读取内容。
for name in filter(None, paths):
    path = pathlib.PurePosixPath(name)
    audit_document = name.startswith(('docs/audit/', 'docs/deployment/', 'docs/editor/', 'docs/account/')) and name.endswith('.md')
    # Only the reviewed, secret-free account verification summary is admitted as JSON evidence.
    verification_evidence = name == 'docs/account/account-v3-verification.json'
    verification_tool = path.parts[0] == 'tools' and path.suffix in {'.py', '.mjs', '.txt'}
    if path.parts[0] not in {'backend', 'frontend', 'skills', '.github'} and name not in allowed_root_files and not audit_document and not verification_tool and not verification_evidence:
        errors.append((name, 'outside active source tree'))
        continue
    # 大小写统一后判断敏感文件后缀；公开的 .env.example 是允许的模板。
    lower = path.name.lower()
    private_config = (lower.startswith('.env') and lower != '.env.example') or (
        re.fullmatch(r'application-.*-secret\.ya?ml', lower) is not None)
    if private_config or lower.endswith(('.pem', '.key', '.p12', '.pfx', '.dump', '.bak')):
        errors.append((name, 'private configuration or key file'))
        continue
    # 数据库只公开版本化迁移，禁止把本机导出的业务数据作为公共源文件提交。
    if lower.endswith('.sql') and '/src/main/resources/db/migration/' not in name:
        errors.append((name, 'database export outside migrations'))
        continue
    full = root / name
    if not full.exists():
        errors.append((name, 'tracked file is missing'))
        continue
    # 二进制资源无法作为 UTF-8 读取时跳过内容匹配；仍已完成其路径与扩展名检查。
    try:
        content = full.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        continue
    expected_license_hash = public_license_snapshots.get(name)
    verified_license = bool(expected_license_hash and hashlib.sha256(content.encode('utf-8')).hexdigest() == expected_license_hash)
    if expected_license_hash and not verified_license:
        errors.append((name, 'third-party license snapshot differs from reviewed upstream content'))
    # 从 1 开始计数便于编辑器定位；只记录行号，不泄露该行的原文。
    for number, line in enumerate(content.splitlines(), 1):
        patterns = secret_patterns[:-1] if verified_license else secret_patterns
        if any(pattern.search(line) for pattern in patterns):
            errors.append((name, f'possible private value at line {number}'))
# CI 通过退出码判定结果；错误写入 stderr，成功摘要写入 stdout。
for name, reason in errors:
    print(f'{name}: {reason}', file=sys.stderr)
if errors:
    sys.exit(1)
print(f'Public source check passed: {len(list(filter(None, paths)))} tracked files')
