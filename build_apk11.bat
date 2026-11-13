@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest assembleDebug > apk11-out.log 2> apk11-err.log
echo EXIT=%ERRORLEVEL% >> apk11-out.log
