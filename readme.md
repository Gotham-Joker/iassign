# IASSIGN 低代码流程审批系统
## 1.如何使用？
项目需要gradle6.9.x以上版本，使用JDK17编译。

## 2.如何搭建开发环境
项目导入idea，修改application-dev.yml文件，并将[ddl.sql](ddl.sql)文件放到你自己的mysql中初始化一遍。

## 3. 如何快速体验
### 3.1 docker环境
如果你有docker环境，外加docker-compose，那么就能很方便的部署，快速体验。
(这里假设你已经知道如何使用docker和docker-compose，并将docker-compose整合到IDEA中。)
请按照以下方式快速构建应用：
1. `gradle build`构建之后得到[docker/app/iassign.jar](docker/app/iassign.jar)
2. 然后去前端项目执行`npm install && npm run build`，得到dist/iassign-ui文件夹
，将编译得到的iassign-ui文件夹复制到后端项目的[docker/nginx](docker/nginx)目录下
3. 打开[compose.yml](compose.yaml)文件，将配置文件里面的`172.16.145.130`替换成你自己虚拟机的ip地址，
如果你已经整合docker插件到idea，此时idea会显示启动按钮，先启动mysql和es，然后在mysql中初始化[ddl.sql](ddl.sql)，
最后将其他所有项目启动起来。(ES首次启动时间可能较久，需要等待一会儿再启动后端项目)