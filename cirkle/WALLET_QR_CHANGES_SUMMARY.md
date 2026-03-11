# Wallet QR System - Summary of Changes

## Quick Overview

Implemented a complete wallet QR code system that generates unique QR codes on first wallet topup with format `{userId}-{walletId}-{timestamp}` and allows users to lock/unlock wallets and regenerate QR codes with updated timestamps.

---

## Files Modified/Created

### Backend (cirkle-backend-common)

#### Database Migrations

1. **V004\_\_add_wallet_qr_and_lock_fields.sql** - Created
   - Added `qr_string` (VARCHAR 1024) column
   - Added `is_wallet_locked` (BOOLEAN, default false) column
   - Added `created_at` (TIMESTAMP) column
   - Changed wallet ID from IDENTITY to UUID

2. **V005\_\_add_wallet_id_to_transactions.sql** - Created
   - Added `wallet_id` (UUID) column to wallet_transactions

#### Entity Classes

1. **Wallet.java** - Modified
   - Changed ID type from `Long` to `String` (UUID)
   - Added `qrString` field
   - Added `isWalletLocked` field
   - Added `createdAt` field
   - Updated getters/setters

2. **WalletWalletTransaction.java** - Modified
   - Added `walletId` field for transaction reference

#### New Service

1. **WalletQrService.java** - Created
   - QR generation with format `{userId}-{walletId}-{timestamp}`
   - QR regeneration (changes timestamp)
   - Parser methods to extract userId, walletId, timestamp
   - Optional BCrypt hashing methods

#### Service Updates

1. **WalletService.java** - Modified
   - Injected `WalletQrService`
   - Enhanced `addCredit()` to generate QR on first wallet creation
   - Only generates QR string once (on wallet creation)

#### Controller Updates

1. **WalletController.java** - Modified
   - Added import for `WalletQrService`
   - Added `getWalletDetails()` endpoint - GET `/api/wallet/details/{userId}/{eventId}`
   - Added `toggleWalletLock()` endpoint - POST `/api/wallet/{userId}/{eventId}/toggle-lock`
   - Added `regenerateQrString()` endpoint - POST `/api/wallet/{userId}/{eventId}/regenerate-qr`

#### Redemption Service

1. **RedemptionService.java** - Modified
   - Enhanced `checkout()` validation
   - Added wallet QR validation block (checks QR format, matches, lock status)
   - Validates minimum ₹500 transaction for event
   - Checks wallet balance before deduction
   - Provides detailed error messages

#### DTOs

1. **CheckoutRequest.java** - Modified
   - Added `walletId` field (optional)
   - Added `qrString` field (optional)

---

### Frontend (cirkle-app)

#### Redux Store

1. **wallet-slice.ts** - Created
   - `WalletState` interface with wallet, QR, lock, loading
   - Actions: setCurrentWallet, setQrString, setWalletLocked, setWalletLoading, clearWallet
   - Persisted to AsyncStorage

2. **store.ts** - Modified
   - Imported wallet slice
   - Added `wallet: persistReducer(walletPersistConfig, walletReducer)`

#### Types

1. **events.ts** - Modified
   - Updated `IWallet` interface
   - Added `qrString` field
   - Added `isWalletLocked` field
   - Added `walletId` field
   - Added `id` field

#### API Hooks

1. **wallet.ts** - Modified
   - Added `useGetWalletDetails()` - Fetches wallet with QR and lock status
   - Added `useToggleWalletLock()` - Lock/unlock mutation
   - Added `useRegenerateQr()` - Regenerate QR mutation

#### Components

1. **wallet-qr-modal.tsx** - Created
   - Modern iOS-style bottom sheet modal
   - QR display section (shows QR code from qrString)
   - Wallet lock section (toggle with icons and descriptions)
   - Wallet info section (balance, wallet ID)
   - "Generate New QR" action button
   - Real-time sync with Redux
   - Loading and error states

#### Screens

1. **wallet-screen.tsx** - Modified
   - Added `selectedWallet` state
   - Imported `WalletQrModal`
   - Made event wallets pressable (tap to open modal)
   - Added modal integration with proper props
   - Event wallet items now open detailed QR modal on tap

---

### Redemption App (redemption-app)

#### Core Logic

1. **order.tsx** - Modified
   - Added `parseWalletQrString()` helper function
   - Enhanced `handleCreateOrder()` to:
     - Try parsing new QR format first: `{userId}-{walletId}-{timestamp}`
     - Fallback to old encrypted format for backward compatibility
     - Include `walletId` and `qrString` in checkout payload
     - Support both old and new QR types seamlessly

---

## Key Features Implemented

### ✅ Backend Features

- [x] QR string generation on first wallet topup
- [x] QR string format: `{userId}-{walletId}-{timestamp}`
- [x] Wallet lock/unlock toggle
- [x] QR regeneration (changes timestamp only)
- [x] Wallet QR validation during checkout
- [x] Locked wallet detection
- [x] Minimum ₹500 transaction validation per event
- [x] Balance checking before deduction
- [x] Transaction tracking with wallet ID
- [x] Database migrations with UUID migration

### ✅ Frontend Features

