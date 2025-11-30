# File Sync Feature - Quick Start Guide

## Overview

Your Entropy's Defiance launcher now supports syncing tasks with a JSON file, enabling you to edit tasks on your laptop and sync them via Syncthing!

## Setup on Phone

1. **Install the APK**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Grant Storage Permission**
   - The app will request storage permission on first launch
   - This is required to read/write the tasks.json file

3. **Grant Permissions** (Required for file sync)
   - Open Settings → Permissions
   - Grant "Calendar Access" permission (for free time calculation)
   - Grant "Storage Access" permission:
     - **Android 13+**: Use the file browser in File Sync settings to select your tasks.json location
     - **Android 12 and below**: Tap "Grant Permission" to request storage access directly
   - Use "Refresh Status" if you grant permissions through system settings

4. **Configure File Path** (Optional)
   - Open Settings → File Sync
   - Tap "Browse for File" to use your phone's file manager
   - Navigate to your preferred location (e.g., Syncthing folder)
   - Select or create `tasks.json`
   - By default, tasks are saved to:
     `/storage/emulated/0/Documents/EntropysDefiance/tasks.json`

## Setup on Laptop

1. **Install Syncthing**
   ```bash
   # On Arch/Manjaro
   sudo pacman -S syncthing
   
   # On Ubuntu/Debian
   sudo apt install syncthing
   ```

2. **Configure Sync Folder**
   - Add the tasks folder from your phone
   - Point to your local folder (e.g., `~/Syncthing/EntropysDefiance/`)

3. **Use the Python Task Manager**
   ```bash
   # Interactive mode
   python laptop_task_manager.py ~/Syncthing/EntropysDefiance/tasks.json
   
   # List tasks
   python laptop_task_manager.py ~/Syncthing/EntropysDefiance/tasks.json list
   
   # Add a task
   python laptop_task_manager.py ~/Syncthing/EntropysDefiance/tasks.json add "Write documentation"
   ```

## How It Works

### On Phone
- Tasks are stored in Room database for fast access
- Every change is automatically saved to tasks.json
- File observer watches for external changes
- When file changes, tasks are reloaded from JSON

### On Laptop
- Edit tasks.json with any text editor or the Python script
- Syncthing syncs changes to phone
- Phone detects change and reloads tasks

### Syncthing
- Keeps tasks.json in sync between devices
- Bidirectional sync - changes on either device propagate
- Works offline - syncs when devices reconnect

## JSON File Format

```json
{
  "version": 1,
  "tasks": [
    {
      "id": "uuid-here",
      "title": "Task title",
      "timeHorizon": "Today",
      "autoId": 1,
      "createdAt": "2025-11-30T10:00:00",
      "isCompleted": false,
      "isDeadlineTask": false,
      "deadline": null,
      "daysNeeded": null,
      "doDate": null
    }
  ]
}
```

### Fields Explained

- `id`: Unique UUID for the task
- `title`: Task description
- `timeHorizon`: One of: `Today`, `ThisWeek`, `ThisMonth`, `ThisYear`, `ThisLife`
- `autoId`: Auto-incrementing ID for ordering
- `createdAt`: ISO 8601 timestamp
- `isCompleted`: Boolean - true if task is done
- `isDeadlineTask`: Boolean - true if task has a deadline
- `deadline`: ISO date (YYYY-MM-DD) or null
- `daysNeeded`: Number of days needed to complete (for deadline tasks)
- `doDate`: Calculated start date based on deadline and daysNeeded

## Editing Tasks Manually

### Add a Simple Task
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "My new task",
  "timeHorizon": "Today",
  "autoId": 10,
  "createdAt": "2025-11-30T15:00:00",
  "isCompleted": false,
  "isDeadlineTask": false,
  "deadline": null,
  "daysNeeded": null,
  "doDate": null
}
```

### Add a Deadline Task
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "title": "Finish project",
  "timeHorizon": "ThisWeek",
  "autoId": 11,
  "createdAt": "2025-11-30T15:00:00",
  "isCompleted": false,
  "isDeadlineTask": true,
  "deadline": "2025-12-10",
  "daysNeeded": 5,
  "doDate": "2025-12-05"
}
```

### Mark Task as Complete
Change `"isCompleted": false` to `"isCompleted": true`

### Delete a Task
Remove the entire task object from the `tasks` array

## Tips & Tricks

### Using jq on Command Line
```bash
# List pending tasks
jq '.tasks[] | select(.isCompleted == false) | .title' tasks.json

# Count pending tasks
jq '[.tasks[] | select(.isCompleted == false)] | length' tasks.json

# Add a task
jq '.tasks += [{"id": "new-uuid", "title": "New task", ...}]' tasks.json > tasks_new.json

# Complete a task by title
jq '(.tasks[] | select(.title == "My task")).isCompleted = true' tasks.json
```

### Vim Integration
Add to your `.vimrc`:
```vim
" Quick task completion in tasks.json
autocmd FileType json nnoremap <leader>c :s/"isCompleted": false/"isCompleted": true/<CR>
```

### Text Editor
Any text editor works! Just:
1. Open `tasks.json`
2. Edit the JSON
3. Save
4. Syncthing syncs to phone
5. Phone reloads automatically

## Troubleshooting

### Tasks not syncing to phone
- Check Syncthing is running on both devices
- Verify file path in Settings → File Sync
- Check storage permissions are granted
- Try Manual Sync → Load from File

### Tasks not syncing from phone
- Check file exists at configured path
- Try Manual Sync → Save to File
- Check Syncthing folder permissions

### Conflicts
- If both devices edit simultaneously, last write wins
- Syncthing may create `.sync-conflict` files
- Manually merge conflicts if needed

### File observer not working
- File must exist at configured path
- Parent directory must exist
- Storage permission must be granted

## Advanced: Creating a Better TUI

The included `laptop_task_manager.py` is a simple starter. For a better experience:

### With Rich library
```bash
pip install rich
```

Then enhance the script with:
- Colored output
- Tables with borders
- Progress bars for deadline tasks
- Live updating display

### With curses (built-in)
```python
import curses

# Full-screen TUI with keyboard navigation
# Arrow keys to select task
# Enter to complete
# 'a' to add
# 'd' to delete
```

### Web Interface
Create a simple Flask/FastAPI server:
```python
from fastapi import FastAPI
import json

app = FastAPI()

@app.get("/tasks")
def get_tasks():
    with open("tasks.json") as f:
        return json.load(f)

# Run: uvicorn server:app
# Access: http://localhost:8000/tasks
```

## Example Workflow

1. **Morning on Phone**
   - Open app, review Today tasks
   - Complete some tasks (auto-saved to JSON)
   
2. **Midday on Laptop**
   - Syncthing syncs changes
   - Open Python script: `python laptop_task_manager.py ~/Syncthing/tasks.json`
   - Add new tasks for tomorrow
   - Changes sync back to phone
   
3. **Evening on Phone**
   - App detects changes
   - New tasks appear automatically
   - Review and adjust time horizons

## Security Note

The tasks.json file is stored in plain text. Do not store sensitive information in task titles or use this for confidential data. For sensitive tasks, use encryption or a different solution.

## Contributing

Ideas for improvements:
- [ ] Add support for task tags/categories
- [ ] Implement task priority levels
- [ ] Add recurring tasks
- [ ] Create web dashboard
- [ ] Add task notes/descriptions
- [ ] Implement smart merge for conflicts
- [ ] Add task history/changelog
- [ ] Create mobile-optimized web view

## Questions?

Check the main documentation:
- `FILE_SYNC_IMPLEMENTATION.md` - Technical details
- `DEVELOPMENT.md` - Development guide
