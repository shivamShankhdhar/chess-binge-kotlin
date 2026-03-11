# Docker Deployment Guide for Cirkle

This guide covers deploying Cirkle Admin and Cirkle Web using Docker on **separate EC2 instances**.

## Deployment Architecture

Each project is **independently deployable** to its own EC2 instance:

```
┌─────────────────────────────┬──────────────────────────────┐
│   EC2 Instance #1           │   EC2 Instance #2            │
│   (Cirkle Admin)            │   (Cirkle Web)               │
├─────────────────────────────┼──────────────────────────────┤
│ Port: 3000 (default)        │ Port: 3001 (default)         │
│ API: admin endpoints        │ API: live revenue endpoints  │
│ Container: cirkle-admin     │ Container: cirkle-web        │
└─────────────────────────────┴──────────────────────────────┘
```

## Deployment Options

### Deploy Cirkle Admin to EC2 Instance #1

```bash
# Clone or pull the repo to EC2 Instance #1
cd cirkle-admin

# Using default environment variables
docker-compose up -d

# Using custom .env.docker file (recommended)
docker-compose --env-file .env.docker up -d

# Build and run
docker-compose up -d --build
```

Admin Panel will be available at: `http://<EC2_IP>:3000`

### Deploy Cirkle Web to EC2 Instance #2

```bash
# Clone or pull the repo to EC2 Instance #2
cd live-revenue-new

# Using default environment variables
docker-compose up -d

# Using custom .env.docker file (recommended)
docker-compose --env-file .env.docker up -d

# Build and run
docker-compose up -d --build
```

Web App will be available at: `http://<EC2_IP>:3001`

## Environment Variables

### Cirkle Admin (.env.docker)

Located in: `cirkle-admin/.env.docker`

```env
NEXT_PUBLIC_API_BASE_URL=https://www.api.cirkle.java.module.monolithic.cirkle.live/api
NEXT_PUBLIC_ADMIN_API_BASE_URL=https://www.api.cirkle.java.module.monolithic.cirkle.live/api/admin
NODE_ENV=production
ADMIN_PORT=3000
```

### Cirkle Web (.env.docker)

Located in: `live-revenue-new/.env.docker`

```env
NEXT_PUBLIC_API_BASE_URL=https://www.api.cirkle.java.module.monolithic.cirkle.live/api/v1/liverevenue
NODE_ENV=production
WEB_PORT=3001
```

## Customizing Deployment

### Step 1: Prepare EC2 Instances

For each EC2 instance, you need:

- Docker installed
- Docker Compose installed
- Project code (git clone or upload)

### Step 2: Update Environment Variables

#### For Cirkle Admin on EC2 Instance #1

```bash
cd cirkle-admin
nano .env.docker

# Update these variables:
NEXT_PUBLIC_API_BASE_URL=https://your-backend-api.com/api
NEXT_PUBLIC_ADMIN_API_BASE_URL=https://your-backend-api.com/api/admin
ADMIN_PORT=3000
```

#### For Cirkle Web on EC2 Instance #2

```bash
cd live-revenue-new
nano .env.docker

# Update these variables:
NEXT_PUBLIC_API_BASE_URL=https://your-backend-api.com/api/v1/liverevenue
WEB_PORT=3001
```

### Step 3: Deploy

```bash
# On both EC2 instances, after updating .env.docker:
docker-compose --env-file .env.docker up -d --build
```

### Changing API URLs

Edit `.env.docker` in the respective project:

```bash
# For cirkle-admin
cd cirkle-admin
nano .env.docker
# Update NEXT_PUBLIC_API_BASE_URL and NEXT_PUBLIC_ADMIN_API_BASE_URL

# For live-revenue-new
cd ../live-revenue-new
nano .env.docker
# Update NEXT_PUBLIC_API_BASE_URL
```

Then redeploy:

```bash
docker-compose --env-file .env.docker up -d --build
```

### Changing Port

Update the port in the `.env.docker` file of the respective project:

```env
# For Admin (default 3000)
ADMIN_PORT=8000

# For Web (default 3001)
WEB_PORT=8001
```

Then redeploy:

```bash
docker-compose --env-file .env.docker up -d --build
```

## Viewing Logs

```bash
# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f cirkle-admin
docker-compose logs -f cirkle-web

# View logs for a specific container
docker logs -f cirkle-admin
docker logs -f cirkle-web
```

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Restart services
docker-compose restart
```

## Troubleshooting

### Port Already in Use

If you get "port already in use" error, change the port in `.env.docker`:

```bash
# For Admin (default 3000)
ADMIN_PORT=3002

# For Web (default 3001)
WEB_PORT=3002
```

### Network Issues

All containers are connected via the `cirkle-network` bridge network. If services can't communicate:

```bash
# Check networks
docker network ls
docker network inspect cirkle-network

# Rebuild containers
docker-compose down
docker-compose up -d --build
```

### Environment Variables Not Applied

Make sure you're using `.env.docker` file:

```bash
docker-compose --env-file .env.docker up -d --build
```

Or rename `.env.docker` to `.env`:

```bash
cp .env.docker .env
docker-compose up -d --build
```

## Production Deployment

For production, consider:

1. **Update API URLs** in `.env.production` files to production API endpoints
2. **Use external network** instead of bridge for better performance
3. **Add reverse proxy** (Nginx/Apache) for routing
4. **Enable HTTPS** with Let's Encrypt
5. **Add health checks** (already included in docker-compose)
6. **Set resource limits** in docker-compose.yml

Example with Nginx:

```yaml
version: '3.9'

services:
  nginx:
    image: nginx:latest
    ports:
      - '80:80'
      - '443:443'
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./certs:/etc/nginx/certs:ro
    depends_on:
      - cirkle-admin
      - cirkle-web

  cirkle-admin:
    # ... (existing config)
    expose:
      - 3000 # Only expose internally

  cirkle-web:
    # ... (existing config)
    expose:
      - 3000 # Only expose internally
```

## Further Documentation

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Next.js Deployment](https://nextjs.org/docs/app/building-your-application/deploying)
- [Node.js Docker Best Practices](https://nodejs.org/en/docs/guides/nodejs-docker-webapp/)
