# QR & Phone Login Flow Diagrams

## 1. QR LOGIN FLOW (Wallet-Based)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    VOLUNTEER MOBILE APP                              │
│                                                                      │
│  1. User Scans QR Code at Event Check-in                           │
│     ↓                                                                │
│  2. Extract Raw QR String                                           │
│     └─→ "USER_ID::WALLET_ID::TIMESTAMP_HASH"                       │
│     ↓                                                                │
│  3. Call: POST /api/volunteer/login-qr                             │
│     ═══════════════════════════════════════════════════════════════│
│     Request Body:                                                   │
│     {                                                              │
│       "qrString": "USER_99::WAL_88::1708616....hash"              │
│     }                                                              │
│     ↓                                                                │
└─────────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    BACKEND PROCESSING                                │
│                                                                      │
│  VolunteerService.loginWithQr()                                    │
│  ═══════════════════════════════════════════════════════════════════│
│                                                                      │
│  STEP 1: Search Wallet by QR String                                │
│  ┌──────────────────────────────────────┐                         │
│  │ walletRepository.findByQrString(qr)  │                         │
│  │ ↓                                     │                         │
│  │ SQL: SELECT * FROM wallets           │                         │
│  │      WHERE qr_string = ?             │                         │
│  │ ↓                                     │                         │
│  │ Result: Wallet {                     │                         │
│  │   id: "wallet_88",                   │                         │
│  │   userId: "user_99",                 │                         │
│  │   balance: 1500.00,                  │                         │
│  │   eventId: "event_10"                │                         │
│  │ }                                     │                         │
│  └──────────────────────────────────────┘                         │
│     ↓                                                                │
│  STEP 2: Verify User Exists                                        │
│  ┌──────────────────────────────┐                                 │
│  │ userRepository.findById(userId)     │                          │
│  │ ↓                             │                                 │
│  │ SQL: SELECT * FROM users     │                                 │
│  │      WHERE user_id = "user_99"      │                          │
│  │ ↓                             │                                 │
│  │ Result: User {               │                                 │
│  │   userId: "user_99",         │                                 │
│  │   name: "John Volunteer",    │                                 │
│  │   phone: "+919876543210",    │                                 │
│  │   email: "john@cirkle.app"   │                                 │
│  │ }                             │                                 │
│  └──────────────────────────────┘                                 │
│     ↓                                                                │
│  STEP 3: Generate JWT Tokens                                       │
│  ┌──────────────────────────────┐                                 │
│  │ jwtTokenProvider.generateToken()    │                          │
│  │ ↓                             │                                 │
│  │ Access Token (15 min):       │                                 │
│  │ eyJhbGciOiJIUzI1NiIsInR5cCI...     │                          │
│  │                               │                                 │
│  │ Refresh Token (7 days):      │                                 │
│  │ eyJhbGciOiJIUzI1NiIsInR5cCI...     │                          │
│  └──────────────────────────────┘                                 │
│     ↓                                                                │
│  STEP 4: Build Response                                            │
│  {                                                                 │
│    "success": true,                                              │
│    "userId": "user_99",                                          │
│    "walletId": "wallet_88",                                      │
│    "balance": 1500.00,                                           │
│    "userName": "John Volunteer",                                 │
│    "phone": "+919876543210",                                     │
│    "email": "john@cirkle.app",                                   │
│    "eventId": "event_10",                                        │
│    "token": "eyJhbGc...",          ← 15 min expiry              │
│    "refreshToken": "eyJhbGc...",   ← 7 days expiry             │
│    "message": "QR login successful"                              │
│  }                                                                │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────────┐
│              MOBILE APP - STORE & AUTHENTICATE                       │
│                                                                      │
│  1. Store Tokens in AsyncStorage                                   │
│     ↓                                                                │
│     await AsyncStorage.setItem('auth_token', token)               │
│     await AsyncStorage.setItem('refresh_token', refreshToken)     │
│     ↓                                                                │
│  2. Store User Data in VolunteerContext                            │
│     ↓                                                                │
│     await setVolunteerData({                                       │
│       userId: "user_99",                                          │
│       walletId: "wallet_88",                                      │
│       token: "eyJhbGc...",                                        │
│       refreshToken: "eyJhbGc...",                                 │
│       walletBalance: 1500.00,                                     │
│       ...                                                          │
│     })                                                             │
│     ↓                                                                │
│  3. Schedule Token Refresh                                         │
│     ↓                                                                │
│     scheduleTokenRefresh(tokenData)                               │
│     └─→ Refresh after 14 minutes (before 15-min expiry)           │
│     ↓                                                                │
│  4. Navigate to App Home                                           │
│     ↓                                                                │
│     router.replace('/event')                                       │
│                                                                      │
│  ✅ USER IS NOW AUTHENTICATED                                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. PHONE LOGIN FLOW (TopUp User - FirstTime vs Returning)

