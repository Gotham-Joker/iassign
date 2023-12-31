# 也可以换成其他jdk17的docker镜像来构建
FROM joengenduvel/jre17
MAINTAINER HONGTAO
ADD iassign.jar /app/
ADD entrypoint.sh /app/
RUN chmod +x /app/entrypoint.sh
ENV APP_HOME="/app"
WORKDIR "/app"
ENTRYPOINT ["/app/entrypoint.sh"]