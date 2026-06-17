# Entropy's Defiance

<p align="center">
  <img src="icon.svg" width="300" alt="Entropy's Defiance Logo">
</p>

<p align="center">
  <a href="https://buymeacoffee.com/hwanderer">
    <img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee">
  </a>
</p>

## Overview

Entropy's Defiance is a minimalist Android launcher and task management app designed to bring order to the chaos of everyday digital life. It offers a unique approach to task management based on time horizons and seamlessly integrates with your calendar to keep you focused on what matters.

## Project Origin: Vibe-Coding

This project was created entirely through "vibe-coding" - a development approach where the entire app was built with the assistance of AI. Unlike traditional software development:

- The code, architecture, and features were generated through AI interactions
- Human input focused on guiding the AI with high-level ideas and feedback
- The project evolved through collaborative human-AI conversation
- Design decisions emerged from this dialogue rather than conventional planning

Entropy's Defiance represents an exploration of how software can be created when human creativity is augmented by AI capabilities, without requiring extensive traditional coding knowledge.

## Platforms

- **📱 Android App**: Minimalist launcher + task manager (primary platform)
- **💻 Terminal UI**: Optional Linux interface for synced tasks
- **🔄 File Sync**: Two-way sync via Syncthing

## Features

### ⏰ Time Horizon Based Task Management
- Organize tasks by time horizons: Today, This Week, This Month, This Year, and This Life
- Simple swipe gestures to move tasks between time horizons
- Visual progress indicators for each time horizon

### 📱 Minimalist Launcher
- Access your most frequently used apps with a simple swipe up gesture
- Clean, distraction-free home screen
- Double-tap to launch dialer (optional)

### 📅 Calendar Integration
- View upcoming calendar events directly in the app
- Add new calendar events with a single tap
- Customize which calendars to display
- Configure how many days of events to show

### ⚙️ Customization Options
- Accent color — follow the system Material You palette, pick a preset, or set a custom color
- Left-handed mode — mirror the controls to the left
- Enable/disable calendar widget
- Select which calendars to display
- Configure double-tap for dialer option

### 🔄 File Sync & Laptop Integration
- Tasks automatically sync to a JSON file
- Edit tasks from your laptop using Syncthing
- Terminal UI for laptop task management
- Two-way sync between phone and computer

## Getting Started

### Prerequisites
- Android 13+ (API level 33 or higher)
- Permissions for calendar access (for calendar features)

### Installation
1. Download the latest APK from the [Releases](https://gitlab.com/hwanderer/entropysdefiance/-/releases) section
2. Install the APK on your device
3. Set Entropy's Defiance as your default launcher when prompted

## Usage

### On Android
- **Add a task**: Tap the + button and enter task details
- **Complete a task**: Tap on a task to mark it as complete
- **Move a task**: Swipe task left/right to change its time horizon
- **Access apps**: Swipe up from the bottom of the screen
- **View settings**: Long press on an empty area of the screen

### On Laptop (Terminal UI - Linux Only)

For users who sync tasks to a Linux computer via Syncthing, a minimal terminal interface is available.

**Installation:**
```bash
pip install -e .
entropysdefiance  # or: ed-tui
```

**Features:**
- Vim-style keybindings (j/k for navigation, a to add, d to mark done)
- Command palette (Ctrl+p) for themes and settings
- Auto-saves theme and preferences to `~/.config/entropysdefiance/config.json`
- Override tasks path: `entropysdefiance /path/to/tasks.json`

**Syncthing Setup:**
1. Install Syncthing on phone and laptop
2. Share folder: `~/tasks/`
3. Android app: Settings → File Sync → point to Syncthing folder
4. Tasks sync automatically between devices

## Building From Source

This codebase was generated through AI assistance rather than traditional development. As such, building from source has some unique characteristics:

1. Clone the repository:
```
git clone https://gitlab.com/hwanderer/entropysdefiance.git
```

2. Open the project in Android Studio

3. Be aware that the code architecture and patterns reflect AI-generated solutions rather than conventional development approaches

4. Build the project:
```
./gradlew assembleDebug
```

5. Install on your device:
```
./gradlew installDebug
```

The codebase may contain structures and approaches that differ from human-written code, as it emerged from human-AI collaboration rather than traditional development practices. This represents an exploration of how software can be created when human creativity is extended through AI capabilities.

## Contributing

While this project was created through AI assistance, thoughtful contributions are welcome. Feel free to open issues or submit merge requests if you're interested in extending this AI-assisted development approach.

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add some amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Merge Request

Please read our [Contributing Guidelines](.gitlab/CONTRIBUTING.md) for more details.

## Support

If you find Entropy's Defiance useful, consider buying me a coffee:

[![Buy Me A Coffee](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://buymeacoffee.com/hwanderer)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Inspiration & Acknowledgments

- [Anthropic's Claude](https://www.anthropic.com/claude) and other AI assistants that made vibe-coding possible
- [Material Design 3](https://m3.material.io/) for UI design language interpreted by AI
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI development
- The emerging field of AI-assisted software development and "vibe-coding"
