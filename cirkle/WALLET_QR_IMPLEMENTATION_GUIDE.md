# Wallet QR Code Implementation - Complete Guide

## Overview

Complete implementation of wallet QR code system with encryption, lock status, and redemption validation across backend, frontend (cirkle-app), and redemption-app.

---

## Backend Implementation (cirkle-backend-common)

### 1. Database Schema Changes

**File:** `src/main/resources/db/migration/V004__add_wallet_qr_and_lock_fields.sql`

Added new columns to `wallets` table:

- `id` - Changed from IDENTITY (Long) to UUID for consistency
- `qr_string` (VARCHAR 1024) - Stores plain QR string in format `{userId}-{walletId}-{timestamp}`
- `is_wallet_locked` (BOOLEAN) - Default false, prevents wallet deductions when true
- `created_at` (TIMESTAMP) - Wallet creation timestamp

**File:** `src/main/resources/db/migration/V005__add_wallet_id_to_transactions.sql`

Added `wallet_id` column to `wallet_transactions` table for tracking transactions by wallet.

### 2. Entity Models

**Wallet.java** - Enhanced with new fields:

```java
@Column(name = "qr_string", length = 1024)
private String qrString;

@Column(name = "is_wallet_locked")
private Boolean isWalletLocked = false;

@Column(name = "created_at")
private LocalDateTime createdAt;
```

**WalletWalletTransaction.java** - Added wallet reference:

```java
@Column(name = "wallet_id")
private String walletId;
```

### 3. Services

**WalletQrService.java** - NEW SERVICE

Handles QR code generation and parsing:

```java
// Generate QR string format: {userId}-{walletId}-{timestamp}
public String generateQrString(String userId, String walletId)

// Regenerate with new timestamp
public String regenerateQrString(String userId, String walletId)

// Extraction helpers
public String extractUserIdFromQr(String qrString)
public String extractWalletIdFromQr(String qrString)
public String extractTimestampFromQr(String qrString)

// BCrypt hashing (for future use)
public String hashQrString(String qrString)
public boolean verifyQrString(String scannedQrString, String hashedQrString)
```

**WalletService.java** - Enhanced

Modified `addCredit()` to generate QR string on first wallet creation:

```java
if (isNewWallet) {
    // Only generate QR string on first wallet creation
    wallet = walletRepository.save(wallet);
    String qrString = walletQrService.generateQrString(userId, wallet.getId());
    wallet.setQrString(qrString);
    wallet.setIsWalletLocked(false);
}
```

### 4. REST API Endpoints

**WalletController.java** - New endpoints:

#### Get Wallet Full Details

```
GET /api/wallet/details/{userId}/{eventId}
Response: {
  walletId, userId, eventId, eventName, balance,
  qrCodeUrl, qrString, isWalletLocked
}
```

#### Toggle Wallet Lock

```
POST /api/wallet/{userId}/{eventId}/toggle-lock
Body: { "lock": true/false }
Response: { walletId, isWalletLocked, message }
```

#### Regenerate QR String

```
POST /api/wallet/{userId}/{eventId}/regenerate-qr
Response: { walletId, qrString, message }
```

### 5. Redemption Service - QR Validation

**RedemptionService.java** - Enhanced checkout validation

New validation added to `checkout()` method:

```java
// NEW: Wallet QR Validation (if QR string is provided)
if (request.getQrString() != null && !request.getQrString().isEmpty()) {
    // 1. Extract userId, walletId, timestamp from QR
    String[] qrParts = request.getQrString().split("-");

    // 2. Verify userId matches
    if (!qrUserId.equals(request.getUserId())) {
        throw new RuntimeException("QR code user ID doesn't match");
    }

    // 3. Validate wallet exists and QR string matches
    if (wallet.getQrString() == null || !wallet.getQrString().equals(qrString)) {
        throw new RuntimeException("Invalid QR code - QR string doesn't match");
    }

    // 4. Check if wallet is locked
    if (wallet.getIsWalletLocked()) {
        throw new RuntimeException("Wallet is locked - no deductions can be made");
    }

    // 5. Check if user has minimum ₹500 transaction for this event
    boolean hasMinimumTransaction = eventTransactions.stream()
        .anyMatch(tx -> "credit".equals(tx.getTransactionType()) && tx.getAmount() >= 500);

    // 6. Check wallet balance is sufficient
    if (wallet.getBalance() < request.getTotalAmount()) {
        throw new RuntimeException("Insufficient wallet balance");
    }
}
```

**CheckoutRequest.java** - Updated:

```java
private String walletId; // Optional: wallet ID from QR
private String qrString; // Optional: full QR string for validation
```

---

## Frontend Implementation (cirkle-app)

### 1. Redux State Management

**wallet-slice.ts** - NEW SLICE

```typescript
interface WalletState {
  currentWallet: IWallet | null;
  qrString: string | null;
  isWalletLocked: boolean;
  loading: boolean;
}

Actions: -setCurrentWallet(wallet) -
  setQrString(qrString) -
  setWalletLocked(isWalletLocked) -
  setWalletLoading(loading) -
  clearWallet();
```

