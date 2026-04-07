# Forge Admin Nginx 配置示例

将此配置添加到你的 Nginx 配置文件中（如 `/etc/nginx/conf.d/forge-admin.conf` 或宝塔面板的站点配置）。

## Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;  # 替换为你的域名或IP

    # 前端静态资源
    location /forge/ {
        alias   /www/wwwroot/html/dist/;
        index  index.html index.htm;
        try_files $uri $uri/ /forge/index.html;
    }

    # 流程服务 API（如部署了 forge-flow 服务）
    location /forge-api/api/flow/ {
        proxy_send_timeout 3000;
        proxy_read_timeout 3000;
        proxy_connect_timeout 3000;
        proxy_pass http://127.0.0.1:8081/api/flow/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 后端主服务 API
    location /forge-api/ {
        proxy_send_timeout 3000;
        proxy_read_timeout 3000;
        proxy_connect_timeout 3000;
        proxy_pass http://127.0.0.1:8580/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 文件上传大小限制
    client_max_body_size 20m;

    # Gzip 压缩（可选）
    gzip on;
    gzip_types text/plain application/json application/javascript text/css;
    gzip_min_length 1000;
}
```

## 注意事项

### 1. 宝塔面板部署注意事项

- 宝塔默认有静态资源缓存规则（`location ~ .*\.(js|css)?$`），会覆盖 `alias` 配置
- 需要注释掉或删除这些默认规则，否则 assets 目录下的 js/css 文件会 404
- 配置文件路径通常为：`/www/server/panel/vhost/nginx/xxx.conf`

### 2. alias 配置要点

- `location /forge/` 和 `alias /xxx/dist/;` 结尾都要加斜杠
- 不加斜杠会导致路径拼接错误（如 `/xxx/distassets/`）

### 3. 前端构建配置

`.env.test` 或 `.env.production` 文件配置：

```env
VITE_PUBLIC_PATH=/forge
VITE_BASE_URL=/forge
VITE_REQUEST_PREFIX=/forge-api
```

### 4. 后端端口说明

| 服务 | 端口 | 说明 |
|------|------|------|
| forge-admin | 8580 | 主服务 |
| forge-flow | 8081 | 流程服务（可选） |

### 5. 部署目录结构

```
/www/wwwroot/html/
└── dist/              # 前端构建产物
    ├── index.html
    ├── assets/
    │   ├── index-xxx.js
    │   └── index-xxx.css
    └── favicon.png
```