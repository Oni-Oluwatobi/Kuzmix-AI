@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat assembleDebug > apk7-out.log 2> apk7-err.log
echo EXIT=%ERRORLEVEL% >> apk7-out.log
