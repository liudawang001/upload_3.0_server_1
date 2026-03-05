# 📦 打包指南 - 如何生成EXE文件

## 问题解决

您遇到的打包失败问题已解决！✅

### ❌ 之前的问题
```bash
mvn clean package -DskipTests
```
这个命令无法正确跳过测试，导致因为服务器集成测试失败而打包中断。

### ✅ 正确的命令
```bash
mvn clean package "-Dmaven.test.skip=true"
```

**关键差异**:
- 使用 `maven.test.skip=true` 而不是 `DskipTests`
- 参数用**双引号**包围以确保在PowerShell中正确解析

---

## 打包流程说明

当您运行上述命令时，Maven会执行以下步骤：

### 1️⃣ 清理阶段 (Clean)
```
[INFO] --- clean:3.2.0:clean (default-clean) @ upload-client ---
[INFO] Deleting target directory
```
删除之前的编译产物。

### 2️⃣ 编译阶段 (Compile)
```
[INFO] --- compiler:3.8.1:compile (default-compile) @ upload-client ---
[INFO] Compiling 16 source files
```
编译所有Java源文件，包含我们新增的复选框功能代码。

### 3️⃣ 资源复制阶段 (Resources)
```
[INFO] --- resources:3.1.0:resources (default-resources) @ upload-client ---
[INFO] Copying 11 resources
```
复制FXML、CSS、图标等资源文件。

### 4️⃣ 测试阶段 (Test) ⏭️ **跳过**
```
[INFO] --- surefire:2.22.2:test (default-test) @ upload-client ---
[INFO] Tests are skipped.
```

### 5️⃣ JAR打包阶段 (JAR)
```
[INFO] --- jar:3.1.2:jar (default-jar) @ upload-client ---
[INFO] Building jar: ...upload-client.jar
```
生成可执行的JAR文件。

### 6️⃣ 依赖复制阶段 (Dependencies)
```
[INFO] --- dependency:3.1.2:copy-dependencies (copy-dependencies) @ upload-client ---
[INFO] Copying 21 dependencies to lib directory
```
复制所有依赖库到 `target/lib` 目录。

### 7️⃣ EXE生成阶段 (Launch4j) ⭐
```
[INFO] --- launch4j:1.7.25:launch4j (l4j-clui) @ upload-client ---
[INFO] launch4j: Wrapping
[INFO] launch4j: Successfully created upload-client.exe
```
**这是最重要的一步！** 使用Launch4j将JAR文件包装成Windows可执行文件。

---

## 输出文件

打包完成后，您会在以下位置找到生成的文件：

### 📍 文件位置
```
upload-client/target/
├── upload-client.jar           ← Java可执行文件
├── upload-client.exe           ← ✅ Windows可执行文件（这个！）
└── lib/
    ├── okhttp-3.14.9.jar
    ├── jackson-databind-2.10.5.jar
    └── ... (其他21个依赖库)
```

### 🎯 使用 upload-client.exe
- 直接双击运行，无需命令行
- 无需安装Java（Launch4j包含了运行时环境检查）
- 可以创建快捷方式到桌面
- 可以添加到Windows开始菜单

---

## 快速打包命令参考

### 📝 标准打包命令（推荐）
```bash
cd upload-client
mvn clean package "-Dmaven.test.skip=true"
```

### 🔧 其他常用命令

**只编译不打包：**
```bash
mvn clean compile "-Dmaven.test.skip=true"
```

**仅生成JAR（不生成EXE）：**
```bash
mvn clean jar "-Dmaven.test.skip=true"
```

**包含运行测试的完整打包：**
```bash
mvn clean package
```
⚠️ 注意：这会运行所有测试，可能因为集成测试而失败（需要服务器运行）

**跳过测试并显示详细信息：**
```bash
mvn clean package "-Dmaven.test.skip=true" -e
```

---

## 打包配置详解

### Launch4j配置 (pom.xml)

项目的 `pom.xml` 中已配置好以下launch4j参数：

```xml
<plugin>
    <groupId>com.akathist.maven.plugins.launch4j</groupId>
    <artifactId>launch4j-maven-plugin</artifactId>
    <version>1.7.25</version>
    <configuration>
        <headerType>gui</headerType>                    <!-- GUI模式，无控制台 -->
        <jar>target/upload-client.jar</jar>            <!-- 源JAR文件 -->
        <outfile>target/upload-client.exe</outfile>    <!-- 输出EXE文件 -->
        <icon>src/main/resources/icon.ico</icon>       <!-- 应用图标 -->
        <jre>
            <minVersion>1.8.0</minVersion>             <!-- 最小Java版本 -->
        </jre>
    </configuration>
</plugin>
```

### 关键配置说明

| 配置项 | 值 | 说明 |
|--------|-----|------|
| headerType | gui | 不显示控制台窗口 |
| minVersion | 1.8.0 | 要求Java 8及以上 |
| icon | icon.ico | 应用图标 |
| jdkPreference | preferJre | 优先使用JRE而不是JDK |

---

## 常见问题解决

### ❓ Q1: 为什么需要用引号包围参数？
**A:** PowerShell会将 `-D` 当作自己的参数而不是Maven参数。用引号包围可以确保整个参数被传递给Maven。

### ❓ Q2: EXE文件无法运行怎么办？
**A:** 可能原因及解决方案：
1. ✅ 确保已安装Java 8或以上版本
2. ✅ 检查 `target/lib` 目录中是否有所有依赖库
3. ✅ 尝试从命令行运行查看错误信息：
   ```bash
   G:\path\to\upload-client.exe
   ```

### ❓ Q3: 可以在其他电脑上运行生成的EXE吗？
**A:** 可以，但目标电脑需要满足：
- ✅ 安装了Java 8或以上版本
- ✅ 建议安装JRE而不是JDK（更小更快）
- ✅ 复制整个EXE文件及其依赖的lib目录

### ❓ Q4: 如何分发EXE给用户？
**A:** 建议的分发方式：
```
software-package/
├── upload-client.exe          ← 主程序
├── lib/                       ← 依赖库目录
│   ├── okhttp-3.14.9.jar
│   ├── jackson-*.jar
│   └── ... (其他库)
├── README.txt                 ← 说明文档
└── icon.ico                   ← 应用图标（可选）
```

用户只需下载并解压即可运行。

---

## 构建时间参考

| 操作 | 耗时 | 备注 |
|-----|------|------|
| 第一次打包 | ~20秒 | 需要下载Launch4j工具 |
| 后续打包 | ~10秒 | 快速，因为已缓存 |
| 仅编译 | ~3秒 | 快速编译 |
| 完整测试+打包 | ~1分钟 | 包含所有测试运行 |

---

## 版本信息

### 当前配置
- **Java版本**: Java 8 (支持JDK8+)
- **Launch4j版本**: 1.7.25
- **Maven插件**: launch4j-maven-plugin 1.7.25

### 依赖库版本
```
OkHttp: 3.14.9
Jackson: 2.10.5
Apache POI: 4.1.2
SLF4j/Logback: 1.7.30 / 1.2.3
```

---

## 下一步

✅ 现在您可以：
1. 使用 `mvn clean package "-Dmaven.test.skip=true"` 生成EXE
2. 在 `target/` 目录中找到 `upload-client.exe`
3. 直接运行exe或分发给用户
4. 继续开发新功能

🎉 **恭喜！您已经掌握了打包流程！**

---

**有任何问题，请参考本指南或查看**:
- 📖 项目根目录的其他文档
- 🔧 pom.xml中的配置注释
- 📝 Maven官方文档

**祝您使用愉快！** 🚀

