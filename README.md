# TextUndoRedo

[![JitPack](https://jitpack.io/v/DevMassive/TextUndoRedo.svg)](https://jitpack.io/#DevMassive/TextUndoRedo)

An Android library that provides robust undo/redo functionality specifically designed for `EditText` components. It intelligently handles text changes, with a strong emphasis on supporting complex input methods (IME), including those used in Japanese and other languages requiring text conversion. It also supports customizable history size.

## Features

*   **Undo/Redo:** Easily revert and reapply text changes.
*   **Advanced IME Support:** Correctly and robustly handles text input from various IME methods, including complex conversion processes common in languages like Japanese, ensuring accurate and reliable undo/redo operations.
*   **History Management:** Allows setting a maximum history size and clearing the history.
*   **State Change Listener:** Provides a callback for monitoring undo/redo state changes, useful for updating UI elements like buttons.

## Installation

Add the JitPack repository to your project's `settings.gradle.kts` (or `settings.gradle`):

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then, add the dependency to your module's `build.gradle.kts` (or `build.gradle`):

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.DevMassive:TextUndoRedo:1.0.0") // Check JitPack for the latest version
}
```

## Usage

1.  **Initialize `TextUndoRedo`:**
    Create an instance of `TextUndoRedo` by passing your `TextView` (or `EditText`) to its constructor.

    ```kotlin
    val myTextView: TextView = findViewById(R.id.my_text_view)
    val textUndoRedo = TextUndoRedo(myTextView)
    ```

2.  **Perform Undo/Redo Operations:**
    Call `undo()` or `redo()` methods as needed.

    ```kotlin
    // To undo the last change
    textUndoRedo.undo()

    // To redo the last undone change
    textUndoRedo.redo()
    ```

3.  **Check Undo/Redo State:**
    Use `canUndo` and `canRedo` properties to determine if undo/redo operations are currently possible. This is useful for enabling/disabling UI buttons.

    ```kotlin
    if (textUndoRedo.canUndo) {
        // Enable undo button
    } else {
        // Disable undo button
    }

    if (textUndoRedo.canRedo) {
        // Enable redo button
    } else {
        // Disable redo button
    }
    ```

4.  **Listen for State Changes (Optional):**
    Assign a lambda function to `stateChangeListener` to receive callbacks when the undo/redo state changes. This is crucial for dynamically updating your UI.

    ```kotlin
    textUndoRedo.stateChangeListener = {
        // Update your undo/redo button states here
        updateUndoRedoButtonStates()
    }

    fun updateUndoRedoButtonStates() {
        undoButton.isEnabled = textUndoRedo.canUndo
        redoButton.isEnabled = textUndoRedo.canRedo
    }
    ```

5.  **Manage History:**
    *   **Set Maximum History Size:**
        ```kotlin
        textUndoRedo.setMaxHistorySize(50) // Keep last 50 changes
        ```
    *   **Clear History:**
        ```kotlin
        textUndoRedo.clearHistory() // Clear all undo/redo history
        ```

## Sample Application

A sample application demonstrating the usage of `TextUndoRedo` is included in the `sample/` module.

To run the sample application:

1.  **Open the project in Android Studio.**
2.  **Sync Gradle files.**
3.  **Select the `sample` run configuration** from the dropdown menu in the toolbar.
4.  **Run the application** on an emulator or a physical device.

## Contributing



## Contributing

Contributions are welcome! Please feel free to open issues or submit pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.