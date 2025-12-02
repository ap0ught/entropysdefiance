#!/usr/bin/env python3
"""
Configuration management for Entropy's Defiance TUI.
Handles reading/writing config from ~/.config/entropysdefiance/config.json
"""

import json
from pathlib import Path
from typing import Optional, Dict, Any

DEFAULT_CONFIG = {
    "tasks_path": "~/tasks/tasks.json",
    "theme": "textual-dark",
    "show_completed": False,
}

# Textual's actual built-in themes (as of Textual 0.41+)
AVAILABLE_THEMES = [
    "catppuccin-latte",
    "catppuccin-mocha",
    "dracula",
    "flexoki",
    "gruvbox",
    "monokai",
    "nord",
    "solarized-light",
    "textual-ansi",
    "textual-dark",
    "textual-light",
    "tokyo-night",
]


class Config:
    """Manages application configuration"""
    
    def __init__(self):
        self.config_dir = Path.home() / ".config" / "entropysdefiance"
        self.config_file = self.config_dir / "config.json"
        self.data = self._load_or_create_config()
    
    def _load_or_create_config(self) -> Dict[str, Any]:
        """Load config from file, or create default if not exists"""
        if self.config_file.exists():
            try:
                with open(self.config_file, 'r') as f:
                    config = json.load(f)
                    # Merge with defaults to ensure all keys exist
                    return {**DEFAULT_CONFIG, **config}
            except (json.JSONDecodeError, IOError):
                print(f"Warning: Could not read config from {self.config_file}, using defaults")
                return DEFAULT_CONFIG.copy()
        else:
            # Create config directory and default config
            self.config_dir.mkdir(parents=True, exist_ok=True)
            self._save_config(DEFAULT_CONFIG)
            print(f"Created default config at {self.config_file}")
            return DEFAULT_CONFIG.copy()
    
    def _save_config(self, config: Dict[str, Any]) -> None:
        """Save config to file"""
        try:
            with open(self.config_file, 'w') as f:
                json.dump(config, f, indent=2)
        except IOError as e:
            print(f"Error: Could not save config to {self.config_file}: {e}")
    
    def get(self, key: str, default: Any = None) -> Any:
        """Get a config value"""
        return self.data.get(key, default)
    
    def set(self, key: str, value: Any) -> None:
        """Set a config value and save"""
        self.data[key] = value
        self._save_config(self.data)
    
    def get_tasks_path(self) -> Path:
        """Get the tasks file path, expanding ~ to home directory"""
        path_str = self.get("tasks_path", DEFAULT_CONFIG["tasks_path"])
        return Path(path_str).expanduser()
    
    def set_tasks_path(self, path: str) -> None:
        """Set the tasks file path"""
        self.set("tasks_path", path)
    
    def get_theme(self) -> str:
        """Get the current theme"""
        theme = self.get("theme", DEFAULT_CONFIG["theme"])
        if theme not in AVAILABLE_THEMES:
            return DEFAULT_CONFIG["theme"]
        return theme
    
    def set_theme(self, theme: str) -> None:
        """Set the theme"""
        if theme not in AVAILABLE_THEMES:
            raise ValueError(f"Invalid theme: {theme}. Available: {', '.join(AVAILABLE_THEMES)}")
        self.set("theme", theme)
    
    def get_show_completed(self) -> bool:
        """Get whether to show completed tasks"""
        return self.get("show_completed", DEFAULT_CONFIG["show_completed"])
    
    def set_show_completed(self, show: bool) -> None:
        """Set whether to show completed tasks"""
        self.set("show_completed", show)


def get_config() -> Config:
    """Get the global config instance"""
    return Config()
