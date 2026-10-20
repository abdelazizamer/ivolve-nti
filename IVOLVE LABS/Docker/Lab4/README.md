# Lab 4: Docker Part 2 - Local Build & Lightweight JRE Image

## Overview
In this lab, the application is pre-built locally using Maven, and only the resulting JAR is copied into a lightweight Java Runtime Environment (`eclipse-temurin:17-jre-noble`).

## Image Comparison (`app1` vs `app2`)
- **`app1` (Built inside Docker)**: Larger size due to full Maven SDK, JDK, and build toolchains.
- **`app2` (Local Build + JRE)**: Significantly smaller because it contains only the minimal runtime environment and compiled JAR.
