@echo off
REM Windows build script
setlocal
cd /d "%~dp0"
set CP=lib\flatlaf-3.5.4.jar;lib\flatlaf-extras-3.5.4.jar;lib\jsvg-1.6.1.jar;mysql-connector-j-8.3.0.jar;.
echo Compiling Java sources (JDK 21)...
javac -cp "%CP%" -d . *.java
if errorlevel 1 exit /b 1
echo Build complete.
endlocal
