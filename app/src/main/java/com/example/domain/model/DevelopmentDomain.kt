package com.example.domain.model

enum class DevelopmentDomain(
    val title: String,
    val iconName: String,
    val defaultPrompt: String,
    val framework: String
) {
    ANDROID(
        title = "Android",
        iconName = "Android",
        defaultPrompt = "Build a modern Jetpack Compose app with Material 3, clean MVVM architecture, and Room persistence.",
        framework = "Kotlin / Compose M3"
    ),
    WEBSITE(
        title = "Website",
        iconName = "Web",
        defaultPrompt = "Develop a responsive React / TypeScript web application with Tailwind CSS and accessible components.",
        framework = "React / Next.js"
    ),
    BACKEND(
        title = "Backend",
        iconName = "Dns",
        defaultPrompt = "Create a high-performance FastAPI service with REST endpoints, Pydantic schemas, and SQLite/PostgreSQL models.",
        framework = "Python / FastAPI"
    ),
    EMBEDDED(
        title = "ESP32 Firmware",
        iconName = "Memory",
        defaultPrompt = "Develop ESP32-S3 IoT firmware with PlatformIO, Wi-Fi telemetry, BLE GATT service, and GPIO pin mapping.",
        framework = "PlatformIO / Arduino"
    ),
    AI_AUTOMATION(
        title = "AI & Agents",
        iconName = "Psychology",
        defaultPrompt = "Create an autonomous tool-calling AI agent pipeline using Hermes Agent SKILL.md specs and RAG vector store.",
        framework = "Hermes Agent / LangChain"
    ),
    PYTHON_DEV(
        title = "Python CLI",
        iconName = "Terminal",
        defaultPrompt = "Build a robust Python CLI utility with Click/Typer, unit tests, automated CI workflows, and package distribution.",
        framework = "Python 3.11"
    )
}

enum class TaskStepState(val order: Int, val label: String, val shortDesc: String) {
    UNDERSTANDING(1, "Understanding", "Parsing requirements & constraints"),
    INSPECTING(2, "Inspecting", "Scanning project files & git status"),
    CONSULTING(3, "Consulting", "Reading docs, repos & SKILL.md specs"),
    PLANNING(4, "Planning", "Formulating multi-step execution plan"),
    CHECKING_DEPS(5, "Checking Dependencies", "Validating toolchain & runtimes"),
    AWAITING_APPROVAL(6, "Awaiting Approval", "Sensitive command approval gate"),
    INSTALLING(7, "Installing Tools", "Resolving packages & compiler tools"),
    EXECUTING(8, "Executing Commands", "Running build tasks & scripts"),
    BUILDING_TESTING(9, "Building & Testing", "Compiling codebase & running tests"),
    REPAIRING(10, "Self-Repairing", "Analyzing error trace & applying patch"),
    VERIFYING(11, "Verifying Artifacts", "Checking outputs against anti-fake policy"),
    DELIVERING(12, "Delivered", "Final verified report & deliverables ready")
}

enum class VerificationStatus {
    COMPLETED_AND_VERIFIED,
    COMPLETED_WITH_LIMITATIONS,
    PARTIALLY_COMPLETED,
    FAILED,
    BLOCKED_USER_ACTION_REQUIRED,
    NOT_VERIFIED
}

enum class ProviderType {
    GEMINI,
    OPENAI,
    ANTHROPIC,
    OLLAMA,
    CUSTOM_REST
}
