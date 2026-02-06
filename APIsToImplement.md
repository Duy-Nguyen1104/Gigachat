# ChatNode API Documentation

## Authentication & User Management

### Auth Routes (`/api/auth`)

| Router      | Endpoint           | Method | Description               | Input                                                                         | Output                                                                                                      | Middleware | Errors                                                | Notes                              |
| ----------- | ------------------ | ------ | ------------------------- | ----------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- | ---------- | ----------------------------------------------------- | ---------------------------------- |
| `/api/auth` | `/register`        | POST   | Register new user         | `{ username: string, email: string, password: string, displayName?: string }` | `{ user: { id, username, email, displayName, avatarUrl, createdAt }, token: string, refreshToken: string }` | None       | `409` Username/email exists<br>`400` Validation error | Password must be hashed (bcrypt)   |
| `/api/auth` | `/login`           | POST   | Login user                | `{ email: string, password: string }`                                         | `{ user: { id, username, email, displayName, avatarUrl, lastSeen }, token: string, refreshToken: string }`  | None       | `401` Invalid credentials<br>`400` Validation error   | Update lastSeen on login           |
| `/api/auth` | `/logout`          | POST   | Logout user               | `{ refreshToken: string }`                                                    | `{ message: "Logged out successfully" }`                                                                    | `auth`     | `401` Unauthorized                                    | Blacklist/invalidate refresh token |
| `/api/auth` | `/refresh-token`   | POST   | Refresh access token      | `{ refreshToken: string }`                                                    | `{ token: string, refreshToken: string }`                                                                   | None       | `401` Invalid refresh token                           | Issue new token pair               |
| `/api/auth` | `/forgot-password` | POST   | Request password reset    | `{ email: string }`                                                           | `{ message: "Reset email sent" }`                                                                           | None       | `404` Email not found<br>`429` Rate limit             | Send reset token via email         |
| `/api/auth` | `/reset-password`  | POST   | Reset password with token | `{ token: string, newPassword: string }`                                      | `{ message: "Password reset successful" }`                                                                  | None       | `400` Invalid/expired token                           | Token expires in 1 hour            |

### User Routes (`/api/user`, `/api/users`)

| Router       | Endpoint    | Method | Description              | Input                                              | Output                                                                            | Middleware       | Errors                                                                | Notes                                  |
| ------------ | ----------- | ------ | ------------------------ | -------------------------------------------------- | --------------------------------------------------------------------------------- | ---------------- | --------------------------------------------------------------------- | -------------------------------------- |
| `/api/user`  | `/me`       | GET    | Get current user profile | None                                               | `{ id, username, email, displayName, avatarUrl, lastSeen, createdAt, updatedAt }` | `auth`           | `401` Unauthorized                                                    | Returns authenticated user             |
| `/api/user`  | `/me`       | PATCH  | Update profile           | `{ displayName?: string, email?: string }`         | `{ id, username, email, displayName, avatarUrl, updatedAt }`                      | `auth`           | `401` Unauthorized<br>`409` Email exists<br>`400` Validation error    | Cannot change username                 |
| `/api/user`  | `/avatar`   | POST   | Upload avatar            | Form-data: `{ file: File }`                        | `{ avatarUrl: string }`                                                           | `auth`, `upload` | `401` Unauthorized<br>`400` Invalid file type<br>`413` File too large | Max 5MB, jpg/png only. Returns CDN URL |
| `/api/user`  | `/password` | PATCH  | Change password          | `{ currentPassword: string, newPassword: string }` | `{ message: "Password updated" }`                                                 | `auth`           | `401` Unauthorized<br>`400` Invalid current password                  | Verify current password first          |
| `/api/user`  | `/me`       | DELETE | Delete account           | `{ password: string }`                             | `{ message: "Account deleted" }`                                                  | `auth`           | `401` Unauthorized<br>`400` Invalid password                          | Soft delete, cascade conversations     |
| `/api/users` | `/search`   | GET    | Search users             | Query: `?q=username&limit=10`                      | `{ users: [{ id, username, displayName, avatarUrl }], hasMore: boolean }`         | `auth`           | `401` Unauthorized<br>`400` Query too short                           | Min 2 chars, exclude current user      |
| `/api/users` | `/:userId`  | GET    | Get user by ID           | Params: `userId`                                   | `{ id, username, displayName, avatarUrl, lastSeen }`                              | `auth`           | `401` Unauthorized<br>`404` User not found                            | Public profile info only               |

