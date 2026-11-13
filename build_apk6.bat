@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat assembleDebug > apk6-out.log 2> apk6-err.log
echo EXIT=%ERRORLEVEL% >> apk6-out.log
