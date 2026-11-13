@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest --no-daemon --console=plain > "test3-out.log" 2> "test3-err.log"
echo EXITCODE=%ERRORLEVEL% >> "test3-out.log"
