# RabbitMQ 安装与配置指南 (Windows)

## 1. 下载 RabbitMQ

RabbitMQ 依赖 Erlang/OTP，需要先安装 Erlang。

### 安装 Erlang
- 下载地址: https://github.com/erlang/otp/releases
- 建议版本: OTP 27.x (Windows 64-bit Installer)
- 安装后会自动添加到系统环境变量

### 安装 RabbitMQ
- 下载地址: https://github.com/rabbitmq/rabbitmq-server/releases
- 建议版本: rabbitmq-server-4.0.x.exe
- 直接安装即可

## 2. 启动 RabbitMQ

安装完成后，通过开始菜单找到 "RabbitMQ Service - start" 启动服务，
或者在命令行执行:
```bash
rabbitmq-service.bat start
```

## 3. 启用管理界面
```bash
rabbitmq-plugins.bat enable rabbitmq_management
```

管理界面访问: http://localhost:15672
默认账号密码: guest / guest

## 4. 验证
```bash
rabbitmqctl.bat status
```
