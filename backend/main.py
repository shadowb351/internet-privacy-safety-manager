from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, HttpUrl
import requests
from bs4 import BeautifulSoup
import google.generativeai as genai
import json

app = FastAPI(title="Privacy Policy Analyzer")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins (update with specific domains in production if needed)
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

# Configure Gemini
import os

genai.configure(api_key=os.getenv("AIzaSyDpfMlWhrUgUtVn4UvzqJDjns6pJ9zu1CY"))
class PolicyRequest(BaseModel):
    policyUrl: HttpUrl

class AppInfo(BaseModel):
    name: str
    packageName: str

class ScanRequestPayload(BaseModel):
    apps: list[AppInfo]

def download_policy(url: str) -> str:
    try:
        response = requests.get(url, headers={"User-Agent": "Mozilla/5.0"}, timeout=15)
        response.raise_for_status()
        return response.text
    except requests.RequestException as e:
        raise HTTPException(status_code=400, detail=f"Failed to download policy: {str(e)}")

def clean_html(html_content: str) -> str:
    soup = BeautifulSoup(html_content, 'html.parser')
    for script_or_style in soup(["script", "style"]):
        script_or_style.extract()
    text = soup.get_text(separator=' ')
    lines = (line.strip() for line in text.splitlines())
    chunks = (phrase.strip() for line in lines for phrase in line.split("  "))
    cleaned_text = ' '.join(chunk for chunk in chunks if chunk)
    return cleaned_text[:9000]

def analyze_with_gemini(text: str) -> dict:
    try:
        model = genai.GenerativeModel("gemini-2.5-flash")
        prompt = f"""You are an AI privacy analysis system.

Analyze the following privacy policy text.

Extract and return ONLY valid JSON with the following structure:

{{
  "data_collected": [list all types of personal data collected],
  "third_party_sharing": true or false,
  "sensitive_data_detected": [list sensitive data types like location, contacts, financial data, biometrics],
  "summary": "3-4 sentence concise explanation"
}}

Rules:
- Return ONLY raw JSON.
- No markdown.
- No explanation.
- No extra text before or after JSON.
- If information is not found, return empty list or false.

Privacy Policy Text:
{text}"""

        response = model.generate_content(prompt)
        response_text = response.text.strip()
        if response_text.startswith("```json"):
            response_text = response_text[7:]
            if response_text.endswith("```"):
                response_text = response_text[:-3]
        elif response_text.startswith("```"):
            response_text = response_text[3:]
            if response_text.endswith("```"):
                response_text = response_text[:-3]
        response_text = response_text.strip()
        return json.loads(response_text)
    except json.JSONDecodeError:
        raise HTTPException(status_code=500, detail="Failed to parse Gemini response as JSON.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Gemini API analysis failed: {str(e)}")

@app.post("/analyze-policy")
async def analyze_policy(request: PolicyRequest):
    html_content = download_policy(str(request.policyUrl))
    cleaned_text = clean_html(html_content)
    analysis_result = analyze_with_gemini(cleaned_text)
    return {"status": "success", "analysis": analysis_result}


# ---------------------------------------------------------------------------
# SMART RISK SCORING LOGIC
# ---------------------------------------------------------------------------

# Vague / suspicious sharing recipients that raise red flags regardless of app
SUSPICIOUS_SHARING_ENTITIES = [
    "law enforcement", "government", "affiliates", "subsidiaries",
    "third parties", "business partners", "advertising partners",
    "data brokers", "analytics providers"
]

# Entities that are suspicious only if the app has no advertising business model
AD_NETWORK_ENTITIES = [
    "meta", "facebook", "google", "admob", "applovin", "ironSource",
    "unity ads", "audience network"
]

# Sensitive data types and their base risk weights
SENSITIVE_DATA_WEIGHTS = {
    "precise location": 15,
    "location": 10,
    "contacts": 12,
    "biometrics": 18,
    "health data": 18,
    "financial data": 18,
    "payment info": 18,
    "credit card": 18,
    "ssn": 20,
    "government id": 20,
    "camera": 8,
    "microphone": 8,
    "call logs": 12,
    "sms": 12,
    "browsing history": 10,
    "clipboard": 8,
    "face data": 18,
    "voice data": 12,
}

