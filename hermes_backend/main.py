"""
FastAPI Server for Hermes Dev Agent.
Provides REST and WebSocket endpoints for agent sessions, GitHub skill imports,
command execution, and artifact verification.
"""

from fastapi import FastAPI, HTTPException, WebSocket
from pydantic import BaseModel
from pathlib import Path
from typing import List, Optional
import os
from .agent import HermesAgent, TaskState, CommandResult
from .skills import SkillRegistry, SkillMetadata

app = FastAPI(
    title="Hermes Dev Agent Backend",
    description="Backend service for Hermes Dev Android Agent based on NousResearch/hermes-agent and morluto/rea.",
    version="1.0.0"
)

WORKSPACE_ROOT = Path(os.environ.get("HERMES_WORKSPACE", "./workspace"))
SKILLS_ROOT = Path(os.environ.get("HERMES_SKILLS", "./skills"))

agent = HermesAgent(workspace_root=WORKSPACE_ROOT)
skill_registry = SkillRegistry(skills_dir=SKILLS_ROOT)

class PromptRequest(BaseModel):
    session_id: str
    project_id: str
    prompt: str
    provider: Optional[str] = "gemini"

class CommandRequest(BaseModel):
    command: str
    timeout_sec: Optional[int] = 60

@app.on_event("startup")
def startup_event():
    skill_registry.scan_and_load()

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "agent": "Hermes Dev Agent",
        "workspace": str(WORKSPACE_ROOT.resolve())
    }

@app.get("/skills", response_model=List[SkillMetadata])
def list_skills():
    return skill_registry.list_skills()

@app.post("/execute", response_model=CommandResult)
def execute_command(req: CommandRequest):
    return agent.execute_command(req.command, timeout=req.timeout_sec or 60)

@app.get("/verify")
def verify_artifact(path: str):
    verified = agent.verify_artifact(path)
    return {"path": path, "verified": verified, "policy": "Anti-Fake-Work Enforced"}
