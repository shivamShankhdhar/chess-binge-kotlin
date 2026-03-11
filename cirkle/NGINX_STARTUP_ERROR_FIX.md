# Nginx Startup Errors - Troubleshooting & Solutions

**Date:** March 1, 2026
**Issue:** Nginx failing to start on Ubuntu EC2 instance

---

## 🔴 Errors Identified

### **Error 1: Port 80 Already in Use**

```
nginx: [emerg] bind() to 0.0.0.0:80 failed (98: Address already in use)
nginx: [emerg] bind() to [::]:80 failed (98: Address already in use)
```

### **Error 2: Permission Denied on Log File**

```
[emerg] 2814#2814: open() "/var/log/nginx/error.log" failed (13: Permission denied)
```

### **Error 3: User Directive Warning**

```
[warn] 2814#2814: the "user" directive makes sense only if the master process
runs with super-user privileges, ignored in /etc/nginx/nginx.conf:1
```

---

## ✅ Quick Fix Commands

Run these in order:

```bash
# 1. Find and kill process using port 80
sudo lsof -i :80
sudo fuser -k 80/tcp

# OR kill specific PID
sudo kill -9 <PID>

# 2. Find and kill process using port 443
sudo lsof -i :443
sudo fuser -k 443/tcp

# 3. Stop any running nginx instances
sudo systemctl stop nginx
sudo pkill -f nginx

# 4. Fix permissions on nginx directories
sudo chown -R root:root /var/log/nginx
sudo chmod -R 755 /var/log/nginx
sudo chown -R root:root /etc/nginx
sudo chmod -R 755 /etc/nginx

# 5. Test nginx configuration
sudo nginx -t

# 6. Start nginx via systemctl (with sudo!)
sudo systemctl start nginx

# 7. Verify it's running
sudo systemctl status nginx
```

---

## 🔧 Detailed Fixes

### **Step 1: Kill Process Using Port 80**

```bash
# Method 1: Using lsof
sudo lsof -i :80
# Output: Shows PID of process using port 80

# Kill that process
sudo kill -9 <PID>

# OR Method 2: Using fuser (faster)
sudo fuser -k 80/tcp

# OR Method 3: Using ss to find and kill
sudo ss -tlnp | grep :80
sudo kill -9 <PID>

# Verify port 80 is free
sudo ss -tlnp | grep :80
# (should have no output if free)
```

### **Step 2: Kill Process Using Port 443**

```bash
# Check if HTTPS port is in use
sudo lsof -i :443

# Kill if exists
sudo fuser -k 443/tcp
```

### **Step 3: Stop All Nginx Instances**

```bash
# Stop via systemctl
sudo systemctl stop nginx

# Force kill any remaining nginx processes
sudo pkill -f nginx

# Verify all stopped
ps aux | grep nginx
# (should only show your grep command)
```

### **Step 4: Fix Permissions**

```bash
# Fix /var/log/nginx directory
sudo mkdir -p /var/log/nginx
sudo chown -R root:root /var/log/nginx
sudo chmod -R 755 /var/log/nginx

# Fix /etc/nginx directory
sudo chown -R root:root /etc/nginx
sudo chmod -R 755 /etc/nginx

# Create empty error.log if doesn't exist
sudo touch /var/log/nginx/error.log
sudo chown root:root /var/log/nginx/error.log
sudo chmod 644 /var/log/nginx/error.log

# Create empty access.log if doesn't exist
sudo touch /var/log/nginx/access.log
sudo chown root:root /var/log/nginx/access.log
sudo chmod 644 /var/log/nginx/access.log
```

### **Step 5: Test Configuration**

```bash
# Test with sudo (important!)
sudo nginx -t

# Expected output:
# nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### **Step 6: Start Nginx Correctly**

```bash
# IMPORTANT: Use sudo with systemctl!
sudo systemctl start nginx

# NOT: nginx (this won't work for system-level operation)
```

### **Step 7: Verify Running**

```bash
# Check status
sudo systemctl status nginx

# Check listening ports
sudo ss -tlnp | grep nginx

# Expected:
# LISTEN 0 511 0.0.0.0:80 0.0.0.0:* users:(("nginx",pid=XXXX,fd=6))

# Test with curl
curl http://localhost
```

---

## 📋 Complete Fix Script

Save as `fix-nginx.sh`:

```bash
#!/bin/bash

