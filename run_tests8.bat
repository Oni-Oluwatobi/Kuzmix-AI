@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest > test8-out.log 2> test8-err.log
echo EXIT=%ERRORLEVEL% >> test8-out.log
