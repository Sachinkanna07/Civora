# Civora Cloud Firestore Schema & Security Model

This document outlines the Firestore data structure, field types, indexes, and document ownership boundaries in Civora.

---

## 1. Collections & Data Models

### `users`
- **Document ID**: `{userId}` (matches Firebase Auth UID)
- **Fields**:
  - `uid`: `String` (User UID)
  - `name`: `String` (Full display name)
  - `email`: `String` (User email address)
  - `role`: `String` (`student`, `faculty`, `admin`, `vendor`, `driver` - informational mirror; actual authority is auth token custom claim)
  - `department`: `String` (e.g., "Computer Science")
  - `year`: `String` (e.g., "3rd Year")
  - `section`: `String` (e.g., "A")
  - `routeId`: `String` (Default bus route)
  - `profileImage`: `String` (Profile photo URL)
  - `notificationsEnabled`: `Boolean`
  - `createdAt`: `Timestamp`

### `users/{uid}/devices`
- **Document ID**: `{deviceId}`
- **Fields**:
  - `token`: `String` (FCM push notification token)
  - `updatedAt`: `Timestamp`

### `announcements`
- **Document ID**: Auto-generated or custom ID
- **Fields**:
  - `title`: `String`
  - `body`: `String`
  - `category`: `String` (`General`, `Academic`, `Department`, `Urgent`)
  - `priority`: `String` (`Low`, `Medium`, `High`, `Urgent`)
  - `audience`: `String` (`students`, `all`)
  - `department`: `String` (Target department or empty for campus-wide)
  - `authorId`: `String` (Faculty / Admin UID)
  - `authorName`: `String`
  - `createdAt`: `Timestamp`
  - `expiresAt`: `Timestamp` / `Number`

### `events`
- **Document ID**: Auto-generated or custom ID
- **Fields**:
  - `title`: `String`
  - `description`: `String`
  - `category`: `String` (`Workshop`, `Cultural`, `Sports`, `Seminar`, `Hackathon`)
  - `department`: `String`
  - `venue`: `String`
  - `startTime`: `Timestamp`
  - `endTime`: `Timestamp`
  - `organizer`: `String`
  - `createdBy`: `String` (Organizer UID)
  - `capacity`: `Number` (Maximum attendee seats)
  - `registeredCount`: `Number` (Current registered count, updated atomically)
  - `registrationEnabled`: `Boolean`
  - `imageUrl`: `String`
  - `clubId`: `String` (Optional associated club ID)

### `eventRegistrations`
- **Document ID**: `{eventId}_{userId}` (deterministic key preventing double-registration)
- **Fields**:
  - `eventId`: `String`
  - `userId`: `String`
  - `organizerId`: `String`
  - `token`: `String` (Unique registration / QR token)
  - `attended`: `Boolean`
  - `createdAt`: `Timestamp`

### `eventAttendance`
- **Document ID**: `{registrationId}`
- **Fields**:
  - `eventId`: `String`
  - `userId`: `String`
  - `organizerId`: `String`
  - `checkedBy`: `String` (UID of faculty checking in attendee)
  - `createdAt`: `Timestamp`

### `timetables`
- **Document ID**: Auto-generated
- **Fields**:
  - `courseCode`: `String` (e.g., "CS301")
  - `courseName`: `String` (e.g., "Data Structures & Algorithms")
  - `faculty`: `String`
  - `dayOfWeek`: `Number` (1 = Monday through 7 = Sunday)
  - `startMinute`: `Number` (Minutes from midnight, e.g., 540 = 09:00 AM)
  - `endMinute`: `Number` (e.g., 600 = 10:00 AM)
  - `room`: `String`
  - `department`: `String`
  - `year`: `String`
  - `section`: `String`

### `canteens`
- **Document ID**: `{canteenId}`
- **Fields**:
  - `name`: `String`
  - `location`: `String`
  - `vendorId`: `String` (Assigned Vendor UID)
  - `open`: `Boolean`

### `menuItems`
- **Document ID**: `{menuItemId}`
- **Fields**:
  - `canteenId`: `String`
  - `name`: `String`
  - `description`: `String`
  - `category`: `String` (`Snacks`, `Beverages`, `Meals`, `Breakfast`)
  - `pricePaise`: `Number` (Price in Indian Paise, e.g. 5000 = ₹50.00)
  - `available`: `Boolean`

