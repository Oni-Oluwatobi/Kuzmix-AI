@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest --no-daemon --console=plain > "test5-out.log" 2> "test5-err.log"
echo EXITCODE=%ERRORLEVEL% >> "test5-out.log"
