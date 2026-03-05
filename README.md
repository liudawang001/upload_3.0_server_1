# 木林森图片上传系统 (MLS Picture Upload System)

## 项目简介

木林森图片上传系统是一个企业级的分布式图片管理平台，专为处理大规模批量图片上传而设计。系统采用客户端-服务器架构，支持数千张图片的并发上传，并集成了AI图像特征提取功能。

**版本**: 1.0.0
**JDK版本**: 1.8
**构建工具**: Maven

---

## 系统架构

### 模块组成

```
upload_3.0_server_1/
├── upload-client/          # JavaFX桌面客户端
│   ├── 批量文件扫描与上传
│   ├── Excel元数据解析
│   └── 实时进度追踪
└── upload-server/          # Spring Boot REST API服务端
    ├── 文件存储与管理
    ├── 数据库持久化
    └── Docker特征提取集成
```

### 技术栈

#### 后端框架
- **Spring Boot 2.1.18** - 核心框架
- **MyBatis 2.1.4** - ORM持久层
- **MySQL 8.0.28** - 关系型数据库
- **HikariCP** - 高性能连接池

#### 客户端技术
- **JavaFX** - 桌面GUI框架
- **OkHttp 3.14.9** - HTTP客户端
- **Apache POI 4.1.2** - Excel文件处理

#### 其他组件
- **Jackson 2.10.5** - JSON序列化
- **Logback 1.2.3** - 日志框架
- **JWT** - 安全认证

---

## 核心功能

### 1. 双类型图片管理

**花型图片 (Type 1)**
- 存储路径: `D:\MLS_Pic\mls_image_use`
- 命名策略: 保留原始文件名（支持覆盖更新）
- 元数据支持: 关联Excel数据到PicInfo表

**配色图片 (Type 2)**
- 存储路径: `D:\MLS_Pic\mls_image_use\color_scheme`
- 命名策略: 时间戳防冲突 (`filename_timestamp.ext`)
- 用途: 配色方案参考图

### 2. Excel元数据集成

系统支持16个字段的产品元数据同步：

| 字段类别 | 字段名称 |
|---------|---------|
| 产品信息 | 产品代码、品类、订单号、面料类型 |
| 客户信息 | 客户名称、市场、套数 |
| 时间信息 | 下单日期、交期、上传日期 |
| 生产信息 | 工厂、设计公司、设计师 |
| 其他 | 备注、跟单、排序 |

### 3. AI特征提取

集成Docker化的图像分析服务，支持5种特征向量提取：

- **Color** - 颜色直方图特征
- **GLCM** - 灰度共生矩阵纹理特征
- **LBP** - 局部二值模式特征
- **VGG** - VGG深度学习特征
- **ViT** - Vision Transformer特征

特征数据存储在`DataVector`表，支持增量更新和覆盖策略。

---

## 大批量文件上传保障机制

### 一、智能API路由 (ApiSelector)

系统根据文件数量自动选择最优API端点：

| 文件数量 | API端点 | 处理策略 |
|---------|---------|---------|
| 1个 | `/api/cache/update/single` | 单文件同步 |
| 2-499个 | `/api/cache/update/batch` | 批量同步 |
| 500-1999个 | `/api/cache/update/large-batch` | 大批量同步 |
| 2000+个 | `/api/cache/update/large-batch-async` | 超大批量异步 |

**动态批次计算**:
```java
// 根据总文件数自动调整批次大小
int optimalBatchSize = calculateOptimalBatchSize(totalFiles);
// 考虑内存限制和并发约束
int adjustedSize = Math.min(optimalBatchSize, maxBatchSize);
```

### 二、并发处理架构

#### 1. 线程池配置

**客户端上传线程池**:
- 可配置线程数（默认根据CPU核心数）
- 独立任务队列
- 异常隔离机制

**服务端处理线程池**:
```yaml
batch:
  thread-pool-size: 10              # 批处理线程数
  max-concurrent-batches: 3         # 最大并发批次
  enable-parallel-processing: true  # 启用并行处理
```

**Tomcat容器配置**:
```yaml
server:
  tomcat:
    max-threads: 200        # 最大工作线程
    min-spare-threads: 10   # 最小空闲线程
    accept-count: 100       # 等待队列长度
```

#### 2. 队列化批次收集 (CacheRefreshService)

```java
// 线程安全的并发队列
ConcurrentLinkedQueue<FileUpdateRequest> pendingQueue;

// 定时批次处理器
@Scheduled(fixedDelay = 2000)  // 2秒收集窗口
public void processBatch() {
    List<FileUpdateRequest> batch = collectBatch();
    executorService.submit(() -> sendBatch(batch));
}
```

**优势**:
- 减少HTTP请求数量（N个文件 → 1个批次请求）
- 平衡延迟与吞吐量
- 避免服务端瞬时压力

### 三、可靠性保障

#### 1. 多层重试机制

