# XX 全域营销平台 (XX Marketing Platform)

> 基于 Spring Boot 3.4 + Vue 3 的全域营销优惠券抢购平台

---

## 📋 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [功能特性](#功能特性)
- [架构设计](#架构设计)
- [数据库设计](#数据库设计)
- [核心模块详解](#核心模块详解)
  - [1. JWT + Redis 认证体系](#1-jwt--redis-认证体系)
  - [2. 缓存优化策略](#2-缓存优化策略)
  - [3. Canal + RabbitMQ 数据同步](#3-canal--rabbitmq-数据同步)
  - [4. 抢券防超卖](#4-抢券防超卖)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [前端页面](#前端页面)

---

## 项目概述

XX 全域营销平台是一个完整的 B2C 优惠券营销系统，支持用户手机验证码/密码登录、浏览商家服务、抢购优惠券等功能。系统采用 **Cache Aside 缓存模式**、**Canal + RabbitMQ 异步同步**、**Redis + Lua 原子操作**等技术保障高并发下的数据一致性和防超卖。

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.4.3 |
| **ORM** | MyBatis-Plus | 3.5.9 |
| **数据库** | MySQL | 8.0 |
| **缓存** | Redis | 6.2 |
| **消息队列** | RabbitMQ | 4.0 |
| **数据同步** | Canal | 1.1.7 |
| **认证** | JWT (jjwt) | 0.12.6 |
| **连接池** | Druid | 1.2.24 |
| **分布式锁** | Redisson | 3.40.2 |
| **工具库** | Hutool | 5.8.34 |
| **前端框架** | Vue 3 | 3.x |
| **构建工具** | Vite | 6.x |
| **UI 组件** | Element Plus | 2.x |
| **状态管理** | Pinia | 2.x |

---

## 功能特性

### ✅ 已实现的核心功能

1. **双因素认证** — 手机验证码登录 + 手机号密码登录
2. **JWT 双 Token 无感刷新** — Access Token（30分钟）+ Refresh Token（7天）
3. **ThreadLocal 用户上下文** — 线程级别的用户信息传递
4. **Cache Aside 缓存模式** — 读时写缓存，写时删缓存
5. **布隆过滤器防穿透** — 快速过滤不存在的优惠券ID
6. **缓存空值防穿透** — 对空结果缓存短TTL的NULL值
7. **随机 TTL 防雪崩** — 30~60分钟随机过期时间
8. **定时任务兜底** — 每5分钟刷新首页缓存
9. **Canal + RabbitMQ 同步** — MySQL binlog → RabbitMQ → Redis
10. **Lua 脚本抢券** — 原子性库存校验+扣减+用户资格检查

---

## 架构设计

```
┌──────────────────────────────────────────────────────────────┐
│                        Nginx (可选)                           │
│                    http://localhost:80                        │
└────────┬──────────────────────────────┬──────────────────────┘
         │                              │
    static files                   /api proxy
         │                              │
         ▼                              ▼
┌─────────────────┐           ┌─────────────────────┐
│   Vue 3 前端     │           │  Spring Boot 后端    │
│   (Vite:3000)   │  ◄───►   │  (Tomcat:8080)      │
│                  │  REST    │                      │
│  - 登录页面      │  API     │  - 认证/授权          │
│  - 首页          │          │  - 优惠券管理         │
│  - 优惠券中心    │          │  - 商家管理           │
│  - 我的订单      │          │  - 订单管理           │
└─────────────────┘           │  - 定时任务           │
                              │  - Canal 消息处理     │
                              └──┬──────┬──────┬─────┘
                                 │      │      │
                    ┌────────────┘      │      └────────────┐
                    ▼                   ▼                    ▼
            ┌──────────┐      ┌──────────────┐     ┌──────────────┐
            │  MySQL   │      │    Redis      │     │   RabbitMQ   │
            │  (3306)  │      │   (6379)      │     │   (5672)     │
            └────┬─────┘      └──────────────┘     └──────▲───────┘
                 │                                         │
                 │ binlog                                  │ JSON 消息
                 ▼                                         │
            ┌──────────┐                                  │
            │  Canal   │ ─────────────────────────────────┘
            │ (11111)  │     投递到 RabbitMQ
            └──────────┘
```

### 数据流说明

| 场景 | 流程 |
|------|------|
| **用户登录** | 前端 → Controller → Service → MySQL(验证) → JWT 生成 Token → Redis 存储 Refresh Token |
| **首页浏览** | 前端 → Controller → Redis(缓存) → (miss) → MySQL → 回写 Redis |
| **抢券** | 前端 → Controller → Redis(Lua原子操作) → 返回结果 → RabbitMQ(异步下单) → MySQL |
| **DB变更同步** | MySQL(binlog) → Canal(监听) → RabbitMQ(投递) → 后端消费 → Redis(更新缓存) |

---

## 数据库设计

### ER 图

```
┌──────────┐       ┌──────────────┐       ┌───────────────┐
│  tb_user │       │  tb_merchant │       │ tb_coupon_    │
│          │1    N │              │1    N │ activity      │
│  id (PK) ├──────►│  id (PK)     ├──────►│               │
│  phone   │       │  user_id(FK) │       │  id (PK)      │
│  password│       │  shop_name   │       │  merchant_id  │
│  role    │       │  rating      │       │  amount       │
└────┬─────┘       │  sales       │       │  total_stock  │
     │             └──────────────┘       │  remain_stock │
     │                                    └───────┬───────┘
     │1                               N            │1
     │         ┌───────────────┐                    │
     │         │ tb_coupon_    │                    │
     └────────►│ order         │◄───────────────────┘
               │               │
               │  id (PK)      │         ┌───────────────┐
               │  order_no     │         │ tb_seckill_   │
               │  user_id (FK) │         │ voucher       │
               │  activity_id  │1     1  │               │
               │  status       ├────────►│  activity_id  │
               └───────────────┘         │  stock        │
                                         └───────────────┘
```

### 表结构说明

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `tb_user` | 用户信息表 | id, phone, password, role(USER/MERCHANT) |
| `tb_merchant` | 商家表 | id, user_id, shop_name, rating, sales |
| `tb_coupon_activity` | 优惠券活动表 | id, merchant_id, amount, total_stock, remain_stock, max_per_user |
| `tb_coupon_order` | 优惠券订单表 | id, order_no, user_id, activity_id, status(0未使用/1已使用/2已过期) |
| `tb_seckill_voucher` | 秒杀券表 | id, activity_id, price, stock, begin_time, end_time |

---

## 核心模块详解

### 1. JWT + Redis 认证体系

```
登录流程:
┌──────┐    (1)POST /api/user/login     ┌──────────┐
│ 前端  │ ─────────────────────────────► │ 后端服务  │
│      │    (2)生成 Access Token(30min)  │          │
│      │       + Refresh Token(7day)     │          │
│      │ ◄───────────────────────────── │          │
│      │    (3)存储到 localStorage       │          │
└──────┘                                └──────────┘

Token 刷新流程:
┌──────┐    请求(带旧AccessToken          ┌──────────┐
│ 前端  │    + X-Refresh-Token)           │ 后端服务  │
│      │ ──────────────────────────────► │          │
│      │    响应头 X-New-Access-Token     │  检测Token│
│      │ ◄────────────────────────────── │  即将过期 │
│      │    自动替换localStorage中的Token │  生成新   │
└──────┘                                 │  Token   │
                                         └──────────┘
```

**关键类:**
- `JwtUtil` — Token 生成/解析
- `JwtInterceptor` — 请求拦截验证，从 Token 解析用户信息设置到 `UserContext`
- `RefreshTokenInterceptor` — 检测 Token 即将过期，自动刷新
- `UserContext` — ThreadLocal 线程级用户上下文

**Redis Key 设计:**
| Key | 值 | TTL |
|-----|-----|-----|
| `sms:code:{phone}` | 验证码 | 5分钟 |
| `sms:interval:{phone}` | 1(发送间隔) | 60秒 |
| `token:blacklist:{token}` | 1(黑名单) | Token剩余时间 |
| `refresh:token:{token}` | userId | 7天 |
| `login:user:{userId}` | 用户JSON | 30分钟 |

### 2. 缓存优化策略

#### Cache Aside 模式（旁路缓存）

```
读流程:
  请求 → 查Redis → (命中) 返回
                  → (未命中) → 查MySQL → 回写Redis → 返回

写流程:
  请求 → 更新MySQL → 删除Redis缓存
```

#### 防缓存穿透：布隆过滤器

```
请求(ID=999) → BloomFilter.mightContain("999")
                → false: 直接返回"不存在"，不查DB
                → true:  继续查Redis → MySQL
```

**关键类**: `BloomFilterUtil` — Guava布隆过滤器+Redis持久化

#### 防缓存雪崩：随机 TTL

```java
// 每个缓存的TTL在30~60分钟之间随机
int ttl = cacheTtlMin + random.nextInt(cacheTtlMax - cacheTtlMin + 1);
redisUtil.set(key, value, ttl, TimeUnit.SECONDS);
```

#### 防缓存击穿：缓存永不过期 + TTL 兜底

- **首页服务数据**: 配合定时任务每5分钟刷新，保证缓存永不过期
- **服务详情数据**: TTL 兜底策略，过期后由下次请求重建

#### 缓存空值

```java
if (data == null) {
    // 缓存"NULL"，短TTL（60秒），防止穿透
    redisUtil.set(key, "NULL", 60, TimeUnit.SECONDS);
}
```

### 3. Canal + RabbitMQ 数据同步

```
MySQL(binlog) ───► Canal(监听) ───► RabbitMQ ───► 后端消费者 ───► Redis
                    |                   |              |
                    | 解析binlog        | 投递JSON    | 更新缓存
                    | 格式化JSON        | 消息         | 删除旧缓存
```

**Canal 消息格式:**
```json
{
  "type": "UPDATE",
  "table": "tb_coupon_activity",
  "data": [{
    "id": 1,
    "remain_stock": 950,
    "status": 1
  }]
}
```

**RabbitMQ 配置:**
| 交换机 | 队列 | 路由键 | 用途 |
|--------|------|--------|------|
| `canal.exchange` (Topic) | `canal.queue.coupon` | `canal.routing.coupon` | 优惠券变更 |
| `canal.exchange` (Topic) | `canal.queue.merchant` | `canal.routing.merchant` | 商家变更 |
| `coupon.order.exchange` (Direct) | `coupon.order.queue` | `coupon.order.routing` | 抢券下单 |

**关键类**: `CanalMessageHandler` — 消费 Canal 消息，更新 Redis 缓存  
**开关**: `canal.enabled=true/false`（默认 false，不影响核心功能）

### 4. 抢券防超卖

使用 **Redis + Lua 脚本** 实现原子性操作：

```lua
-- grab_coupon.lua 核心逻辑
1. 检查库存(stock > 0)
2. 检查是否已领取(SISMEMBER)
3. 检查领取上限(SCARD)
4. 扣减库存(DECR)
5. 记录领取(SADD)
```

**调用方式:**
```java
RScript script = redissonClient.getScript(StringCodec.INSTANCE);
List<Object> result = script.eval(
    RScript.Mode.READ_WRITE,
    grabCouponScript,
    RScript.ReturnType.MULTI,
    Arrays.asList(stockKey),
    userKey, userId, maxPerUser
);
```

**返回值:**
| 返回值 | 含义 |
|--------|------|
| 1 | 抢券成功 |
| -1 | 库存不足 |
| -2 | 达到每人领取上限 |
| -3 | 已领取过该券 |
| -4 | 活动已结束 |

**Redis Key 设计:**
| Key | 类型 | 说明 |
|-----|------|------|
| `coupon:stock:{activityId}` | String | 活动库存 |
| `coupon:user:{activityId}` | Set | 该活动已领取用户集合 |
| `coupon:user:{activityId}:{userId}` | Set | 用户已领取数量 |

---

## 项目结构

```
XX_Marketing_platform/
├── marketing-server/                 # 后端 Spring Boot 项目
│   ├── src/main/java/com/xx/marketing/
│   │   ├── MarketingApplication.java # 启动类
│   │   ├── config/                   # 配置类
│   │   │   ├── WebMvcConfig.java     # 拦截器注册
│   │   │   ├── RabbitMQConfig.java   # RabbitMQ 配置
│   │   │   ├── RedissonConfig.java   # Redisson 配置
│   │   │   ├── ScheduleConfig.java   # 定时任务配置
│   │   │   └── MyMetaObjectHandler.java # MyBatis 自动填充
│   │   ├── controller/              # 控制器
│   │   │   ├── UserController.java   # 用户/登录
│   │   │   ├── CouponActivityController.java # 优惠券活动
│   │   │   ├── CouponOrderController.java   # 优惠券订单
│   │   │   ├── MerchantController.java      # 商家
│   │   │   └── HomeController.java          # 首页
│   │   ├── service/                 # 服务接口
│   │   │   ├── UserService.java
│   │   │   ├── CouponActivityService.java
│   │   │   ├── CouponOrderService.java
│   │   │   ├── MerchantService.java
│   │   │   ├── impl/                # 服务实现
│   │   │   │   ├── UserServiceImpl.java      # 认证逻辑
│   │   │   │   ├── CouponActivityServiceImpl.java # 缓存+抢券
│   │   │   │   ├── CouponOrderServiceImpl.java
│   │   │   │   └── MerchantServiceImpl.java
│   │   │   └── CanalMessageHandler.java # Canal消息处理
│   │   ├── mapper/                  # MyBatis Mapper
│   │   ├── entity/                  # 实体类
│   │   ├── dto/                     # 数据传输对象
│   │   ├── common/                  # 公共类
│   │   │   ├── UserContext.java     # ThreadLocal 用户上下文
│   │   │   ├── RedisKeys.java       # Redis Key 常量
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── utils/                   # 工具类
│   │   │   ├── JwtUtil.java         # JWT 工具
│   │   │   ├── RedisUtil.java       # Redis 工具
│   │   │   └── BloomFilterUtil.java # 布隆过滤器
│   │   ├── interceptor/             # 拦截器
│   │   │   ├── JwtInterceptor.java  # JWT 验证
│   │   │   ├── RefreshTokenInterceptor.java # Token 刷新
│   │   │   └── SpringContextHolder.java
│   │   └── task/                    # 定时任务
│   │       └── CacheSyncTask.java   # 缓存同步
│   ├── src/main/resources/
│   │   ├── application.yml          # 主配置
│   │   └── lua/
│   │       └── grab_coupon.lua      # 抢券 Lua 脚本
│   └── pom.xml
├── marketing-web/                    # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/index.js             # API 封装 + 拦截器
│   │   ├── router/index.js          # 路由配置
│   │   ├── stores/user.js           # Pinia 状态管理
│   │   ├── views/
│   │   │   ├── Login.vue            # 登录页
│   │   │   ├── Home.vue             # 首页
│   │   │   ├── Coupons.vue          # 优惠券中心
│   │   │   ├── MyOrders.vue         # 我的订单
│   │   │   └── MerchantDetail.vue   # 商家详情
│   │   ├── App.vue                  # 根组件
│   │   └── main.js                  # 入口
│   └── vite.config.js               # Vite配置(代理)
├── sql/
│   └── init.sql                     # 数据库初始化 + 测试数据
├── canal-config/                    # Canal 配置文件
│   ├── canal.properties
│   ├── instance.properties
│   └── INSTALL.md
├── rabbitmq-config/                 # RabbitMQ 安装指南
│   └── INSTALL.md
└── README.md                        # 本文档
```

---

## 快速开始

### 前置条件

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 25 | 已安装在 `D:\Java\jdk-25` |
| MySQL | 8.0+ | 已安装在默认路径 |
| Redis | 6.0+ | 已安装在 `D:\java_anything\Redis\` |
| RabbitMQ | 4.0+ | 可选，需手动安装 |
| Canal | 1.1.7+ | 可选，需手动安装 |
| Node.js | 18+ | 已安装 v22.22.2 |

### 第一步：初始化数据库

1. 启动 MySQL 服务
2. 执行初始化脚本：
```bash
mysql -u root -p < sql/init.sql
```

### 第二步：启动 Redis

```bash
cd D:\java_anything\Redis
redis-server.exe redis.windows.conf
```

### 第三步：启动后端

**方式一：IDEA 打开项目直接运行**

1. 用 IntelliJ IDEA 打开 `marketing-server` 目录
2. 等待 Maven 依赖下载完成
3. 运行 `MarketingApplication.java`

**方式二：命令行构建运行**

```bash
cd marketing-server

# Windows CMD:
mvnw.bat clean package -DskipTests
java -jar target/marketing-platform-1.0.0.jar
```

后端默认端口: **8080**

### 第四步：启动前端

```bash
cd marketing-web
npm install
npm run dev
```

前端默认端口: **3000**  
访问地址: http://localhost:3000

Vite 已配置 `/api` 代理到 `http://localhost:8080`

### 第五步（可选）：配置 Canal + RabbitMQ

详见:
- [RabbitMQ 安装指南](rabbitmq-config/INSTALL.md)
- [Canal 安装指南](canal-config/INSTALL.md)

启动后，在 `application.yml` 中将 `canal.enabled` 设置为 `true`。

### 测试账号

| 角色 | 手机号 | 密码 | 说明 |
|------|--------|------|------|
| 普通用户 | 13800138000 | 123456 | 可用于抢券 |
| 普通用户 | 13800138001 | 123456 | 可用于抢券 |
| 商家 | 13800138002 | 123456 | 商家角色 |

> **演示环境验证码登录**：发送验证码后，后端日志会打印验证码（模拟短信发送）

---

## API 文档

### 用户模块

| 方法 | URL | 说明 | 认证 |
|------|-----|------|------|
| POST | `/api/user/send-code?phone=xxx` | 发送验证码 | 否 |
| POST | `/api/user/login` | 登录 | 否 |
| POST | `/api/user/refresh?refreshToken=xxx` | 刷新Token | 否 |
| POST | `/api/user/logout` | 登出 | 是 |
| GET | `/api/user/me` | 获取当前用户 | 是 |

**登录请求体 (验证码登录):**
```json
{
  "phone": "13800138000",
  "loginType": "SMS",
  "code": "123456"
}
```

**登录请求体 (密码登录):**
```json
{
  "phone": "13800138000",
  "loginType": "PASSWORD",
  "password": "123456"
}
```

**登录响应:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "expiresIn": 1800
  }
}
```

### 优惠券模块

| 方法 | URL | 说明 | 认证 |
|------|-----|------|------|
| GET | `/api/coupon/list` | 活动列表 | 否 |
| GET | `/api/coupon/detail/{id}` | 活动详情 | 否 |
| POST | `/api/coupon/grab` | 抢券 | 是 |
| GET | `/api/coupon/stock/{id}` | 查询库存 | 否 |

**抢券请求体:**
```json
{ "activityId": 1 }
```

### 订单模块

| 方法 | URL | 说明 | 认证 |
|------|-----|------|------|
| GET | `/api/order/my` | 我的优惠券 | 是 |
| POST | `/api/order/use/{orderId}` | 使用优惠券 | 是 |

### 商家模块

| 方法 | URL | 说明 | 认证 |
|------|-----|------|------|
| GET | `/api/merchant/hot` | 热门商家 | 否 |
| GET | `/api/merchant/detail/{id}` | 商家详情 | 否 |

### 请求认证

需要在请求头中携带 Token：
```
Authorization: Bearer {accessToken}
X-Refresh-Token: {refreshToken}
```

---

## 前端页面

| 页面 | 路径 | 说明 |
|------|------|------|
| 登录页 | `/login` | 手机验证码/密码双模式登录 |
| 首页 | `/home` | 热门商家 + 热门优惠券展示 |
| 优惠券中心 | `/coupons` | 全部优惠券，支持抢购 |
| 我的优惠券 | `/my-orders` | 已领取的优惠券，支持使用 |
| 商家详情 | `/merchant/:id` | 商家信息展示 |

---

## 关键设计决策

### 为什么不直接用数据库事务防超卖？

高并发场景下数据库锁会成为瓶颈。使用 Redis + Lua 脚本：
- ✅ 单线程执行，天然原子性
- ✅ 内存操作，延迟在微秒级
- ✅ 支持复杂条件判断（库存+用户资格）

### 为什么需要双 Token？

- **Access Token (30min)**: 高频使用，短有效期降低泄露风险
- **Refresh Token (7day)**: 低频使用，长有效期减少登录次数
- **无感刷新**: 前端无需感知，后端自动检测并返回新 Token

### Cache Aside 的一致性保证

| 策略 | 说明 |
|------|------|
| 读时写缓存 | Cache miss 时从 DB 加载并写入 Redis |
| 写时删缓存 | 数据更新后删除 Redis 对应 Key |
| 定时任务兜底 | 每5分钟从 DB 全量刷新首页缓存 |
| TTL 兜底 | 服务详情缓存设置随机过期时间 |
| Canal 兜底 | 可选，通过 binlog 实时同步 |

---

## 项目运行验证清单

- [ ] MySQL 服务已启动，`xx_marketing` 数据库已创建
- [ ] Redis 服务已启动 (6379)
- [ ] 后端启动成功 (8080)，无报错
- [ ] 前端启动成功 (3000)，可访问页面
- [ ] 可以发送验证码并登录
- [ ] 首页显示优惠券和商家数据
- [ ] 可以正常抢券
- [ ] 可以查看"我的优惠券"
- [ ] (可选) RabbitMQ + Canal 已配置并运行
