@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat assembleDebug --no-daemon --console=plain > "apk4-out.log" 2> "apk4-err.log"
echo EXITCODE=%ERRORLEVEL% >> "apk4-out.log"
