@echo off
echo 正在启动木林森图片上传服务端...
echo.

REM 检查Java环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误：未找到Java运行环境，请确保已安装JDK8或更高版本
    pause
    exit /b 1
)

REM 检查jar文件是否存在
if not exist "target\upload-server.jar" (
    echo 错误：未找到upload-server.jar文件，请先运行mvn package命令进行打包
    pause
    exit /b 1
)

echo 服务端配置信息：
echo - 端口：8081
echo - 上下文路径：/api
echo - 数据库：MySQL (192.168.1.78:3306)
echo.

echo 启动服务端...
java -jar target\upload-server.jar

pause
