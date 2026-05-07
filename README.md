# PrivacyLens AI (Internet Privacy & Safety Manager)

## Core Idea
PrivacyLens AI is a mobile application designed to scan and analyze the privacy policies of apps installed on your device. Its primary purpose is to empower users by making complex legal language transparent. By leveraging the power of Google's Generative AI (Gemini APIs), it reads through lengthy privacy policies and extracts the most critical details on how an application collects, uses, and shares your personal data. It also calculates an overall risk score and presents a clean, easy-to-understand Scan Result UI so you know exactly what you are agreeing to.

Beyond static policy analysis, PrivacyLens AI features live **Permission Monitoring** and a **Privacy Fix Center**. It automatically tracks apps accessing sensitive permissions (such as Microphone or Camera) in the background, logging precise timestamps and generating Weekly Behavior Reports. It identifies dangerous permission combinations and gives users actionable, one-tap buttons to immediately open system settings and revoke access where needed.

## Project Architecture & Technologies

This project is divided into two main components: an Android Frontend (Mobile App) and a Python Backend (REST API).

### 1. Android Frontend (Mobile App)
The frontend provides the user interface to select apps, initiate scans, and view easily digestible risk reports.
- **Language**: Kotlin
- **UI & Layout**: XML Layouts, ConstraintLayout, and Material Design Components. ViewBinding is utilized for safer view interactions. Edge-to-edge system window inset handling ensures a modern Android UI.
- **Architecture & Local Storage**: AndroidX ViewModel and Kotlin Coroutines handle UI data and asynchronous background tasks without blocking the main thread. AndroidX WorkManager (Workers) and Foreground Services manage continuous, real-time permission monitoring. A local Room Database is used to safely persist logged permission access events.
- **Networking**: Retrofit2 and Gson are used to communicate with the Python backend API and parse the incoming JSON responses.

### 2. Python Backend (REST API)
The backend acts as the processing engine, responsible for fetching privacy policies from the internet and utilizing AI for complex text analysis to return structured insights to the app.
- **Core Framework**: FastAPI running on an Uvicorn server, providing high-performance RESTful API endpoints (e.g., `/analyze-policy`).
- **Data Validation**: Pydantic ensures data correctly conforms to expected formats automatically.
- **Web Scraping & Parsing**: The `requests` library and BeautifulSoup4 fetch live privacy policy webpages and extract clean, readable text from raw HTML content.
- **AI Analysis**: Google Generative AI (Gemini SDK) analyzes the cleaned text, identifying data collection points, third-party sharing practices, and formatting the analysis into a structured JSON payload for risk assessment.

## Summary
By combining modern Android application architecture with cutting-edge AI processing on a FastAPI backend, PrivacyLens AI simplifies the complex world of internet privacy and gives users straightforward access to safety insights.
