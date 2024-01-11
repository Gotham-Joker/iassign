FROM nginx
RUN rm -rf /usr/share/nginx/html/*
# 把打包好的前端文件复制进来
COPY ./iassign-ui/browser/ /usr/share/nginx/html
ADD entrypoint.sh /
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]