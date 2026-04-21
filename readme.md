# ChatApp — Multi-Client Real-Time Chat Application

A full-featured, Java-based client-server chat application with GUI, multiple chat rooms, private messaging, typing indicators, XOR encryption, and MySQL persistence.

> Built for Object-Oriented Programming — SEECS, NUST

---

## Table of Contents

- [Features](#features)
- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Dependencies](#dependencies)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [How to Run](#how-to-run)
- [All Commands](#all-commands)
- [Encryption](#encryption)
- [Troubleshooting](#troubleshooting)

---

## Features

| Feature | Details |
|---|---|
| Real-time group messaging | All users in a room receive messages instantly |
| Multiple chat rooms | Create, join, and switch rooms freely |
| Private messaging | Direct messages between two users only |
| Typing indicator | Live "Ali is typing..." shown to others |
| Message history | Last 50 messages loaded from DB on join |
| Online users list | See who is currently connected |
| Admin controls | Kick users with elevated admin account |
| XOR encryption | All messages encrypted over the network |
| Persistent storage | All messages saved to MySQL database |
| Swing GUI | Full graphical interface with emoji bar and sound |

---

## Architecture Overview

```
┌─────────────────┐        Encrypted TCP        ┌──────────────────────┐
│   ClientGUI.java│◄──────────────────────────►│    Server.java        │
│   Client.java   │        (port 1234)          │    ClientHandler.java │
└─────────────────┘                             └──────────┬───────────┘
                                                           │
                                                           ▼
                                                ┌──────────────────────┐
                                                │  DatabaseManager.java │
                                                │  MySQL (chatapp DB)   │
                                                └──────────────────────┘
```

- **Server** runs on one machine, opens port `1234`, spawns one thread per connected client
- **Client** connects to the server's IP/hostname, encrypts all outgoing messages
- **Server never stores or re-broadcasts encrypted data** — it decrypts, saves plain text to DB, then re-encrypts for each recipient
- **Database** persists all messages, users, rooms, and online status

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Java JDK | 17 or higher | JDK 25 also confirmed working |
| MySQL Server | 8.0 or higher | Must be running before starting Server |
| MySQL Connector/J | 9.x | `.jar` file added to classpath |
| IntelliJ IDEA | Any | Community Edition is free |

---

## Dependencies

### 1. Java JDK
Download from: https://www.oracle.com/java/technologies/downloads/

Verify installation:
```bash
java -version
```

### 2. MySQL Server
Download from: https://dev.mysql.com/downloads/mysql/

Start MySQL service (Windows):
```bash
net start MySQL80
```

Start MySQL service (Linux/macOS):
```bash
sudo systemctl start mysql
```

### 3. MySQL Connector/J (JDBC Driver)
Download from: https://dev.mysql.com/downloads/connector/j/

In IntelliJ:
1. `File` → `Project Structure` → `Modules` → `Dependencies`
2. Click `+` → `JARs or Directories`
3. Select your downloaded `mysql-connector-j-x.x.x.jar`
4. Click `Apply` → `OK`

> No other external libraries are required. All other features use the Java Standard Library (Swing, Socket, Thread, etc.)

---

## Database Setup

### Step 1 — Create the Database

Open MySQL Workbench or your MySQL client and run:

```sql
CREATE DATABASE IF NOT EXISTS chatapp;
USE chatapp;
```

### Step 2 — Create All Tables

```sql
-- Users table
CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(64) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_online     BOOLEAN DEFAULT FALSE
);

-- Rooms table
CREATE TABLE rooms (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    room_name  VARCHAR(50) UNIQUE NOT NULL,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Public messages table
CREATE TABLE messages (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    sender_username VARCHAR(50) NOT NULL,
    room_name       VARCHAR(50) NOT NULL,
    message_text    TEXT NOT NULL,
    sent_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_username) REFERENCES users(username),
    FOREIGN KEY (room_name)       REFERENCES rooms(room_name)
);

-- Private messages table
CREATE TABLE private_messages (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    sender_username   VARCHAR(50) NOT NULL,
    receiver_username VARCHAR(50) NOT NULL,
    message_text      TEXT NOT NULL,
    sent_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read           BOOLEAN DEFAULT FALSE
);

-- Room members table
CREATE TABLE room_members (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50) NOT NULL,
    room_name VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default room (required — messages table has a FK to rooms)
INSERT IGNORE INTO rooms (room_name, created_by) VALUES ('general', 'system');
```

### Step 3 — Verify Tables

```sql
SHOW TABLES;
-- Should show: messages, private_messages, room_members, rooms, users
```

---

## Configuration

### Change Database Password

Open `DatabaseManager.java` and edit these three lines at the top:

```java
private static final String URL  = "jdbc:mysql://localhost:3306/chatapp";
private static final String USER = "root";          // ← your MySQL username
private static final String PASS = "your_password"; // ← your MySQL password
```

> If MySQL is on a different machine, replace `localhost` with that machine's IP address.

### Change Server Address (for Clients)

In `Client.java`:
```java
private static final String SERVER_ADDRESS = "bore.pub"; // ← change to your server IP
private static final int PORT = 61664;                   // ← change to match Server.java port
```

In `ClientGUI.java` (line inside `connectToServer()`):
```java
Socket socket = new Socket("bore.pub", Integer.parseInt(portTx));
// ← change "bore.pub" to your server's IP if not using bore/tunnel
```

### Change Server Port

In `Server.java`:
```java
private static final int PORT = 1234; // ← change to any available port
```

Make sure the same port is used in `Client.java` and `ClientGUI.java`.

### Using a Public Tunnel (bore.pub)

If you want clients to connect from outside your local network, use [bore](https://github.com/ekzhang/bore):

```bash
# On the server machine, after starting Server.java:
bore local 1234 --to bore.pub
```

This gives you a public port like `bore.pub:61664`. Put that hostname and port in your client files.

---

## Project Structure

```
ChatApp/
├── src/
│   ├── server/
│   │   ├── Server.java          # Main server — accepts connections, manages rooms
│   │   ├── ClientHandler.java   # One instance per connected user — handles all I/O
│   │   └── DatabaseManager.java # All MySQL queries (save/load messages, users, rooms)
│   ├── client/
│   │   ├── ClientGUI.java       # Swing GUI client (use this one)
│   │   └── Client.java          # Console client (for testing without GUI)
│   └── common/
│       └── EncryptionUtil.java  # XOR + Base64 encryption shared by client and server
└── lib/
    └── mysql-connector-j-x.x.x.jar
```

---

## How to Run

### Step 1 — Start MySQL
Make sure your MySQL server is running and the `chatapp` database and all tables exist (see [Database Setup](#database-setup)).

### Step 2 — Run the Server

In IntelliJ, right-click `Server.java` → `Run`. You should see:

```
DATABASE: Connection Successful!
Server started on port 1234
```

If you see `DATABASE: Connection Failed!`, your password in `DatabaseManager.java` is wrong or MySQL is not running.

### Step 3 — Run the Client

Right-click `ClientGUI.java` → `Run`. A login dialog will appear asking for:
- **Username** — any name you want to use
- **Port** — the port bore.pub gave you (or `1234` if running locally)

To run multiple clients simultaneously, go to IntelliJ `Run` → `Edit Configurations` → check **Allow multiple instances**, then run `ClientGUI.java` again.

### Step 4 — Connect Locally (No Tunnel)

If the server and client are on the same machine, change the server address in `ClientGUI.java` to:
```java
Socket socket = new Socket("localhost", Integer.parseInt(portTx));
```
And use port `1234`.

---

## All Commands

Type these in the chat input field and press Send or Enter.

| Command | Description | Example |
|---|---|---|
| `/users` | List all currently online users | `/users` |
| `/rooms` | List all available chat rooms | `/rooms` |
| `/create [name]` | Create a new chat room | `/create coding` |
| `/join [name]` | Switch to a different room | `/join coding` |
| `/msg [user] [text]` | Send a private message to one user | `/msg Ali hello there` |
| `/typing` | Manually send a typing signal (auto-sent by GUI) | `/typing` |
| `/kick [user]` | (Admin only) Disconnect a user | `/kick spammer` |
| `/quit` | Disconnect from the server cleanly | `/quit` |

### Admin Account

Connect with the username `admin` to unlock `/kick`. The admin can remove any connected user:

```
/kick username
```

The kicked user sees: `SYSTEM: You have been kicked by the admin.`
All other users are notified.

> **Note:** Currently any user can log in as `admin` since there is no password prompt. This is a known limitation.

---

## Encryption

All messages are encrypted **before leaving the client machine** and decrypted **after arriving at the server**. The server saves plain text to the database and re-encrypts before forwarding to other clients.

### How It Works

```
Sender types:  "hello"
               ↓  XOR each character with 'X'
               ↓  Base64 encode the result
Sent over network: "MR8="

Server receives "MR8="
               ↓  Base64 decode
               ↓  XOR each byte with 'X'
Saved to DB:   "hello"
               ↓  XOR + Base64 again
Sent to others: "MR8="

Recipient receives "MR8="
               ↓  Base64 decode + XOR
Displayed:     "hello"
```

The XOR key is the character `'X'` (ASCII 88), defined in `EncryptionUtil.java`:
```java
private static final char XOR_KEY = 'X';
```

> This is a basic symmetric cipher suitable for academic demonstration. For real security, replace with AES-256.

---

## Troubleshooting

### `DATABASE: Connection Failed!`
- MySQL is not running → start it with `net start MySQL80` (Windows) or `sudo systemctl start mysql` (Linux)
- Wrong password in `DatabaseManager.java` → update `PASS`
- Database `chatapp` does not exist → run `CREATE DATABASE chatapp;`

### `DB ERROR in saveMessage: Cannot add or update a child row: foreign key constraint fails`
The `general` room does not exist in the `rooms` table. Run:
```sql
INSERT IGNORE INTO rooms (room_name, created_by) VALUES ('general', 'system');
```

### `DB ERROR in getRoomHistory: Unknown column 'sender' in 'field list'`
Your database column names don't match the code. The correct column names are:
- `messages` table: `sender_username`, `room_name`, `message_text`, `sent_at`
- `private_messages` table: `sender_username`, `receiver_username`, `message_text`

Recreate tables using the SQL in [Database Setup](#database-setup).

### Client cannot connect to server
- Server is not running → start `Server.java` first
- Wrong IP/hostname in `ClientGUI.java` → update the address
- Firewall blocking port 1234 → allow it or change the port
- Using bore.pub tunnel → make sure bore is running on the server machine

### Messages showing as encrypted in database
- Both `ClientGUI.java` and `ClientHandler.java` must use `StandardCharsets.UTF_8` on their streams — charset mismatch causes `decrypt()` to return raw Base64
- Make sure you are using the latest versions of both files

### History not showing on join
- The `general` room must exist in the `rooms` table (see FK fix above)
- Messages must have been saved successfully (check server console for `SAVING TO DB [plain text]: ...`)
- History is limited to the last 50 messages per room

### `ConcurrentModificationException` crash with multiple users
Replace in `Server.java`:
```java
// Change this:
static final List<ClientHandler> clients = new ArrayList<>();
// To this:
static final List<ClientHandler> clients = new java.util.concurrent.CopyOnWriteArrayList<>();
```

---
