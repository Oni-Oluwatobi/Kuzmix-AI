@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest --no-daemon --console=plain > "test6-out.log" 2> "test6-err.log"
echo EXITCODE=%ERRORLEVEL% >> "test6-out.log"
