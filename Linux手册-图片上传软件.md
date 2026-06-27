# Linux手册-图片上传软件

适用项目：项目B `upload_3.0_server_1` 图片上传软件，以及同服务器上的项目A `mls_pic_manage_`

服务器：`192.168.30.114`

系统形态：

- 项目A：图片管理/检索系统，Linux服务器运行，端口 `8080`，上下文路径 `/api`
- 项目B后端：图片上传服务，Linux服务器运行，端口 `8081`，上下文路径 `/api`
- CLIP Docker：同服务器运行，端口 `8000`
- MySQL：同服务器或服务器本机访问，数据库 `pic_system_mls`
- 图片目录：`/data/MLS_Pic/mls_pic`
- Windows客户端：项目B前端打包为 `upload-client.exe`，分发给 Windows 用户使用

## 一、部署目标

迁移完成后，Windows 用户运行 `upload-client.exe` 上传图片到：

```text
http://192.168.30.114:8081/api/upload/...
```

项目B后端完成：

1. 接收 Windows 客户端上传请求。
2. 保存花型图到 `/data/MLS_Pic/mls_pic`。
3. 保存配色图到 `/data/MLS_Pic/mls_pic/color_scheme`。
4. 对花型图调用 CLIP Docker：

```text
http://192.168.30.114:8000/api/extract
```

5. 将 CLIP 特征写入 `image_feature_clip`。
6. 写入成功后调用项目A缓存刷新接口：

```text
http://127.0.0.1:8080/api/cache/update/...
```

注意：项目A和项目B在同一台服务器，因此项目B调用项目A建议使用 `127.0.0.1:8080`，不需要走公网或局域网IP。

## 二、端口规划

| 服务 | 端口 | 访问方式 | 说明 |
| --- | --- | --- | --- |
| 项目A | `8080` | 服务器本机、浏览器或反代 | 图片管理系统 |
| 项目B后端 | `8081` | Windows客户端访问 | 上传服务 |
| CLIP Docker | `8000` | 项目A/项目B访问 | CLIP特征提取 |
| MySQL | `3306` | 本机访问优先 | 数据库 |

对外最少开放：

- `8081`：Windows 上传客户端必须能访问。
- `8080`：如果项目A管理页面需要浏览器访问，则开放。
- `8000`：建议只允许服务器本机或内网访问，不建议公网开放。
- `3306`：不建议公网开放。

## 三、Linux服务器基础准备

以下命令以常见 Linux 发行版为例，按实际系统调整包管理命令。

### 1. 创建运行用户

```bash
sudo useradd -r -m -s /sbin/nologin mls
```

如果系统已经存在 `mls` 用户，可以跳过。

### 2. 安装 JDK 8

项目B基于 JDK 8 开发，生产环境建议使用 JDK 8。

检查：

```bash
java -version
```

预期看到 `1.8.0_xxx`。

如果一台机器同时有多个 Java 版本，要在 systemd 环境变量中明确 `JAVA_HOME`。

### 3. 创建目录

```bash
sudo mkdir -p /opt/mls/upload-server
sudo mkdir -p /opt/mls/mls-pic-manage
sudo mkdir -p /data/MLS_Pic/mls_pic/color_scheme
sudo mkdir -p /data/logs/upload-server
sudo mkdir -p /data/logs/mls-pic
sudo chown -R mls:mls /opt/mls /data/MLS_Pic /data/logs
```

目录说明：

| 目录 | 用途 |
| --- | --- |
| `/opt/mls/upload-server` | 项目B后端部署目录 |
| `/opt/mls/mls-pic-manage` | 项目A部署目录 |
| `/data/MLS_Pic/mls_pic` | 花型图主目录 |
| `/data/MLS_Pic/mls_pic/color_scheme` | 配色图目录 |
| `/data/logs/upload-server` | 项目B日志 |
| `/data/logs/mls-pic` | 项目A日志 |

### 4. 防火墙

如果使用 `firewalld`：

