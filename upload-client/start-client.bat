@echo off
chcp 65001 >nul 2>&1
setlocal

set APP_DIR=%~dp0
cd /d "%APP_DIR%"

echo Starting MLS Image Upload Client...
echo.

set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8

set CLIENT_EXE=
set CLIENT_JAR=
set CLIENT_LIB=

if exist "upload-client.exe" (
    set CLIENT_EXE=upload-client.exe
) else if exist "target\upload-client.exe" (
    set CLIENT_EXE=target\upload-client.exe
)

if exist "upload-client.jar" (
    set CLIENT_JAR=upload-client.jar
    set CLIENT_LIB=lib\*
) else if exist "target\upload-client.jar" (
    set CLIENT_JAR=target\upload-client.jar
    set CLIENT_LIB=target\lib\*
)

echo Client Configuration:
if exist "application.properties" (
    echo - External config: %APP_DIR%application.properties
) else if exist "config\application.properties" (
    echo - External config: %APP_DIR%config\application.properties
) else (
    echo - External config: not found, using built-in defaults
)
echo - Server URL is read from server.url in application.properties
echo - Supported Excel: .xlsx, .xls
echo - Supported Images: .jpg, .jpeg, .png, .bmp, .gif, .tiff, .webp
echo.

if defined CLIENT_EXE (
    echo Using executable file: %CLIENT_EXE%
    start "" "%CLIENT_EXE%"
) else if defined CLIENT_JAR (
    echo Using JAR file: %CLIENT_JAR%
    java %JAVA_OPTS% -cp "%CLIENT_JAR%;%CLIENT_LIB%" com.mls.upload.client.UploadClientApplication
) else (
    echo Error: neither upload-client.exe nor upload-client.jar was found.
    echo Please put this script in the client install directory or run mvn package first.
    pause
    exit /b 1
)

echo Client started, please check the new window.
pause