---

## Friend System

### Friend Request Routes (`/api/friend-requests`)

| Router                 | Endpoint             | Method | Description           | Input                       | Output                                                                                                         | Middleware | Errors                                                                                      | Notes                                 |
| ---------------------- | -------------------- | ------ | --------------------- | --------------------------- | -------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------------------------------------------------- | ------------------------------------- |
| `/api/friend-requests` | `/`                  | POST   | Send friend request   | `{ toUserId: string }`      | `{ id, fromUserId, toUserId, status: "pending", createdAt }`                                                   | `auth`     | `401` Unauthorized<br>`404` User not found<br>`409` Request exists<br>`400` Already friends | Cannot send to self                   |
| `/api/friend-requests` | `/sent`              | GET    | Get sent requests     | Query: `?limit=20&offset=0` | `{ requests: [{ id, toUser: { id, username, displayName, avatarUrl }, status, createdAt }], total: number }`   | `auth`     | `401` Unauthorized                                                                          | Only pending requests                 |
| `/api/friend-requests` | `/received`          | GET    | Get received requests | Query: `?limit=20&offset=0` | `{ requests: [{ id, fromUser: { id, username, displayName, avatarUrl }, status, createdAt }], total: number }` | `auth`     | `401` Unauthorized                                                                          | Only pending requests                 |
| `/api/friend-requests` | `/:requestId/accept` | PATCH  | Accept friend request | Params: `requestId`         | `{ request: { id, status: "accepted", updatedAt }, friend: { id, userId, friendId, becameFriendsAt } }`        | `auth`     | `401` Unauthorized<br>`404` Request not found<br>`403` Not recipient                        | Creates Friend records for both users |
| `/api/friend-requests` | `/:requestId/reject` | PATCH  | Reject friend request | Params: `requestId`         | `{ id, status: "rejected", updatedAt }`                                                                        | `auth`     | `401` Unauthorized<br>`404` Request not found<br>`403` Not recipient                        | Updates status to rejected            |
| `/api/friend-requests` | `/:requestId`        | DELETE | Cancel sent request   | Params: `requestId`         | `{ message: "Request cancelled" }`                                                                             | `auth`     | `401` Unauthorized<br>`404` Request not found<br>`403` Not sender                           | Only sender can delete                |

### Friend Routes (`/api/friends`)

| Router         | Endpoint     | Method | Description        | Input                       | Output                                                                                              | Middleware | Errors                                  | Notes                       |
| -------------- | ------------ | ------ | ------------------ | --------------------------- | --------------------------------------------------------------------------------------------------- | ---------- | --------------------------------------- | --------------------------- |
| `/api/friends` | `/`          | GET    | Get all friends    | Query: `?limit=50&offset=0` | `{ friends: [{ id, username, displayName, avatarUrl, lastSeen, becameFriendsAt }], total: number }` | `auth`     | `401` Unauthorized                      | Sorted by displayName       |
| `/api/friends` | `/:friendId` | GET    | Get friend details | Params: `friendId`          | `{ id, username, displayName, avatarUrl, lastSeen, becameFriendsAt }`                               | `auth`     | `401` Unauthorized<br>`404` Not friends | Check friendship exists     |
| `/api/friends` | `/:friendId` | DELETE | Unfriend user      | Params: `friendId`          | `{ message: "Friendship removed" }`                                                                 | `auth`     | `401` Unauthorized<br>`404` Not friends | Deletes both Friend records |

---

## Conversations

### Conversation Routes (`/api/conversations`)

