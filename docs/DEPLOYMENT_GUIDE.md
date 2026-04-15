# 钱酷报销 - 编译、调试、部署指南

---

## 一、技术栈与环境要求

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 21+ (推荐 Eclipse Temurin) | 后端运行时 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 22+ (LTS) | 前端构建 |
| pnpm | 10+ | 前端包管理 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7+ | 缓存/会话/限流/分布式锁 |
| RabbitMQ | 3.12+ | 消息队列 |
| Docker | 24+ | 容器化部署 |
| Docker Compose | 2.20+ | 编排部署 |
| Nginx | 1.25+ | 反向代理 |

---

## 二、项目结构

```
钱酷报销/
├── backend/                    # Java 后端 (Spring Boot 3.4.4)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/qiankubx/
│       └── main/resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/schema.sql    # 数据库初始化脚本
│
├── frontend/                   # 前端 (Vue 3 + TypeScript)
│   ├── package.json
│   ├── pnpm-workspace.yaml
│   ├── Dockerfile              # H5 移动端构建
│   ├── Dockerfile.desktop      # PC 端构建
│   └── packages/
│       ├── shared/             # 共享层 (API/types/stores/analytics)
│       ├── mobile/             # H5 移动端 (Vant 4)
│       ├── desktop/            # PC 桌面端 (Element Plus)
│       └── miniprogram/        # 微信小程序壳
│
├── nginx/                      # Nginx 配置
│   └── nginx.conf
│
├── docker-compose.yml          # Docker 编排
│
└── docs/                       # 文档
    ├── SYSTEM_DESIGN.md
    ├── USER_AGREEMENT.md
    ├── PRIVACY_POLICY.md
    └── MARKET_RESEARCH.md
```

---

## 三、本地开发环境搭建

### 3.1 安装前置依赖

**macOS:**
```bash
# Java 21
brew install --cask temurin@21

# Maven
brew install maven

# Node.js 22 + pnpm
brew install node@22
npm install -g pnpm

# MySQL 8 + Redis + RabbitMQ (推荐用 Docker)
brew install docker docker-compose
```

**Ubuntu/Debian:**
```bash
# Java 21
sudo apt update
sudo apt install -y openjdk-21-jdk maven

# Node.js 22 + pnpm
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
npm install -g pnpm

# Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

**Windows:**
```powershell
# 使用 scoop 或手动下载安装
scoop install temurin21-jdk maven nodejs-lts pnpm
# Docker Desktop: https://www.docker.com/products/docker-desktop
```

### 3.2 启动基础服务 (MySQL + Redis + RabbitMQ)

推荐使用 Docker 启动基础服务，避免本地安装：

```bash
# 进入项目根目录
cd 钱酷报销

# 仅启动基础服务 (不启动应用)
docker compose up -d mysql redis rabbitmq

# 验证服务状态
docker compose ps

# 查看 MySQL 日志 (等待 ready for connections)
docker compose logs -f mysql
```

**服务端口：**
| 服务 | 端口 | 默认账号 |
|------|------|---------|
| MySQL | 3306 | root / qianku2024 |
| Redis | 6379 | 无密码 |
| RabbitMQ | 5672 (AMQP) / 15672 (管理界面) | guest / guest |

### 3.3 初始化数据库

MySQL 容器启动时会自动执行 `schema.sql` 初始化。如果需要手动执行：

```bash
# 方式一：通过 Docker 进入 MySQL
docker compose exec mysql mysql -uroot -pqianku2024 qianku < backend/src/main/resources/db/schema.sql

# 方式二：用 MySQL 客户端连接
mysql -h127.0.0.1 -P3306 -uroot -pqianku2024 qianku < backend/src/main/resources/db/schema.sql
```

### 3.4 启动后端

```bash
cd backend

# 方式一：Maven 命令行
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式二：IDE (IntelliJ IDEA)
# 1. 打开 backend 目录作为 Maven 项目
# 2. 运行 QianKuApplication.java
# 3. Active Profile 设为 dev

# 验证后端是否启动
curl http://localhost:8080/api/v1/member/plans
# 应返回 {"code":200,"message":"success","data":[...]}
```

**后端关键端口和地址：**
| 地址 | 说明 |
|------|------|
| http://localhost:8080 | 后端 API |
| http://localhost:8080/swagger-ui.html | API 文档 (Swagger UI) |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

### 3.5 启动前端

```bash
cd frontend

