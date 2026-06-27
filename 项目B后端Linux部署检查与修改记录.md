# 项目B后端Linux部署检查与修改记录

检查日期：2026-06-27

## 结论

本项目后端可以迁移到 Linux 服务器运行，但正式部署前需要准备 Linux 启动方式、运行目录权限、环境变量和健康检查。已完成必要代码与部署资产完善，不改变原有上传、CLIP特征提取、队列收集、项目A缓存刷新触发流程。

## 已修改内容

1. 增加 Linux 启动脚本：`upload-server/start-server.sh`
   - 默认使用 `prod` profile。
   - 自动创建上传目录、配色目录、日志目录。
   - 支持 `JAVA_HOME`、`JAVA_OPTS`、`UPLOAD_SERVER_JAR` 和现有生产环境变量。
   - 默认 JVM 参数：`-Xms512m -Xmx2g -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai`。

2. 增加 systemd 部署模板：
   - `upload-server/deploy/upload-server.service`
   - `upload-server/deploy/mls-upload-server.env.example`

3. 调整后端文件路径兜底：
   - `FileService` 的默认花型图路径改为 `/data/MLS_Pic/mls_pic`。
   - 默认配色图路径改为 `/data/MLS_Pic/mls_pic/color_scheme`。
   - 避免 Linux 环境配置缺失时落到 Windows `D:\...` 路径。

4. 放行健康检查端点：
   - `/api/upload/health`
   - `/api/actuator/health`
   - `/api/actuator/info`
   - 便于 systemd、Nginx、负载均衡、监控平台探活。

5. 完善 CLIP 特征删除一致性：
   - `DataService.deleteFeatureVector` 现在会同时删除 `image_feature_clip` 和旧 `data_vector`。
   - 避免删除图片后 CLIP 特征残留。

## Linux服务器必须确认

1. Java版本
   - 推荐 JDK 8。
   - Spring Boot 2.1.18 可运行在 JDK 8；不要用缺失兼容性的过高版本直接上线。

2. MySQL连通性
   - 默认数据库：`pic_system_mls`
   - 默认连接：`127.0.0.1:3306`
   - 需要确认表 `image_feature_clip` 已存在，且账号有 `SELECT/INSERT/UPDATE/DELETE` 权限。

3. 文件目录权限
   - `/data/MLS_Pic/mls_pic`
   - `/data/MLS_Pic/mls_pic/color_scheme`
   - `/data/logs/upload-server`
   - systemd 中的运行用户，例如 `mls`，必须对这些目录有读写权限。

4. CLIP Docker服务
   - 后端服务器必须能访问 `http://192.168.30.114:8000/health`。
   - 特征提取接口为 `http://192.168.30.114:8000/api/extract`。
   - 健康检查应返回模型已加载且 `feature_dim=768`。

5. 项目A缓存刷新地址
   - 配置项 `PROJECT_A_BASE_URL` 不能带 `/api`。
   - 如果项目A和项目B在同一台 Linux 服务器，通常配置为：`http://127.0.0.1:8080`
   - 项目B内部会拼接 `/api/cache/update/...`。

6. Linux大小写敏感
   - Linux 文件名大小写敏感，`ABC.jpg` 与 `abc.jpg` 是两个不同文件。
   - 前端、Excel商品编号、项目A查询逻辑需要保持文件名生成规则一致。

## 推荐部署方式

打包：

```bash
mvn -pl upload-server -am -DskipTests package
```

部署目录示例：

```bash
/opt/mls/upload-server/
├── upload-server.jar
├── start-server.sh
└── deploy/
```

环境变量文件：

```bash
sudo cp upload-server/deploy/mls-upload-server.env.example /etc/mls-upload-server.env
sudo vim /etc/mls-upload-server.env
```

systemd：

```bash
sudo cp upload-server/deploy/upload-server.service /etc/systemd/system/upload-server.service
sudo systemctl daemon-reload
sudo systemctl enable upload-server
sudo systemctl start upload-server
```

探活：

```bash
curl http://127.0.0.1:8081/api/actuator/health
curl http://127.0.0.1:8081/api/upload/health
```

## 验证记录

已执行：

```bash
bash -n upload-server/start-server.sh
mvn -pl upload-server -am -DskipTests compile
```

结果：

- Shell脚本语法检查通过。
- 后端编译通过。

未执行：

- 未连接正式 MySQL。
- 未连接正式 CLIP Docker。
- 未调用项目A正式缓存刷新接口。