| Router               | Endpoint           | Method | Description               | Input                                                                                             | Output                                                                                                                                                                                                                                            | Middleware                         | Errors                                                                                                          | Notes                                                              |
| -------------------- | ------------------ | ------ | ------------------------- | ------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- | --------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| `/api/conversations` | `/`                | GET    | Get all conversations     | Query: `?limit=20&offset=0`                                                                       | `{ conversations: [{ id, type, name, avatarUrl, isMuted, unreadCount, lastMessage: { id, content, senderId, senderName, createdAt, type }, participants: [{ userId, displayName, avatarUrl, isAdmin }], createdAt, updatedAt }], total: number }` | `auth`                             | `401` Unauthorized                                                                                              | Ordered by lastMessage.createdAt DESC                              |
| `/api/conversations` | `/`                | POST   | Create conversation       | `{ type: "direct" \| "group", participantId?: string, participantIds?: string[], name?: string }` | `{ id, type, name, avatarUrl, participants: [...], createdBy, createdAt }`                                                                                                                                                                        | `auth`                             | `401` Unauthorized<br>`400` Validation error<br>`404` Participant not found<br>`409` Direct conversation exists | Direct: needs participantId<br>Group: needs participantIds, name   |
| `/api/conversations` | `/:conversationId` | GET    | Get conversation details  | Params: `conversationId`                                                                          | `{ id, type, name, avatarUrl, participants: [{ userId, displayName, avatarUrl, isAdmin, joinedAt }], createdBy, createdAt, updatedAt }`                                                                                                           | `auth`, `isParticipant`            | `401` Unauthorized<br>`403` Not participant<br>`404` Not found                                                  | Full conversation info                                             |
| `/api/conversations` | `/:conversationId` | PATCH  | Update conversation       | Params: `conversationId`<br>Body: `{ name?: string, avatarUrl?: string }`                         | `{ id, type, name, avatarUrl, updatedAt }`                                                                                                                                                                                                        | `auth`, `isParticipant`, `isAdmin` | `401` Unauthorized<br>`403` Not admin<br>`404` Not found<br>`400` Direct chats can't be renamed                 | Group chats only, admin required                                   |
| `/api/conversations` | `/:conversationId` | DELETE | Leave/delete conversation | Params: `conversationId`                                                                          | `{ message: "Left conversation" }`                                                                                                                                                                                                                | `auth`, `isParticipant`            | `401` Unauthorized<br>`403` Not participant<br>`404` Not found                                                  | Removes user from participants. If last user, deletes conversation |

### Conversation Participant Routes (`/api/conversations/:conversationId/participants`)

| Router                               | Endpoint                | Method | Description          | Input                                                              | Output                                                                                  | Middleware                         | Errors                                                                                                                                 | Notes                            |
| ------------------------------------ | ----------------------- | ------ | -------------------- | ------------------------------------------------------------------ | --------------------------------------------------------------------------------------- | ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------- |
| `/api/conversations/:conversationId` | `/participants`         | GET    | Get all participants | Params: `conversationId`                                           | `{ participants: [{ userId, displayName, avatarUrl, isAdmin, joinedAt, lastReadAt }] }` | `auth`, `isParticipant`            | `401` Unauthorized<br>`403` Not participant<br>`404` Not found                                                                         | Returns all conversation members |
| `/api/conversations/:conversationId` | `/participants`         | POST   | Add participant      | Params: `conversationId`<br>Body: `{ userId: string }`             | `{ userId, displayName, avatarUrl, isAdmin: false, joinedAt }`                          | `auth`, `isParticipant`, `isAdmin` | `401` Unauthorized<br>`403` Not admin<br>`404` User/conversation not found<br>`409` Already participant<br>`400` Direct chat can't add | Group chats only, admin required |
| `/api/conversations/:conversationId` | `/participants/:userId` | DELETE | Remove participant   | Params: `conversationId`, `userId`                                 | `{ message: "Participant removed" }`                                                    | `auth`, `isParticipant`, `isAdmin` | `401` Unauthorized<br>`403` Not admin<br>`404` User not participant<br>`400` Can't remove last admin                                   | Admin required, group chats only |
| `/api/conversations/:conversationId` | `/participants/:userId` | PATCH  | Update participant   | Params: `conversationId`, `userId`<br>Body: `{ isAdmin: boolean }` | `{ userId, isAdmin, updatedAt }`                                                        | `auth`, `isParticipant`, `isAdmin` | `401` Unauthorized<br>`403` Not admin<br>`404` User not participant<br>`400` Can't demote last admin                                   | Promote/demote admin, group only |

### Conversation Settings Routes (`/api/conversations/:conversationId`)

