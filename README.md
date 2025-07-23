# extensible-android-keyboard

## Overview

**extensible-android-keyboard** is an Android keyboard application designed to support multiple languages and customizable layouts using JSON-based configurations. Its extensible architecture allows users to easily add new language support, change keyboard layouts, and tweak settings for an enhanced typing experience.

## Features

- **Multi-language support:** Easily switch between languages with built-in language processors.
- **Customizable keyboard layouts:** Define your own layouts in JSON format to suit your needs.
- **Predictive text and suggestions:** Integrated dictionary manager provides candidate text suggestions as you type.
- **Emoji and numeric keyboards:** Access emoji and numeric input views directly from the keyboard.
- **Settings popup:** In-app settings dialog allows users to tweak appearance and behavior (colors, prediction, etc).

## Getting Started

1. **Clone the repository:**

   ```bash
   git clone https://github.com/vikas-jha/extensible-android-keyboard.git
   ```

2. **Import into Android Studio:**
   
   - Open Android Studio.
   - Select "Import Project" and choose the cloned directory.
   - Ensure your environment is set up for Java-based Android development.

3. **Build and Run:**
   
   - Build the project.
   - Deploy to an Android device or emulator.
   - Enable the keyboard in device settings under "Language & Input" > "Keyboards".

4. **Customizing Layouts:**
   
   - Edit or add new JSON layout files in the configuration directory.
   - Restart the keyboard or reload layouts from the keyboard settings popup.

## Usage

- **Switch language:** Use the language switch key (if enabled by configuration).
- **Open settings:** Tap the settings icon on the keyboard to adjust colors, layouts, and enable/disable predictions.
- **Emoji/Numeric input:** Tap the emoji or numeric key for respective boards.

## Extending

To add a new language or layout:
- Create a new JSON configuration file describing the keyboard layout.
- Implement or configure a language processor if needed for suggestions/transliteration.
- Add dictionary files for predictive text as needed.

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](https://www.apache.org/licenses/LICENSE-2.0) for details.

## Author

- [vikas-jha](https://github.com/vikas-jha)
