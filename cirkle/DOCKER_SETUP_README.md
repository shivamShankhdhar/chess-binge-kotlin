# Cirkle - Docker Deployment Setup

This repository contains two independent Next.js applications that are deployed to **separate EC2 instances** using Docker and Docker Compose.

## Projects

### 1. **Cirkle Admin** (`cirkle-admin/`)

- **Purpose**: Admin panel for managing events, users, and live revenue
- **Default Port**: 3000
- **EC2 Instance**: Instance #1 (or your choice)

### 2. **Cirkle Web** (`live-revenue-new/`)

- **Purpose**: Live revenue dashboard for event monitoring
- **Default Port**: 3001
- **EC2 Instance**: Instance #2 (or your choice)

## Quick Start

### Deploy Cirkle Admin

```bash
cd cirkle-admin

# Option 1: Using deployment script
./deploy.sh       # Linux/Mac
deploy.bat        # Windows

# Option 2: Using docker-compose directly
docker-compose --env-file .env.docker up -d --build

# Or with default environment
docker-compose up -d --build
```

Open: `http://localhost:3000`

### Deploy Cirkle Web

```bash
cd live-revenue-new

# Option 1: Using deployment script
./deploy.sh       # Linux/Mac
deploy.bat        # Windows

# Option 2: Using docker-compose directly
docker-compose --env-file .env.docker up -d --build

# Or with default environment
docker-compose up -d --build
```

Open: `http://localhost:3001`

## Project Structure

```
cirkle-admin/
├── Dockerfile                 # Multi-stage build
├── docker-compose.yml         # Docker Compose config
├── .env.production           # Production environment
├── .env.docker              # Docker environment (customize here)
├── deploy.bat               # Windows deployment script
├── deploy.sh                # Linux/Mac deployment script
├── package.json
├── next.config.ts
└── src/

live-revenue-new/
├── Dockerfile               # Multi-stage build
├── docker-compose.yml       # Docker Compose config
├── .env.production         # Production environment
├── .env.docker            # Docker environment (customize here)
├── deploy.bat             # Windows deployment script
├── deploy.sh              # Linux/Mac deployment script
├── package.json
├── next.config.ts
└── src/
```

## Environment Variables

Each project has its own `.env.docker` file for configuration:

### Cirkle Admin

```env
NEXT_PUBLIC_API_BASE_URL=https://your-backend-api.com/api
NEXT_PUBLIC_ADMIN_API_BASE_URL=https://your-backend-api.com/api/admin
NODE_ENV=production
ADMIN_PORT=3000
```

### Cirkle Web

```env
NEXT_PUBLIC_API_BASE_URL=https://your-backend-api.com/api/v1/liverevenue
NODE_ENV=production
WEB_PORT=3001
```

## Deployment to EC2

### Prerequisites

```bash
# Update system
sudo yum update -y          # Amazon Linux
sudo apt update && apt upgrade -y  # Ubuntu

# Install Docker
sudo yum install docker -y  # Amazon Linux
sudo apt install docker.io -y  # Ubuntu

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Start Docker
sudo systemctl start docker
sudo systemctl enable docker

# (Optional) Add user to docker group
sudo usermod -aG docker $USER
newgrp docker
```

### Deploy Steps

**For each EC2 instance:**

1. **SSH into EC2**

   ```bash
   ssh -i your-key.pem ec2-user@your-ec2-ip
   ```

2. **Clone or upload project**

   ```bash
   git clone <repository-url> cirkle-admin
   cd cirkle-admin
   ```

3. **Update environment variables**

   ```bash
   nano .env.docker
   # Edit API URLs to your production backend
   ```

4. **Deploy**

   ```bash
   # Using script
   chmod +x deploy.sh
   ./deploy.sh build

   # Or using docker-compose
   docker-compose --env-file .env.docker up -d --build
   ```

5. **Verify**
   ```bash
   docker-compose ps
   docker-compose logs -f
   ```

## Docker Commands

### Common Commands

```bash
# Deploy
docker-compose --env-file .env.docker up -d --build

# View status
docker-compose ps

# View logs
docker-compose logs -f

# Restart
docker-compose restart

# Stop
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Deployment Scripts

Each project includes convenient deployment scripts:

```bash
# Deploy (default: docker-compose up -d)
./deploy.sh

# Build and deploy
./deploy.sh build

# View logs
./deploy.sh logs

# Restart service
./deploy.sh restart

# Check status
./deploy.sh status

# Stop service
./deploy.sh stop
```

## Monitoring

```bash
# Check container status
docker-compose ps

# View logs
docker-compose logs

# View logs with timestamps
docker-compose logs --timestamps

# Follow logs
docker-compose logs -f

# Check container resource usage
docker stats

# Inspect specific container
docker inspect cirkle-admin
```

## Troubleshooting

### Port Already in Use

Change the port in `.env.docker`:

```env
ADMIN_PORT=3002  # Change from 3000
WEB_PORT=3002    # Change from 3001
```

Then redeploy:

```bash
docker-compose --env-file .env.docker up -d --build
```

### API Connection Failed

Verify API URL in container:

```bash
docker-compose exec cirkle-admin env | grep API
docker-compose exec cirkle-web env | grep API
```

If incorrect, update `.env.docker` and redeploy.

### Rebuild Image

```bash
docker-compose build --no-cache
docker-compose up -d
```

## Production Best Practices

- [ ] Update `.env.docker` with production API URLs
- [ ] Set `NODE_ENV=production`
- [ ] Configure EC2 security groups/firewall
- [ ] Monitor disk space: `docker system df`
- [ ] Set up log rotation
- [ ] Enable CloudWatch monitoring
- [ ] Configure auto-scaling if needed
- [ ] Use HTTPS with valid SSL certificates
- [ ] Regular backups of configuration

## Documentation

- [Full Deployment Guide](DOCKER_DEPLOYMENT_GUIDE.md)
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Next.js Deployment](https://nextjs.org/docs/app/building-your-application/deploying)

## Architecture

```
┌──────────────────────────────────┬──────────────────────────────────┐
│    EC2 Instance #1               │    EC2 Instance #2               │
│    (Cirkle Admin)                │    (Cirkle Web)                  │
├──────────────────────────────────┼──────────────────────────────────┤
│ Port: 3000                       │ Port: 3001                       │
│ Container: cirkle-admin          │ Container: cirkle-web            │
│ API: /api/admin                  │ API: /api/v1/liverevenue         │
│ Region: Your choice              │ Region: Your choice              │
│ Auto-scaling: Optional           │ Auto-scaling: Optional           │
└──────────────────────────────────┴──────────────────────────────────┘
                  ↓                                  ↓
         ┌─────────────────────────────────────────┐
         │   Backend API (Separate EC2/RDS)        │
         │   https://api.example.com               │
         └─────────────────────────────────────────┘
```

## Support

For issues or questions about deployment:

1. Check [DOCKER_DEPLOYMENT_GUIDE.md](DOCKER_DEPLOYMENT_GUIDE.md)
2. Review Docker logs: `docker-compose logs`
3. Verify environment variables: `docker-compose exec <service> env`
4. Check container health: `docker-compose ps`