| Router                               | Endpoint  | Method | Description          | Input                    | Output                                      | Middleware              | Errors                                                         | Notes                                               |
| ------------------------------------ | --------- | ------ | -------------------- | ------------------------ | ------------------------------------------- | ----------------------- | -------------------------------------------------------------- | --------------------------------------------------- |
| `/api/conversations/:conversationId` | `/mute`   | PATCH  | Mute notifications   | Params: `conversationId` | `{ conversationId, isMuted: true }`         | `auth`, `isParticipant` | `401` Unauthorized<br>`403` Not participant<br>`404` Not found | Updates ConversationParticipant.isMuted             |
| `/api/conversations/:conversationId` | `/unmute` | PATCH  | Unmute notifications | Params: `conversationId` | `{ conversationId, isMuted: false }`        | `auth`, `isParticipant` | `401` Unauthorized<br>`403` Not participant<br>`404` Not found | Updates ConversationParticipant.isMuted             |
| `/api/conversations/:conversationId` | `/read`   | PATCH  | Mark as read         | Params: `conversationId` | `{ conversationId, lastReadAt: timestamp }` | `auth`, `isParticipant` | `401` Unauthorized<br>`403` Not participant<br>`404` Not found | Updates ConversationParticipant.lastReadAt to NOW() |

---

## Messages

### Message Routes (`/api/conversations/:conversationId/messages`, `/api/messages`)

| Router                               | Endpoint            | Method | Description      | Input                                                                                                                                              | Output                                                                                                                                                                                                                                          | Middleware               | Errors                                                                                                        | Notes                                              |
| ------------------------------------ | ------------------- | ------ | ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------ | ------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| `/api/conversations/:conversationId` | `/messages`         | GET    | Get messages     | Params: `conversationId`<br>Query: `?limit=50&before=msgId&after=msgId`                                                                            | `{ messages: [{ id, conversationId, senderId, sender: { displayName, avatarUrl }, content, type, attachmentUrl, replyTo: { messageId, content, senderId }, isEdited, isDeleted, createdAt, updatedAt }], pagination: { hasMore, nextCursor } }` | `auth`, `isParticipant`  | `401` Unauthorized<br>`403` Not participant<br>`404` Not found                                                | Ordered by createdAt DESC. Auto marks as read      |
| `/api/conversations/:conversationId` | `/messages`         | POST   | Send message     | Params: `conversationId`<br>Body: `{ content: string, type: "text"\|"image"\|"file"\|"voice", attachmentUrl?: string, replyToMessageId?: string }` | `{ id, conversationId, senderId, content, type, attachmentUrl, replyToMessageId, createdAt }`                                                                                                                                                   | `auth`, `isParticipant`  | `401` Unauthorized<br>`403` Not participant<br>`404` Conversation/replyTo not found<br>`400` Validation error | Broadcasts via WebSocket                           |
| `/api/messages`                      | `/:messageId`       | PATCH  | Edit message     | Params: `messageId`<br>Body: `{ content: string }`                                                                                                 | `{ id, content, isEdited: true, updatedAt }`                                                                                                                                                                                                    | `auth`, `isMessageOwner` | `401` Unauthorized<br>`403` Not owner<br>`404` Not found<br>`400` Can't edit deleted                          | Only text messages, within 15 min                  |
| `/api/messages`                      | `/:messageId`       | DELETE | Delete message   | Params: `messageId`                                                                                                                                | `{ id, isDeleted: true, deletedAt }`                                                                                                                                                                                                            | `auth`, `isMessageOwner` | `401` Unauthorized<br>`403` Not owner<br>`404` Not found                                                      | Soft delete, sets isDeleted=true                   |
| `/api/messages`                      | `/:messageId/reply` | POST   | Reply to message | Params: `messageId`<br>Body: `{ content: string, type: "text"\|"image"\|"file"\|"voice", attachmentUrl?: string }`                                 | `{ id, conversationId, senderId, content, type, replyToMessageId, createdAt }`                                                                                                                                                                  | `auth`, `isParticipant`  | `401` Unauthorized<br>`403` Not participant<br>`404` Message not found                                        | Shorthand for POST /messages with replyToMessageId |

### Message Upload Routes (`/api/messages`)

