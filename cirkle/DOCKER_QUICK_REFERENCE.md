# Cirkle Docker Deployment - Quick Reference

## TL;DR - Deploy Each Project to Separate EC2 Instances

### Cirkle Admin (Instance #1)

```bash
cd cirkle-admin
nano .env.docker          # Update API URLs
./deploy.sh build         # or: deploy.bat build
# Available at: http://<EC2_IP>:3000
```

### Cirkle Web (Instance #2)

```bash
cd live-revenue-new
nano .env.docker          # Update API URLs
./deploy.sh build         # or: deploy.bat build
# Available at: http://<EC2_IP>:3001
```

## File Structure (Independent Projects)

```
cirkle-admin/                          live-revenue-new/
├── Dockerfile                         ├── Dockerfile
├── docker-compose.yml                 ├── docker-compose.yml
├── .env.docker                        ├── .env.docker
├── .env.production                    ├── .env.production
├── deploy.sh (Linux/Mac)              ├── deploy.sh (Linux/Mac)
├── deploy.bat (Windows)               ├── deploy.bat (Windows)
└── ...                                └── ...
```

## Key Points

✅ **Each project is self-contained** - Can be deployed independently  
✅ **Separate EC2 instances** - No single point of failure  
✅ **Environment variables** - Easy customization per instance  
✅ **Health checks** - Automatic monitoring included  
✅ **Easy scaling** - Scale each service independently

## Deployment Checklist

- [ ] EC2 Instance #1 created (for Cirkle Admin)
- [ ] EC2 Instance #2 created (for Cirkle Web) - or use same instance if preferred
- [ ] Docker & Docker Compose installed on each instance
- [ ] Clone/upload project code
- [ ] Update `.env.docker` with production API URLs
- [ ] Run `./deploy.sh build` (or `deploy.bat build`)
- [ ] Verify access via browser

## Environment Variables

### Cirkle Admin (.env.docker)

```env
NEXT_PUBLIC_API_BASE_URL=https://your-backend-api.com/api
NEXT_PUBLIC_ADMIN_API_BASE_URL=https://your-backend-api.com/api/admin
ADMIN_PORT=3000
```

### Cirkle Web (.env.docker)

```env
NEXT_PUBLIC_API_BASE_URL=https://your-backend-api.com/api/v1/liverevenue
WEB_PORT=3001
```

## Common Commands

```bash
# Deploy
./deploy.sh           # or: deploy.bat

# Build and deploy
./deploy.sh build     # or: deploy.bat build

# View logs
./deploy.sh logs      # or: deploy.bat logs

# Check status
./deploy.sh status    # or: docker-compose ps

# Restart
./deploy.sh restart   # or: deploy.bat restart

# Stop
./deploy.sh stop      # or: deploy.bat stop (then: docker-compose down)
```

## Documentation

- **[DOCKER_SETUP_README.md](./DOCKER_SETUP_README.md)** - Complete setup guide
- **[DOCKER_DEPLOYMENT_GUIDE.md](./DOCKER_DEPLOYMENT_GUIDE.md)** - Detailed deployment steps
- **[cirkle-admin/Dockerfile](./cirkle-admin/Dockerfile)** - Admin build config
- **[live-revenue-new/Dockerfile](./live-revenue-new/Dockerfile)** - Web build config

## Support

Need help? Check the detailed guides above or review Docker logs:

```bash
docker-compose logs -f
```
