# Canal 安装与配置指南 (Windows)

## 1. 下载 Canal
- 下载地址: https://github.com/alibaba/canal/releases
- 建议版本: canal.deployer-1.1.7.tar.gz
- 解压到任意目录，如 `D:\canal`

## 2. 配置 MySQL

### 启用 MySQL binlog
编辑 MySQL 配置文件 `my.ini`（通常在 `C:\ProgramData\MySQL\MySQL Server 8.0\`），添加：
```ini
[mysqld]
log-bin=mysql-bin
binlog-format=ROW
server-id=1
```

### 创建 Canal 用户
```sql
CREATE USER 'canal'@'%' IDENTIFIED BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;
```

### 重启 MySQL
```bash
net stop MySQL80
net start MySQL80
```

## 3. 配置 Canal

### 编辑 canal.properties (D:\canal\conf\canal.properties)
```properties
canal.serverMode = rabbitMQ
rabbitmq.host = 127.0.0.1
rabbitmq.port = 5672
rabbitmq.virtual.host = /
rabbitmq.username = guest
rabbitmq.password = guest
```

### 编辑 instance.properties (D:\canal\conf\example\instance.properties)
使用本项目 `canal-config/instance.properties` 替换即可。

## 4. 启动 Canal
```bash
cd D:\canal
bin\startup.bat
```

查看日志确认启动成功:
```bash
tail -f logs/example/example.log
tail -f logs/canal/canal.log
```

## 5. Canal 消息格式

Canal 投递到 RabbitMQ 的消息格式:
```json
{
  "type": "INSERT",
  "table": "tb_coupon_activity",
  "data": [{
    "id": 1,
    "name": "星巴克满50减10",
    "remain_stock": 999,
    ...
  }]
}
```
