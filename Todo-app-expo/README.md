# Todo app

This repository contains two separate projects:

- `mobile-app/` — Expo / React Native application.
- `backend/` — Express API backed by MongoDB.

## Run locally

1. In `backend/.env`, paste your MongoDB connection string after `MONGODB_URI=` and set a strong `JWT_SECRET`.
2. In `mobile-app/.env`, set `EXPO_PUBLIC_API_URL` to the API route, for example `http://localhost:5001/api`. Use your computer's LAN IP instead of `localhost` when opening Expo Go on a phone. The local API uses port `5001`.
3. Run each project in its own terminal:

   ```bash
   cd backend && npm install && npm run dev
   ```

   ```bash
   cd mobile-app && npm install && npm start
   ```
