# YOLO-VOC Labeler (Android) 

A modern, open-source Android application for on-the-go image labeling and computer vision dataset curation. Designed for flexibility, this app allows you to annotate images directly from your mobile device or tablet, making it perfect for field data collection and rapid dataset building without needing a PC.

##  Key Features

*   **Media Import:** Seamlessly load raw images from your device's Gallery, external SD card, or capture them instantly via the Camera.
*   **Built-in Cropping:** Integrated `CropImageActivity` with support for Bitmaps, Resources, and Android URIs (Gallery, Dropbox, etc.).
*   **Precision Bounding Boxes:** Highly responsive touch interface for drawing bounding boxes. Fully optimized for both finger touch and active stylus input for pixel-perfect annotations.
*   **Class Management:** Easily create, assign, and manage object class names for your datasets.
*   **Dual Export Formats:** 
    *   **YOLO (.txt):** Ready-to-use normalized labels perfect for training YOLOv8, YOLOv11, YOLO26, and other variants.
    *   **PASCAL VOC (.xml):** Industry-standard XML format.
*   **Edit & Re-export:** Import existing PASCAL VOC XML files, tweak bounding box coordinates or class names, and re-export the updated files.
*   **Responsive UI:** A clean Material Design 3 interface that dynamically adapts to both smartphones and tablets, supporting both landscape and portrait orientations.
*   **Deep Storage Access:** Full read/write capabilities for both Internal and External Storage, making it easy to manage large computer vision datasets on high-capacity SD cards.

## 📱 Hardware Optimization
The UI and drawing mechanics have been optimized and tested across various form factors to ensure a seamless labeling workflow:
*   **Smartphones:** Efficient landscape and portrait operation for quick bounding box creation.
*   **Tablets:** Extended workspace with active stylus palm-rejection support for high-precision workflows.

## 🚀 Getting Started

### Prerequisites
*   Android Studio (Latest version recommended)
*   Android device running Android 8.0 (API level 26) or higher.

### 📥 Installation

**For Users (Easy Way):**
1. Go to the [Releases](https://github.com/JamiliHse/YOLO-VOC-Labeler-Android/releases) page of this repository.
2. Download the latest `.apk` file.
3. Transfer it to your Android device and install it (you may need to allow "Install from unknown sources").

**For Developers (Build from Source):**
1. Clone the repository:
   ```bash
   git clone [https://github.com/JamiliHse/YOLO-VOC-Labeler-Android.git](https://github.com/JamiliHse/YOLO-VOC-Labeler-Android.git)

   Screenshots
Note: Add screenshots of your app here to show users what the interface looks like.

Format: ![Tablet Landscape](link-t<img width="960" height="571" alt="photo_1_2026-09-25_07-24-40" src="https://github.com/user-attachments/assets/d035c409-6bae-4266-a8a4-31657d856d2d" />
<img width="960" height="561" alt="photo_2_2026-09-25_07-24-40" src="https://github.com/user-attachments/assets/06ae5f57-27ed-484d-9b9d-ed3476ad8fce" />
o-image)


🛠️ Tech Stack
Language: Kotlin

UI Framework: Jetpack Compose / XML (Material Design 3)

File Output: Standard Android I/O for TXT and XML generation.

🤝 Contributing
Contributions are always welcome! If you have suggestions to improve the app, feel free to fork the repository, create a feature branch, and submit a Pull Request. You can also open an issue for any bugs or feature requests.

📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
