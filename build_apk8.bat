@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest assembleDebug > apk8-out.log 2> apk8-err.log
echo EXIT=%ERRORLEVEL% >> apk8-out.log
