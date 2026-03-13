# PreBoard Exam Checker

PreBoard Exam Checker is an Android application designed to automate and streamline the process of checking and managing preboard examination results. It leverages mobile camera technology, image processing, and cloud services to provide a fast and efficient experience for educators.

## Features

- **Automated Exam Scanning:** Uses the device camera and OpenCV to scan and grade shaded answer sheets.
- **Student & Exam Management:** Organize student records and exam sessions within the app.
- **Firebase Integration:** Secure authentication and real-time data storage using Firebase Auth and Firestore.
- **Detailed Analytics:** (Coming soon/Available) Get insights into student performance.
- **Export Options:** Export results to Excel (XLSX) or PDF formats for official record-keeping.

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM (Model-View-ViewModel)
- **Dependency Injection:** Hilt
- **Database:** Room (Local) & Firebase Firestore (Cloud)
- **Image Processing:** OpenCV
- **Authentication:** Firebase Authentication
- **External Libraries:** Apache POI (Excel), iText7 (PDF), CameraX

## Prerequisites

- Android Studio Iguana (2023.2.1) or newer
- JDK 17
- Android Device or Emulator running API 26 (Android 8.0) or higher
- Firebase Project (configured with `google-services.json`)

## Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/jsonrls/preboard.git
cd preboard
```

### 2. Firebase Setup
1. Create a new project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android App to your Firebase project using the package name `com.pbec.preboardexamchecker`.
3. Download the `google-services.json` file and place it in the `app/` directory of the project.
4. Enable **Anonymous Authentication** and **Firestore** in the Firebase console.

### 3. Open and Build
1. Open Android Studio and select **Open**.
2. Navigate to the project folder and click **OK**.
3. Let Gradle sync and download all necessary dependencies.

### 4. Run the App
1. Connect an Android device via USB or start an emulator.
2. Click the **Run** button (green play icon) in Android Studio.

## Project Structure

- `app/src/main/java/com/pbec/preboardexamchecker/ui`: Contains all Compose screens and ViewModels.
- `app/src/main/java/com/pbec/preboardexamchecker/data`: Contains Room database entities, DAOs, and repository classes.
- `app/src/main/java/com/pbec/preboardexamchecker/util`: Utility classes for image processing, file handling, and formatting.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Developed for **Computer Arts and Technological College, Inc.**