# 安装依赖 (首次或 package.json 变更后)
pnpm install

# 启动 H5 移动端开发服务器
pnpm dev:mobile
# 访问 http://localhost:3000

# 启动 PC 桌面端开发服务器 (新终端)
pnpm dev:desktop
# 访问 http://localhost:3001
```

**前端开发服务器已配置 API 代理：**
- `/api` 请求自动代理到 `http://localhost:8080`
- 无需额外配置 CORS

### 3.6 完整的本地开发启动步骤总结

```bash
# 终端 1: 基础服务
docker compose up -d mysql redis rabbitmq

# 终端 2: 后端
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 终端 3: H5 移动端
cd frontend && pnpm install && pnpm dev:mobile

# 终端 4: PC 端 (可选)
cd frontend && pnpm dev:desktop
```

---

## 四、编译与构建

### 4.1 后端编译

```bash
cd backend

# 编译 (不打包)
mvn compile

# 打包 (跳过测试)
mvn package -DskipTests

# 打包 (含测试)
mvn package

# 产物位置
ls target/qianku-backend-1.0.0-SNAPSHOT.jar
```

### 4.2 前端构建

```bash
cd frontend

# 安装依赖
pnpm install

# 构建 H5 移动端
pnpm build:mobile
# 产物: packages/mobile/dist/

# 构建 PC 桌面端
pnpm build:desktop
# 产物: packages/desktop/dist/
```

### 4.3 Docker 镜像构建

```bash
# 构建全部镜像
docker compose build

# 单独构建某个服务
docker compose build backend
docker compose build mobile
docker compose build desktop
```

---

## 五、调试指南

### 5.1 后端调试

**IntelliJ IDEA 调试：**
1. 打开 `backend` 目录作为项目
2. 找到 `QianKuApplication.java`
3. 右键 → Debug
4. 在需要的位置设置断点
5. 访问对应的 API 触发断点

**远程调试（Docker 环境）：**
```bash
# 修改 docker-compose.yml 中 backend 服务
# 添加环境变量和端口映射
environment:
  JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ports:
  - "8080:8080"
  - "5005:5005"  # 调试端口

# IDEA → Run → Edit Configurations → Remote JVM Debug → Port: 5005
```

**查看 SQL 日志：**
`application-dev.yml` 已配置 `log-impl: StdOutImpl`，所有 SQL 会打印到控制台。

**API 测试工具推荐：**
- Swagger UI: http://localhost:8080/swagger-ui.html
- Postman / Apifox
- curl 命令行

**常用调试接口：**
```bash
# 发送验证码 (模拟，控制台会打印验证码)
curl -X POST http://localhost:8080/api/v1/auth/send-sms \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000"}'

# 登录
curl -X POST http://localhost:8080/api/v1/auth/sms-login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","code":"123456"}'
# 返回的 token 用于后续请求

# 带 token 的请求
curl -H "Authorization: Bearer {token}" \
  http://localhost:8080/api/v1/user/profile
```

### 5.2 前端调试

**浏览器 DevTools：**
1. Chrome/Edge 打开 F12
2. Sources 面板设置断点
3. Vue DevTools 扩展查看组件状态和 Pinia Store

**Vite HMR 热更新：**
修改 `.vue` / `.ts` 文件自动刷新，无需手动重启。

**移动端调试（手机上测试 H5）：**
```bash
# 启动时绑定局域网 IP
cd frontend && pnpm dev:mobile -- --host 0.0.0.0

# 手机浏览器访问 http://你的电脑IP:3000
# 例如 http://192.168.1.100:3000
```

**微信开发者工具调试小程序：**
1. 下载 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
2. 导入 `frontend/packages/miniprogram` 目录
3. 填入你的小程序 AppID（或使用测试号）
4. 修改 `utils/config.js` 中的 `H5_BASE_URL` 为开发环境地址

---

## 六、环境变量配置

### 6.1 必须配置的环境变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `MYSQL_HOST` | MySQL 地址 | localhost |
| `MYSQL_PORT` | MySQL 端口 | 3306 |
| `MYSQL_DB` | 数据库名 | qianku |
| `MYSQL_USER` | 数据库用户 | root |
| `MYSQL_PASSWORD` | 数据库密码 | your_password |
| `REDIS_HOST` | Redis 地址 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `JWT_SECRET` | JWT 签名密钥 (≥32字节) | your-jwt-secret-key-32bytes |
| `SIGN_KEY` | 数据签名密钥 (≥32字节) | your-data-sign-key-32bytes |
| `AES_SECRET_KEY` | AES 加密密钥 (16字节) | your16byteskey!! |