| Router          | Endpoint  | Method | Description              | Input                                                            | Output                                                      | Middleware              | Errors                                                                 | Notes                                                                                  |
| --------------- | --------- | ------ | ------------------------ | ---------------------------------------------------------------- | ----------------------------------------------------------- | ----------------------- | ---------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `/api/messages` | `/upload` | POST   | Get presigned upload URL | `{ fileName: string, fileType: string, conversationId: string }` | `{ uploadUrl: string, fileUrl: string, expiresIn: number }` | `auth`, `isParticipant` | `401` Unauthorized<br>`403` Not participant<br>`400` Invalid file type | Returns S3 presigned URL. Client uploads directly to S3, then sends fileUrl in message |

### Message Status Routes (`/api/messages/:messageId`)

| Router                     | Endpoint  | Method | Description        | Input               | Output                                                                                                     | Middleware              | Errors                                                         | Notes                              |
| -------------------------- | --------- | ------ | ------------------ | ------------------- | ---------------------------------------------------------------------------------------------------------- | ----------------------- | -------------------------------------------------------------- | ---------------------------------- |
| `/api/messages/:messageId` | `/status` | GET    | Get message status | Params: `messageId` | `{ messageId, status: { sent: true, delivered: [{ userId, timestamp }], read: [{ userId, timestamp }] } }` | `auth`, `isParticipant` | `401` Unauthorized<br>`403` Not participant<br>`404` Not found | Shows delivery/read receipts       |
| `/api/messages/:messageId` | `/status` | PATCH  | Update read status | Params: `messageId` | `{ messageId, userId, status: "read", statusAt: timestamp }`                                               | `auth`, `isParticipant` | `401` Unauthorized<br>`403` Not participant<br>`404` Not found | Auto-called when fetching messages |

---

## WebSocket Real-time

### WebSocket Connection

| Router | Endpoint          | Method | Description          | Input               | Output                 | Middleware       | Errors                                     | Notes                                             |
| ------ | ----------------- | ------ | -------------------- | ------------------- | ---------------------- | ---------------- | ------------------------------------------ | ------------------------------------------------- |
| N/A    | `/ws?token=<jwt>` | WS     | WebSocket connection | Query: `?token=jwt` | Connection established | Token validation | `401` Invalid token<br>`403` Token expired | Heartbeat every 30s. Auto-reconnect on disconnect |

### WebSocket Events (Client → Server)

| Event Type     | Description          | Payload                      | Response                                    | Errors                                           | Notes                          |
| -------------- | -------------------- | ---------------------------- | ------------------------------------------- | ------------------------------------------------ | ------------------------------ |
| `typing_start` | User started typing  | `{ conversationId: string }` | Broadcasts to other participants            | `403` Not participant                            | Expires after 5s of inactivity |
| `typing_stop`  | User stopped typing  | `{ conversationId: string }` | Broadcasts to other participants            | `403` Not participant                            | Explicit stop signal           |
| `message_read` | Mark message as read | `{ messageId: string }`      | Updates MessageStatus, broadcasts to sender | `403` Not participant<br>`404` Message not found | Also updates lastReadAt        |
| `online`       | Set user online      | None                         | Broadcasts to friends                       | N/A                                              | Sets lastSeen to NOW()         |
| `offline`      | Set user offline     | None                         | Broadcasts to friends                       | N/A                                              | Sets lastSeen to NOW()         |

### WebSocket Events (Server → Client)

