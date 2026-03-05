@echo off
chcp 65001 >nul 2>&1
echo Starting MLS Image Upload Client...
echo.

REM Set encoding environment variables
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8

REM Check if exe file exists first, then jar file
if exist "target\upload-client.exe" (
    echo Using executable file: upload-client.exe
    echo Client Configuration:
    echo - Server Address: http://192.168.0.81:8081
    echo - Supported Excel: .xlsx, .xls
    echo - Supported Images: .jpg, .jpeg, .png, .bmp, .gif
    echo.
    echo Starting client with UTF-8 encoding...
    start "" "target\upload-client.exe" %JAVA_OPTS%
) else if exist "target\upload-client.jar" (
    echo Using JAR file: upload-client.jar
    echo Client Configuration:
    echo - Server Address: http://192.168.0.81:8081
    echo - Supported Excel: .xlsx, .xls
    echo - Supported Images: .jpg, .jpeg, .png, .bmp, .gif
    echo.
    echo Starting client with UTF-8 encoding...
    java %JAVA_OPTS% -cp "target\upload-client.jar;target\lib\*" com.mls.upload.client.UploadClientApplication
) else (
    echo Error: Neither upload-client.exe nor upload-client.jar found
    echo Please run 'mvn package' first to build the application
    pause
    exit /b 1
)

echo Client started, please check the new window
pause