```bash
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

如果项目A只给本机或内网访问，可以不对公网开放 `8080`。

## 四、数据库准备

项目A和项目B使用同一个库：

```text
pic_system_mls
```

推荐项目B连接本机 MySQL：

```text
jdbc:mysql://127.0.0.1:3306/pic_system_mls?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
```

正式部署前确认：

```sql
SHOW DATABASES LIKE 'pic_system_mls';
USE pic_system_mls;
SHOW TABLES LIKE 'image_feature_clip';
SHOW TABLES LIKE 'pic_info';
SHOW TABLES LIKE 'image_upload';
```

`image_feature_clip` 必须存在。CLIP特征字段要求：

- `filename` 唯一
- `clip_feature` 保存 768 维 float32 小端序 BLOB，长度应为 `3072` 字节
- `feature_dim` 为 `768`
- `model_name` 为 `clip-vit-large-patch14`
- `rotation_augment` 为 `1`

数据库账号至少需要：

```sql
SELECT, INSERT, UPDATE, DELETE
```

建议不要让项目服务使用 MySQL root 账号。

## 五、确认 CLIP Docker 服务

项目A和项目B都会调用同一套 CLIP Docker：

```text
http://192.168.30.114:8000
```

健康检查：

```bash
curl http://192.168.30.114:8000/health
```

预期重点字段：

```json
{
  "status": "ok",
  "model_loaded": true,
  "feature_dim": 768
}
```

如果该接口不可访问，项目B上传图片可以保存文件，但 CLIP 特征提取会失败，项目A以图搜图缓存也不会得到新特征。

## 六、部署项目A

项目A已经适配 CLIP，正式配置中关键项应保持：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/pic_system_mls?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true

pic:
  storage:
    base-path: /data/MLS_Pic/mls_pic
    color-scheme-path: /data/MLS_Pic/mls_pic/color_scheme
  image-search:
    algorithm: clip
    clip-cache:
      auto-initialize: true

docker:
  clip-image-search:
    url: http://192.168.30.114:8000
    extract-path: /api/extract
    timeout: 120
    rotation-augment: true
```

项目A启动后检查：

```bash
curl http://127.0.0.1:8080/api/actuator/health
```

如果项目A没有开放 actuator，可改用项目A已有健康检查接口或查看日志：

```bash
tail -f /data/logs/mls-pic/application.log
```

重点确认：

1. 项目A连接的是 `pic_system_mls`。
2. 项目A图片根目录是 `/data/MLS_Pic/mls_pic`。
3. 项目A以图搜图算法是 `clip`。
4. 项目A启动后能加载 `image_feature_clip` 缓存。
5. 项目A的缓存刷新接口 `/api/cache/update/...` 能正确刷新 CLIP 缓存。

## 七、打包项目B后端

在开发机或服务器上打包均可。服务器打包需要 Maven 和 JDK 8。

在项目B根目录执行：

```bash
mvn -pl upload-server -am -DskipTests package
```

生成文件：

```text
upload-server/target/upload-server.jar
```

如果只想先确认编译：

```bash
mvn -pl upload-server -am -DskipTests compile
```

## 八、部署项目B后端到 Linux

### 1. 上传文件到服务器

将以下文件复制到服务器：

```text
upload-server/target/upload-server.jar
upload-server/start-server.sh
upload-server/deploy/upload-server.service
upload-server/deploy/mls-upload-server.env.example
```

部署到：

```text
/opt/mls/upload-server
```

示例：

```bash
sudo cp upload-server.jar /opt/mls/upload-server/
sudo cp start-server.sh /opt/mls/upload-server/
sudo cp -r deploy /opt/mls/upload-server/
sudo chown -R mls:mls /opt/mls/upload-server
sudo chmod +x /opt/mls/upload-server/start-server.sh
```

### 2. 创建项目B环境变量文件

```bash
sudo cp /opt/mls/upload-server/deploy/mls-upload-server.env.example /etc/mls-upload-server.env
sudo vim /etc/mls-upload-server.env
```

推荐正式内容：

```properties
SPRING_PROFILES_ACTIVE=prod

UPLOAD_DB_URL=jdbc:mysql://127.0.0.1:3306/pic_system_mls?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
UPLOAD_DB_USERNAME=mls01
UPLOAD_DB_PASSWORD=你的数据库密码

UPLOAD_FLOWER_PATH=/data/MLS_Pic/mls_pic
UPLOAD_COLOR_PATH=/data/MLS_Pic/mls_pic/color_scheme
UPLOAD_LOG_FILE=/data/logs/upload-server/application.log

CLIP_DOCKER_EXTRACT_URL=http://192.168.30.114:8000/api/extract
CLIP_DOCKER_HEALTH_URL=http://192.168.30.114:8000/health
CLIP_DOCKER_TIMEOUT=120000
CLIP_DOCKER_CONNECT_TIMEOUT=30000
CLIP_DOCKER_READ_TIMEOUT=120000

PROJECT_A_BASE_URL=http://127.0.0.1:8080

JAVA_OPTS=-Xms512m -Xmx2g -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai
```

重要：`PROJECT_A_BASE_URL` 不能写成 `http://127.0.0.1:8080/api`。项目B代码会自动拼接 `/api/cache/update/...`。

### 3. 安装 systemd 服务