### 6.2 可选配置（按需启用功能）

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `OSS_ENDPOINT` | 阿里云 OSS 端点 | oss-cn-hangzhou.aliyuncs.com |
| `OSS_ACCESS_KEY_ID` | OSS AccessKey | - |
| `OSS_ACCESS_KEY_SECRET` | OSS Secret | - |
| `OSS_BUCKET_NAME` | OSS Bucket 名 | qianku-bucket |
| `OSS_CDN_DOMAIN` | CDN 加速域名 | - |
| `BAIDU_OCR_API_KEY` | 百度 OCR API Key | - (不配置则图片发票不可识别) |
| `BAIDU_OCR_SECRET_KEY` | 百度 OCR Secret | - |
| `MAIL_HOST` | 邮件 SMTP 服务器 | smtp.example.com |
| `MAIL_PORT` | 邮件端口 | 465 |
| `MAIL_USER` | 邮件账号 | - |
| `MAIL_PASSWORD` | 邮件密码/授权码 | - |
| `ADMIN_USER_IDS` | 管理员用户ID列表 | 1 (第一个注册用户) |

### 6.3 生产环境 .env 文件示例

```bash
# 创建 .env 文件 (不要提交到 git)
cat > .env << 'EOF'
# 数据库
MYSQL_HOST=your-mysql-host
MYSQL_PORT=3306
MYSQL_DB=qianku_prod
MYSQL_USER=qianku_app
MYSQL_PASSWORD=your_strong_password

# Redis
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# RabbitMQ
RABBITMQ_HOST=your-rabbitmq-host
RABBITMQ_USER=qianku
RABBITMQ_PASSWORD=your_mq_password

# 安全密钥 (务必更换为强随机值!)
JWT_SECRET=prod-jwt-secret-key-must-be-at-least-32-bytes-long-change-me
SIGN_KEY=prod-data-sign-hmac-sha256-secret-key-32bytes
AES_SECRET_KEY=prod16byteskey!!

# 阿里云 OSS
OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
OSS_ACCESS_KEY_ID=LTAI5t...
OSS_ACCESS_KEY_SECRET=...
OSS_BUCKET_NAME=qianku-prod
OSS_CDN_DOMAIN=https://cdn.your-domain.com

# 百度 OCR
BAIDU_OCR_API_KEY=your_baidu_api_key
BAIDU_OCR_SECRET_KEY=your_baidu_secret_key

# 邮件
MAIL_HOST=smtp.aliyun.com
MAIL_PORT=465
MAIL_USER=noreply@your-domain.com
MAIL_PASSWORD=your_email_auth_code

# 管理员
ADMIN_USER_IDS=1
EOF
```

---

## 七、生产环境部署

### 7.1 方式一：Docker Compose 一键部署（推荐）

```bash
# 1. 克隆代码到服务器
git clone https://github.com/your-org/qianku.git
cd qianku

# 2. 创建 .env 文件 (参考 6.3 节)
vim .env

# 3. 修改 docker-compose.yml 中的密码为 .env 引用
# MySQL: MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD}
# 其他服务同理

# 4. 构建并启动全部服务
docker compose --env-file .env up -d --build

# 5. 查看服务状态
docker compose ps

# 6. 查看日志
docker compose logs -f backend     # 后端日志
docker compose logs -f nginx       # Nginx 日志
docker compose logs -f mysql       # 数据库日志

# 7. 访问系统
# PC 端: http://your-server-ip
# H5 端: http://your-server-ip/m/
# API:   http://your-server-ip/api/v1/
```

### 7.2 方式二：手动部署

**服务器准备 (2核4G 以上)：**
```bash
# 安装 Java 21
sudo apt install -y openjdk-21-jre-headless

# 安装 MySQL 8
sudo apt install -y mysql-server-8.0

# 安装 Redis
sudo apt install -y redis-server

# 安装 RabbitMQ
sudo apt install -y rabbitmq-server

# 安装 Nginx
sudo apt install -y nginx

# 安装 Node.js (用于构建前端)
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
npm install -g pnpm
```

