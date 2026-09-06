# Full-Stack Portfolio Setup Guide

This is a modern, full-stack portfolio application built with **Next.js** (frontend), **Node.js/Express** (backend), and **MongoDB** (database).

## 📋 Table of Contents
- [Features](#features)
- [Setup Instructions](#setup-instructions)
- [Environment Variables](#environment-variables)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
- [Project Structure](#project-structure)

## ✨ Features

### Frontend
- Modern, responsive UI built with React & Tailwind CSS
- Display projects, experience, and education
- Beautiful cards with hover animations
- Fully functional admin panel to manage portfolio content

### Backend
- RESTful API with Express.js
- MongoDB integration with Mongoose
- CRUD operations for all portfolio sections
- Proper error handling and validation

### Admin Panel
- Add, edit, and delete projects
- Add, edit, and delete education records
- Add, edit, and delete work experience
- Real-time updates across frontend and backend

## 🚀 Setup Instructions

### Prerequisites
- Node.js (v18 or higher)
- MongoDB (local or Atlas)
- npm or yarn

### Step 1: Install Dependencies

```bash
npm install
```

### Step 2: Configure MongoDB

#### Option A: Using Local MongoDB
1. Install MongoDB from [mongodb.com](https://www.mongodb.com/try/download/community)
2. Start the MongoDB service
3. Keep the default connection string in `.env.local`: `mongodb://localhost:27017/portfolio`

#### Option B: Using MongoDB Atlas (Cloud)
1. Go to [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
2. Create a free account
3. Create a new cluster
4. Get your connection string
5. Replace the `MONGO_URI` in `.env.local` with your Atlas connection string

### Step 3: Update Environment Variables

Create a `.env.local` file in the root directory with:
```
# Database Configuration
MONGODB_URI=mongodb://localhost:27017/portfolio

# Email Configuration (Optional)
GMAIL_USER=your-email@gmail.com
GMAIL_PASSWORD=your-app-password

# Admin Email
ADMIN_EMAIL=your-admin-email@gmail.com

# API Configuration (Frontend)
NEXT_PUBLIC_API_URL=/api
```

**Note:** The `NEXT_PUBLIC_API_URL=/api` is the default and uses Next.js built-in API routes. You don't need a separate backend server.

## 🏃 Running the Application

### Development Mode

```bash
npm run dev
```

This will start the Next.js application with both frontend and API routes:
- **Portfolio Frontend**: http://localhost:3000
- **API Routes**: http://localhost:3000/api/...

### Production Build

```bash
npm run build
npm start
```

## 📡 API Endpoints

### Projects
- `GET /api/projects` - Get all projects
- `GET /api/projects/:id` - Get single project
- `POST /api/projects` - Create project
- `PUT /api/projects/:id` - Update project
- `DELETE /api/projects/:id` - Delete project

### Education
- `GET /api/education` - Get all education records
- `GET /api/education/:id` - Get single education record
- `POST /api/education` - Create education record
- `PUT /api/education/:id` - Update education record
- `DELETE /api/education/:id` - Delete education record

### Experience
- `GET /api/experience` - Get all experiences
- `GET /api/experience/:id` - Get single experience
- `POST /api/experience` - Create experience
- `PUT /api/experience/:id` - Update experience
- `DELETE /api/experience/:id` - Delete experience

### Health Check
- `GET /api/health` - Check if API is running

## 📁 Project Structure

```
shivam_portfolio/
├── server.js              # Express backend server
├── models/                # MongoDB schemas
│   ├── Education.js
│   ├── Project.js
│   └── Experience.js
├── lib/
│   └── db.js             # Database connection
├── src/
│   ├── app/
│   │   ├── page.tsx      # Main portfolio page
│   │   ├── admin/
│   │   │   └── page.tsx  # Admin dashboard
│   │   ├── layout.tsx
│   │   └── globals.css
│   └── components/       # React components
│       ├── Header.tsx
│       ├── ProjectCard.tsx
│       ├── ProjectForm.tsx
│       ├── EducationCard.tsx
│       ├── EducationForm.tsx
│       ├── ExperienceCard.tsx
│       └── ExperienceForm.tsx
├── .env.local            # Backend configuration
├── .env                  # Frontend configuration
└── package.json
```

## 🎨 Customization

### Styling
The project uses **Tailwind CSS** for styling. Customize colors and typography in:
- `tailwind.config.ts`
- `postcss.config.mjs`

### API URL
The API routes are built into Next.js and use relative paths (e.g., `/api/projects`, `/api/education`). 
To use a custom API URL in production, update `NEXT_PUBLIC_API_URL` in `.env.local`:
```
NEXT_PUBLIC_API_URL=https://your-api-domain.com
```

## 🔧 Build for Production

```bash
npm run build
npm start
```

## 📝 Notes

- The admin panel is accessible at `/admin`
- All data is persisted in MongoDB
- The API server is built-in with Next.js (no separate Express backend needed)
- All API routes use relative paths (e.g., `/api/projects`, `/api/education`)
- Ensure MongoDB is running before starting the application

## 🐛 Troubleshooting

### Port 3000 Already in Use
If port 3000 is already in use:
```bash
# Kill process on port 3000
lsof -ti:3000 | xargs kill -9
```

### MongoDB Connection Error
- Ensure MongoDB is running: `mongod`
- Check your connection string in `.env.local`
- For MongoDB Atlas, whitelist your IP address

### API Routes Not Responding
- Check if the dev server is running (`npm run dev`)
- Verify `.env.local` has correct `MONGODB_URI`
- Check browser console for CORS or fetch errors
- Ensure relative paths are used (e.g., `/api/projects`)