**store.ts** - Updated

Added wallet slice to Redux store with persistence:

```typescript
wallet: persistReducer(walletPersistConfig, walletReducer);
```

### 2. Types

**events.ts** - Updated IWallet interface:

```typescript
export interface IWallet {
  _id: string;
  id?: string;
  walletId?: string;
  userId: string;
  balance: number;
  eventId?: string;
  eventName?: string;
  qrCodeUrl?: string;
  qrString?: string; // NEW
  isWalletLocked?: boolean; // NEW
  eventImage?: string;
  lastFundAddedDate?: string;
}
```

### 3. API Hooks

**wallet.ts** - New hooks added:

```typescript
/**         Get wallet details with QR string and lock status */
export const useGetWalletDetails = (userId?, eventId?, enabled?) => {
  return useQuery({
    queryKey: ['wallet', 'details', userId, eventId],
    queryFn: async () => {
      const res = await api({ isAuthenticate: true }).get(`/wallet/details/${userId}/${eventId}`);
      return res.data?.data || res.data || {};
    },
  });
};

/** Toggle wallet lock status */
export const useToggleWalletLock = () => {
  return useMutation({
    mutationFn: async ({ userId, eventId, lock }) => {
      const res = await api({ isAuthenticate: true }).post(
        `/wallet/${userId}/${eventId}/toggle-lock`,
        { lock }
      );
      return res.data?.data || res.data;
    },
  });
};

/** Regenerate wallet QR string (changes timestamp) */
export const useRegenerateQr = () => {
  return useMutation({
    mutationFn: async ({ userId, eventId }) => {
      const res = await api({ isAuthenticate: true }).post(
        `/wallet/${userId}/${eventId}/regenerate-qr`
      );
      return res.data?.data || res.data;
    },
  });
};
```

### 4. Components

**wallet-qr-modal.tsx** - NEW MODAL COMPONENT

Modern iOS-style modal with:

- **QR Display Section**
  - Shows QR code from wallet's QR string
  - Graceful loading and empty states
  - Displays raw QR string data

- **Wallet Lock Section**
  - Modern iOS toggle-like UI
  - Shows lock/unlock status with appropriate icons
  - Toggle button to lock/unlock wallet
  - Status descriptions

- **Wallet Info Section**
  - Balance display
  - Wallet ID
  - Event details

- **Actions**
  - "Generate New QR" button
  - Regenerates QR string (changes timestamp, keeps userId and walletId same)

Features:

- Fetches wallet details on open
- Syncs lock status to Redux
- Real-time lock/unlock toggle
- QR regeneration with confirmation
- Bottom sheet presentation style
- Responsive design for all screen sizes

### 5. Screens

**wallet-screen.tsx** - Updated

- Added `selectedWallet` state to track tapped wallet
- Imported `WalletQrModal` component
- Made event wallet items pressable (tap to open modal)
- Added modal integration:
  ```tsx
  <WalletQrModal
    visible={showQR && !!selectedWallet}
    onClose={() => {
      setShowQR(false);
      setSelectedWallet(null);
    }}
    userId={user?._id}
    eventId={selectedWallet?.eventId}
    walletData={selectedWallet}
  />
  ```

---

## Redemption App Implementation (redemption-app)

### 1. QR Parsing Helper

**order.tsx** - Added parser function:

```typescript
const parseWalletQrString = (qrData: string) => {
  if (!qrData || typeof qrData !== 'string') return null;
  const parts = qrData.split('-');
  if (parts.length < 3) return null;
  return {
    userId: parts[0].trim(),
    walletId: parts[1].trim(),
    timestamp: parts.slice(2).join('-'), // timestamp can contain dashes
  };
};
```

### 2. Enhanced Checkout Logic

**handleCreateOrder()** - Updated processing:

```typescript
const handleCreateOrder = async ({ data }: { data: string }) => {
  try {
    let userId: string | null = null;
    let walletId: string | null = null;

    // 1. Try new wallet QR format first: {userId}-{walletId}-{timestamp}
    const walletQrData = parseWalletQrString(data);
    if (walletQrData) {
      userId = walletQrData.userId;
      walletId = walletQrData.walletId;
    } else {
      // 2. Fallback to old encrypted format
      const parsedData = decryptData(data);
      userId = parsedData?.userId;
    }

    // 3. Prepare checkout payload with wallet data
    const payload = {
      eventId: checkoutEventId,
      userId,
      walletId, // Include wallet ID if available
      qrString: data, // Pass raw QR string for backend validation
      items: getApiCartItemsPayload,
      totalAmount: cartTotal,
      token: token || '',
    };

    // 4. Execute checkout
    const res = await mutateAsync(payload);
  } catch (error) {
    // Error handling...
  }
};
```

### 3. Error Handling

Supports all existing error scenarios plus new wallet validation errors:

- "Wallet is locked - no deductions can be made"
- "Invalid QR code - QR string doesn't match"
- "QR code user ID doesn't match"
- "Wallet hasn't made a minimum ₹500 top-up for this event"
- "Insufficient wallet balance"