### `foodOrders`
- **Document ID**: `{userId}_{requestId}`
- **Fields**:
  - `userId`: `String`
  - `canteenId`: `String`
  - `vendorId`: `String`
  - `items`: `List<Map>` (`menuItemId`, `name`, `quantity`, `pricePaise`)
  - `totalPaise`: `Number`
  - `token`: `String` (Order pickup token, e.g., "A42")
  - `status`: `String` (`PLACED`, `ACCEPTED`, `PREPARING`, `READY`, `COMPLETED`, `CANCELLED`)
  - `paymentState`: `String` (`PENDING`, `COMPLETED`, `COD`)
  - `createdAt`: `Timestamp`
  - `updatedAt`: `Timestamp`

### `busRoutes` & `buses`
- **`busRoutes`**:
  - `id`: `String`
  - `name`: `String` (e.g., "Route 4 - North Campus")
  - `stops`: `List<Map>` (`name`, `latitude`, `longitude`, `order`)
- **`buses`**:
  - `id`: `String` (e.g., "Bus-04")
  - `routeId`: `String`
  - `driverId`: `String` (Assigned Driver UID)
  - `licensePlate`: `String`

### `activeTrips`
- **Document ID**: `{busId}`
- **Fields**:
  - `busId`: `String`
  - `routeId`: `String`
  - `driverId`: `String`
  - `active`: `Boolean`
  - `latitude`: `Number`
  - `longitude`: `Number`
  - `accuracy`: `Number`
  - `locationUpdatedAt`: `Timestamp`
  - `locationCapturedAt`: `Timestamp`
  - `startedAt`: `Timestamp`
  - `endedAt`: `Timestamp`

### `campusReports`
- **Document ID**: Auto-generated
- **Fields**:
  - `createdBy`: `String` (Student UID)
  - `title`: `String`
  - `description`: `String`
  - `category`: `String` (`ELECTRICAL`, `WATER`, `CLEANLINESS`, `SAFETY`, `INFRASTRUCTURE`, `TRANSPORT`, `OTHER`)
  - `location`: `String`
  - `imageUrl`: `String`
  - `status`: `String` (`SUBMITTED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`)
  - `assignedTo`: `String` (Staff UID or team name)
  - `resolutionNote`: `String`
  - `createdAt`: `Timestamp`
  - `updatedAt`: `Timestamp`

### `notifications` & `activities`
- **Document ID**: Auto-generated
- **Fields**:
  - `userId`: `String`
  - `title`: `String`
  - `body`: `String`
  - `type`: `String`
  - `status`: `String`
  - `destination`: `String` (`FOOD`, `REPORT`, `EVENT`, `ANNOUNCEMENT`, `TRANSPORT`)
  - `entityId`: `String`
  - `category`: `String` (`URGENT`, `RELEVANT`, `DISCOVER`)
  - `read`: `Boolean`
  - `createdAt`: `Timestamp`

### `clubs` & `clubFollowers`
- **`clubs`**:
  - `id`: `String`
  - `name`: `String`
  - `description`: `String`
  - `category`: `String` (`Tech`, `Cultural`, `Sports`, `Literary`, `Arts`)
  - `department`: `String`
  - `logoUrl`: `String`
  - `coverImageUrl`: `String`
- **`clubFollowers`**:
  - Document ID: `{clubId}_{userId}`
  - Fields: `clubId`, `userId`, `createdAt`

### `campusLocations`
- **Document ID**: Auto-generated
- **Fields**:
  - `name`: `String`
  - `category`: `String` (`Academic`, `Library`, `Canteen`, `Auditorium`, `Sports`, `Admin`, `Hostel`, `Bus Stop`, `Labs`)
  - `description`: `String`
  - `latitude`: `Number`
  - `longitude`: `Number`

---

## 2. Firestore Indexes

All composite index requirements for compound queries (`department + createdAt`, `status + createdAt`, `active + locationUpdatedAt`, `audience + department + createdAt`, etc.) are defined in `firestore.indexes.json`.