# Domain profiles: for each app category, what data is EXPECTED (necessary)
# Everything else collected by that app is flagged as unnecessary
DOMAIN_PROFILES = {
    "image_sharing": {
        "keywords": ["photo", "camera", "picture", "gallery", "instagram", "pinterest", "snapchat", "flickr"],
        "expected_data": ["photos", "camera", "device info", "account info", "email", "username", "search history", "usage data"]
    },
    "social_media": {
        "keywords": ["social", "chat", "messenger", "twitter", "facebook", "linkedin", "tiktok", "reddit"],
        "expected_data": ["account info", "email", "username", "profile", "contacts", "messages", "photos", "usage data", "device info", "ip address"]
    },
    "navigation": {
        "keywords": ["map", "navigation", "gps", "waze", "transit", "directions"],
        "expected_data": ["location", "precise location", "search history", "device info", "account info"]
    },
    "health_fitness": {
        "keywords": ["health", "fitness", "workout", "diet", "sleep", "heart", "medical", "steps"],
        "expected_data": ["health data", "location", "device info", "account info", "usage data"]
    },
    "finance": {
        "keywords": ["bank", "payment", "finance", "wallet", "invest", "crypto", "money", "paypal", "venmo"],
        "expected_data": ["financial data", "payment info", "account info", "email", "device info", "ip address", "identity verification"]
    },
    "ecommerce": {
        "keywords": ["shop", "store", "buy", "amazon", "flipkart", "ebay", "market", "commerce"],
        "expected_data": ["payment info", "financial data", "account info", "email", "address", "purchase history", "search history", "device info"]
    },
    "productivity": {
        "keywords": ["notes", "task", "calendar", "office", "docs", "sheets", "drive", "productivity", "todo"],
        "expected_data": ["account info", "email", "files", "usage data", "device info"]
    },
    "entertainment": {
        "keywords": ["music", "video", "stream", "netflix", "spotify", "youtube", "podcast", "game"],
        "expected_data": ["account info", "email", "usage data", "device info", "payment info", "search history"]
    },
    "communication": {
        "keywords": ["call", "voip", "video call", "whatsapp", "telegram", "signal", "zoom", "meet"],
        "expected_data": ["contacts", "account info", "email", "messages", "device info", "ip address", "microphone", "camera"]
    },
    "utility": {
        "keywords": ["cleaner", "battery", "vpn", "antivirus", "file manager", "launcher", "keyboard"],
        "expected_data": ["device info", "usage data", "account info"]
    }
}

def detect_app_domain(app_name: str, package_name: str, data_collected: list[str]) -> str:
    """Detect the app's domain category from its name and package name."""
    combined = (app_name + " " + package_name).lower()
    for domain, profile in DOMAIN_PROFILES.items():
        for keyword in profile["keywords"]:
            if keyword in combined:
                return domain
    return "unknown"

def get_unnecessary_data(domain: str, data_collected: list[str]) -> list[str]:
    """Return data types collected by the app that are NOT expected for its domain."""
    if domain == "unknown":
        return []  # Can't judge if domain is unknown
    expected = [e.lower() for e in DOMAIN_PROFILES[domain]["expected_data"]]
    unnecessary = []
    for item in data_collected:
        item_lower = item.lower()
        if not any(exp in item_lower or item_lower in exp for exp in expected):
            unnecessary.append(item)
    return unnecessary

def score_sharing_entities(entities: list[str], domain: str) -> tuple[int, list[str]]:
    """
    Score suspiciousness of data sharing entities.
    Returns (score_addition, list_of_flagged_entities).
    """
    score = 0
    flagged = []
    is_ad_supported = domain in ("entertainment", "social_media", "image_sharing", "utility")

    for entity in entities:
        entity_lower = entity.lower()

        # Flag vague/broad sharing recipients
        for suspicious in SUSPICIOUS_SHARING_ENTITIES:
            if suspicious in entity_lower:
                score += 10
                flagged.append(f"{entity} (vague/broad recipient)")
                break

        # Flag ad network sharing for non-ad-supported apps
        if not is_ad_supported:
            for ad_net in AD_NETWORK_ENTITIES:
                if ad_net in entity_lower:
                    score += 12
                    flagged.append(f"{entity} (ad network sharing unexpected for this app type)")
                    break

    return min(score, 40), flagged  # Cap sharing score contribution at 40

def calculate_smart_risk_score(
    app_name: str,
    package_name: str,
    data_collected: list[str],
    sensitive_data: list[str],
    sharing_entities: list[str],
    third_party_sharing: bool
) -> dict:
    """
    Calculate a smart risk score based on:
    1. Unnecessary data collection for the app's domain
    2. Sensitive data collected (weighted by severity)
    3. Suspicious/vague third-party sharing
    """
    score = 0
    reasons = []

    domain = detect_app_domain(app_name, package_name, data_collected)

    # --- Factor 1: Unnecessary data for the domain ---
    unnecessary = get_unnecessary_data(domain, data_collected)
    for item in unnecessary:
        item_lower = item.lower()
        # Check if it's a sensitive type — penalize more
        is_sensitive = any(s in item_lower for s in SENSITIVE_DATA_WEIGHTS)
        if is_sensitive:
            score += 12
            reasons.append(f"Collects '{item}' which is sensitive and unnecessary for a {domain} app")
        else:
            score += 5
            reasons.append(f"Collects '{item}' which is unnecessary for a {domain} app")

    # --- Factor 2: Sensitive data weights ---
    for item in sensitive_data:
        item_lower = item.lower()
        for key, weight in SENSITIVE_DATA_WEIGHTS.items():
            if key in item_lower:
                score += weight
                reasons.append(f"Collects sensitive data: {item}")
                break

    # --- Factor 3: Third-party sharing suspicion ---
    if third_party_sharing and sharing_entities:
        sharing_score, flagged_entities = score_sharing_entities(sharing_entities, domain)
        score += sharing_score
        reasons.extend(flagged_entities)
    elif third_party_sharing and not sharing_entities:
        # Shares with third parties but doesn't say who — very suspicious
        score += 20
        reasons.append("Shares data with third parties but does not disclose recipients")

    # Clamp score to 0-100
    score = min(score, 100)

    # Determine risk level
    if score >= 60:
        risk_level = "High"
    elif score >= 30:
        risk_level = "Medium"
    else:
        risk_level = "Low"

    return {
        "riskScore": score,
        "riskLevel": risk_level,
        "detectedDomain": domain,
        "unnecessaryDataCollected": unnecessary,
        "riskReasons": reasons[:6]  # Return top 6 reasons to keep payload lean
    }


