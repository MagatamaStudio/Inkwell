@echo off
setlocal
pushd "%~dp0"

echo ==========================================
echo   Inkwell - build
echo ==========================================
echo.

set "JAVA_HOME="
if exist "C:\Program Files\Java\jdk-25.0.2\bin\javac.exe" set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Java\jdk-2*") do (
  if not defined JAVA_HOME if exist "%%~D\bin\javac.exe" set "JAVA_HOME=%%~D"
)
if not defined JAVA_HOME (
  echo [ERROR] JDK 25 not found: https://adoptium.net/temurin/releases/?version=25
  pause
  popd
  exit /b 1
)
echo JDK: %JAVA_HOME%
echo.

if exist gradlew.bat (
  call gradlew.bat build
) else (
  call gradle build
)
if errorlevel 1 (
  echo.
  echo [ERROR] build failed
  pause
  popd
  exit /b 1
)

echo.
echo ---- build ok ----
if exist "build\libs" dir /b "build\libs\*.jar"
echo.
echo Copy the jar into .minecraft\mods\
echo.
pause
popd
