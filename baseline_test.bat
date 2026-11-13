@echo off
call gradlew.bat testDebugUnitTest > baseline-tests.log 2> baseline-err.log
echo EXIT=%ERRORLEVEL% >> baseline-tests.log
