#!/usr/bin/env python3
"""
Simple TUI for managing Entropy's Defiance tasks from your laptop.
Usage: python laptop_task_manager.py /path/to/tasks.json
"""

import json
import sys
from datetime import datetime, date
from typing import List, Dict, Optional

def load_tasks(filepath: str) -> Dict:
    """Load tasks from JSON file"""
    try:
        with open(filepath, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        return {"version": 1, "tasks": []}

def save_tasks(filepath: str, data: Dict) -> None:
    """Save tasks to JSON file"""
    with open(filepath, 'w') as f:
        json.dump(data, f, indent=2)
    print(f"✓ Saved to {filepath}")

def show_tasks(data: Dict, show_completed: bool = False) -> None:
    """Display tasks in a formatted table"""
    tasks = data.get('tasks', [])
    
    if not show_completed:
        tasks = [t for t in tasks if not t.get('isCompleted', False)]
    
    if not tasks:
        print("No tasks found!")
        return
    
    print("\n" + "="*80)
    print(f"{'ID':<4} {'Title':<40} {'Horizon':<12} {'Deadline':<12}")
    print("="*80)
    
    for i, task in enumerate(tasks, 1):
        title = task['title'][:38] + '..' if len(task['title']) > 40 else task['title']
        horizon = task['timeHorizon']
        deadline = task.get('deadline', '-') if task.get('isDeadlineTask') else '-'
        status = '✓' if task.get('isCompleted') else ' '
        
        print(f"{status}{i:<3} {title:<40} {horizon:<12} {deadline:<12}")
    
    print("="*80 + "\n")

def add_task(data: Dict, title: str, horizon: str = "Today") -> None:
    """Add a new task"""
    import uuid
    
    # Get next auto ID
    max_id = max([t.get('autoId', 0) for t in data['tasks']], default=0)
    
    new_task = {
        "id": str(uuid.uuid4()),
        "title": title,
        "timeHorizon": horizon,
        "autoId": max_id + 1,
        "createdAt": datetime.now().isoformat(),
        "isCompleted": False,
        "isDeadlineTask": False,
        "deadline": None,
        "daysNeeded": None,
        "doDate": None
    }
    
    data['tasks'].append(new_task)
    print(f"✓ Added task: {title}")

def complete_task(data: Dict, task_index: int) -> None:
    """Mark a task as completed"""
    tasks = [t for t in data['tasks'] if not t.get('isCompleted', False)]
    
    if 0 <= task_index < len(tasks):
        tasks[task_index]['isCompleted'] = True
        print(f"✓ Completed: {tasks[task_index]['title']}")
    else:
        print(f"✗ Invalid task number: {task_index + 1}")

def delete_task(data: Dict, task_index: int) -> None:
    """Delete a task"""
    tasks = [t for t in data['tasks'] if not t.get('isCompleted', False)]
    
    if 0 <= task_index < len(tasks):
        task_to_delete = tasks[task_index]
        data['tasks'].remove(task_to_delete)
        print(f"✓ Deleted: {task_to_delete['title']}")
    else:
        print(f"✗ Invalid task number: {task_index + 1}")

def interactive_mode(filepath: str) -> None:
    """Run interactive TUI"""
    print("\n=== Entropy's Defiance Task Manager ===")
    print(f"File: {filepath}\n")
    
    while True:
        print("\nCommands:")
        print("  l - List tasks")
        print("  a - Add task")
        print("  c - Complete task")
        print("  d - Delete task")
        print("  q - Quit")
        
        choice = input("\n> ").strip().lower()
        
        data = load_tasks(filepath)
        
        if choice == 'l':
            show_tasks(data)
        
        elif choice == 'a':
            title = input("Task title: ").strip()
            if title:
                print("Horizon: 1=Today, 2=ThisWeek, 3=ThisMonth, 4=ThisYear, 5=ThisLife")
                horizon_choice = input("Choose (default=1): ").strip() or "1"
                horizons = {
                    "1": "Today",
                    "2": "ThisWeek",
                    "3": "ThisMonth",
                    "4": "ThisYear",
                    "5": "ThisLife"
                }
                horizon = horizons.get(horizon_choice, "Today")
                add_task(data, title, horizon)
                save_tasks(filepath, data)
        
        elif choice == 'c':
            show_tasks(data)
            try:
                num = int(input("Task number to complete: ")) - 1
                complete_task(data, num)
                save_tasks(filepath, data)
            except (ValueError, IndexError):
                print("✗ Invalid input")
        
        elif choice == 'd':
            show_tasks(data)
            try:
                num = int(input("Task number to delete: ")) - 1
                delete_task(data, num)
                save_tasks(filepath, data)
            except (ValueError, IndexError):
                print("✗ Invalid input")
        
        elif choice == 'q':
            print("Goodbye!")
            break
        
        else:
            print("✗ Unknown command")

def main():
    if len(sys.argv) < 2:
        print("Usage: python laptop_task_manager.py /path/to/tasks.json")
        print("\nExample:")
        print("  python laptop_task_manager.py ~/Syncthing/EntropysDefiance/tasks.json")
        sys.exit(1)
    
    filepath = sys.argv[1]
    
    # Quick commands
    if len(sys.argv) >= 3:
        data = load_tasks(filepath)
        
        cmd = sys.argv[2]
        if cmd == 'list':
            show_tasks(data)
        elif cmd == 'add' and len(sys.argv) >= 4:
            title = ' '.join(sys.argv[3:])
            add_task(data, title)
            save_tasks(filepath, data)
        else:
            print("Commands: list, add <title>")
    else:
        # Interactive mode
        interactive_mode(filepath)

if __name__ == "__main__":
    main()
