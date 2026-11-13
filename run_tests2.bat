@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest --no-daemon --console=plain > "test2-out.log" 2> "test2-err.log"
echo EXITCODE=%ERRORLEVEL% >> "test2-out.log"