**部署后端：**
```bash
# 1. 本地构建 JAR
cd backend
mvn package -DskipTests -Pprod

# 2. 上传到服务器
scp target/qianku-backend-1.0.0-SNAPSHOT.jar user@server:/opt/qianku/

# 3. 创建 systemd 服务
sudo cat > /etc/systemd/system/qianku.service << 'EOF'
[Unit]
Description=钱酷报销后端
After=network.target mysql.service redis.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/qianku
ExecStart=/usr/bin/java -jar -Xms512m -Xmx1024m \
  -Dspring.profiles.active=prod \
  qianku-backend-1.0.0-SNAPSHOT.jar
Restart=always
RestartSec=10
EnvironmentFile=/opt/qianku/.env

[Install]
WantedBy=multi-user.target
EOF

# 4. 启动
sudo systemctl daemon-reload
sudo systemctl enable qianku
sudo systemctl start qianku

# 5. 查看日志
sudo journalctl -u qianku -f
```

**部署前端：**
```bash
# 1. 本地构建
cd frontend
pnpm install
pnpm build:mobile
pnpm build:desktop

# 2. 上传到服务器
scp -r packages/mobile/dist user@server:/var/www/qianku-mobile/
scp -r packages/desktop/dist user@server:/var/www/qianku-desktop/

# 3. Nginx 配置 (参考下方 Nginx 配置节)
```

### 7.3 Nginx 生产配置

```nginx
# /etc/nginx/conf.d/qianku.conf

# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name your-domain.com m.your-domain.com;
    return 301 https://$host$request_uri;
}

# PC 端
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    
    # 安全头
    add_header X-Frame-Options SAMEORIGIN;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # PC 端静态文件
    root /var/www/qianku-desktop;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 文件上传大小限制
        client_max_body_size 20m;
    }
    
    # 静态资源缓存
    location /assets/ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}

# H5 移动端
server {
    listen 443 ssl http2;
    server_name m.your-domain.com;
    
    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    
    root /var/www/qianku-mobile;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 20m;
    }
    
    location /assets/ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 7.4 SSL 证书申请（免费）

```bash
# 安装 Certbot
sudo apt install -y certbot python3-certbot-nginx

# 申请证书 (自动配置 Nginx)
sudo certbot --nginx -d your-domain.com -d m.your-domain.com

# 自动续期 (crontab)
echo "0 3 * * * certbot renew --quiet" | sudo crontab -
```

---

## 八、微信小程序发布

### 8.1 准备工作

1. 注册 [微信小程序账号](https://mp.weixin.qq.com/)
2. 获取 AppID 和 AppSecret
3. 在小程序管理后台配置业务域名：`m.your-domain.com`

### 8.2 配置小程序

```javascript
// frontend/packages/miniprogram/utils/config.js
module.exports = {
  H5_BASE_URL: 'https://m.your-domain.com',  // 生产环境H5域名
  API_BASE_URL: 'https://your-domain.com',
  VERSION: '1.0.0'
}
```

```json
// frontend/packages/miniprogram/project.config.json
{
  "appid": "wx你的真实AppID",
  ...
}
```

### 8.3 上传发布

1. 用微信开发者工具打开 `frontend/packages/miniprogram`
2. 点击「上传」
3. 填写版本号和备注
4. 在小程序管理后台提交审核
5. 审核通过后发布

---

## 九、运维与监控

### 9.1 日志管理

```bash
# Docker 日志
docker compose logs -f --tail=100 backend

# 后端日志级别配置 (application-prod.yml)
logging:
  level:
    com.qiankubx: INFO
    org.springframework.web: WARN
  file:
    name: /var/log/qianku/app.log
    max-size: 100MB
    max-history: 30
```

### 9.2 数据库备份

```bash
# 手动备份
docker compose exec mysql mysqldump -uroot -pqianku2024 qianku > backup_$(date +%Y%m%d).sql

# 自动备份 (crontab)
echo "0 2 * * * docker compose -f /path/to/docker-compose.yml exec -T mysql mysqldump -uroot -pqianku2024 qianku > /backup/qianku_\$(date +\%Y\%m\%d).sql" | sudo crontab -
```

### 9.3 服务健康检查

```bash
# 后端健康
curl -s http://localhost:8080/api/v1/member/plans | jq '.code'
# 应返回 200

