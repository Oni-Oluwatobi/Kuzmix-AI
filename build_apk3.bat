@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat assembleDebug --no-daemon --console=plain > "apk3-out.log" 2> "apk3-err.log"
echo EXITCODE=%ERRORLEVEL% >> "apk3-out.log"
