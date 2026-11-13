@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest > test7-out.log 2> test7-err.log
echo EXIT=%ERRORLEVEL% >> test7-out.log
