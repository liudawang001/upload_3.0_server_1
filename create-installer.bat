@echo off
echo ========================================
echo 木林森图片上传系统安装包制作工具
echo ========================================
echo.

REM 设置变量
set INSTALL_DIR=MLS-Upload-System-Installer
set CLIENT_DIR=%INSTALL_DIR%\client
set SERVER_DIR=%INSTALL_DIR%\server
set DOCS_DIR=%INSTALL_DIR%\docs

echo 正在创建安装包目录结构...

REM 创建目录结构
if exist %INSTALL_DIR% rmdir /s /q %INSTALL_DIR%
mkdir %INSTALL_DIR%
mkdir %CLIENT_DIR%
mkdir %SERVER_DIR%
mkdir %DOCS_DIR%

echo.
echo 正在复制客户端文件...

REM 复制客户端文件
if exist "upload-client\target\upload-client.exe" (
    copy "upload-client\target\upload-client.exe" "%CLIENT_DIR%\"
    echo ✓ 客户端exe文件已复制
) else (
    echo ✗ 错误：未找到客户端exe文件，请先运行 mvn package
    pause
    exit /b 1
)

if exist "upload-client\target\upload-client.jar" (
    copy "upload-client\target\upload-client.jar" "%CLIENT_DIR%\"
    echo ✓ 客户端jar文件已复制
)

if exist "upload-client\target\lib" (
    xcopy "upload-client\target\lib" "%CLIENT_DIR%\lib" /s /e /i
    echo ✓ 客户端依赖库已复制
)

if exist "upload-client\start-client.bat" (
    copy "upload-client\start-client.bat" "%CLIENT_DIR%\"
    echo ✓ 客户端启动脚本已复制
)

echo.
echo 正在复制服务端文件...

REM 复制服务端文件
if exist "upload-server\target\upload-server.jar" (
    copy "upload-server\target\upload-server.jar" "%SERVER_DIR%\"
    echo ✓ 服务端jar文件已复制
) else (
    echo ✗ 错误：未找到服务端jar文件，请先运行 mvn package
    pause
    exit /b 1
)

if exist "upload-server\start-server.bat" (
    copy "upload-server\start-server.bat" "%SERVER_DIR%\"
    echo ✓ 服务端启动脚本已复制
)

REM 复制配置文件
if exist "upload-server\src\main\resources\application.yml" (
    copy "upload-server\src\main\resources\application.yml" "%SERVER_DIR%\"
    echo ✓ 服务端配置文件已复制
)

echo.
echo 正在复制文档文件...

REM 复制文档
if exist "部署指南.md" (
    copy "部署指南.md" "%DOCS_DIR%\"
    echo ✓ 部署指南已复制
)

if exist "用户使用手册.md" (
    copy "用户使用手册.md" "%DOCS_DIR%\"
    echo ✓ 用户手册已复制
)

if exist "项目任务书.md" (
    copy "项目任务书.md" "%DOCS_DIR%\"
    echo ✓ 项目任务书已复制
)

if exist "技术方案详细说明.md" (
    copy "技术方案详细说明.md" "%DOCS_DIR%\"
    echo ✓ 技术方案已复制
)

echo.
echo 正在创建安装脚本...