# ---------------------------------------------------------------------------
# GEMINI ANALYSIS — now asks for facts only, no risk scoring
# ---------------------------------------------------------------------------

def analyze_apps_with_gemini(apps: list[AppInfo]) -> dict:
    try:
        model = genai.GenerativeModel("gemini-2.5-flash")
        apps_list = "\n".join([f"- Name: {app.name}, Package: {app.packageName}" for app in apps])

        prompt = f"""You are an AI privacy analysis system.

Analyze the likely privacy policies and data collection practices of the following Android applications based on their names and package names.

Apps to analyze:
{apps_list}

For EACH app, return a factual privacy profile. Do NOT calculate a risk score — just return the raw facts.

Return ONLY valid JSON with this exact structure:
{{
  "results": [
    {{
      "appName": "App Name",
      "packageName": "com.example.app",
      "appCategory": "One of: image_sharing, social_media, navigation, health_fitness, finance, ecommerce, productivity, entertainment, communication, utility, unknown",
      "dataCollected": ["list of specific data types collected, e.g. Precise Location, Contacts, Browsing History, Photos, Device ID, Email, etc."],
      "dataSharingEntities": ["list of entities they share data with, e.g. Meta (Facebook Audience Network), Google Analytics, Law Enforcement, Affiliates, etc."],
      "thirdPartySharing": true or false,
      "sensitiveDataDetected": ["list only genuinely sensitive items like Precise Location, Contacts, Biometrics, Financial Data, Health Data, Camera, Microphone, Call Logs, SMS"],
      "summary": "1-2 sentence factual description of what data this app collects and shares."
    }}
  ]
}}

Rules:
- Return ONLY raw JSON starting with {{ and ending with }}.
- No markdown, no code fences, no explanations outside JSON.
- Be specific about data types — avoid vague terms like "usage data" alone; list the actual data points.
- If an app is unknown, make a reasonable inference from the package name or return a minimal low-data profile.
- For dataSharingEntities, be specific about WHO they share with and include any vague/broad categories mentioned (like "business partners", "affiliates", "law enforcement").
"""

        response = model.generate_content(prompt)
        response_text = response.text.strip()

        if response_text.startswith("```json"):
            response_text = response_text[7:]
        if response_text.startswith("```"):
            response_text = response_text[3:]
        if response_text.endswith("```"):
            response_text = response_text[:-3]
        response_text = response_text.strip()

        return json.loads(response_text)

    except json.JSONDecodeError as e:
        print(f"Failed to parse JSON: {e} - Response: {response_text}")
        raise ValueError("Failed to parse Gemini response as JSON.")
    except Exception as e:
        raise ValueError(f"Gemini API analysis failed: {str(e)}")


@app.post("/scan")
async def scan_apps(request: ScanRequestPayload):
    if not request.apps:
        return {"status": "No apps provided", "count": 0, "results": []}

    try:
        analysis_dict = analyze_apps_with_gemini(request.apps)
        results = analysis_dict.get("results", [])

        formatted_results = []
        for res in results:
            data_collected = res.get("dataCollected", [])
            sensitive_data = res.get("sensitiveDataDetected", [])
            sharing_entities = res.get("dataSharingEntities", [])
            third_party_sharing = res.get("thirdPartySharing", False)
            app_name = res.get("appName", "Unknown")
            package_name = res.get("packageName", "unknown.package")

            # Calculate smart risk score using our logic (not Gemini's guess)
            risk_info = calculate_smart_risk_score(
                app_name=app_name,
                package_name=package_name,
                data_collected=data_collected,
                sensitive_data=sensitive_data,
                sharing_entities=sharing_entities,
                third_party_sharing=third_party_sharing
            )

            formatted_results.append({
                "appName": app_name,
                "packageName": package_name,
                "dataCollected": data_collected,
                "dataSharingEntities": sharing_entities,
                "thirdPartySharing": third_party_sharing,
                "sensitiveDataDetected": sensitive_data,
                "summary": res.get("summary", "No summary available."),
                # Smart scoring fields
                "riskScore": risk_info["riskScore"],
                "riskLevel": risk_info["riskLevel"],
                "detectedDomain": risk_info["detectedDomain"],
                "unnecessaryDataCollected": risk_info["unnecessaryDataCollected"],
                "riskReasons": risk_info["riskReasons"],
            })

        return {
            "status": "Success",
            "count": len(formatted_results),
            "results": formatted_results
        }

    except Exception as e:
        return {"status": f"Error: {str(e)}", "count": 0, "results": []}