**指数退避重试**:
```java
int maxAttempts = 3;
long[] delays = {1000, 2000, 4000};  // 1s, 2s, 4s

for (int attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
        return sendRequest();
    } catch (Exception e) {
        if (attempt < maxAttempts) {
            Thread.sleep(delays[attempt - 1]);
        }
    }
}
```

**降级策略**:
- 异步API失败 → 降级到同步API
- 大批量失败 → 拆分为小批量
- 批量失败 → 降级到单文件

#### 2. 超时控制

```yaml
batch:
  batch-timeout-seconds: 600   # 单批次超时: 10分钟
  total-timeout-seconds: 3600  # 总超时: 1小时

cache.refresh:
  retry:
    initial-delay: 1000ms      # 重试初始延迟
```

#### 3. 部分成功支持

```yaml
batch:
  enable-partial-success: true  # 允许部分失败
```

系统记录每个文件的上传状态，即使批次中部分文件失败，成功的文件也会被正确处理。

### 四、性能优化

#### 1. 数据库连接池

```yaml
datasource:
  hikari:
    maximum-pool-size: 20        # 最大连接数
    minimum-idle: 5              # 最小空闲连接
    connection-timeout: 30000    # 连接超时30秒
    max-lifetime: 1800000        # 连接最大生命周期30分钟
```

#### 2. 文件传输优化

**Base64编码传输**:
- 优点: 跨平台兼容，JSON友好
- 缺点: 增加33%体积
- 适用场景: 中小文件（<50MB）

**分块上传支持**:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB      # 单文件最大50MB
      max-request-size: 500MB  # 单次请求最大500MB
```

#### 3. 批次大小优化

```yaml
batch:
  optimal-batch-size: 1000     # 推荐批次大小
  max-batch-size: 5000         # 绝对最大批次
  large-batch-threshold: 500   # 大批量阈值
  mega-batch-threshold: 2000   # 超大批量阈值
```

**自动拆分逻辑**:
```java
if (batchSize > 3000) {
    // 强制拆分为多个子批次
    List<List<File>> subBatches = splitBatch(files, 1000);
    subBatches.forEach(this::processBatch);
}
```

### 五、监控与日志

#### 1. 实时统计

```java
AtomicInteger successCount = new AtomicInteger(0);
AtomicInteger failureCount = new AtomicInteger(0);
AtomicInteger activeThreads = new AtomicInteger(0);

// 批次处理统计
logger.info("Batch processed: {} success, {} failed, queue size: {}",
    successCount.get(), failureCount.get(), pendingQueue.size());
```

#### 2. 进度回调

```java
public interface UploadProgressCallback {
    void onProgress(int completed, int total);
    void onFileComplete(String filename, boolean success);
    void onAllComplete(int success, int failed);
}
```

客户端实时显示：
- 当前上传文件名
- 完成数量/总数量
- 成功/失败统计
- 预估剩余时间

---

## 数据库设计

### 核心表结构

**ImageUpload** - 上传记录表
```sql
CREATE TABLE image_upload (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    filename VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    image_type INT,           -- 1:花型图 2:配色图
    username VARCHAR(100),
    upload_time DATETIME
);
```

**PicInfo** - 图片元数据表
```sql
CREATE TABLE pic_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(100),
    category VARCHAR(100),
    order_number VARCHAR(100),
    fabric_type VARCHAR(100),
    customer_name VARCHAR(200),
    market VARCHAR(100),
    set_count INT,
    order_date DATE,
    delivery_date DATE,
    upload_date DATE,
    factory VARCHAR(200),
    design_company VARCHAR(200),
    designer VARCHAR(100),
    remarks TEXT,
    merchandiser VARCHAR(100),
    sort_order INT
);
```

**DataVector** - 特征向量表
```sql
CREATE TABLE data_vector (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    image_name VARCHAR(255),
    color_vector TEXT,        -- Color特征
    glcm_vector TEXT,         -- GLCM特征
    lbp_vector TEXT,          -- LBP特征
    vgg_vector TEXT,          -- VGG特征
    vit_vector TEXT,          -- ViT特征
    update_time DATETIME
);
```

---

## 配置说明

### 应用配置 (application.yml)

```yaml
server:
  port: 8081
  servlet:
    context-path: /api

# 文件存储路径
app:
  file:
    upload:
      flower-path: D:\MLS_Pic\mls_image_use
      color-path: D:\MLS_Pic\mls_image_use\color_scheme
      allowed-types: jpg,jpeg,png,bmp,gif,tiff,webp
      max-file-size: 50MB

# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://192.168.0.79:3306/test_02
    username: mls01
    password: 12345@Mls
    driver-class-name: com.mysql.cj.jdbc.Driver

# 批处理配置
batch:
  max-size: 1000
  thread-pool-size: 10
  enable-parallel-processing: true
  batch-timeout-seconds: 600

# 缓存刷新配置
cache:
  refresh:
    enabled: true
    target-url: http://192.168.0.79:8080
    retry:
      max-attempts: 3
