#!/usr/bin/env python3
# Copyright (c) 2024 Jonas Freiburghaus
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0

"""
Textual TUI for managing Entropy's Defiance tasks.
Interactive interface with vim-style keybindings.

Usage: entropysdefiance [/path/to/tasks.json]

If no path is provided, uses the path from config (~/.config/entropysdefiance/config.json)
"""

import json
import sys
import uuid
from datetime import datetime, date, timedelta
from pathlib import Path
from typing import Optional

from textual.app import App, ComposeResult
from textual.command import Provider, Hit, DiscoveryHit
from textual.containers import Container, Horizontal
from textual.widgets import Header, Footer, DataTable, Input, Label, Button, Static
from textual.binding import Binding
from textual.screen import Screen, ModalScreen
from textual.reactive import reactive

from .config import Config


class TaskManager:
    """Manages task operations and file I/O"""
    
    TIME_HORIZONS = ["Today", "ThisWeek", "ThisMonth", "ThisYear", "ThisLife"]
    
    def __init__(self, filepath: Path):
        self.filepath = filepath
        self.data = self.load_tasks()
    
    def load_tasks(self) -> dict:
        """Load tasks from JSON file"""
        try:
            with open(self.filepath, 'r') as f:
                return json.load(f)
        except FileNotFoundError:
            return {"version": 1, "tasks": []}
        except json.JSONDecodeError:
            return {"version": 1, "tasks": []}
    
    def save_tasks(self) -> None:
        """Save tasks to JSON file atomically"""
        try:
            self.filepath.parent.mkdir(parents=True, exist_ok=True)
            
            # Write to temp file first
            temp_path = self.filepath.with_suffix('.json.tmp')
            with open(temp_path, 'w') as f:
                json.dump(self.data, f, indent=2)
                f.flush()  # Ensure buffer is written
                import os
                os.fsync(f.fileno())  # Ensure data is on disk
            
            # Atomic rename
            temp_path.replace(self.filepath)
        except Exception as e:
            pass
    
    def get_tasks(self, include_completed: bool = False):
        """Get tasks, optionally filtering out completed ones"""
        tasks = self.data.get('tasks', [])
        if not include_completed:
            tasks = [t for t in tasks if not t.get('isCompleted', False)]
        # Sort by horizon order
        horizon_order = {h: i for i, h in enumerate(self.TIME_HORIZONS)}
        tasks.sort(key=lambda t: (
            horizon_order.get(t.get('timeHorizon', 'ThisLife'), 99),
            t.get('createdAt', '')
        ))
        return tasks
    
    def add_task(self, title: str, time_horizon: str, is_deadline: bool = False,
                 deadline: Optional[str] = None, days_needed: Optional[int] = None):
        """Add a new task"""
        max_id = max([t.get('autoId', 0) for t in self.data['tasks']], default=0)
        
        new_task = {
            "id": str(uuid.uuid4()),
            "title": title,
            "timeHorizon": time_horizon,
            "autoId": max_id + 1,
            "createdAt": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "isCompleted": False,
            "isDeadlineTask": is_deadline,
            "deadline": deadline,
            "daysNeeded": days_needed,
            "doDate": None
        }
        
        self.data['tasks'].append(new_task)
    
    def complete_task(self, task_id: str) -> bool:
        """Mark a task as completed"""
        for task in self.data['tasks']:
            if task['id'] == task_id:
                task['isCompleted'] = True
                return True
        return False
    
    def calculate_time_horizon_from_date(self, target_date: date) -> str:
        """Calculate time horizon based on date"""
        today = date.today()
        
        if target_date == today:
            return "Today"
        
        # This Week (Monday to Sunday)
        start_of_week = today - timedelta(days=today.weekday())
        end_of_week = start_of_week + timedelta(days=6)
        if start_of_week <= target_date <= end_of_week:
            return "ThisWeek"
        
        # This Month
        start_of_month = today.replace(day=1)
        if today.month == 12:
            end_of_month = today.replace(year=today.year + 1, month=1, day=1) - timedelta(days=1)
        else:
            end_of_month = today.replace(month=today.month + 1, day=1) - timedelta(days=1)
        if start_of_month <= target_date <= end_of_month:
            return "ThisMonth"
        
        # This Year
        start_of_year = today.replace(month=1, day=1)
        end_of_year = today.replace(year=today.year, month=12, day=31)
        if start_of_year <= target_date <= end_of_year:
            return "ThisYear"
        
        return "ThisLife"


