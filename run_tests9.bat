@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest > test9-out.log 2> test9-err.log
echo EXIT=%ERRORLEVEL% >> test9-out.log