---

## QR String Format

### Format: `{userId}-{walletId}-{timestamp}`

**Example:**

```
550e8400-e29b-41d4-a716-446655440000-12345678-90ab-cdef-1234-567890abcdef-2024-01-15T10:30:45.123456
```

**Parsing:**

- **userId**: First UUID segment before first dash
- **walletId**: Second UUID segment between first and second dash
- **timestamp**: ISO 8601 format with dashes (remaining part)

**Benefits:**

- Human-readable structure for debugging
- Easy to parse on both frontend and backend
- Timestamp allows QR rotation for security
- No encryption needed (simple plain text)
- UserId and walletId never change (immutable wallet references)

---

## Wallet Lifecycle

### 1. Initial Creation (First Topup)

```
User performs first topup → Payment verified
→ Wallet created with balance
→ QR string generated: {userId}-{walletId}-{timestamp}
→ isWalletLocked = false
→ Transaction recorded with wallet_id
```

### 2. Wallet in Use

```
User can:
- View wallet details with QR
- Download/share QR code
- Lock/unlock wallet (modern iOS toggle)
- Regenerate QR (changes timestamp)
- Use wallet for redemptions
```

### 3. Redemption Flow

```
Scanner scans QR → Parse QR string
→ Extract userId, walletId from QR
→ Validate QR string matches wallet.qrString
→ Check wallet not locked
→ Check user has ≥₹500 topup for event
→ Check sufficient balance
→ Deduct from wallet.balance
→ Create transaction with wallet_id
```

---

## Security Features

1. **QR String Validation**: Backend verifies scanned QR string matches stored value
2. **Wallet Lock**: Prevents unauthorized transactions when toggled
3. **User Verification**: Extracted userId must match authenticated user
4. **Minimum Transaction**: Requires ₹500 minimum topup per event before redemption
5. **Balance Checks**: Validates sufficient balance before checkout
6. **Wallet ID Immutability**: walletId never changes (UUID primary key ensures)

---

## Future Enhancements

1. **Bcrypt Hashing**: Optional hashing of QR strings for additional security
2. **QR Expiration**: Add TTL to QR codes for time-based security
3. **Rate Limiting**: Limit QR regenerations per user/time period
4. **Audit Log**: Track all wallet operations (lock/unlock, QR regeneration)
5. **Device Binding**: Bind wallet to device for additional security
6. **Biometric Unlock**: Require fingerprint/face to unlock wallet before redemption

---

## Testing Checklist

### Backend

- [ ] Wallet created with QR string on first topup
- [ ] Lock/unlock toggle updates wallet status
- [ ] QR regeneration changes timestamp
- [ ] Wallet validation rejects locked wallets
- [ ] Minimum ₹500 check enforced
- [ ] QR string format parsing works correctly

### Frontend (cirkle-app)

- [ ] Wallet details loaded on modal open
- [ ] Lock toggle UI responsive
- [ ] QR regeneration creates new string
- [ ] Redux state persists correctly
- [ ] Modal closes properly
- [ ] Tapping wallet opens modal

### Redemption App

- [ ] New QR format parsed correctly
- [ ] Fallback to old format works
- [ ] Wallet validation errors shown
- [ ] Locked wallet rejected with message
- [ ] Minimum topup check enforced
- [ ] Sufficient balance validated

---

## Deployment Notes

1. **Database**: Run migrations V004 and V005 before deployment
2. **Backend**: Compile with Spring Boot 3.x, Java 17+
3. **Frontend**: Update package.json if needed, test with EAS build
4. **Redemption**: Update API payload structure to include qrString and walletId
5. **Rollback**: Maintains backward compatibility with old encrypted QR format

---

## API Reference

### Wallet QR Endpoints

| Method | Endpoint                                       | Auth | Description                        |
| ------ | ---------------------------------------------- | ---- | ---------------------------------- |
| GET    | `/api/wallet/details/{userId}/{eventId}`       | Yes  | Get wallet with QR and lock status |
| POST   | `/api/wallet/{userId}/{eventId}/toggle-lock`   | Yes  | Lock/unlock wallet                 |
| POST   | `/api/wallet/{userId}/{eventId}/regenerate-qr` | Yes  | Generate new QR string             |

### Updated Endpoints

| Method | Endpoint              | Change                                      |
| ------ | --------------------- | ------------------------------------------- |
| POST   | `/api/stall/checkout` | Added walletId, qrString to CheckoutRequest |

---

## Configuration

No additional environment variables required. Uses existing:

- `EXPO_PUBLIC_CRYPTO_SECRET_KEY` - For backward compat with encrypted QR
- Database connection settings
- Razorpay configuration

---

## Contact & Support

For issues or questions about the wallet QR implementation, refer to:

- Backend: `WalletQrService`, `WalletService`
- Frontend: `wallet-slice.ts`, `wallet-qr-modal.tsx`
- Redemption: `order.tsx`, `RedemptionService`
