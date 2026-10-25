const mysql = require("mysql2/promise");

const pool = mysql.createPool({
  host: process.env.DB_HOST || "db",
  user: process.env.DB_USER || "ivolve_user",
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME || "ivolve",
  waitForConnections: true,
  connectionLimit: 10
});

async function initializeDatabase() {
  await pool.execute(`
    CREATE TABLE IF NOT EXISTS messages (
      id INT AUTO_INCREMENT PRIMARY KEY,
      message VARCHAR(255) NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
  `);
}

module.exports = { pool, initializeDatabase };
