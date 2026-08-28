#!/usr/bin/env bash
# Star Rain Notes V1 — database + media backup (06-testing-deployment.md §11)
#
# - single-instance lock; temporary files are atomically renamed only after validation
# - mysqldump credentials are supplied through a mode-600 temporary option file
# - gzip/tar archives are verified before old backups are pruned
#
# Install: copy to the server, make executable, add a cron line:
#   30 2 * * * /opt/star-rain-notes/backup.sh >> /var/log/star-rain-notes/backup.log 2>&1
#
# The DB password is read from /etc/star-rain-notes/star-rain-notes.env
# (root:starrain, chmod 640) — it is never embedded in this script or committed to Git.
set -euo pipefail

BACKUP_ROOT="${BACKUP_ROOT:-/var/backups/star-rain-notes}"
KEEP_DAYS="${KEEP_DAYS:-14}"
MIN_FREE_KB="${MIN_FREE_KB:-1048576}"
STAMP="$(date +%Y%m%d-%H%M%S)"

# Load runtime configuration (secrets) if present
ENV_FILE="${STAR_RAIN_ENV_FILE:-/etc/star-rain-notes/star-rain-notes.env}"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-star_rain}"
MYSQL_DATABASE="${MYSQL_DATABASE:-star_rain_notes}"
MEDIA_DIR="${MEDIA_STORAGE_DIR:-/srv/star-rain-notes/uploads}"

mkdir -p "$BACKUP_ROOT"

exec 9>"$BACKUP_ROOT/.backup.lock"
if ! flock -n 9; then
  echo "[$(date -Is)] another backup is already running; exiting" >&2
  exit 0
fi

DB_FINAL="$BACKUP_ROOT/${MYSQL_DATABASE}-${STAMP}.sql.gz"
DB_TEMP="${DB_FINAL}.tmp"
MEDIA_FINAL="$BACKUP_ROOT/media-${STAMP}.tar.gz"
MEDIA_TEMP="${MEDIA_FINAL}.tmp"
MYSQL_CNF="$(mktemp)"
cleanup() {
  rm -f "$DB_TEMP" "$MEDIA_TEMP" "$MYSQL_CNF"
}
trap cleanup EXIT INT TERM

available_kb="$(df -Pk "$BACKUP_ROOT" | awk 'NR==2 {print $4}')"
if [[ -z "$available_kb" || "$available_kb" -lt "$MIN_FREE_KB" ]]; then
  echo "ERROR: less than ${MIN_FREE_KB}KB is available under $BACKUP_ROOT" >&2
  exit 1
fi

echo "[$(date -Is)] backup start (db=$MYSQL_DATABASE, media=$MEDIA_DIR)"

# 1) Database dump (UTC data; restore with: zcat file.sql.gz | mysql ...)
if [[ -z "${MYSQL_PASSWORD:-}" ]]; then
  echo "ERROR: MYSQL_PASSWORD is not set in $ENV_FILE" >&2
  exit 1
fi
umask 077
cat > "$MYSQL_CNF" <<EOF
[client]
host=$MYSQL_HOST
port=$MYSQL_PORT
user=$MYSQL_USER
password=$MYSQL_PASSWORD
EOF
mysqldump --defaults-extra-file="$MYSQL_CNF" \
  --single-transaction --routines --triggers --no-tablespaces \
  --set-gtid-purged=OFF "$MYSQL_DATABASE" | gzip > "$DB_TEMP"
gzip -t "$DB_TEMP"
[[ -s "$DB_TEMP" ]] || { echo "ERROR: database backup is empty" >&2; exit 1; }
mv "$DB_TEMP" "$DB_FINAL"

# 2) Media directory (independent of the DB, per 06 §11)
if [[ -d "$MEDIA_DIR" ]]; then
  tar -czf "$MEDIA_TEMP" \
    -C "$(dirname "$MEDIA_DIR")" "$(basename "$MEDIA_DIR")"
  tar -tzf "$MEDIA_TEMP" >/dev/null
  mv "$MEDIA_TEMP" "$MEDIA_FINAL"
else
  echo "WARN: media dir $MEDIA_DIR does not exist; skipping media backup" >&2
fi

# 3) Prune old backups
find "$BACKUP_ROOT" -maxdepth 1 -type f -name '*.sql.gz' -mtime "+$KEEP_DAYS" -delete
find "$BACKUP_ROOT" -maxdepth 1 -type f -name '*.tar.gz' -mtime "+$KEEP_DAYS" -delete

echo "[$(date -Is)] backup done → $BACKUP_ROOT"
ls -1 "$BACKUP_ROOT" | tail -n 5
