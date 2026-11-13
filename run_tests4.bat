@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest --no-daemon --console=plain > "test4-out.log" 2> "test4-err.log"
echo EXITCODE=%ERRORLEVEL% >> "test4-out.log"
