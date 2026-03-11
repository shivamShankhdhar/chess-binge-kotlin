@echo off
:: Docker Deployment Script for Cirkle Admin
:: Usage: deploy.bat [build|stop|logs]

setlocal enabledelayedexpansion

if "%1"=="" (
    echo Deploying Cirkle Admin with Docker Compose...
    docker-compose --env-file .env.docker up -d
    echo.
    echo Admin Panel will be available at: http://localhost:3000
    exit /b 0
)

set "OPTION=%1"

if "!OPTION!"=="build" (
    echo Building and deploying Cirkle Admin...
    docker-compose --env-file .env.docker up -d --build
    echo.
    echo Admin Panel will be available at: http://localhost:3000
) else if "!OPTION!"=="stop" (
    echo Stopping Cirkle Admin...
    docker-compose down
    echo Admin Panel stopped.
) else if "!OPTION!"=="logs" (
    echo Showing logs for Cirkle Admin...
    docker-compose logs -f
) else if "!OPTION!"=="restart" (
    echo Restarting Cirkle Admin...
    docker-compose restart
) else if "!OPTION!"=="status" (
    echo Checking status...
    docker-compose ps
) else (
    echo Unknown option: !OPTION!
    echo.
    echo Usage: deploy.bat [build^|stop^|logs^|restart^|status]
    echo.
    echo Options:
    echo   build   - Build and deploy ^(default: docker-compose up -d^)
    echo   stop    - Stop the service
    echo   logs    - View logs
    echo   restart - Restart the service
    echo   status  - Check container status
    exit /b 1
)

endlocal