class AddTaskScreen(ModalScreen):
    """Screen for adding a new task"""
    
    BINDINGS = [
        Binding("escape", "dismiss", "Cancel"),
    ]
    
    def __init__(self, task_manager: TaskManager):
        super().__init__()
        self.tm = task_manager
    
    def compose(self) -> ComposeResult:
        yield Container(
            Label("Add New Task", id="add-title"),
            Input(placeholder="Task title", id="task-title"),
            Label("Type: 1=Today 2=Week 3=Month 4=Year 5=Life 6=Deadline"),
            Input(placeholder="1-6", id="task-type", max_length=1),
            Input(placeholder="Deadline (YYYY-MM-DD)", id="deadline", classes="hidden"),
            Input(placeholder="Days needed", id="days-needed", classes="hidden"),
            Horizontal(
                Button("Add", variant="primary", id="add-btn"),
                Button("Cancel", id="cancel-btn"),
            ),
            id="add-container"
        )
    
    def on_mount(self) -> None:
        """Focus the title input when screen opens"""
        self.query_one("#task-title", Input).focus()
    
    def on_input_changed(self, event: Input.Changed) -> None:
        """Show deadline fields if type is 6"""
        if event.input.id == "task-type":
            deadline_input = self.query_one("#deadline", Input)
            days_input = self.query_one("#days-needed", Input)
            
            if event.value == "6":
                deadline_input.remove_class("hidden")
                days_input.remove_class("hidden")
            else:
                deadline_input.add_class("hidden")
                days_input.add_class("hidden")
    
    def on_button_pressed(self, event: Button.Pressed) -> None:
        if event.button.id == "cancel-btn":
            self.dismiss()
        elif event.button.id == "add-btn":
            self.add_task()
    
    def add_task(self) -> None:
        """Add the task"""
        title = self.query_one("#task-title", Input).value.strip()
        type_choice = self.query_one("#task-type", Input).value.strip()
        
        if not title or not type_choice:
            return
        
        is_deadline = type_choice == "6"
        deadline = None
        days_needed = None
        time_horizon = "Today"
        
        if is_deadline:
            deadline = self.query_one("#deadline", Input).value.strip()
            days_str = self.query_one("#days-needed", Input).value.strip()
            
            if not deadline or not days_str:
                return
            
            try:
                deadline_date = datetime.strptime(deadline, "%Y-%m-%d").date()
                days_needed = int(days_str)
                do_date = deadline_date - timedelta(days=days_needed)
                time_horizon = self.tm.calculate_time_horizon_from_date(do_date)
            except:
                return
        else:
            horizon_idx = int(type_choice) - 1
            if 0 <= horizon_idx < len(TaskManager.TIME_HORIZONS):
                time_horizon = TaskManager.TIME_HORIZONS[horizon_idx]
        
        self.tm.add_task(title, time_horizon, is_deadline, deadline, days_needed)
        self.tm.save_tasks()
        self.dismiss()


class TaskListWidget(DataTable):
    """Custom DataTable for tasks with vim-style navigation"""

    BINDINGS = [
        Binding("j", "cursor_down", "Down", show=False),
        Binding("k", "cursor_up", "Up", show=False),
        Binding("h", "cursor_left", "Left", show=False),
        Binding("l", "cursor_right", "Right", show=False),
        Binding("g", "scroll_home", "Top", show=False),
        Binding("G", "scroll_end", "Bottom", show=False),
    ]


