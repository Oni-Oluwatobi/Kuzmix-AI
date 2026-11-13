@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest assembleDebug > apk13-out.log 2> apk13-err.log
echo EXIT=%ERRORLEVEL% >> apk13-out.log