```bash
sudo cp /opt/mls/upload-server/deploy/upload-server.service /etc/systemd/system/upload-server.service
sudo systemctl daemon-reload
sudo systemctl enable upload-server
sudo systemctl start upload-server
```

查看状态：

```bash
sudo systemctl status upload-server
```

查看日志：

```bash
journalctl -u upload-server -f
tail -f /data/logs/upload-server/application.log
```

### 4. 项目B健康检查

本机检查：

```bash
curl http://127.0.0.1:8081/api/actuator/health
curl http://127.0.0.1:8081/api/upload/health
```

Windows客户端访问检查：

```bash
curl http://192.168.30.114:8081/api/upload/health
```

预期返回成功结果。

## 九、项目B与项目A联动检查

项目B上传花型图后的链路是：

```text
Windows客户端
  -> 项目B /api/upload/single 或 /api/upload/batch
  -> 保存图片到 /data/MLS_Pic/mls_pic
  -> 调用 CLIP Docker /api/extract
  -> 写入 image_feature_clip
  -> 调用项目A /api/cache/update/...
  -> 项目A刷新内存缓存
```

上线前至少检查这些点：

```bash
curl http://127.0.0.1:8080/api/cache/update/single?filename=test.jpg
```

如果没有 `test.jpg`，该接口可能返回业务失败，但不能是连接失败、404 或 500。

检查项目B能访问项目A：

```bash
curl http://127.0.0.1:8080/api/
```

检查项目B能访问 CLIP：

```bash
curl http://192.168.30.114:8000/health
```

检查项目B日志中是否有：

```text
CLIP特征向量保存成功
缓存刷新
```

## 十、Windows客户端EXE打包与分发

前端不部署到 Linux。前端打包成 Windows 可运行的 `upload-client.exe`，发给 Windows 用户。

### 1. 打包环境

建议在 Windows 机器打包：

- JDK 8
- Maven
- 项目B完整源码

原因：客户端是 JavaFX 项目，JDK 8 自带 JavaFX；当前如果使用较高版本 JDK，可能出现 `javafx.*` 包缺失。

### 2. 客户端配置

客户端配置文件：

```text
upload-client/src/main/resources/application.properties
```

生产配置必须是：

```properties
server.url=http://192.168.30.114:8081
upload.feature.extraction.enabled=true
```

注意：`server.url` 不要带 `/api`，客户端代码会自动拼接 `/api/upload/...`。

### 3. 打包命令

在项目B根目录执行：

```bat
mvn -pl upload-client -am -DskipTests package
```

生成：

```text
upload-client\target\upload-client.exe
```

如果需要制作分发目录：

```bat
create-installer.bat
```

分发目录中应包含：

```text
client\upload-client.exe
client\start-client.bat
client\application.properties
```

### 4. Windows用户分发要求

发给用户前确认 `client\application.properties`：

```properties
server.url=http://192.168.30.114:8081
upload.feature.extraction.enabled=true
```

如果以后服务器地址变化，只需要修改 exe 同目录下的 `application.properties`，不一定重新打包 exe。

### 5. Windows客户端连通性检查

在 Windows 用户电脑浏览器打开：

```text
http://192.168.30.114:8081/api/upload/health
```

能访问后再运行 `upload-client.exe`。

如果访问失败，优先检查：

1. Windows 电脑是否能 ping 通 `192.168.30.114`。
2. Linux 防火墙是否开放 `8081`。
3. 项目B后端是否启动。
4. 客户端 `server.url` 是否误写成带 `/api`。

## 十一、推荐上线顺序

1. 备份数据库。
2. 备份 `/data/MLS_Pic/mls_pic` 图片目录。
3. 确认 CLIP Docker 服务正常。
4. 启动或重启项目A。
5. 确认项目A使用 `clip` 算法。
6. 部署项目B后端。
7. 确认项目B健康检查正常。
8. 用 Windows 客户端上传一张花型图。
9. 检查图片是否保存到 `/data/MLS_Pic/mls_pic`。
10. 检查 `image_feature_clip` 是否新增记录。
11. 检查项目A缓存刷新日志。
12. 在项目A做一次以图搜图验证。
13. 分发正式 Windows EXE 给用户。

## 十二、常用运维命令

项目B：

```bash
sudo systemctl start upload-server
sudo systemctl stop upload-server
sudo systemctl restart upload-server
sudo systemctl status upload-server
journalctl -u upload-server -f
tail -f /data/logs/upload-server/application.log
```

项目A：

```bash
sudo systemctl status mls-pic-manage
tail -f /data/logs/mls-pic/application.log
```

端口：