### Case A: User Has Existing Wallet

```
┌─────────────────────────────────────────────────────────────────────┐
│                   TOPUP USER INPUT                                   │
│                                                                      │
│  1. Enter Phone Number: "+919876543210"                            │
│  2. Call: POST /api/volunteer/login-phone                          │
│     ═══════════════════════════════════════════════════════════════│
│     Request Body:                                                   │
│     { "phone": "+919876543210" }                                   │
│     ↓                                                                │
└─────────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────────┐
│              BACKEND - VolunteerService.loginWithPhone()            │
│                                                                      │
│  STEP 1: Find User by Phone                                        │
│  ┌────────────────────────────────┐                               │
│  │ userRepository.findByPhone("+919876543210")                     │
│  │ ↓                               │                               │
│  │ SQL: SELECT * FROM users       │                               │
│  │      WHERE phone = "+919876543210"                              │
│  │ ↓                               │                               │
│  │ Result: User Found ✓           │                               │
│  │ {                               │                               │
│  │   userId: "user_99",           │                               │
│  │   name: "John Staff",          │                               │
│  │   phone: "+919876543210"       │                               │
│  │ }                               │                               │
│  └────────────────────────────────┘                               │
│     ↓                                                                │
│  STEP 2: Find Existing Wallet                                      │
│  ┌────────────────────────────────┐                               │
│  │ walletRepository.findByUserId("user_99")                        │
│  │ ↓                               │                               │
│  │ SQL: SELECT * FROM wallets     │                               │
│  │      WHERE user_id = "user_99" │                               │
│  │ ↓                               │                               │
│  │ Result: Found Wallet!          │                               │
│  │ {                               │                               │
│  │   id: "wallet_88",             │                               │
│  │   userId: "user_99",           │                               │
│  │   balance: 500.00,             │                               │
│  │   eventId: "GENERAL"           │                               │
│  │ }                               │                               │
│  └────────────────────────────────┘                               │
│     ↓                                                                │
│  STEP 3: Generate Tokens & Return Full Info                       │
│  {                                                                 │
│    "success": true,                                              │
│    "userId": "user_99",                                          │
│    "walletId": "wallet_88",         ← WALLET EXISTS              │
│    "balance": 500.00,               ← HAS BALANCE               │
│    "userName": "John Staff",                                      │
│    "token": "eyJhbGc...",                                         │
│    "refreshToken": "eyJhbGc...",                                  │
│    "message": "Login successful"     ← USER CAN PROCEED          │
│  }                                                                │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Case B: User's First Time (No Wallet)

```
┌─────────────────────────────────────────────────────────────────────┐
│                FIRST TIME TOPUP USER LOGIN                          │
│                                                                      │
│  1. Phone: "+919876543289" (NEW USER)                              │
│  2. Call: POST /api/volunteer/login-phone                          │
│     ↓                                                                │
└─────────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────────┐
│           BACKEND - VolunteerService.loginWithPhone()               │
│                                                                      │
│  STEP 1: Find User by Phone                                        │
│  ┌────────────────────────────────┐                               │
│  │ userRepository.findByPhone("+919876543289")                     │
│  │ ↓                               │                               │
│  │ Result: User Found ✓           │                               │
│  │ { userId: "user_100", ... }   │                               │
│  └────────────────────────────────┘                               │
│     ↓                                                                │
│  STEP 2: Find Wallet                                               │
│  ┌────────────────────────────────┐                               │
│  │ walletRepository.findByUserId("user_100")                       │
│  │ ↓                               │                               │
│  │ SQL: SELECT * FROM wallets     │                               │
│  │      WHERE user_id = "user_100"│                               │
│  │ ↓                               │                               │
│  │ Result: EMPTY - No Wallet Yet  │                               │
│  │ wallets = []                    │                               │
│  └────────────────────────────────┘                               │
│     ↓                                                                │
│  STEP 3: Generate Tokens & Return Partial Info                    │
│  {                                                                 │
│    "success": true,                                              │
│    "userId": "user_100",                                         │
│    "walletId": null,                ← NO WALLET YET             │
│    "balance": null,                 ← NO BALANCE YET            │
│    "userName": "Jane NewStaff",                                   │
│    "token": "eyJhbGc...",                                         │
│    "refreshToken": "eyJhbGc...",                                  │
│    "message": "First topup - wallet will be created"              │
│  }                                                                │
│                                                                      │
│  ⚠️ NOTE: User can still login, but must create wallet on first   │
│           topup transaction                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────────┐
│                   TOPUP PROCESS (First Transaction)                │
│                                                                      │
│  User selects amount and payment method                            │
│  ↓                                                                   │
│  BACKEND: When topup transaction completes:                        │
│  ↓                                                                   │
│  1. Check if wallet exists                                         │
│     if (wallet == null) {                                          │
│       wallet = create new wallet                                   │
│       wallet.setUserId(userId)                                     │
│       wallet.setBalance(amountToppedup)                            │
│       walletRepository.save(wallet)                                │
│     }                                                               │
│     else {                                                          │
│       wallet.setBalance(wallet.getBalance() + amount)              │
│       walletRepository.save(wallet)                                │
│     }                                                               │
│     ↓                                                                │
│  2. Create Transaction Record                                      │
│     VolunteerWalletTransaction {                                   │
│       walletId: wallet.getId(),                                    │
│       userId: "user_100",                                          │
│       topupUserId: "topup_user_50",   ← WHO PERFORMED IT          │
│       amount: 500.00,                                              │
│       paymentMethod: "CASH" or "UPI", ← MODE                      │
│       status: "SUCCESS"                                            │
│     }                                                               │
│     ↓                                                                │
│  3. Update TopupBalance                                            │
│     Update topup_balances                                          │
│       SET available_balance = 500.00                               │
│     WHERE user_id = "user_100"                                     │
│     ↓                                                                │
│  ✅ WALLET CREATED & FUNDS ADDED                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. TOKEN REFRESH FLOW (When Token Expires)

