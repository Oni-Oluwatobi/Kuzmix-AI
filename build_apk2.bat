@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat assembleDebug --no-daemon --console=plain > "apk2-out.log" 2> "apk2-err.log"
echo EXITCODE=%ERRORLEVEL% >> "apk2-out.log"
