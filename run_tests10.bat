@echo off
cd /d "C:\Users\The Kreadive Galaxy\Desktop\My Products\Kuzmix AI"
call gradlew.bat testDebugUnitTest > test10-out.log 2> test10-err.log
echo EXIT=%ERRORLEVEL% >> test10-out.log
