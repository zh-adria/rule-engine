@echo off
setlocal

set "ROOT=%~dp0.."
set "BACKEND=%ROOT%\backend\rule-engine-service"
set "FRONTEND=%ROOT%\frontend\rule-engine-ui"

for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr "version"') do (
    echo Using Java: %%i
    goto :found_java
)
:found_java

echo Starting backend with H2 profile...
pushd "%BACKEND%"
call mvn -q -DskipTests -Djacoco.skip=true install
if %ERRORLEVEL% neq 0 (
    popd
    echo Build failed.
    exit /b 1
)
popd

start "rule-engine-backend" /min cmd /c "cd /d "%BACKEND%" && mvn -f rule-engine-start\pom.xml spring-boot:run -Dspring-boot.run.profiles=h2"

echo Starting frontend...
start "rule-engine-frontend" /min cmd /c "cd /d "%FRONTEND%" && npm run dev -- --port 5173"

echo Waiting for backend...
set WAIT_COUNT=0
:wait_loop
ping -n 3 127.0.0.1 >nul 2>&1
curl -s -o nul -w "%%{http_code}" http://localhost:8080/rule-engine/actuator/health >"%TEMP%\health_check.txt" 2>nul
set /p HEALTH_CODE=<"%TEMP%\health_check.txt"
if "%HEALTH_CODE%"=="200" (
    del "%TEMP%\health_check.txt" >nul 2>&1
    goto :ready
)
set /a WAIT_COUNT+=1
if %WAIT_COUNT% geq 60 (
    del "%TEMP%\health_check.txt" >nul 2>&1
    echo Backend did not start in time.
    goto :print_urls
)
goto :wait_loop

:ready
echo Backend started successfully.

:print_urls
echo.
echo Backend:  http://localhost:8080/rule-engine
echo Swagger:  http://localhost:8080/rule-engine/swagger-ui.html
echo H2:       http://localhost:8080/rule-engine/h2-console
echo H2 JDBC:  jdbc:h2:file:./data/rule_engine
echo H2 User:  sa
echo H2 Pass:  ^<empty^>
echo Frontend: http://localhost:5173

endlocal