class HelpScreen(ModalScreen):
    """Help screen showing vim-style commands"""

    BINDINGS = [
        Binding("escape", "dismiss", "Close"),
        Binding("q", "dismiss", "Close"),
        Binding("?", "dismiss", "Close"),
    ]

    def compose(self) -> ComposeResult:
        yield Container(
            Label("Entropy's Defiance - Commands", id="help-title"),
            Static("""
Navigation:
  j/k or ↑/↓    Move up/down
  h/l or ←/→    Move left/right
  g/G           Jump to top/bottom

Task Actions:
  a             Add new task
  d             Mark selected task done
  r             Reload from file

Command Palette (Ctrl+p):
  - Change theme
  - Toggle show/hide completed tasks
  - View config info

Config File:
  ~/.config/entropysdefiance/config.json

Help & Exit:
  ?             Show this help
  q             Quit application
            """, id="help-content"),
            Horizontal(
                Button("Close", variant="primary", id="close-btn"),
            ),
            id="help-container"
        )

    def on_button_pressed(self, event: Button.Pressed) -> None:
        if event.button.id == "close-btn":
            self.dismiss()


class EntropyCommandProvider(Provider):
    """Custom command provider for Entropy's Defiance specific commands"""
    
    async def startup(self) -> None:
        """Called when the command provider starts"""
        app = self.app
        if hasattr(app, 'config'):
            self.app_ref = app
    
    async def discover(self):
        """Provide discovery hits for common commands"""
        yield DiscoveryHit(
            "Toggle completed tasks",
            self.toggle_completed,
            help="Show or hide completed tasks"
        )
        
        yield DiscoveryHit(
            "Show config info",
            self.show_config_info,
            help="Display current configuration"
        )
    
    async def search(self, query: str):
        """Search for commands matching the query"""
        matcher = self.matcher(query)
        
        if not hasattr(self, 'app_ref'):
            return
        
        app = self.app_ref
        
        # Toggle completed tasks
        show_completed = app.config.get_show_completed()
        action = "Hide" if show_completed else "Show"
        command_text = f"{action} completed tasks"
        score = matcher.match(command_text)
        if score > 0:
            yield Hit(
                score,
                matcher.highlight(command_text),
                self.toggle_completed,
                help=f"Currently: {'showing' if show_completed else 'hiding'} completed tasks"
            )
        
        # Show config info
        command_text = "Show config info"
        score = matcher.match(command_text)
        if score > 0:
            yield Hit(
                score,
                matcher.highlight(command_text),
                self.show_config_info,
                help="Display configuration file location and settings"
            )
    
    def toggle_completed(self) -> None:
        """Toggle showing completed tasks"""
        if hasattr(self, 'app_ref'):
            app = self.app_ref
            current = app.config.get_show_completed()
            app.config.set_show_completed(not current)
            app.refresh_tasks()
            status = "Showing" if not current else "Hiding"
            app.notify(f"{status} completed tasks")
    
    def show_config_info(self) -> None:
        """Show configuration information"""
        if hasattr(self, 'app_ref'):
            app = self.app_ref
            config_path = app.config.config_file
            theme = app.config.get_theme()
            tasks_path = app.config.get_tasks_path()
            show_completed = app.config.get_show_completed()
            
            info = f"""Config: {config_path}
Theme: {theme}
Tasks: {tasks_path}
Show completed: {show_completed}"""
            app.notify(info, timeout=10)