set -e

echo "════════════════════════════════════════"
echo "  Nginx Emergency Fix & Startup Script"
echo "════════════════════════════════════════"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. Kill existing processes
echo -e "${YELLOW}Step 1: Killing existing nginx processes...${NC}"
sudo pkill -f nginx || true
sudo fuser -k 80/tcp 2>/dev/null || true
sudo fuser -k 443/tcp 2>/dev/null || true
sleep 2
echo -e "${GREEN}✓ Done${NC}"
echo ""

# 2. Stop systemctl service
echo -e "${YELLOW}Step 2: Stopping nginx service...${NC}"
sudo systemctl stop nginx 2>/dev/null || true
sleep 1
echo -e "${GREEN}✓ Done${NC}"
echo ""

# 3. Fix permissions
echo -e "${YELLOW}Step 3: Fixing permissions...${NC}"
sudo mkdir -p /var/log/nginx
sudo mkdir -p /etc/nginx
sudo chown -R root:root /var/log/nginx
sudo chmod -R 755 /var/log/nginx
sudo chown -R root:root /etc/nginx
sudo chmod -R 755 /etc/nginx
sudo touch /var/log/nginx/error.log /var/log/nginx/access.log
sudo chown root:root /var/log/nginx/error.log /var/log/nginx/access.log
sudo chmod 644 /var/log/nginx/error.log /var/log/nginx/access.log
echo -e "${GREEN}✓ Done${NC}"
echo ""

# 4. Test configuration
echo -e "${YELLOW}Step 4: Testing configuration...${NC}"
if sudo nginx -t 2>&1 | grep -q "successful"; then
    echo -e "${GREEN}✓ Configuration OK${NC}"
else
    echo -e "${RED}✗ Configuration test failed!${NC}"
    sudo nginx -t
    exit 1
fi
echo ""

# 5. Start nginx
echo -e "${YELLOW}Step 5: Starting nginx service...${NC}"
sudo systemctl start nginx
sleep 2

if sudo systemctl is-active nginx > /dev/null; then
    echo -e "${GREEN}✓ Nginx started successfully${NC}"
else
    echo -e "${RED}✗ Failed to start nginx${NC}"
    sudo systemctl status nginx
    exit 1
fi
echo ""

# 6. Enable on boot
echo -e "${YELLOW}Step 6: Enabling on boot...${NC}"
sudo systemctl enable nginx
echo -e "${GREEN}✓ Done${NC}"
echo ""

# 7. Verify
echo -e "${YELLOW}Step 7: Verification...${NC}"
echo "Status:"
sudo systemctl status nginx --no-pager | head -10
echo ""

echo "Listening ports:"
sudo ss -tlnp | grep nginx
echo ""

echo "Testing HTTP:"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost || echo "Error connecting"
echo ""

echo "════════════════════════════════════════"
echo -e "${GREEN}✅ Nginx is now running!${NC}"
echo "════════════════════════════════════════"
echo ""
echo "Logs:"
echo "  Error: sudo tail -f /var/log/nginx/error.log"
echo "  Access: sudo tail -f /var/log/nginx/access.log"
echo ""
```

Make executable and run:

```bash
chmod +x fix-nginx.sh
./fix-nginx.sh
```

---

## 🚨 Your Specific Issues - Detailed Solutions

### **Issue 1: "Address already in use" (Port 80 Conflict)**

**Root Cause:** Another process is listening on port 80

**Solution:**

```bash
# Step 1: Identify what's using port 80
sudo lsof -i :80
# OR
sudo ss -tlnp | grep :80

# Step 2: Kill the process
sudo kill -9 <PID>
# OR
sudo fuser -k 80/tcp

# Step 3: Verify port is free
sudo ss -tlnp | grep :80
# (should have no output)

# Step 4: Start nginx
sudo systemctl start nginx
```

---

### **Issue 2: "Permission denied" on /var/log/nginx/error.log**

**Root Cause:** Incorrect permissions on nginx log directory

**Solution:**

```bash
# Fix ownership and permissions
sudo chown -R root:root /var/log/nginx
sudo chmod -R 755 /var/log/nginx

# Verify permissions
ls -la /var/log/nginx/
# Should show: drwxr-xr-x root root

