#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-https://example.com}"
BASE_URL="${BASE_URL%/}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT INT TERM

check() {
  local path="$1"
  local expected="${2:-200}"
  local status
  status="$(curl --silent --show-error --location --output /dev/null \
    --write-out '%{http_code}' --connect-timeout 5 --max-time 15 "$BASE_URL$path")"
  if [[ "$status" != "$expected" ]]; then
    echo "FAIL $path returned $status (expected $expected)" >&2
    exit 1
  fi
  echo "OK   $path ($status)"
}

check_header() {
  local path="$1"
  local header="$2"
  local expected_pattern="$3"
  local headers="$TMP_DIR/headers"
  curl --silent --show-error --location --head --connect-timeout 5 --max-time 15 \
    "$BASE_URL$path" > "$headers"
  if ! grep -Eiq "^${header}:[[:space:]]*${expected_pattern}" "$headers"; then
    echo "FAIL $path missing header $header matching $expected_pattern" >&2
    exit 1
  fi
  echo "OK   $path header $header"
}

health_body="$(curl --silent --show-error --location --connect-timeout 5 --max-time 15 \
  "$BASE_URL/actuator/health")"
if ! grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' <<<"$health_body"; then
  echo "FAIL /actuator/health did not report UP" >&2
  exit 1
fi
echo "OK   /actuator/health reports UP"

check "/"
check "/admin/login"
check "/api/v1/public/home"
check "/api/v1/auth/csrf"
check "/api/v1/admin/blog/posts" 401
check "/api/v1/setup/status" 410

check_header "/" "strict-transport-security" "max-age=31536000"
check_header "/" "x-content-type-options" "nosniff"

asset="$(curl --silent --show-error --max-time 15 "$BASE_URL/" \
  | grep -oE '/assets/[^\"[:space:]]+\.(js|css)' | head -n 1 || true)"
if [[ -n "$asset" ]]; then
  check "$asset"
  check_header "$asset" "cache-control" ".*immutable"
  check_header "$asset" "x-content-type-options" "nosniff"
else
  echo "FAIL no hashed JS/CSS asset was discovered in the HTML shell" >&2
  exit 1
fi

echo "Public deployment health checks passed for $BASE_URL"
