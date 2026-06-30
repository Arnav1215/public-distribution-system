@echo off
REM Windows run script
setlocal
cd /d "%~dp0"
set CP=lib\flatlaf-3.5.4.jar;lib\flatlaf-extras-3.5.4.jar;lib\jsvg-1.6.1.jar;mysql-connector-j-8.3.0.jar;.;resources
set ENTRY=%1
if "%ENTRY%"=="" set ENTRY=LoginFrame
echo Launching %ENTRY% ...
java -cp "%CP%" %ENTRY%
endlocal
