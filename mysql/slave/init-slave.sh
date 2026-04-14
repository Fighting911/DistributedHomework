#!/bin/bash
# 等待主库就绪
until mysql -h mysql-master -uroot -p123456 -e "SELECT 1" &>/dev/null; do
  echo "Waiting for master..."
  sleep 2
done

# 获取主库 binlog 位置
MASTER_STATUS=$(mysql -h mysql-master -uroot -p123456 -e "SHOW MASTER STATUS\G" 2>/dev/null)
MASTER_LOG_FILE=$(echo "$MASTER_STATUS" | grep "File:" | awk '{print $2}')
MASTER_LOG_POS=$(echo "$MASTER_STATUS" | grep "Position:" | awk '{print $2}')

# 配置复制
mysql -uroot -p123456 <<EOF
CHANGE MASTER TO
  MASTER_HOST='mysql-master',
  MASTER_USER='repl',
  MASTER_PASSWORD='repl123',
  MASTER_LOG_FILE='${MASTER_LOG_FILE}',
  MASTER_LOG_POS=${MASTER_LOG_POS};
START SLAVE;
EOF
echo "Slave configured: file=${MASTER_LOG_FILE}, pos=${MASTER_LOG_POS}"