- [x] Redux wallet state management with persistence
- [x] Fetch wallet details with QR and lock status
- [x] Modern wallet QR detail modal
- [x] Lock/unlock toggle UI (iOS-style)
- [x] QR regeneration button
- [x] Tap event wallet to view QR details
- [x] Real-time lock status sync
- [x] Loading states and error handling
- [x] Responsive modal design

### ✅ Redemption Features

- [x] Parse new wallet QR format
- [x] Backward compatibility with encrypted QR
- [x] Support for wallet ID in checkout payload
- [x] Support for QR string validation
- [x] Fallback mechanism for old vs new QR types

---

## API Contracts

### Wallet QR Endpoints

```
GET /api/wallet/details/{userId}/{eventId}
Authorization: Bearer {token}

Response:
{
  "success": true,
  "data": {
    "walletId": "550e8400-...",
    "userId": "user123",
    "eventId": "event456",
    "eventName": "Event Name",
    "balance": 1500.50,
    "qrCodeUrl": "...",
    "qrString": "550e8400-...-user123-...-2024-01-15T10:30:45",
    "isWalletLocked": false
  }
}
```

```
POST /api/wallet/{userId}/{eventId}/toggle-lock
Authorization: Bearer {token}
Body: { "lock": true/false }

Response:
{
  "success": true,
  "data": {
    "walletId": "550e8400-...",
    "isWalletLocked": true,
    "message": "Wallet locked successfully"
  }
}
```

```
POST /api/wallet/{userId}/{eventId}/regenerate-qr
Authorization: Bearer {token}

Response:
{
  "success": true,
  "data": {
    "walletId": "550e8400-...",
    "qrString": "550e8400-...-user123-...-2024-01-15T10:35:22",
    "message": "QR string regenerated successfully"
  }
}
```

### Updated Checkout Endpoint

```
POST /api/stall/checkout (redemption)

Request:
{
  "eventId": "event456",
  "userId": "user123",
  "walletId": "550e8400-...",          // Optional: if using wallet QR
  "qrString": "550e8400-...-2024-...", // Optional: full QR for validation
  "items": [...],
  "totalAmount": 500.00,
  "token": "..."
}
```

---

## QR String Format Details

### Format: `{userId}-{walletId}-{timestamp}`

- **userId**: User identifier (UUID usually)
- **walletId**: Unique wallet identifier (UUID, never changes)
- **timestamp**: ISO 8601 format with dashes (can contain dashes)

### Example

```
550e8400-e29b-41d4-a716-446655440000-12345678-90ab-cdef-1234-567890abcdef-2024-01-15T10:30:45.123456Z
```

### Parsing

```
parts = split('-')
userId = parts[0]
walletId = parts[1]
timestamp = parts[2:].join('-')
```

---

## Security Considerations

1. **QR String Validation**: Backend validates scanned QR matches stored value
2. **User Verification**: Extracted userId must match authenticated user
3. **Wallet Lock Status**: Prevents unauthorized deductions
4. **Minimum Transaction**: Requires ₹500 per event before wallet usage
5. **Balance Validation**: Checks sufficient funds before checkout
6. **Immutable References**: walletId and userId never change in QR
7. **Timestamp Rotation**: QR can be regenerated with new timestamp for security

---

## Backward Compatibility

The implementation maintains backward compatibility:

- Old encrypted QR format still supported
- Frontend tries new wallet QR format first, falls back to decryption
- Existing topupBalance system still works alongside new wallet system
- No breaking changes to existing endpoints

---

## Deployment Checklist

- [ ] Run database migrations V004 and V005
- [ ] Update backend JAR with changes
- [ ] Compile frontend with new Redux store
- [ ] Update redemption-app with QR parsing
- [ ] Test QR generation on first topup
- [ ] Verify wallet lock toggle
- [ ] Test QR regeneration
- [ ] Test redemption with new QR format
- [ ] Test backward compatibility with old QR format
- [ ] Verify transaction tracking with wallet ID
- [ ] Test error messages for locked wallets
- [ ] Validate minimum transaction requirements

---

## Testing Scenarios

### Scenario 1: First Wallet Topup

1. User does first topup for event
2. Payment verified ✓
3. Wallet created with balance ✓
4. QR string generated ✓
5. isWalletLocked = false ✓

### Scenario 2: View & Lock Wallet

1. User opens wallet modal
2. QR code displays ✓
3. Wallet status shows "unlocked" ✓
4. User toggles lock ✓
5. Status changes to "locked" ✓

### Scenario 3: Regenerate QR

1. User clicks "Generate New QR" ✓
2. New timestamp generated ✓
3. UserId and walletId stay same ✓
4. Old QR no longer valid ✓

### Scenario 4: Redemption with Locked Wallet

1. Stall scans QR
2. Checkout attempted ✓
3. Backend detects locked wallet ✓
4. Returns error: "Wallet is locked" ✓

### Scenario 5: Insufficient Topup

1. Stall scans QR
2. User hasn't topped up ₹500+ for event ✓
3. Returns error about minimum topup ✓

---

## Version History

- **v1.0.0** (2024-01-15): Initial implementation
  - QR string generation with userId-walletId-timestamp format
  - Wallet lock/unlock toggle
  - QR regeneration with timestamp rotation
  - Wallet validation in redemption
  - Backend and frontend integration
  - Redemption app support

---