# MySQL
docker compose exec mysql mysqladmin ping -uroot -pqianku2024

# Redis
docker compose exec redis redis-cli ping
# 应返回 PONG

# RabbitMQ
curl -u guest:guest http://localhost:15672/api/overview | jq '.node'
```

### 9.4 常用运维命令

```bash
# 重启后端
docker compose restart backend

# 更新代码并重新部署
git pull
docker compose up -d --build backend

# 仅更新前端
docker compose up -d --build mobile desktop

# 查看资源占用
docker stats

# 清理无用镜像
docker image prune -f
```

---

## 十、版本发布流程

### 10.1 发布检查清单

```
□ 代码已合并到 main 分支
□ mvn compile 编译通过
□ pnpm build:mobile 构建通过
□ pnpm build:desktop 构建通过
□ 数据库变更脚本已准备
□ 环境变量是否需要新增
□ 配置是否需要更新
□ 用户协议/隐私政策版本是否需要更新
□ 小程序是否需要提交新版本审核
```

### 10.2 发布步骤

```bash
# 1. 拉取最新代码
cd /opt/qianku
git pull origin main

# 2. 如果有数据库变更
docker compose exec mysql mysql -uroot -pqianku2024 qianku < migration.sql

# 3. 重新构建并部署
docker compose up -d --build

# 4. 验证服务
curl -s http://localhost:8080/api/v1/member/plans | jq '.code'

# 5. 检查日志
docker compose logs -f --tail=50 backend
```

### 10.3 回滚

```bash
# 回滚到上一个版本
git log --oneline -5    # 找到上一个 commit
git checkout <commit-hash>
docker compose up -d --build

# 如果有数据库回滚脚本
docker compose exec mysql mysql -uroot -pqianku2024 qianku < rollback.sql
```

---

## 十一、常见问题排查

### Q: 后端启动失败 "Could not create connection to database"
```bash
# 检查 MySQL 是否正常运行
docker compose ps mysql
docker compose logs mysql

# 检查连接参数
docker compose exec mysql mysql -uroot -pqianku2024 -e "SELECT 1"
```

### Q: 前端构建失败 "pnpm install 报错"
```bash
# 清除缓存重装
rm -rf node_modules packages/*/node_modules pnpm-lock.yaml
pnpm install
```

### Q: 上传发票报 "文件大小不能超过10MB"
在 Nginx 配置中增加 `client_max_body_size 20m;`

### Q: 阿里云 OSS 上传报错
检查 OSS_ACCESS_KEY_ID、OSS_ACCESS_KEY_SECRET、OSS_BUCKET_NAME 环境变量是否正确。
确认 Bucket 的地域与 OSS_ENDPOINT 一致。

### Q: 百度 OCR 不可用
检查 BAIDU_OCR_API_KEY 和 BAIDU_OCR_SECRET_KEY 是否配置。
未配置时图片发票会提示"暂不支持自动解析"，不影响 PDF 发票。

### Q: RabbitMQ 连接失败
```bash
# 检查 RabbitMQ 状态
docker compose exec rabbitmq rabbitmqctl status

# 检查管理界面
curl http://localhost:15672  # guest/guest
```

---

## 十二、性能优化建议

### 12.1 后端优化
- JVM 参数调优：`-Xms1g -Xmx2g -XX:+UseG1GC`
- 数据库连接池：HikariCP max-pool-size 建议 CPU 核数 × 2
- Redis 缓存热点数据（用户信息、会员状态）
- 大量埋点数据走 RabbitMQ 异步写入

### 12.2 前端优化
- 启用 CDN 加速静态资源
- 开启 Gzip/Brotli 压缩
- 图片懒加载
- 路由懒加载（已配置）

### 12.3 数据库优化
- 埋点表按月分表
- 大表加合适索引
- 慢查询日志分析
- 定期清理历史数据

---

## 十三、安全加固清单

```
□ 修改所有默认密码 (MySQL/Redis/RabbitMQ/JWT)
□ 配置 HTTPS (SSL 证书)
□ 配置防火墙 (仅开放 80/443 端口)
□ MySQL 限制远程访问
□ Redis 设置密码
□ 关闭 Swagger (生产环境)
□ 配置 Nginx 安全头 (已在配置中)
□ 定期备份数据库
□ 监控异常告警
□ OSS Bucket 设为私有 + STS 临时授权
```
