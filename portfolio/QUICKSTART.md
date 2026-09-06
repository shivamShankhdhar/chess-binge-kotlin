# 🚀 Quick Start Guide

Get your portfolio up and running in 5 minutes!

## Step 1: Install Dependencies
```bash
npm install
```

## Step 2: Set Up MongoDB

### If you have MongoDB installed locally:
Skip this step, it will connect to `localhost:27017` by default.

### If you want to use MongoDB Atlas (Cloud):
1. Go to [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
2. Sign up and create a free cluster
3. Get your connection string (it looks like: `mongodb+srv://username:password@cluster.mongodb.net/database`)
4. Open `.env.local` file and replace:
   ```
   MONGO_URI=your_connection_string_here
   ```

## Step 3: Run the Application

```bash
npm run dev
```

This starts the Next.js application with both frontend and API routes:
- **Portfolio**: http://localhost:3000
- **API Routes**: http://localhost:3000/api/...

## Step 4: Access the Admin Panel

1. Go to http://localhost:3000
2. Click "Login" button in the top right
3. Enter your email to receive an OTP
4. Verify OTP and access the admin panel
5. Start adding your projects, experience, and education!

## What You Can Do

### In Admin Panel:
✅ Add a new project with title, description, technologies, links
✅ Add education records with school, degree, GPA
✅ Add work experience with company and position
✅ Edit any of your content
✅ Delete entries you no longer need

### On the Portfolio Page:
✅ All your content displays beautifully
✅ Projects show with tags and links
✅ Experience shows with current role badge
✅ Education shows with dates and GPA
✅ Modern gradient UI with smooth animations

## 📝 Example Data to Add

### Project Example:
- Title: My Awesome App
- Description: A web app built with React and Node.js
- Technologies: React, Node.js, MongoDB
- Link: https://myapp.com
- Date: 2024-01-01 to 2024-03-01

### Experience Example:
- Company: Tech Company
- Position: Full Stack Developer
- Description: Developed web applications and APIs
- Technologies: JavaScript, React, Node.js
- Date: 2023-06-01 to Present

### Education Example:
- School: University Name
- Degree: Bachelor
- Field: Computer Science
- GPA: 3.8
- Date: 2020-09-01 to 2024-05-01

## 🎨 Customize Your Portfolio

- Change colors in `tailwind.config.ts`
- Update footer links in `src/app/page.tsx`
- Modify hero text in the main portfolio page
- Add your social media links

## ⚠️ Troubleshooting

### "Cannot connect to MongoDB"
- Make sure MongoDB is running locally, or
- Update the MONGO_URI in `.env.local` with your Atlas connection string

### "Port 3000/5000 already in use"
```bash
# Kill port 3000
lsof -ti:3000 | xargs kill -9
```

### "API not responding"
- Check if dev server is running: `npm run dev`
- Verify MongoDB is connected
- Check browser console for errors
- Ensure `.env.local` has correct `MONGODB_URI`

---

**That's it! Your full-stack portfolio is ready! 🎉**