```bash
ss -lntp | grep -E '8080|8081|8000|3306'
```

磁盘：

```bash
df -h
du -sh /data/MLS_Pic/mls_pic
```

权限：

```bash
ls -ld /data/MLS_Pic/mls_pic
ls -ld /data/MLS_Pic/mls_pic/color_scheme
ls -ld /data/logs/upload-server
```

数据库抽查：

```sql
USE pic_system_mls;
SELECT COUNT(*) FROM image_feature_clip;
SELECT filename, feature_dim, model_name, rotation_augment, LENGTH(clip_feature), update_time
FROM image_feature_clip
ORDER BY update_time DESC
LIMIT 10;
```

## 十三、故障排查

### 1. Windows客户端上传失败

检查：

```bash
curl http://192.168.30.114:8081/api/upload/health
sudo systemctl status upload-server
journalctl -u upload-server -n 200
```

常见原因：

- `8081` 未开放。
- `upload-server` 没启动。
- 客户端 `server.url` 配置错误。
- 上传文件超过 `50MB`。

### 2. 图片保存成功但没有CLIP特征

检查：

```bash
curl http://192.168.30.114:8000/health
tail -f /data/logs/upload-server/application.log
```

数据库检查：

```sql
SELECT filename, feature_dim, LENGTH(clip_feature), update_time
FROM image_feature_clip
WHERE filename = '实际文件名.jpg';
```

常见原因：

- CLIP Docker 不可用。
- `upload.feature.extraction.enabled=false`。
- 上传的是配色图，配色图不会触发特征提取。
- 文件名与数据库记录大小写不一致。

### 3. CLIP特征已入库但项目A搜不到

检查项目B是否调用项目A缓存刷新：

```bash
grep -i "缓存刷新" /data/logs/upload-server/application.log
```

检查项目A日志：

```bash
tail -f /data/logs/mls-pic/application.log
```

重点确认：

- `PROJECT_A_BASE_URL=http://127.0.0.1:8080`
- 项目A当前算法为 `clip`
- 项目A缓存刷新接口已经适配 `image_feature_clip`

### 4. Linux路径或权限错误

检查：

```bash
sudo -u mls test -w /data/MLS_Pic/mls_pic && echo OK
sudo -u mls test -w /data/MLS_Pic/mls_pic/color_scheme && echo OK
sudo -u mls test -w /data/logs/upload-server && echo OK
```

如果失败：

```bash
sudo chown -R mls:mls /data/MLS_Pic /data/logs/upload-server
```

### 5. 大小写问题

Linux 文件系统大小写敏感：

```text
ABC.jpg
abc.jpg
```

这是两个不同文件。Excel商品编号、上传文件名、项目A查询文件名必须保持一致。

## 十四、回滚方案

### 1. 回滚项目B后端

部署前保留旧 jar：

```bash
sudo cp /opt/mls/upload-server/upload-server.jar /opt/mls/upload-server/upload-server.jar.bak
```

回滚：

```bash
sudo systemctl stop upload-server
sudo cp /opt/mls/upload-server/upload-server.jar.bak /opt/mls/upload-server/upload-server.jar
sudo systemctl start upload-server
```

### 2. 回滚客户端

保留上一版 Windows 分发目录。如果新版客户端异常，直接让用户换回旧版 `upload-client.exe` 和旧版 `application.properties`。

### 3. 数据库回滚

上线前必须备份：

```bash
mysqldump -u 用户名 -p pic_system_mls > pic_system_mls_before_upload_server_deploy.sql
```

如需恢复，按数据库管理员规范执行。不要在未确认影响范围时直接覆盖正式库。

## 十五、最终上线检查清单

- [ ] JDK 8 已安装。
- [ ] MySQL `pic_system_mls` 可连接。
- [ ] `image_feature_clip` 表存在。
- [ ] `/data/MLS_Pic/mls_pic` 可写。
- [ ] `/data/MLS_Pic/mls_pic/color_scheme` 可写。
- [ ] CLIP Docker `http://192.168.30.114:8000/health` 正常。
- [ ] 项目A `8080` 正常。
- [ ] 项目A算法配置为 `clip`。
- [ ] 项目B `8081` 正常。
- [ ] 项目B `PROJECT_A_BASE_URL=http://127.0.0.1:8080`。
- [ ] Windows客户端 `server.url=http://192.168.30.114:8081`。
- [ ] Windows客户端 `upload.feature.extraction.enabled=true`。
- [ ] 上传花型图后 `image_feature_clip` 新增记录。
- [ ] 项目A缓存刷新成功。
- [ ] 项目A以图搜图可以搜到新上传图片。
