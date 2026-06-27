# 项目B上传服务CLIP特征提取适配执行计划

## 1. 目标

项目B `upload_3.0_server_1` 当前上传图片后，会调用旧Docker特征提取接口，解析 `color/glcm/lbp/vgg/vit` 五组向量，并写入旧表 `data_vector`。项目A已经切换为CLIP检索，正式配置中使用 `image_feature_clip` 表、768维CLIP向量和CLIP缓存。因此项目B需要在不破坏原上传流程、异步队列、批量缓存刷新触发机制的前提下，将特征提取链路适配为：

1. 调用CLIP Docker接口 `POST /api/extract`。
2. 传入 `Pic` 和 `rotation_augment=true`。
3. 校验响应 `code=0`、`data.feature` 存在、特征维度为768。
4. 将768个 `float32` 按小端序编码为3072字节BLOB。
5. 使用文件名写入 `image_feature_clip`，存在则覆盖更新。
6. 仅在CLIP特征成功入库后，继续收集文件名并触发项目A缓存刷新。

## 2. 现有链路不变式

必须保持以下行为不变：

1. 上传接口不改，`FileUploadController` 仍然在图片保存成功后调用 `DataService.requestFeatureExtraction(filename, imagePath)`。
2. `DataService.requestFeatureExtraction` 仍负责去重、覆盖策略、Docker可用性判断和任务提交。
3. Docker请求仍通过单线程串行执行器提交，避免同时向特征服务压入大量并发请求。
4. 缓存刷新仍只在数据库保存成功后触发。
5. 队列清空后仍调用 `cacheRefreshService.triggerBatchRefreshIfReady()`，保留原批量刷新时机。
6. `ApiSelector` 当前拼接的项目A接口路径仍保持 `/api/cache/update/single|batch|large-batch|large-batch-async`。

## 3. 配置调整计划

### 3.1 项目B服务端配置

将 `app.feature.extraction.docker` 默认配置调整为CLIP Docker：

- `url`: `http://192.168.30.114:8000/api/extract`
- `health-check-url`: `http://192.168.30.114:8000/health`
- `timeout/read-timeout`: 建议120秒
- `rotation-augment`: `true`
- `feature-dim`: `768`
- `model-name`: `clip-vit-large-patch14`
- `overwrite-existing`: 继续保留，由上传软件决定是否覆盖已存在CLIP特征

配置默认值允许通过环境变量覆盖，方便正式服、测试机和本机之间切换。

### 3.2 项目A相关配置

项目A正式配置显示：

- 服务端口：`8080`
- context-path：`/api`
- 数据库：`pic_system_mls`
- 图片目录：`/data/MLS_Pic/mls_pic`
- CLIP Docker：`http://192.168.30.114:8000`

因此项目B生产配置应对齐：

- 图片根目录：`/data/MLS_Pic/mls_pic`
- 配色图目录：`/data/MLS_Pic/mls_pic/color_scheme`
- 数据库：`pic_system_mls`
- 项目A缓存刷新基础地址：默认 `http://127.0.0.1:8080`，如项目A不在同机部署，可用环境变量覆盖为实际地址

注意：`cache.refresh.target-url` 不能包含 `/api`，因为 `ApiSelector` 内部已经拼接 `/api/cache/update/...`。

## 4. 代码改造计划

### 4.1 新增CLIP入库模型

新增 `ImageFeatureClip` 实体，对应项目A已使用的新表：

- `id`
- `filename`
- `clipFeature`
- `featureDim`
- `modelName`
- `rotationAugment`
- `clipTime`
- `createTime`
- `updateTime`

新增 `ImageFeatureClipMapper`：

- `insertOrUpdate(ImageFeatureClip entity)`
- `findByFilename(String filename)`
- `countByFilename(String filename)`
- `count()`
- `deleteByFilename(String filename)`
- `findExtractedFilenames()`

入库SQL使用 `INSERT ... ON DUPLICATE KEY UPDATE`，确保重复上传或覆盖时更新同一行。

### 4.2 扩展特征提取配置类

