@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest assembleDebug > apk10-out.log 2> apk10-err.log
echo EXIT=%ERRORLEVEL% >> apk10-out.log
