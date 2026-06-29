# RabbitMQ + Canal 集成变更日志

> 日期: 2026-06-29

---

## 修改的文件

| 文件 | 改动 |
|------|------|
| `application.yml` | RabbitMQ 凭据 guest/guest → xx/526570324；canal.enabled: false → true |
| `canal-config/canal.properties` | RabbitMQ 凭据更新为 xx/526570324 |
| `canal-config/instance.properties` | MySQL 密码 root→1234；添加动态路由区分优惠券/商家表；扩展监听表范围 |

---

## RabbitMQ 配置

| 项目 | 值 |
|------|-----|
| 地址 | localhost:5672 |
| 用户名 | xx |
| 密码 | 526570324 |
| 管理界面 | http://localhost:15672 |

### 交换机/队列（Spring Boot 启动时自动创建）

| 交换机 | 队列 | 路由键 | 用途 |
|--------|------|--------|------|
| `canal.exchange` (Topic) | `canal.queue.coupon` | `canal.routing.coupon` | 优惠券活动变更 |
| `canal.exchange` (Topic) | `canal.queue.merchant` | `canal.routing.merchant` | 商家信息变更 |
| `coupon.order.exchange` (Direct) | `coupon.order.queue` | `coupon.order.routing` | 抢券异步下单（预留） |

---

## Canal 配置

| 项目 | 值 |
|------|-----|
| 安装路径 | D:\java_anything\canal\canal.deployer-1.1.8 |
| 监听表 | tb_coupon_activity, tb_merchant, tb_user, tb_coupon_order |
| 投递方式 | RabbitMQ → canal.exchange |

---

## 数据流

```
MySQL (INSERT/UPDATE/DELETE)
  → binlog
    → Canal 捕获
      → RabbitMQ (canal.exchange)
        → CanalMessageHandler 消费
          → 更新 Redis 缓存
          → 更新布隆过滤器
          → 清除首页缓存
```

---

## 你需要手动做的事

### 1. 启动 RabbitMQ（管理员 PowerShell）
```powershell
cd D:\java_anything\RabbitMQ\rabbitmq_server-4.3.2\sbin
.\rabbitmq-server.bat
```
保持窗口开着，看到 `started` 就行。

### 2. 复制 Canal 配置文件（管理员 PowerShell）
```powershell
Copy-Item "D:\java_anything\IDEA\IDEA_Project\XX_Marketing_platform\canal-config\canal.properties" "D:\java_anything\canal\canal.deployer-1.1.8\conf\canal.properties" -Force
Copy-Item "D:\java_anything\IDEA\IDEA_Project\XX_Marketing_platform\canal-config\instance.properties" "D:\java_anything\canal\canal.deployer-1.1.8\conf\example\instance.properties" -Force
```

### 3. 开启 MySQL binlog（如果还没做）
编辑 `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`，在 `[mysqld]` 下加：
```ini
log-bin=mysql-bin
binlog-format=ROW
server-id=1
```
然后重启 MySQL 服务。

### 4. 重启 Spring Boot
IDEA 中停止 → 重新运行 MarketingApplication。启动日志会看到：
- RabbitMQ 连接成功
- 自动创建交换机和队列
- `CanalMessageHandler` 已注册

### 5. 启动 Canal（管理员 PowerShell，新窗口）
```powershell
cd D:\java_anything\canal\canal.deployer-1.1.8\bin
.\startup.bat
```

### 6. 验证
- RabbitMQ: http://localhost:15672 用 xx/526570324 登录，确认 `canal.exchange` 存在
- Canal: 查看 `D:\java_anything\canal\canal.deployer-1.1.8\logs\example\example.log` 无报错
- 后端: 启动日志无 RabbitMQ 连接报错
- 端到端: 在 DataGrip 中修改 tb_coupon_activity 的 remain_stock → 看后端日志是否有 "Canal sync" 输出
