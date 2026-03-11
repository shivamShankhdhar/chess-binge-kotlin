#!/bin/bash
# Docker Deployment Script for Cirkle Admin - Linux/Mac
# Usage: ./deploy.sh [build|stop|logs|restart|status]

set -e

print_usage() {
    echo "Usage: $0 [build|stop|logs|restart|status]"
    echo ""
    echo "Options:"
    echo "  (none)  - Deploy with docker-compose up -d (default)"
    echo "  build   - Build and deploy"
    echo "  stop    - Stop the service"
    echo "  logs    - View logs"
    echo "  restart - Restart the service"
    echo "  status  - Check container status"
    echo ""
}

OPTION="${1:-}"

case "$OPTION" in
    "")
        echo "Deploying Cirkle Admin with Docker Compose..."
        docker-compose --env-file .env.docker up -d
        echo ""
        echo "Admin Panel will be available at: http://localhost:3000"
        ;;
    
    build)
        echo "Building and deploying Cirkle Admin..."
        docker-compose --env-file .env.docker up -d --build
        echo ""
        echo "Admin Panel will be available at: http://localhost:3000"
        ;;
    
    stop)
        echo "Stopping Cirkle Admin..."
        docker-compose down
        echo "Admin Panel stopped."
        ;;
    
    logs)
        echo "Showing logs for Cirkle Admin..."
        docker-compose logs -f
        ;;
    
    restart)
        echo "Restarting Cirkle Admin..."
        docker-compose restart
        ;;
    
    status)
        echo "Checking status..."
        docker-compose ps
        ;;
    
    *)
        echo "Unknown option: $OPTION"
        print_usage
        exit 1
        ;;
esac
