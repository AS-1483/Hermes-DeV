"""
Hermes Agent SKILL.md specification parser and runtime executor.
Supports discovering and loading skills from GitHub repositories (e.g. NousResearch/hermes-agent and morluto/rea).
"""

import os
import yaml
from pathlib import Path
from typing import Dict, List, Any, Optional
from pydantic import BaseModel

class SkillMetadata(BaseModel):
    name: str
    description: str
    author: Optional[str] = "Nous Research"
    version: Optional[str] = "1.0.0"
    permissions: List[str] = []
    dependencies: List[str] = []
    tools: List[str] = []

class Skill:
    def __init__(self, metadata: SkillMetadata, markdown_instructions: str, file_path: Path):
        self.metadata = metadata
        self.markdown_instructions = markdown_instructions
        self.file_path = file_path

    @classmethod
    def load_from_file(cls, path: Path) -> "Skill":
        content = path.read_text(encoding="utf-8")
        if content.startswith("---"):
            parts = content.split("---", 2)
            if len(parts) >= 3:
                frontmatter_raw = parts[1]
                markdown_body = parts[2].strip()
                data = yaml.safe_load(frontmatter_raw) or {}
                meta = SkillMetadata(
                    name=data.get("name", path.parent.name),
                    description=data.get("description", ""),
                    author=data.get("author", "Hermes Contributor"),
                    version=str(data.get("version", "1.0.0")),
                    permissions=data.get("permissions", []),
                    dependencies=data.get("dependencies", []),
                    tools=data.get("tools", [])
                )
                return cls(metadata=meta, markdown_instructions=markdown_body, file_path=path)

        meta = SkillMetadata(
            name=path.stem,
            description="Hermes Agent Skill loaded from Markdown"
        )
        return cls(metadata=meta, markdown_instructions=content, file_path=path)

class SkillRegistry:
    def __init__(self, skills_dir: Path):
        self.skills_dir = skills_dir
        self.skills_dir.mkdir(parents=True, exist_ok=True)
        self._skills: Dict[str, Skill] = {}

    def scan_and_load(self):
        self._skills.clear()
        for root, _, files in os.walk(self.skills_dir):
            for file in files:
                if file.upper() == "SKILL.MD":
                    p = Path(root) / file
                    try:
                        skill = Skill.load_from_file(p)
                        self._skills[skill.metadata.name] = skill
                    except Exception as e:
                        print(f"[WARN] Failed to parse skill at {p}: {e}")

    def get_skill(self, name: str) -> Optional[Skill]:
        return self._skills.get(name)

    def list_skills(self) -> List[SkillMetadata]:
        return [s.metadata for s in self._skills.values()]
