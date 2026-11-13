@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat assembleDebug > apk5-out.log 2> apk5-err.log
echo EXIT=%ERRORLEVEL% >> apk5-out.log