class EntropyTUI(App):
    """Main Textual TUI application"""

    CSS = """
    Screen {
        layout: vertical;
        overflow: auto;
    }

    DataTable {
        height: 1fr;
        width: 100%;
        border: none;
    }

    #task-table {
        height: 1fr;
        border: none;
    }

    /* Modal screens for overlays */
    ModalScreen {
        align: center middle;
    }

    #help-container, #add-container {
        background: $surface;
        border: solid $primary;
        width: 70;
        height: auto;
        padding: 1 2;
    }

    #add-container {
        width: 60;
    }

    #help-title, #add-title {
        text-align: center;
        text-style: bold;
        margin-bottom: 1;
    }

    #help-content {
        margin-bottom: 1;
    }

    Input {
        margin-bottom: 1;
    }

    Label {
        margin-bottom: 0;
    }

    .hidden {
        display: none;
    }

    Horizontal {
        height: auto;
        align: center middle;
    }

    Button {
        margin: 0 1;
    }
    """

    BINDINGS = [
        Binding("q", "quit", "Quit"),
        Binding("a", "add_task", "Add"),
        Binding("d", "mark_done", "Done"),
        Binding("r", "reload", "Reload"),
        Binding("?", "show_help", "Help"),
        Binding("j", "cursor_down", "Down", show=False),
        Binding("k", "cursor_up", "Up", show=False),
        Binding("h", "cursor_left", "Left", show=False),
        Binding("l", "cursor_right", "Right", show=False),
    ]
    
    # Add our custom command provider
    COMMANDS = App.COMMANDS | {EntropyCommandProvider}
    
    def __init__(self, config: Config, filepath: Optional[Path] = None):
        super().__init__()
        self.config = config
        
        # Use provided path or config path
        if filepath:
            self.tasks_path = filepath
        else:
            self.tasks_path = config.get_tasks_path()
        
        self.tm = TaskManager(self.tasks_path)
        self.title = "Entropy's Defiance"
    
    def compose(self) -> ComposeResult:
        """Create child widgets"""
        yield Header(show_clock=False)
        yield TaskListWidget(id="task-table", cursor_type="row", zebra_stripes=True)
        yield Footer()
    
    def on_mount(self) -> None:
        """Set up the UI when the app starts"""
        # Load and apply saved theme
        theme = self.config.get_theme()
        available = self.available_themes
        if theme in available:
            self.theme = theme
        
        self.refresh_tasks()
        # Focus the task table
        self.query_one("#task-table").focus()
    
    def watch_theme(self, theme: str) -> None:
        """Called when theme changes - save to config"""
        if hasattr(self, 'config'):
            self.config.set_theme(theme)
    
    def refresh_tasks(self) -> None:
        """Refresh the task table"""
        table = self.query_one(TaskListWidget)
        table.clear(columns=True)
        
        # Add columns
        table.add_column("#", width=4)
        table.add_column("Task", width=50)
        table.add_column("Horizon", width=12)
        table.add_column("Deadline", width=15)
        
        # Get tasks
        show_completed = self.config.get_show_completed()
        tasks = self.tm.get_tasks(include_completed=show_completed)
        
        # Add rows
        for i, task in enumerate(tasks, 1):
            title = task['title']
            if task.get('isCompleted'):
                title = f"✓ {title}"
            if len(title) > 48:
                title = title[:45] + "..."
            
            horizon = task['timeHorizon']
            
            deadline_str = ""
            if task.get('isDeadlineTask') and task.get('deadline'):
                try:
                    deadline_date = datetime.strptime(task['deadline'], "%Y-%m-%d").date()
                    days_until = (deadline_date - date.today()).days
                    
                    if days_until < 0:
                        deadline_str = f"!{abs(days_until)}d overdue"
                    elif days_until == 0:
                        deadline_str = "!now"
                    else:
                        deadline_str = f"{days_until}d"
                except:
                    pass
            
            # Store task ID in row key
            table.add_row(str(i), title, horizon, deadline_str, key=task['id'])
    
    def action_add_task(self) -> None:
        """Show add task screen"""
        def callback(result):
            self.refresh_tasks()
        self.push_screen(AddTaskScreen(self.tm), callback)
    
    def action_mark_done(self) -> None:
        """Mark selected task as done"""
        table = self.query_one(TaskListWidget)
        if table.cursor_row is not None:
            show_completed = self.config.get_show_completed()
            tasks = self.tm.get_tasks(include_completed=show_completed)
            if table.cursor_row < len(tasks):
                task = tasks[table.cursor_row]
                self.tm.complete_task(task['id'])
                self.tm.save_tasks()
                self.refresh_tasks()
                self.notify(f"Marked task as done: {task['title'][:30]}")
    
    def action_reload(self) -> None:
        """Reload tasks from file"""
        self.tm.data = self.tm.load_tasks()
        self.refresh_tasks()
        self.notify("Tasks reloaded from file")

    def action_show_help(self) -> None:
        """Show help screen"""
        self.push_screen(HelpScreen())


def main():
    """Main entry point"""
    config = Config()
    
    # Check for command-line argument
    filepath = None
    if len(sys.argv) > 1:
        filepath = Path(sys.argv[1])
    
    app = EntropyTUI(config, filepath)
    app.run()


if __name__ == "__main__":
    main()
