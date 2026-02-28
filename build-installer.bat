@echo off
setlocal enabledelayedexpansion

echo ====================================================
echo Jewelry Management System - Installer Build Script
echo ====================================================

REM 1. Set paths for tools
set WIX_PATH=C:\Program Files (x86)\WiX Toolset v3.14\bin
set JAVA_HOME=C:\Program Files\Java\jdk-19

REM Add WiX to the PATH for this script execution
set "PATH=%WIX_PATH%;%JAVA_HOME%\bin;%PATH%"

echo.
echo [1/3] Verifying tools...
where candle >nul 2>&1
if !ERRORLEVEL! NEQ 0 (
    echo [ERROR] WiX toolset not found at "!WIX_PATH!"
    echo Please verify WiX is installed or update WIX_PATH in this script.
    exit /b 1
)
java -version

echo.
echo [2/3] Building project and copying dependencies...
call mvn package -DskipTests
if !ERRORLEVEL! NEQ 0 (
    echo [ERROR] Maven build failed!
    exit /b !ERRORLEVEL!
)

echo.
echo [3/3] Running jpackage to create MSI installer...
set APP_NAME=JewelryManagementSystem
set APP_VERSION=1.0.0
set MAIN_CLASS=com.jewelry.Launcher
set MAIN_JAR=jewelry-management-1.0.0-SNAPSHOT.jar

REM Create output directory
if not exist "target\dist" mkdir "target\dist"

REM Copy main jar to libs directory
copy "target\%MAIN_JAR%" "target\libs\"

REM Run jpackage
jpackage ^
  --type msi ^
  --dest "target\dist" ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --input "target\libs" ^
  --icon "src\main\resources\images\app_logo.ico" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser

if !ERRORLEVEL! NEQ 0 (
    echo [ERROR] jpackage failed!
    exit /b !ERRORLEVEL!
)

echo.
echo ====================================================
echo BUILD SUCCESSFUL!
echo Installer is located at: target\dist\%APP_NAME%-%APP_VERSION%.msi
echo ====================================================