```
┌──────────────────────────────────────────┐
│  User Makes API Call                     │
│  (15 min from login)                     │
│  ↓                                        │
│  Are we close to expiry?                 │
│  YES → Proactive refresh triggered       │
│  ↓                                        │
└──────────────────────────────────────────┘
              ↓
┌──────────────────────────────────────────────────────────┐
│  Proactive Token Refresh                                 │
│                                                          │
│  Check: Is token expiring in next 30 min?              │
│  IF YES:                                                 │
│    POST /api/volunteer/refresh-token                   │
│    {                                                    │
│      "userId": "user_99",                              │
│      "refreshToken": "eyJhbGc..."                      │
│    }                                                    │
│    ↓                                                     │
│    Backend Validates Refresh Token                     │
│    ↓                                                     │
│    Response:                                            │
│    {                                                    │
│      "token": "NEW_eyJhbGc",   ← 15 min from now       │
│      "refreshToken": "NEW_eyJhbGc"  ← Still 7 days   │
│    }                                                    │
│    ↓                                                     │
│    Store New Tokens in AsyncStorage                    │
│    ↓                                                     │
│    Schedule Next Refresh (14 min from now)            │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 4. SESSION EXPIRED ERROR FLOW (What Goes Wrong)

```
┌─────────────────────────────────────────────────────────┐
│  ❌ SCENARIO 1: Empty String Token                      │
│                                                         │
│  Login Response:                                        │
│  {                                                      │
│    "success": true,                                    │
│    "token": "",  ← EMPTY!                              │
│    "refreshToken": ""  ← EMPTY!                        │
│  }                                                      │
│  ↓                                                       │
│  setVolunteerData({ token: "" })                       │
│  ↓                                                       │
│  AsyncStorage saved: ""                                │
│  ↓                                                       │
│  Next API call:                                        │
│  Authorization: Bearer ""  ← INVALID!                  │
│  ↓                                                       │
│  Backend: 401 Unauthorized                            │
│  Client: "Session expired. Please login again"         │
│                                                         │
│  FIX: Validate tokens before setVolunteerData          │
│  ↓                                                       │
│  if (!token || token.trim() === '') {                  │
│    throw new Error('Invalid token')                    │
│  }                                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  ❌ SCENARIO 2: Token Not Stored                        │
│                                                         │
│  setVolunteerData({ token: "valid_token" })           │
│  ↓                                                       │
│  await AsyncStorage.setItem('auth_token', token)       │
│       ↓ FAILS (storage full, permission issue)        │
│  Stored in context but NOT in persistent storage      │
│  ↓                                                       │
│  User closes app → Context lost                        │
│  ↓                                                       │
│  App restarts → Load from AsyncStorage                │
│  ↓                                                       │
│  No token found → "Session expired"                    │
│                                                         │
│  FIX: Check AsyncStorage operations                    │
│  ↓                                                       │
│  const stored = await AsyncStorage.getItem('auth_token')
│  if (!stored) {                                        │
│    console.error('Token storage failed')              │
│  }                                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  ❌ SCENARIO 3: Token Not Sent in Requests            │
│                                                         │
│  Token stored OK: ✓                                   │
│  ↓                                                       │
│  API call made:                                        │
│  GET /api/volunteer/event-details                     │
│  Headers: {                                            │
│    'Content-Type': 'application/json'                 │
│    // Missing: Authorization header!                  │
│  }                                                      │
│  ↓                                                       │
│  Backend: 401 - No token provided                     │
│  Client: "Session expired"                             │
│                                                         │
│  FIX: Check token-refresh-interceptor                 │
│  ↓                                                       │
│  Ensure interceptor adds Authorization header         │
│  const token = await AsyncStorage.getItem('auth_token')
│  if (token) {                                          │
│    config.headers.Authorization = `Bearer ${token}`   │
│  }                                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  ❌ SCENARIO 4: Cascading Refresh Loop                 │
│                                                         │
│  Fresh token after login: 15 min expiry               │
│  ↓                                                       │
│  Refresh logic checks: 15 - 12 hours = NEGATIVE       │
│  ↓                                                       │
│  scheduleTime = Math.max(0, negative) = 0            │
│  ↓                                                       │
│  Refresh scheduled immediately!                        │
│  ↓                                                       │
│  Refresh completes → New 15 min token                 │
│  ↓                                                       │
│  Immediately refresh again → Loop!                     │
│  ↓                                                       │
│  After many loops → Rate limit → 401                  │
│  Client: "Session expired"                             │
│                                                         │
│  FIX: Set minimum threshold                           │
│  ↓                                                       │
│  const MINIMUM_THRESHOLD = 30  // minutes            │
│  if (timeRemaining < MINIMUM_THRESHOLD) {            │
│    return  // Skip refresh for short-lived tokens    │
│  }                                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 5. COMPLETE REQUEST LIFECYCLE

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  1. QR/PHONE LOGIN                                                  │
│     └─→ Get tokens & wallet info                                    │
│                                                                      │
│  2. STORE & CONTEXT                                                 │
│     └─→ AsyncStorage + VolunteerContext                            │
│                                                                      │
│  3. SCHEDULE REFRESH                                                │
│     └─→ scheduleTokenRefresh(14 min)                               │
│                                                                      │
│  4. SUBSEQUENT API CALLS                                            │
│     ↓                                                                │
│     Request Interceptor:                                            │
│       • Get token from AsyncStorage                                 │
│       • ADD Authorization header: Bearer {token}                    │
│       • Send request                                                │
│     ↓                                                                │
│     Response:                                                       │
│       • If 200: Success ✓                                           │
│       • If 401: Token expired                                       │
│         └─→ Call refresh-token endpoint                            │
│         └─→ Get new token                                          │
│         └─→ Retry original request                                 │
│       • If 401 on refresh: Logout                                   │
│                                                                      │
│  5. BACKGROUND ACTIVITY                                            │
│     │                                                                │
│     ├─→ Token Refresh (Every 14 min)                               │
│     │   • POST /volunteer/refresh-token                            │
│     │   • Update AsyncStorage                                      │
│     │   • Schedule next refresh (14 min from now)                  │
│     │                                                                │
│     └─→ App Closure & Restart                                      │
│         • AsyncStorage persists token                              │
│         • startup() loads token                                    │
│         • Validates token expiry                                   │
│         • If valid: Continue without login                         │
│         • If expired: Refresh using refresh token                  │
│         • If refresh invalid: Force logout                         │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```