| Event Type                | Description                    | Payload                                                                                    | Trigger                     | Notes                             |
| ------------------------- | ------------------------------ | ------------------------------------------------------------------------------------------ | --------------------------- | --------------------------------- |
| `message_new`             | New message received           | `{ conversationId, message: { id, senderId, sender, content, type, replyTo, createdAt } }` | When message is sent        | Only to conversation participants |
| `message_updated`         | Message was edited             | `{ conversationId, messageId, content, isEdited: true, updatedAt }`                        | When message edited         | Only to conversation participants |
| `message_deleted`         | Message was deleted            | `{ conversationId, messageId, isDeleted: true, deletedAt }`                                | When message deleted        | Only to conversation participants |
| `user_typing`             | User is typing                 | `{ conversationId, userId, displayName }`                                                  | When typing_start received  | Only to other participants        |
| `user_stopped_typing`     | User stopped typing            | `{ conversationId, userId }`                                                               | When typing_stop or timeout | Only to other participants        |
| `user_online`             | User came online               | `{ userId, displayName, avatarUrl }`                                                       | When user connects          | Only to user's friends            |
| `user_offline`            | User went offline              | `{ userId, lastSeen }`                                                                     | When user disconnects       | Only to user's friends            |
| `message_read`            | Message was read               | `{ conversationId, messageId, userId, readAt }`                                            | When message_read received  | Only to message sender            |
| `conversation_updated`    | Conversation changed           | `{ conversationId, changes: { name?, avatarUrl? } }`                                       | When conversation edited    | Only to participants              |
| `participant_added`       | User added to conversation     | `{ conversationId, participant: { userId, displayName, avatarUrl } }`                      | When participant added      | Only to participants              |
| `participant_removed`     | User removed from conversation | `{ conversationId, userId }`                                                               | When participant removed    | Only to participants              |
| `friend_request_received` | New friend request             | `{ requestId, fromUser: { id, username, displayName, avatarUrl }, createdAt }`             | When friend request sent    | Only to recipient                 |
| `friend_request_accepted` | Friend request accepted        | `{ requestId, user: { id, username, displayName, avatarUrl } }`                            | When request accepted       | Only to sender                    |

---

## Optional/Advanced Routes

### Search Routes (`/api/messages`, `/api/conversations`)

| Router               | Endpoint  | Method | Description          | Input                                                | Output                                                                                            | Middleware | Errors                                      | Notes                         |
| -------------------- | --------- | ------ | -------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------- | ----------------------------- |
| `/api/messages`      | `/search` | GET    | Search messages      | Query: `?q=keyword&conversationId=conv-123&limit=20` | `{ messages: [{ id, conversationId, content, senderId, senderName, createdAt }], total: number }` | `auth`     | `401` Unauthorized<br>`400` Query too short | Min 3 chars, full-text search |
| `/api/conversations` | `/search` | GET    | Search conversations | Query: `?q=keyword&limit=20`                         | `{ conversations: [{ id, name, type, participants }], total: number }`                            | `auth`     | `401` Unauthorized<br>`400` Query too short | Searches conversation names   |

### Media Routes (`/api/conversations/:conversationId`)

| Router                               | Endpoint | Method | Description   | Input                                                                    | Output                                                                                            | Middleware              | Errors                                      | Notes                     |
| ------------------------------------ | -------- | ------ | ------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------- | ----------------------- | ------------------------------------------- | ------------------------- |
| `/api/conversations/:conversationId` | `/media` | GET    | Get all media | Params: `conversationId`<br>Query: `?type=image\|file&limit=20&offset=0` | `{ media: [{ messageId, type, attachmentUrl, senderId, senderName, createdAt }], total: number }` | `auth`, `isParticipant` | `401` Unauthorized<br>`403` Not participant | Ordered by createdAt DESC |

### User Presence Routes (`/api/user`)

| Router       | Endpoint          | Method | Description          | Input                                             | Output                         | Middleware | Errors                                     | Notes                                 |
| ------------ | ----------------- | ------ | -------------------- | ------------------------------------------------- | ------------------------------ | ---------- | ------------------------------------------ | ------------------------------------- |
| `/api/user`  | `/status`         | PATCH  | Update online status | `{ status: "online"\|"away"\|"busy"\|"offline" }` | `{ userId, status, lastSeen }` | `auth`     | `401` Unauthorized                         | Custom status. Also set via WebSocket |
| `/api/users` | `/:userId/status` | GET    | Get user status      | Params: `userId`                                  | `{ userId, status, lastSeen }` | `auth`     | `401` Unauthorized<br>`404` User not found | Check if user is online               |

### Notification Routes (`/api/notifications`)

