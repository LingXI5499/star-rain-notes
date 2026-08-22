#!/usr/bin/env bash
# Star Rain Notes V1 — database + media backup (06-testing-deployment.md §11)
#
# - mysqldump (single transaction, no locks) → gzip
# - media directory → tar.gz
# - keeps KEEP_DAYS worth of backups, then prunes
#
# Install: copy to the server, make executable, add a cron line:
#   30 2 * * * /opt/star-rain-notes/backup.sh >> /var/log/star-rain-notes/backup.log 2>&1
#
# The DB password is read from /etc/star-rain-notes/star-rain-notes.env
# (chmod 600) — it is never embedded in this script or committed to Git.
set -euo pipefail

BACKUP_ROOT="${BACKUP_ROOT:-/var/backups/star-rain-notes}"
KEEP_DAYS="${KEEP_DAYS:-14}"
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

echo "[$(date -Is)] backup start (db=$MYSQL_DATABASE, media=$MEDIA_DIR)"

# 1) Database dump (UTC data; restore with: zcat file.sql.gz | mysql ...)
if [[ -z "${MYSQL_PASSWORD:-}" ]]; then
  echo "ERROR: MYSQL_PASSWORD is not set in $ENV_FILE" >&2
  exit 1
fi
mysqldump --single-transaction --routines --triggers \
  -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" \
  "$MYSQL_DATABASE" | gzip > "$BACKUP_ROOT/${MYSQL_DATABASE}-${STAMP}.sql.gz"

# 2) Media directory (independent of the DB, per 06 §11)
if [[ -d "$MEDIA_DIR" ]]; then
  tar -czf "$BACKUP_ROOT/media-${STAMP}.tar.gz" \
    -C "$(dirname "$MEDIA_DIR")" "$(basename "$MEDIA_DIR")"
else
  echo "WARN: media dir $MEDIA_DIR does not exist; skipping media backup" >&2
fi

# 3) Prune old backups
find "$BACKUP_ROOT" -maxdepth 1 -type f -name '*.sql.gz' -mtime "+$KEEP_DAYS" -delete
find "$BACKUP_ROOT" -maxdepth 1 -type f -name '*.tar.gz' -mtime "+$KEEP_DAYS" -delete

echo "[$(date -Is)] backup done → $BACKUP_ROOT"
ls -1 "$BACKUP_ROOT" | tail -n 5
