@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest assembleDebug > apk12-out.log 2> apk12-err.log
echo EXIT=%ERRORLEVEL% >> apk12-out.log
