FROM nginx:1.25-alpine
# 复制自定义配置
COPY nginx.conf /etc/nginx/nginx.conf
# 复制前端静态文件
COPY ../frontend /usr/share/nginx/html
EXPOSE 80
