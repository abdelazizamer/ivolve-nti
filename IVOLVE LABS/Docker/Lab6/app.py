import os
from flask import Flask, jsonify

app = Flask(__name__)


@app.get("/")
def home():
    return jsonify(
        application=os.getenv("APP_NAME", "iVolveApp"),
        environment=os.getenv("APP_ENV", "dev"),
        message="Welcome to the Docker Env & Resource Management Lab!",
    )


@app.get("/health")
def health():
    return jsonify(status="healthy")


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
