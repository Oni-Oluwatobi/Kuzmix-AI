@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat assembleDebug --no-daemon --console=plain > "apk1-out.log" 2> "apk1-err.log"
echo EXITCODE=%ERRORLEVEL% >> "apk1-out.log"