# Create log files if missing
sudo touch /var/log/nginx/error.log
sudo touch /var/log/nginx/access.log

# Verify
ls -la /var/log/nginx/
```

---

### **Issue 3: "user" directive warning**

**Root Cause:** nginx config has `user` directive but running as non-root

**Solution:**

```bash
# ALWAYS use sudo when running nginx!
sudo systemctl start nginx
# NOT: nginx

# OR edit nginx.conf to comment out user directive
sudo nano /etc/nginx/nginx.conf
# Comment line: # user nginx;

# Then test
sudo nginx -t
```

---

## 🔄 Complete Startup Procedure (Ubuntu/EC2)

```bash
# 1. SSH into server
ssh -i key.pem ubuntu@your-server-ip

# 2. Navigate to project
cd ~/cirkle-webs

# 3. Kill any existing processes on ports 80 & 443
sudo fuser -k 80/tcp 2>/dev/null || true
sudo fuser -k 443/tcp 2>/dev/null || true
sudo pkill -f nginx || true

# 4. Stop systemctl nginx
sudo systemctl stop nginx 2>/dev/null || true

# 5. Wait a moment
sleep 2

# 6. Fix permissions
sudo chown -R root:root /var/log/nginx /etc/nginx
sudo chmod -R 755 /var/log/nginx /etc/nginx

# 7. Test configuration
sudo nginx -t

# 8. Start nginx
sudo systemctl start nginx

# 9. Enable on boot
sudo systemctl enable nginx

# 10. Verify
sudo systemctl status nginx
sudo ss -tlnp | grep nginx

# 11. Test connectivity
curl http://localhost
```

---

## 🔍 Verification Commands

```bash
# Check if running
sudo systemctl is-active nginx
# Output: active or inactive

# Check if enabled on boot
sudo systemctl is-enabled nginx
# Output: enabled or disabled

# Check listening ports
sudo ss -tlnp | grep nginx

# Check process
ps aux | grep nginx

# Check recent logs
sudo journalctl -u nginx -n 20

# Test HTTP
curl -v http://localhost

# Full status
sudo systemctl status nginx -l
```

---

## 🐛 If Still Failing

Run these diagnostic commands:

```bash
# Check what's really listening on port 80
sudo netstat -tlnp | grep :80
sudo lsof -i :80

# Check nginx config syntax
sudo nginx -T
# (capital T shows full config)

# Try starting manually to see error
sudo /usr/sbin/nginx

# Check systemd journal for detailed errors
sudo journalctl -u nginx.service -n 50 --no-pager

# Check if nginx user exists
id nginx
getent passwd nginx

# Check /etc/nginx permissions
ls -la /etc/nginx/

# Check /var/log/nginx permissions
ls -la /var/log/nginx/
```

---

## 📝 Prevention - Systemctl Best Practices

```bash
# ✅ DO: Always use sudo with systemctl
sudo systemctl start nginx
sudo systemctl stop nginx
sudo systemctl restart nginx

# ❌ DON'T: Use manual nginx command
nginx          # Wrong! Permission issues
nginx -s quit  # Wrong! Won't work for daemon

# ✅ DO: Use systemctl for all operations
sudo systemctl reload nginx     # Reload config
sudo systemctl status nginx     # Check status
sudo systemctl enable nginx     # Enable on boot

# ❌ DON'T: Mix methods
# Don't use both: nginx command AND systemctl
```

---

## Summary

Your errors are caused by:

1. **Port 80 already in use** → Kill that process
2. **Permission denied on log files** → Fix permissions with chown/chmod
3. **Not using sudo** → Always use `sudo systemctl`

**Quick Fix (Copy & Paste):**

```bash
sudo pkill -f nginx || true
sudo systemctl stop nginx 2>/dev/null || true
sudo fuser -k 80/tcp 2>/dev/null || true
sudo fuser -k 443/tcp 2>/dev/null || true
sudo chown -R root:root /var/log/nginx /etc/nginx
sudo chmod -R 755 /var/log/nginx /etc/nginx
sudo nginx -t && sudo systemctl start nginx && sudo systemctl enable nginx
```

**Verify:**

```bash
sudo systemctl status nginx
sudo ss -tlnp | grep nginx
curl http://localhost
```

---

**Status: ✅ Solutions provided - Ready to implement!**
