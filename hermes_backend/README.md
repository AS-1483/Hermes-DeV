# Hermes Dev Agent Backend

Hermes Dev Agent backend server inspired by:
- **NousResearch/hermes-agent**: `https://github.com/NousResearch/hermes-agent.git`
- **morluto/rea**: `https://github.com/morluto/rea.git`

## Features
- **Official Hermes SKILL.md Parser**: Parses frontmatter, tool definitions, permissions, and dependencies.
- **Controlled Command Execution**: Captures stdout, stderr, execution duration, and exit codes within sandbox workspaces.
- **Anti-Fake-Work Verification**: Directly validates physical files and sizes before reporting success.
- **Multi-Provider LLM Support**: Seamless routing across Gemini, OpenAI, Anthropic Claude, and local Ollama.

## Quick Start
```bash
cd hermes_backend
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
