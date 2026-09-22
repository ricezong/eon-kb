#!/bin/bash
# =============================================
# Eon 知识库 - PostgreSQL 备份脚本
# 建议加入 crontab 每天凌晨 2 点执行
# 0 2 * * * /home/postgres/scripts/backup.sh
# =============================================

set -euo pipefail

CONTAINER_NAME="eon-postgres"
BACKUP_DIR="/home/postgres/backups"
DB_NAME="eon-kb"
DB_USER="postgres"
KEEP_DAYS=7  # 保留最近 N 天的备份

TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
BACKUP_FILE="${BACKUP_DIR}/eon_${TIMESTAMP}.sql.gz"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 开始备份数据库 ${DB_NAME}..."

# 确保备份目录存在
mkdir -p "$BACKUP_DIR"

# 使用 pg_dump 导出并压缩
docker exec "$CONTAINER_NAME" pg_dump \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    --format=plain \
    | gzip > "$BACKUP_FILE"

BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 备份完成: ${BACKUP_FILE} (${BACKUP_SIZE})"

# 清理过期备份
DELETED_COUNT=$(find "$BACKUP_DIR" -name "eon_*.sql.gz" -mtime +${KEEP_DAYS} -delete -print | wc -l)
if [ "$DELETED_COUNT" -gt 0 ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 已清理 ${DELETED_COUNT} 个过期备份（>${KEEP_DAYS}天）"
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 当前备份列表："
ls -lh "${BACKUP_DIR}"/eon_*.sql.gz 2>/dev/null || echo "  （无备份文件）"

# =============================================
# 恢复命令参考：
#
# 1. 从自定义格式备份恢复：
#    docker exec -i eon-postgres pg_restore \
#        -U postgres -d eon --clean --if-exists \
#        < backup_file.sql.gz | gunzip
#
# 2. 从纯 SQL 备份恢复：
#    gunzip -c backup_file.sql.gz | docker exec -i eon-postgres \
#        psql -U postgres -d eon
#
# 3. Docker Volume 整体备份：
#    docker run --rm -v eon_pgdata:/data -v /backup:/backup alpine \
#        tar czf /backup/pgdata_$(date +%Y%m%d).tar.gz /data
# =============================================
