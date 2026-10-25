const express = require("express");
const path = require("path");
const { pool, initializeDatabase } = require("./db");

const app = express();
const port = 3000;

app.use(express.json());
app.use(express.static(path.join(__dirname, "frontend")));

app.get("/health", async (req, res) => {
  try {
    await pool.query("SELECT 1");
    res.json({ status: "healthy", database: "connected" });
  } catch (error) {
    res.status(503).json({ status: "unhealthy", error: error.message });
  }
});

app.get("/api/messages", async (req, res) => {
  try {
    const [messages] = await pool.query(
      "SELECT * FROM messages ORDER BY id DESC"
    );
    res.json(messages);
  } catch (error) {
    res.status(500).json({ error: "Could not retrieve messages" });
  }
});

app.post("/api/messages", async (req, res) => {
  const message = req.body.message?.trim();

  if (!message) {
    return res.status(400).json({ error: "Message is required" });
  }

  try {
    const [result] = await pool.execute(
      "INSERT INTO messages (message) VALUES (?)",
      [message]
    );
    res.status(201).json({ id: result.insertId, message });
  } catch (error) {
    res.status(500).json({ error: "Could not save message" });
  }
});

async function start() {
  await initializeDatabase();
  app.listen(port, "0.0.0.0", () => {
    console.log(`App listening on port ${port}`);
  });
}

start().catch(error => {
  console.error("Failed to start application:", error);
  process.exit(1);
});