| Router               | Endpoint                | Method | Description       | Input                                       | Output                                                                                         | Middleware | Errors                                | Notes                             |
| -------------------- | ----------------------- | ------ | ----------------- | ------------------------------------------- | ---------------------------------------------------------------------------------------------- | ---------- | ------------------------------------- | --------------------------------- |
| `/api/notifications` | `/`                     | GET    | Get notifications | Query: `?limit=20&offset=0&unreadOnly=true` | `{ notifications: [{ id, type, title, body, data, isRead, createdAt }], unreadCount: number }` | `auth`     | `401` Unauthorized                    | Message, friend request, mentions |
| `/api/notifications` | `/read`                 | PATCH  | Mark all as read  | None                                        | `{ message: "All notifications marked as read" }`                                              | `auth`     | `401` Unauthorized                    | Sets isRead=true for all          |
| `/api/notifications` | `/:notificationId/read` | PATCH  | Mark one as read  | Params: `notificationId`                    | `{ id, isRead: true }`                                                                         | `auth`     | `401` Unauthorized<br>`404` Not found | Individual notification           |

<!-- ### Blocking Routes (`/api/blocks`)

| Router | Endpoint | Method | Description | Input | Output | Middleware | Errors | Notes |
|--------|----------|--------|-------------|-------|--------|------------|--------|-------|
| `/api/blocks` | `/` | POST | Block a user | `{ userId: string }` | `{ id, blockedUserId, createdAt }` | `auth` | `401` Unauthorized<br>`404` User not found<br>`409` Already blocked<br>`400` Can't block self | Blocks messages, friend requests |
| `/api/blocks` | `/` | GET | Get blocked users | Query: `?limit=20&offset=0` | `{ blockedUsers: [{ id, username, displayName, avatarUrl, blockedAt }] }` | `auth` | `401` Unauthorized | List of blocked users |
| `/api/blocks` | `/:userId` | DELETE | Unblock user | Params: `userId` | `{ message: "User unblocked" }` | `auth` | `401` Unauthorized<br>`404` Not blocked | Remove block |

--- -->

## Middleware Definitions

| Middleware       | Description                            | Implementation                                                   |
| ---------------- | -------------------------------------- | ---------------------------------------------------------------- |
| `auth`           | Verify JWT token                       | Check Authorization header, verify token, attach user to request |
| `upload`         | Handle file uploads                    | Multer/multipart middleware, validate file type/size             |
| `isParticipant`  | Check user is conversation participant | Query ConversationParticipant table                              |
| `isAdmin`        | Check user is conversation admin       | Query ConversationParticipant.isAdmin                            |
| `isMessageOwner` | Check user owns the message            | Query Message.senderId                                           |
| `rateLimit`      | Rate limiting                          | Limit requests per IP/user (e.g., 100/min)                       |

---

## Common Error Codes

| Status | Code                    | Description                              |
| ------ | ----------------------- | ---------------------------------------- |
| 400    | `VALIDATION_ERROR`      | Invalid input data                       |
| 400    | `BAD_REQUEST`           | General bad request                      |
| 401    | `UNAUTHORIZED`          | Not authenticated                        |
| 403    | `FORBIDDEN`             | Not authorized for this action           |
| 404    | `NOT_FOUND`             | Resource doesn't exist                   |
| 409    | `CONFLICT`              | Resource already exists / state conflict |
| 413    | `PAYLOAD_TOO_LARGE`     | File/request too large                   |
| 429    | `RATE_LIMIT_EXCEEDED`   | Too many requests                        |
| 500    | `INTERNAL_SERVER_ERROR` | Server error                             |

---

## Rate Limiting

| Endpoint Pattern              | Limit        | Window     |
| ----------------------------- | ------------ | ---------- |
| `/api/auth/register`          | 3 requests   | 1 hour     |
| `/api/auth/login`             | 5 requests   | 15 minutes |
| `/api/auth/forgot-password`   | 3 requests   | 1 hour     |
| `/api/messages` (POST)        | 60 requests  | 1 minute   |
| `/api/friend-requests` (POST) | 10 requests  | 1 hour     |
| `/api/*/search`               | 30 requests  | 1 minute   |
| All other endpoints           | 100 requests | 1 minute   |

---

## Notes

- All timestamps are in ISO 8601 format (UTC)
- All IDs are UUIDv4
- Pagination uses cursor-based for messages, offset-based for lists
- File uploads use presigned URLs (S3) for direct client-to-storage uploads
- WebSocket automatically reconnects on disconnect
- All endpoints return JSON
- CORS enabled for specified origins
- HTTPS required in production
