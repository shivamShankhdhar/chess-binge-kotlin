# Nginx Commands & Scripts

Start Date: March 1, 2026

---

## Quick Start Commands

### **Option 1: Docker Compose (Recommended)**

```bash
# Navigate to cirkle-webs directory
cd d:\Projects\cirkle-webs

# Start ONLY nginx container
docker-compose -f docker-compose.nginx-only.yml up -d

# Start ALL services (including nginx)
docker-compose up -d

# Check nginx status
docker-compose ps

# View nginx logs
docker-compose logs -f nginx

# Stop nginx
docker-compose down

# Restart nginx
docker-compose restart nginx
```

---

### **Option 2: System Service (Linux/Mac)**

```bash
# Start nginx service
sudo systemctl start nginx

# Check status
sudo systemctl status nginx

# Stop nginx
sudo systemctl stop nginx

# Restart nginx
sudo systemctl restart nginx

# Reload configuration (without dropping connections)
sudo systemctl reload nginx

# Enable nginx to start on boot
sudo systemctl enable nginx

# Verify nginx is running on ports 80 & 443
sudo netstat -tuln | grep -E ':(80|443)'

# View nginx logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

---

### **Option 3: Manual Start (Linux/Mac)**

```bash
# Start nginx with default config
sudo nginx

# Start with custom config
sudo nginx -c /path/to/nginx.conf

# Test configuration
sudo nginx -t

# Graceful stop
sudo nginx -s quit

# Force stop
sudo pkill -f nginx
```

---

### **Option 4: Windows (if nginx installed)**

```powershell
# Navigate to nginx directory (example path)
cd "C:\nginx"

# Start nginx
start nginx

# Stop nginx
taskkill /IM nginx.exe /F

# Test configuration
nginx -t

# View running processes
Get-Process | Where-Object {$_.Name -like "*nginx*"}
```

---

## Complete Startup Script

### **Linux/Mac (bash)**

Save as `start-nginx.sh`:

```bash
#!/bin/bash

echo "=========================================="
echo "🚀 Starting Cirkle Nginx Server"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    exit 1
fi

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    exit 1
fi

# Navigate to project directory
cd "d:\Projects\cirkle-webs" || exit

echo -e "${YELLOW}📦 Starting Nginx container...${NC}"
echo ""

# Start nginx only
docker-compose -f docker-compose.nginx-only.yml up -d

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Nginx started successfully!${NC}"
    echo ""
    sleep 2

    # Check status
    echo -e "${YELLOW}📊 Nginx Status:${NC}"
    docker-compose -f docker-compose.nginx-only.yml ps
    echo ""

    # Show port info
    echo -e "${YELLOW}🔌 Nginx running on:${NC}"
    echo "  HTTP:  http://localhost:80"
    echo "  HTTPS: https://localhost:443"
    echo ""

    # Show logs
    echo -e "${YELLOW}📋 Nginx Logs (last 10 lines):${NC}"
    docker-compose -f docker-compose.nginx-only.yml logs --tail=10 nginx
    echo ""

    echo -e "${GREEN}✅ All set! Nginx is ready to serve requests.${NC}"
else
    echo -e "${RED}❌ Failed to start Nginx${NC}"
    docker-compose -f docker-compose.nginx-only.yml logs nginx
    exit 1
fi
```

Make executable: `chmod +x start-nginx.sh`
Run: `./start-nginx.sh`

---

### **Windows (PowerShell)**

Save as `start-nginx.ps1`:

```powershell
# Start Cirkle Nginx Server

Write-Host "=========================================" -ForegroundColor Green
Write-Host "🚀 Starting Cirkle Nginx Server" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""

# Check if docker is installed
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Docker is not installed" -ForegroundColor Red
    exit 1
}

