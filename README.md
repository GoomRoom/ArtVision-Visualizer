# AR Flutter Application

## Overview

This AR Flutter application enables users to place and interact with artwork and frames in an augmented reality environment. The app leverages the AR Flutter Plugin to provide AR capabilities, such as placing 3D models on detected planes using anchors.

### Key Features:

- Selection of different artwork and frames to visualize in AR.
- Ability to scale the placed artwork and frames using a slider.
- Updates happen in real-time when users interact with different artwork and frame options.

## File Structure

- `main.dart` – Entry point of the Flutter app, initializes the app and navigates to AR experiences.
- `ar_page.dart` – Handles the core AR functionality, including placing, removing, and updating artwork and frames in the AR environment.
- `gallery_page.dart`, `homepage.dart`, `info_page.dart`, `webview_page.dart` – Additional pages to provide extra content or functionalities to the user.
- `artwork_initializer.dart` – A utility file for initializing and managing demo artworks.

## How It Works

### Initialization

- The AR session is managed by `ARSessionManager`, which initializes the AR view and configures the plane detection (in this case, vertical planes).
- When the user taps on the screen, an anchor is created at that point, and artwork and frame nodes are attached to the anchor.

### Functions and Usage

- **`onARViewCreated`** – Initializes the AR environment, sets up plane detection, and manages plane taps.
- **`onPlaneOrPointTapped`** – Handles user taps on the detected plane, finds the furthest hit result, and adds a new anchor to place artwork and frames at the selected location.
- **`_addNodeToAnchor`** – Adds a new anchor and places the artwork and frame nodes. It also ensures that only one anchor exists by removing the previous one.
- **`_updateArtworkNode`**, **`_updateFrameNode`**, **`_updateNodeScale`** – These functions update the nodes in real-time when the user changes artwork, frames, or the scale value using the slider.
- **Slider on the right side of the screen** – Allows users to adjust the size of the placed AR nodes. The slider triggers an update only when the user releases it.

## Implementation Notes

### Depth Sensor Hardware Integration

To make the AR experience more accurate, we should consider incorporating depth sensor hardware (e.g., LiDAR on newer iPhones or depth sensors on Android). This can help improve the accuracy of plane detection, especially when dealing with surfaces without much detail.

### Frame and Artwork Aspect Ratio

Ensure that the aspect ratios of frames and artworks are visually consistent to provide a natural look. Adjust the 3D model scaling to maintain the intended appearance of frames and artwork combinations.

### Challenges and Improvements

- **Real-Time Anchor Updates**: The app currently removes and recreates anchors whenever a new artwork or frame is selected. Future improvements could focus on directly modifying the nodes, reducing the need for removing and recreating anchors.
- **Overlapping Nodes**: Ensuring that overlapping objects do not occur is important to maintain a clean AR experience. This is managed by clearing the previous anchor before placing a new one.

### Unimplemented Features

- **Additional Augmented Reality Features**: Implementing gesture recognition to allow users to manually reposition and rotate AR objects directly could enhance usability.
- **Efficient aspect ratio functionality**: A way that ensures artwork and frames maintain their intended proportions across different screen sizes and placements.

## Usage Guide

1. **Home Page**: Start from the home page and navigate to different pages, including the AR experience.
2. **AR Page**: Tap "Tap to Begin AR Session" to begin.
3. **Placing Objects**:
    - Tap on a detected vertical plane to place the artwork and frame.
    - Select different artworks or frames from the available options.
    - Use the slider on the right side of the screen to adjust the scale of the AR objects.
4. **Resetting Scene**: Tap "Remove Everything" to clear all placed objects.

## Dependencies

- **AR Flutter Plugin**: This app uses the `ar_flutter_plugin` to provide AR capabilities. Make sure to follow the setup instructions on [pub.dev](https://pub.dev/packages/ar_flutter_plugin).
- **Path Provider**: The `artwork_initializer.dart` uses the `path_provider` package to manage file storage for demo artworks.

## Installation

1. **Clone the Repository**: Clone the repository to your local machine.
2. **Install Dependencies**: Run `flutter pub get` to install the required dependencies.
3. **Run the App**: Use `flutter run` to run the app on a connected AR-compatible device.

## Future Work

- **Gesture Interactions**: Implement gestures like dragging and rotation to enhance user interaction with AR objects.

- **Depth Sensor Integration**: Improve accuracy in AR placement by incorporating depth sensor capabilities where available.

## Contribution

Feel free to submit pull requests to add new features, fix bugs, or improve code readability and performance.

## License

This project is licensed under the MIT License.

## Exporting 3D Models in GLTF Format

To add new artwork or frames, you need to export your 3D models in GLTF format. Below are the steps for exporting GLTF models correctly, (NOTE: ENSURE THAT MODELS HAVE OPTIMIZED POLYCOUNT and TEXTURE SIZE FOR STABLE PERFORMANCE):

1. **Center the Object at the Origin**: Ensure that your model is centered at the origin (0,0,0) in your 3D modeling software. This helps with consistent placement in AR.
2. **Set Transform to +Y Up**: Make sure the model's transform is set with the +Y axis pointing up. This is the standard for GLTF models and will ensure correct orientation in the AR environment.
3. **Apply Transformations**: Apply all transformations to the model, such as scaling, rotation, and position. This makes sure that the exported model matches what you see in the 3D software.
4. **Keep Files Together**: Export the model in GLTF format, ensuring that the `.bin` file, `.gltf` file, and any textures are kept in the same directory. This will help the AR application correctly load the model with all its assets.
5. **Copy to Asset Folder**: After exporting, copy the `.gltf`, `.bin`, and texture files to the `assets` folder in your Flutter project. Update the `pubspec.yaml` file to include these assets for Flutter to use them.
6. **Create Preview Images**: For each artwork and frame, create a preview image. For artwork, you can use an image of the artwork itself. For frames, you may need to render the frame to create a preview image.

### Example Workflow for Blender

1. **Model Preparation**:
    - Import or create your models in Blender.
    - Position the model at the origin and ensure the +Y axis is up.
    - Apply all transformations (`Ctrl + A` -> Apply All Transformations).
2. **Exporting**:
    - Go to `File` > `Export` > `glTF 2.0 (.glb/.gltf)`.
    - Choose `GLTF Separate` to get `.gltf`, `.bin`, and texture files.
    - Make sure to include the textures in the export options.
3. **Add to Project**:
    - Copy the exported files to your Flutter project’s `assets` directory.
    - Add references in `pubspec.yaml` to ensure Flutter includes these assets.
4. **Create Previews**:
    - Render images in Blender or take screenshots of your 3D model.
    - Save these preview images in the `images` directory and reference them in the code for selection UI.

