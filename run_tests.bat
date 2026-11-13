@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest --no-daemon --console=plain > "test1-out.log" 2> "test1-err.log"
echo EXITCODE=%ERRORLEVEL% >> "test1-out.log"
