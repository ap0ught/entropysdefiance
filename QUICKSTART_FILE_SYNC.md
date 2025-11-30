# File Sync - Quick Start (60 seconds)

## On Phone

1. **Install**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Grant Permissions**
   - Tap ⚙️ icon
   - Tap "Permissions"
   - Grant "Storage Access" permission

3. **Grant Permissions**
   - Tap "Permissions"
   - Grant "Calendar Access" permission
   - Grant "Storage Access" permission:
     - **Android 13+**: Use file browser in File Sync settings (no separate permission needed)
     - **Android 12 and below**: Tap "Grant Permission" to request storage access

4. **Configure File Location**
   - Tap "File Sync"
   - Tap "Browse for File"
   - Navigate to Syncthing folder
   - Select or create `tasks.json`

3. **Done!** Tasks now sync automatically.

## On Laptop

1. **Install Syncthing**
   ```bash
   sudo pacman -S syncthing  # or your package manager
   ```

2. **Use Python TUI**
   ```bash
   python laptop_task_manager.py ~/Syncthing/EntropysDefiance/tasks.json
   ```

   Commands:
   - `l` - List tasks
   - `a` - Add task
   - `c` - Complete task
   - `q` - Quit

**Or just edit `tasks.json` in any text editor!**

## That's It!

Your tasks now sync between phone and laptop automatically via Syncthing. Edit anywhere, changes appear everywhere.

## Quick Commands

```bash
# List tasks
python laptop_task_manager.py ~/Syncthing/tasks.json list

# Add a task
python laptop_task_manager.py ~/Syncthing/tasks.json add "Buy groceries"

# View with jq
jq '.tasks[] | select(.isCompleted == false) | .title' ~/Syncthing/tasks.json
```

## File Location

**Default:** `/storage/emulated/0/Documents/EntropysDefiance/tasks.json`

**Syncthing:** Use file browser in app to point to your Syncthing folder

## Troubleshooting

**Not syncing?**
- Check Syncthing is running
- Check file path in Settings → File Sync
- Try Manual Sync → "Load from File"

**Need help?** See `README_FILE_SYNC.md` for full docs.
