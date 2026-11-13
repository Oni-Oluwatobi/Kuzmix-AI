@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest assembleDebug > apk9-out.log 2> apk9-err.log
echo EXIT=%ERRORLEVEL% >> apk9-out.log