在 `FeatureExtractionProperties` 中新增：

- `rotationAugment`
- `featureDim`
- `modelName`

保留原 `enabled/timeout/retry/healthCheck/overwriteExisting`，避免影响现有配置绑定和监控入口。

### 4.3 改造Docker响应解析

保留 `DockerFeatureExtractionService` 类名和原健康检查入口，新增CLIP结果对象 `ClipFeatureVector`，并让实际保存流程使用CLIP对象。

响应校验规则：

1. HTTP状态必须为200。
2. JSON顶层 `code` 必须为0。
3. `data.feature` 必须存在且为数组。
4. 数组长度必须等于配置 `featureDim`，默认768。
5. 数组元素必须为数值类型。
6. BLOB长度必须等于 `featureDim * 4`，默认3072。

### 4.4 修改保存流程

`DockerFeatureExtractionServiceImpl.extractAndSaveFeatures` 从旧逻辑：

1. 提取五组向量。
2. 写入 `data_vector`。
3. 成功后触发缓存刷新。

改为新逻辑：

1. 提取CLIP向量。
2. 编码为小端序BLOB。
3. 写入 `image_feature_clip`。
4. 成功后触发缓存刷新。

原 `saveFeatureVector` 和旧 `data_vector` 查询接口暂保留，降低对已有管理接口和旧测试代码的破坏范围。

### 4.5 修改去重判断

`DataService.requestFeatureExtraction` 的“是否已存在特征”判断切换为 `image_feature_clip.filename`。这样同名图片是否重提取，由新CLIP表决定，而不是旧 `data_vector` 表决定。

## 5. 项目A风险与处理建议

已检查到本地项目A存在一个关键风险：现有 `/api/cache/update/...` 控制器可能仍刷新旧 `data_vector` 缓存，而不是 `image_feature_clip` 的CLIP缓存。

本次任务只修改项目B，不直接修改项目A。项目B会继续按照原触发方式调用项目A缓存刷新接口，保证上传软件侧行为不变。正式部署前需要确认项目A正式代码满足以下任一条件：

1. 现有 `/api/cache/update/...` 在 `pic.image-search.algorithm=clip` 时刷新 `ClipFeatureCacheManager`。
2. 或项目A提供新的CLIP缓存刷新接口，并将项目B `ApiSelector` 改到新接口。

如果项目A没有做这一步，项目B即使成功写入 `image_feature_clip`，项目A内存CLIP缓存也可能不会立即看到新图片，直到项目A重启或全量重建缓存。

## 6. 暂不执行的验证

由于当前电脑无法连接正式服务器，本次不执行需要正式服务的验证：

- 不调用 `192.168.30.114:8000/health`
- 不调用CLIP Docker `/api/extract`
- 不连接正式MySQL
- 不调用项目A正式缓存刷新接口

本次只做本地静态代码检查和必要的编译级检查；如本机Maven依赖不可用或用户要求完全不测试，则记录未验证项。

## 7. 执行结果

- [x] 新增计划文档
- [x] 新增 `ImageFeatureClip` 实体
- [x] 新增 `ImageFeatureClipMapper`
- [x] 扩展 `FeatureExtractionProperties`
- [x] 扩展 `DockerFeatureExtractionService` CLIP结果对象和方法
- [x] 改造 `DockerFeatureExtractionServiceImpl` 为CLIP调用、解析、入库
- [x] 修改 `DataService` 使用 `image_feature_clip` 做去重和保存
- [x] 更新 `application.yml`、`application-dev.yml`、`application-prod.yml`
- [x] 复核缓存刷新触发链路未改变
- [x] 记录未执行的正式服验证项

## 8. 本地检查结果

已执行本地编译级检查：

```bash
mvn -pl upload-server -am -DskipTests compile
```

结果：`BUILD SUCCESS`。

说明：

1. `-DskipTests` 跳过了测试执行。
2. 本次没有连接正式MySQL。
3. 本次没有调用CLIP Docker正式服务。
4. 本次没有调用项目A正式缓存刷新接口。