# Check if docker-compose is installed
if (-not (Get-Command docker-compose -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Docker Compose is not installed" -ForegroundColor Red
    exit 1
}

# Navigate to project directory
Set-Location "d:\Projects\cirkle-webs"

Write-Host "📦 Starting Nginx container..." -ForegroundColor Yellow
Write-Host ""

# Start nginx only
docker-compose -f docker-compose.nginx-only.yml up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Nginx started successfully!" -ForegroundColor Green
    Write-Host ""
    Start-Sleep -Seconds 2

    # Check status
    Write-Host "📊 Nginx Status:" -ForegroundColor Yellow
    docker-compose -f docker-compose.nginx-only.yml ps
    Write-Host ""

    # Show port info
    Write-Host "🔌 Nginx running on:" -ForegroundColor Yellow
    Write-Host "  HTTP:  http://localhost:80"
    Write-Host "  HTTPS: https://localhost:443"
    Write-Host ""

    # Show logs
    Write-Host "📋 Nginx Logs (last 10 lines):" -ForegroundColor Yellow
    docker-compose -f docker-compose.nginx-only.yml logs --tail=10 nginx
    Write-Host ""

    Write-Host "✅ All set! Nginx is ready to serve requests." -ForegroundColor Green
} else {
    Write-Host "❌ Failed to start Nginx" -ForegroundColor Red
    docker-compose -f docker-compose.nginx-only.yml logs nginx
    exit 1
}
```

Run: `powershell -ExecutionPolicy Bypass -File start-nginx.ps1`

---

## Verification Commands

### **Check if Nginx is Running**

```bash
# Docker status
docker ps | grep nginx

# Check port 80
curl http://localhost
# or
curl http://localhost:80

# Check port 443
curl -k https://localhost
# or
curl -k https://localhost:443

# Check using netstat/ss
ss -tuln | grep -E ':(80|443)'     # Linux
netstat -an | findstr ":80"         # Windows

# Check docker container logs
docker logs cirkle-nginx

# Check detailed container info
docker inspect cirkle-nginx
```

---

## Stop & Cleanup

```bash
# Stop just nginx
docker-compose -f docker-compose.nginx-only.yml down

# Stop all services
docker-compose down

# Stop and remove all data
docker-compose down -v

# Force remove container
docker rm -f cirkle-nginx

# Clean up unused images
docker image prune -a
```

---

## Health Check

```bash
# Check nginx health endpoint
curl http://localhost/health

# Check if responding to requests
curl -v http://localhost

# Check SSL certificate (if HTTPS)
curl -v --insecure https://localhost

# Test nginx configuration
docker exec cirkle-nginx nginx -t

# View nginx configuration inside container
docker exec cirkle-nginx cat /etc/nginx/nginx.conf
```

---

## Common Issues & Solutions

### **Nginx Container Won't Start**

```bash
# Check logs for errors
docker-compose logs nginx

# Test configuration
docker-compose run --rm nginx nginx -t

# Check if ports are in use
sudo lsof -i :80
sudo lsof -i :443
```

### **Port Already in Use**

```bash
# Kill process using port 80
sudo lsof -i :80 | grep LISTEN | awk '{print $2}' | xargs sudo kill -9

# Kill process using port 443
sudo lsof -i :443 | grep LISTEN | awk '{print $2}' | xargs sudo kill -9

# Or change ports in .env file
# NGINX_PORT=8080
# NGINX_HTTPS_PORT=8443
```

### **SSL Certificate Issues**

```bash
# Check certificate validity
docker exec cirkle-nginx openssl x509 -in /etc/letsencrypt/live/domain.com/cert.pem -text -noout

# Check certificate expiry
docker exec cirkle-nginx openssl x509 -in /etc/letsencrypt/live/domain.com/cert.pem -noout -dates

# Renew Let's Encrypt certificate
docker-compose -f docker-compose.nginx-only.yml run --rm certbot renew
```

---

## Configuration Testing

```bash
# Validate nginx configuration
docker exec cirkle-nginx nginx -t

# Reload configuration without restart
docker exec cirkle-nginx nginx -s reload

