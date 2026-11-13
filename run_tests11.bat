@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest > test11-out.log 2> test11-err.log
echo EXIT=%ERRORLEVEL% >> test11-out.log
