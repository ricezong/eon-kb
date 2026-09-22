#!/bin/bash
# =============================================
# Eon 知识库 - PostgreSQL 健康检查脚本
# 建议加入 crontab 每 5 分钟执行一次
# */5 * * * * /home/postgres/scripts/healthcheck.sh
# =============================================

set -euo pipefail

CONTAINER_NAME="eon-postgres"
LOG_FILE="/var/log/eon/healthcheck.log"
ALERT_EMAIL="${ALERT_EMAIL:-}"  # 设置此变量启用邮件告警

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 确保日志目录存在
mkdir -p "$(dirname "$LOG_FILE")"

# 检查容器是否运行
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    log "CRITICAL: 容器 ${CONTAINER_NAME} 未运行！"
    log "尝试重启容器..."
    cd "$(dirname "$0")/.." && docker compose up -d
    sleep 10
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        log "ERROR: 容器重启失败！"
        [ -n "$ALERT_EMAIL" ] && echo "PostgreSQL容器重启失败，请立即检查！" | mail -s "[ALERT] eon PG Down" "$ALERT_EMAIL"
        exit 1
    fi
    log "INFO: 容器已成功重启"
fi

# 检查 PostgreSQL 连接
if ! docker exec "$CONTAINER_NAME" pg_isready -U postgres -d eon > /dev/null 2>&1; then
    log "WARNING: PostgreSQL 无法接受连接"
    exit 1
fi

# 检查扩展是否加载
EXTENSIONS_OK=$(docker exec "$CONTAINER_NAME" psql -U postgres -d eon -tAc \
    "SELECT count(*) FROM pg_extension WHERE extname IN ('vector','pg_trgm','zhparser');" 2>/dev/null || echo "0")

if [ "$EXTENSIONS_OK" -lt 3 ]; then
    log "WARNING: 扩展加载不完整 (已加载: ${EXTENSIONS_OK}/3)"
else
    log "OK: 服务正常 - 所有扩展已加载"
fi

# 检查磁盘空间（数据卷使用率）
DISK_USAGE=$(df -h /var/lib/docker/volumes | tail -1 | awk '{print $5}' | tr -d '%')
if [ "$DISK_USAGE" -gt 85 ]; then
    log "WARNING: 磁盘使用率 ${DISK_USAGE}% 超过 85% 阈值"
    [ -n "$ALERT_EMAIL" ] && echo "PostgreSQL服务器磁盘使用率${DISK_USAGE}%，请及时清理！" | mail -s "[ALERT] Disk Space Low" "$ALERT_EMAIL"
fi

exit 0