```

### 安全配置

```yaml
app:
  security:
    jwt:
      secret: mls-upload-system-jwt-secret-key-2024
      expiration: 86400000  # 24小时
      header: Authorization
      prefix: Bearer
    password:
      encoder: bcrypt
      strength: 10
```

---

## 部署指南

### 环境要求

- JDK 1.8+
- MySQL 8.0+
- Maven 3.6+
- Docker (可选，用于特征提取)

### 构建步骤

```bash
# 1. 克隆项目
cd upload_3.0_server_1

# 2. 构建服务端
cd upload-server
mvn clean package -DskipTests

# 3. 构建客户端
cd ../upload-client
mvn clean package

# 4. 运行服务端
java -jar upload-server/target/upload-server-1.0.0.jar

# 5. 运行客户端
java -jar upload-client/target/upload-client-1.0.0.jar
```

### 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE test_02 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入表结构
source schema.sql;

-- 创建用户并授权
CREATE USER 'mls01'@'%' IDENTIFIED BY '12345@Mls';
GRANT ALL PRIVILEGES ON test_02.* TO 'mls01'@'%';
FLUSH PRIVILEGES;
```

---

## API接口

### 文件上传

**单文件上传**
```http
POST /api/upload/single
Content-Type: application/json

{
  "filename": "product001.jpg",
  "fileData": "base64EncodedString",
  "imageType": 1,
  "username": "admin"
}
```

**批量上传**
```http
POST /api/upload/batch
Content-Type: application/json

{
  "requests": [
    {"filename": "file1.jpg", "fileData": "...", "imageType": 1},
    {"filename": "file2.jpg", "fileData": "...", "imageType": 1}
  ]
}
```

### 缓存刷新

**单文件刷新**
```http
POST /api/cache/update/single
Content-Type: application/json

{
  "filename": "product001.jpg",
  "operation": "add"
}
```

**批量刷新**
```http
POST /api/cache/update/batch
Content-Type: application/json

{
  "files": [
    {"filename": "file1.jpg", "operation": "add"},
    {"filename": "file2.jpg", "operation": "update"}
  ]
}
```

### 响应格式

```json
{
  "success": true,
  "message": "上传成功",
  "data": {
    "filename": "product001.jpg",
    "filePath": "D:\\MLS_Pic\\mls_image_use\\product001.jpg",
    "fileSize": 1024000,
    "imageType": 1
  }
}
```

---

## 性能指标

### 测试环境

- CPU: Intel i7-9700K (8核)
- 内存: 32GB DDR4
- 硬盘: NVMe SSD
- 网络: 千兆局域网

### 性能数据

| 场景 | 文件数量 | 平均文件大小 | 总耗时 | 吞吐量 |
|-----|---------|------------|--------|--------|
| 小批量 | 100 | 2MB | 15秒 | 6.7文件/秒 |
| 中批量 | 500 | 2MB | 65秒 | 7.7文件/秒 |
| 大批量 | 1000 | 2MB | 125秒 | 8.0文件/秒 |
| 超大批量 | 5000 | 2MB | 580秒 | 8.6文件/秒 |

**关键指标**:
- 单文件上传延迟: 100-200ms
- 批次处理延迟: 2-5秒
- 并发连接数: 最大200
- 数据库连接池: 20个连接
- 内存占用: 服务端约512MB，客户端约256MB

---

## 故障排查

### 常见问题

**1. 上传失败: "文件大小超过限制"**
```yaml
# 调整application.yml
spring:
  servlet:
    multipart:
      max-file-size: 100MB  # 增加限制
```

**2. 数据库连接超时**
```yaml
# 增加连接池配置
datasource:
  hikari:
    maximum-pool-size: 30
    connection-timeout: 60000
```

**3. 批量上传卡住**
- 检查日志: `logs/upload-server.log`
- 查看队列大小: `/api/upload/health`
- 重启批处理服务: 调用管理接口

**4. 特征提取失败**
```bash
# 检查Docker服务状态
docker ps | grep feature-extraction

# 查看Docker日志
docker logs feature-extraction-service
```

---

## 技术亮点总结

### 1. 企业级可靠性
- ✅ 多层重试机制（指数退避）
- ✅ 降级策略（异步→同步→单文件）
- ✅ 部分成功支持
- ✅ 超时控制（批次级+总体级）

### 2. 高性能架构
- ✅ 智能API路由（根据文件数量自适应）
- ✅ 队列化批次收集（减少HTTP请求）
- ✅ 并发线程池（客户端+服务端）
- ✅ 数据库连接池优化

### 3. 可扩展设计
- ✅ 模块化架构（客户端/服务端分离）
- ✅ 插件化特征提取（Docker容器化）
- ✅ 配置驱动（YAML外部化配置）
- ✅ RESTful API设计

### 4. 生产级监控
- ✅ 实时进度追踪
- ✅ 详细日志记录
- ✅ 统计指标收集
- ✅ 健康检查接口

---

## 许可证

本项目为木林森公司内部使用系统，版权所有。

---

## 联系方式

如有问题或建议，请联系开发团队。
