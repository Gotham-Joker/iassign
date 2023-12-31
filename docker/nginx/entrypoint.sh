#!/bin/sh

# 替换掉服务器接口地址
if [ "x$SERVER_URL" != "x" ]; then
    echo "replace default url to SERVER_URL: $SERVER_URL"
    sed -i "s|http://localhost:8080|$SERVER_URL|" /usr/share/nginx/html/main.*.js
fi

/docker-entrypoint.sh nginx -g "daemon off;"