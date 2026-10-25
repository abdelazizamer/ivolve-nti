const express = require("express");
const mysql = require("mysql2/promise");
const fs = require("fs");

const app = express();
app.use(express.json());

const port = 3000;
const logFile = "/app/logs/app.log";

function log(message) {
  const line = `${new Date().toISOString()} ${message}\n`;
  console.log(line.trim());
  fs.appendFileSync(logFile, line);
}

async function connectToDatabase() {
  while (true) {
    try {
      const connection = await mysql.createConnection({
        host: process.env.DB_HOST || "db",
        user: process.env.DB_USER || "ivolve_user",
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME || "ivolve"
      });

      await connection.execute(`
        CREATE TABLE IF NOT EXISTS messages (
          id INT AUTO_INCREMENT PRIMARY KEY,
          message VARCHAR(255) NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
      `);

      log("Connected to MySQL and initialized the messages table");
      return connection;
    } catch (error) {
      log(`Waiting for MySQL: ${error.message}`);
      await new Promise(resolve => setTimeout(resolve, 3000));
    }
  }
}

async function start() {
  fs.mkdirSync("/app/logs", { recursive: true });
  const db = await connectToDatabase();

  app.get("/", (req, res) => {
    res.json({ message: "iVolve Lab 8 app is running", database: "MySQL" });
  });

  app.get("/health", async (req, res) => {
    try {
      await db.query("SELECT 1");
      res.json({ status: "healthy", database: "connected" });
    } catch {
      res.status(503).json({ status: "unhealthy", database: "disconnected" });
    }
  });

  app.get("/messages", async (req, res) => {
    try {
      const [rows] = await db.query("SELECT * FROM messages ORDER BY id");
      res.json(rows);
    } catch (error) {
      log(`Could not retrieve messages: ${error.message}`);
      res.status(500).json({ error: "Could not retrieve messages" });
    }
  });

  app.post("/messages", async (req, res) => {
    const message = req.body.message;
    if (!message) {
      return res.status(400).json({ error: "Provide a message in the request body" });
    }

    try {
      const [result] = await db.execute(
        "INSERT INTO messages (message) VALUES (?)",
        [message]
      );
      log(`Saved message with id ${result.insertId}`);
      res.status(201).json({ id: result.insertId, message });
    } catch (error) {
      log(`Could not save message: ${error.message}`);
      res.status(500).json({ error: "Could not save message" });
    }
  });

  app.listen(port, "0.0.0.0", () => log(`App listening on port ${port}`));
}

start();