# View active connections
docker exec cirkle-nginx netstat -an | grep ESTABLISHED | wc -l

# Check nginx version
docker exec cirkle-nginx nginx -v
```

---

## Monitoring & Logs

```bash
# Real-time logs
docker-compose logs -f nginx

# Last 50 lines
docker-compose logs --tail=50 nginx

# Error logs only
docker logs cirkle-nginx 2>&1 | grep error

# Access logs
docker exec cirkle-nginx tail -f /var/log/nginx/access.log

# View all statistics
docker stats cirkle-nginx
```

---

## Full Startup with Status Report

Create `full-startup.sh`:

```bash
#!/bin/bash

set -e

echo ""
echo "╔════════════════════════════════════════╗"
echo "║  🚀 Cirkle Project Startup Script 🚀  ║"
echo "╚════════════════════════════════════════╝"
echo ""

cd "d:\Projects\cirkle-webs"

# Check prerequisites
echo "✓ Checking prerequisites..."
docker --version > /dev/null
docker-compose --version > /dev/null

echo "✓ Prerequisites OK"
echo ""

# Start services
echo "📦 Starting all services..."
docker-compose up -d
echo "✓ Services started"
echo ""

# Wait for services to be ready
echo "⏳ Waiting for services to be healthy (30 seconds)..."
sleep 30

# Health checks
echo ""
echo "🔍 Running health checks..."
echo ""

# Check nginx
echo -n "Nginx: "
if curl -sf http://localhost/health > /dev/null 2>&1; then
    echo "✅ Healthy"
else
    echo "⚠️  Check http://localhost"
fi

# Check admin
echo -n "Admin: "
if curl -sf http://localhost:3000 > /dev/null 2>&1; then
    echo "✅ Healthy"
else
    echo "⚠️  Check http://localhost:3000"
fi

# Check main web
echo -n "Main Web: "
if curl -sf http://localhost:3001 > /dev/null 2>&1; then
    echo "✅ Healthy"
else
    echo "⚠️  Check http://localhost:3001"
fi

echo ""
echo "╔════════════════════════════════════════╗"
echo "║         ✅ Startup Complete!          ║"
echo "╚════════════════════════════════════════╝"
echo ""
echo "📍 Services running at:"
echo "   Admin:     http://localhost:3000"
echo "   Main Web:  http://localhost:3001"
echo "   Nginx:     http://localhost:80 / https://localhost:443"
echo ""
echo "📋 Logs: docker-compose logs -f"
echo "⏹️  Stop:  docker-compose down"
echo ""
```

---

## Summary of Commands

| Task              | Command                                                 |
| ----------------- | ------------------------------------------------------- |
| **Start**         | `docker-compose -f docker-compose.nginx-only.yml up -d` |
| **Stop**          | `docker-compose -f docker-compose.nginx-only.yml down`  |
| **Status**        | `docker-compose ps`                                     |
| **Logs**          | `docker-compose logs -f nginx`                          |
| **Restart**       | `docker-compose restart nginx`                          |
| **Health**        | `curl http://localhost/health`                          |
| **Test Config**   | `docker exec cirkle-nginx nginx -t`                     |
| **Reload Config** | `docker exec cirkle-nginx nginx -s reload`              |

---

## Starting Instructions

### **Quick Start (One Command)**

```bash
# Windows
cd d:\Projects\cirkle-webs && docker-compose -f docker-compose.nginx-only.yml up -d

# Linux/Mac
cd d:/Projects/cirkle-webs && docker-compose -f docker-compose.nginx-only.yml up -d
```

### **Check Status**

```bash
docker-compose ps
```

### **View Logs**

```bash
docker-compose logs -f nginx
```

### **Access Services**

- **HTTP:** http://localhost
- **HTTPS:** https://localhost (self-signed, use -k flag with curl)
- **Health Check:** http://localhost/health

---

**Status: ✅ All commands ready to use**
