@echo off
setlocal
set GRADLE_VERSION=8.7
set ROOT_DIR=%~dp0
set BOOTSTRAP_DIR=%ROOT_DIR%.gradle-bootstrap
set GRADLE_HOME=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%BOOTSTRAP_DIR%" mkdir "%BOOTSTRAP_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%-bin.zip'; Expand-Archive -Force '%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%-bin.zip' '%BOOTSTRAP_DIR%'"
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