REM 创建主安装脚本
echo @echo off > %INSTALL_DIR%\install.bat
echo echo ======================================== >> %INSTALL_DIR%\install.bat
echo echo 木林森图片上传系统安装程序 >> %INSTALL_DIR%\install.bat
echo echo ======================================== >> %INSTALL_DIR%\install.bat
echo echo. >> %INSTALL_DIR%\install.bat
echo echo 正在检查Java环境... >> %INSTALL_DIR%\install.bat
echo java -version ^>nul 2^>^&1 >> %INSTALL_DIR%\install.bat
echo if %%errorlevel%% neq 0 ^( >> %INSTALL_DIR%\install.bat
echo     echo 错误：未检测到Java运行环境 >> %INSTALL_DIR%\install.bat
echo     echo 请先安装JDK 8或更高版本 >> %INSTALL_DIR%\install.bat
echo     pause >> %INSTALL_DIR%\install.bat
echo     exit /b 1 >> %INSTALL_DIR%\install.bat
echo ^) >> %INSTALL_DIR%\install.bat
echo echo ✓ Java环境检查通过 >> %INSTALL_DIR%\install.bat
echo echo. >> %INSTALL_DIR%\install.bat
echo echo 安装选项： >> %INSTALL_DIR%\install.bat
echo echo 1. 安装客户端 >> %INSTALL_DIR%\install.bat
echo echo 2. 安装服务端 >> %INSTALL_DIR%\install.bat
echo echo 3. 安装完整系统 >> %INSTALL_DIR%\install.bat
echo echo 4. 查看文档 >> %INSTALL_DIR%\install.bat
echo echo 5. 退出 >> %INSTALL_DIR%\install.bat
echo echo. >> %INSTALL_DIR%\install.bat
echo set /p choice=请选择安装选项 ^(1-5^): >> %INSTALL_DIR%\install.bat
echo if "%%choice%%"=="1" goto install_client >> %INSTALL_DIR%\install.bat
echo if "%%choice%%"=="2" goto install_server >> %INSTALL_DIR%\install.bat
echo if "%%choice%%"=="3" goto install_full >> %INSTALL_DIR%\install.bat
echo if "%%choice%%"=="4" goto show_docs >> %INSTALL_DIR%\install.bat
echo if "%%choice%%"=="5" goto exit >> %INSTALL_DIR%\install.bat
echo goto install >> %INSTALL_DIR%\install.bat
echo. >> %INSTALL_DIR%\install.bat
echo :install_client >> %INSTALL_DIR%\install.bat
echo echo 正在安装客户端... >> %INSTALL_DIR%\install.bat
echo xcopy client C:\MLS-Upload-Client\ /s /e /i /y >> %INSTALL_DIR%\install.bat
echo echo ✓ 客户端安装完成 >> %INSTALL_DIR%\install.bat
echo echo 安装路径：C:\MLS-Upload-Client\ >> %INSTALL_DIR%\install.bat
echo goto end >> %INSTALL_DIR%\install.bat
echo. >> %INSTALL_DIR%\install.bat
echo :install_server >> %INSTALL_DIR%\install.bat
echo echo 正在安装服务端... >> %INSTALL_DIR%\install.bat
echo xcopy server C:\MLS-Upload-Server\ /s /e /i /y >> %INSTALL_DIR%\install.bat
echo echo ✓ 服务端安装完成 >> %INSTALL_DIR%\install.bat
echo echo 安装路径：C:\MLS-Upload-Server\ >> %INSTALL_DIR%\install.bat
echo goto end >> %INSTALL_DIR%\install.bat
echo. >> %INSTALL_DIR%\install.bat
echo :install_full >> %INSTALL_DIR%\install.bat
echo echo 正在安装完整系统... >> %INSTALL_DIR%\install.bat
echo xcopy client C:\MLS-Upload-Client\ /s /e /i /y >> %INSTALL_DIR%\install.bat
echo xcopy server C:\MLS-Upload-Server\ /s /e /i /y >> %INSTALL_DIR%\install.bat
echo xcopy docs C:\MLS-Upload-System\docs\ /s /e /i /y >> %INSTALL_DIR%\install.bat
echo echo ✓ 系统安装完成 >> %INSTALL_DIR%\install.bat
echo echo 客户端路径：C:\MLS-Upload-Client\ >> %INSTALL_DIR%\install.bat
echo echo 服务端路径：C:\MLS-Upload-Server\ >> %INSTALL_DIR%\install.bat
echo echo 文档路径：C:\MLS-Upload-System\docs\ >> %INSTALL_DIR%\install.bat
echo goto end >> %INSTALL_DIR%\install.bat
echo. >> %INSTALL_DIR%\install.bat
echo :show_docs >> %INSTALL_DIR%\install.bat
echo start docs >> %INSTALL_DIR%\install.bat
echo goto install >> %INSTALL_DIR%\install.bat
echo. >> %INSTALL_DIR%\install.bat
echo :end >> %INSTALL_DIR%\install.bat
echo echo. >> %INSTALL_DIR%\install.bat
echo echo 安装完成！请查看文档了解使用方法。 >> %INSTALL_DIR%\install.bat
echo pause >> %INSTALL_DIR%\install.bat
echo. >> %INSTALL_DIR%\install.bat
echo :exit >> %INSTALL_DIR%\install.bat
echo exit >> %INSTALL_DIR%\install.bat

echo ✓ 安装脚本已创建

echo.
echo 正在创建README文件...

REM 创建README文件
echo # 木林森图片上传系统安装包 > %INSTALL_DIR%\README.md
echo. >> %INSTALL_DIR%\README.md
echo ## 安装说明 >> %INSTALL_DIR%\README.md
echo. >> %INSTALL_DIR%\README.md
echo 1. 双击运行 `install.bat` 开始安装 >> %INSTALL_DIR%\README.md
echo 2. 根据提示选择安装选项 >> %INSTALL_DIR%\README.md
echo 3. 查看 `docs` 文件夹中的文档了解详细使用方法 >> %INSTALL_DIR%\README.md
echo. >> %INSTALL_DIR%\README.md
echo ## 文件结构 >> %INSTALL_DIR%\README.md
echo. >> %INSTALL_DIR%\README.md
echo - `client/` - 客户端程序文件 >> %INSTALL_DIR%\README.md
echo - `server/` - 服务端程序文件 >> %INSTALL_DIR%\README.md
echo - `docs/` - 系统文档 >> %INSTALL_DIR%\README.md
echo - `install.bat` - 安装脚本 >> %INSTALL_DIR%\README.md
echo. >> %INSTALL_DIR%\README.md
echo ## 系统要求 >> %INSTALL_DIR%\README.md
echo. >> %INSTALL_DIR%\README.md
echo - Windows 7/8/10/11 >> %INSTALL_DIR%\README.md
echo - JDK 8 或更高版本 >> %INSTALL_DIR%\README.md
echo - MySQL 8.0 ^(服务端^) >> %INSTALL_DIR%\README.md
echo. >> %INSTALL_DIR%\README.md
echo 版本：v1.0.0 >> %INSTALL_DIR%\README.md
echo 日期：2025-09-11 >> %INSTALL_DIR%\README.md

echo ✓ README文件已创建

echo.
echo ========================================
echo 安装包制作完成！
echo ========================================
echo.
echo 安装包位置：%INSTALL_DIR%\
echo.
echo 包含文件：
echo ✓ 客户端程序 (upload-client.exe)
echo ✓ 服务端程序 (upload-server.jar)
echo ✓ 启动脚本
echo ✓ 配置文件
echo ✓ 完整文档
echo ✓ 安装程序
echo.
echo 您可以将整个 %INSTALL_DIR% 文件夹打包分发给用户。
echo.
pause
