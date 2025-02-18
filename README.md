**Project Documentation for A11y**
--

### 📘 Overview  

- **Project Name:** A11y  
- **Purpose:** Gesture Recognition using IMU sensor data to enhance accessibility for users.  
- **Tech Stack:** Java, XML, Android SDK  

#### 📝 Description  
A11y is an Android application designed to recognize user gestures through IMU (Inertial Measurement Unit) sensor data. It utilizes machine learning models, Bluetooth Low Energy (BLE) for sensor communication, and provides feedback through visual charts and logs.  

#### 🚀 Key Features  
- Real-time gesture detection from wearable sensors  
- BLE connectivity for receiving IMU data  
- Gesture-to-action mapping for customizable controls  

#### 🧰 Tools & Frameworks  
- **IDE:** Android Studio  
- **Language:** Java  
- **UI Design:** XML 
- **ML Framework:** Weka, MediaPipe (Future Work)
- **Camera:** CameraX

---

### 📦 Dependencies  

  The **A11y** app uses several third-party libraries and AndroidX components to ensure functionality, performance, and compatibility. Below is a list of the dependencies and their purposes:



#### 🛠 Core Android Libraries

- **androidx.appcompat\:appcompat:1.6.1** – Provides backward-compatible versions of Android components. &#x20;

- **androidx.constraintlayout\:constraintlayout:2.1.4** – Supports complex layouts with ConstraintLayout. &#x20;

- **androidx.navigation\:navigation-fragment:2.5.3** – Handles navigation between fragments. &#x20;

- **androidx.navigation\:navigation-ui:2.5.3** – UI utilities for navigation components. &#x20;



#### 🎨 UI Components

- **com.google.android.material\:material:1.10.0** – Material Design components for building modern UIs. &#x20;

- **com.github.skydoves\:powerspinner:1.2.7** – Customizable dropdown spinner for modern UIs. &#x20;

- **com.github.PhilJay\:MPAndroidChart\:v3.1.0** – Charting library for displaying graphs and statistics. &#x20;



#### 📸 CameraX

- **androidx.camera\:camera-core:\${camerax\_version}** – Core CameraX functionality. &#x20;

- **androidx.camera\:camera-camera2:\${camerax\_version}** – Camera2 extensions for CameraX. &#x20;

- **androidx.camera\:camera-lifecycle:\${camerax\_version}** – Manages camera lifecycle with Lifecycle components. &#x20;

- **androidx.camera\:camera-video:\${camerax\_version}** – Video capture support. &#x20;

- **androidx.camera\:camera-view:\${camerax\_version}** – Provides a camera preview view. &#x20;

- **androidx.camera\:camera-mlkit-vision:\${camerax\_version}** – Integration with ML Kit for vision tasks. &#x20;

- **androidx.camera\:camera-extensions:\${camerax\_version}** – Additional camera capabilities like HDR and Night mode. &#x20;


#### 🤖 Machine Learning & BLE

- **com.google.mediapipe\:tasks-vision\:latest.release** – MediaPipe Tasks for computer vision models. &#x20;

- **com.polidea.rxandroidble2\:rxandroidble:1.17.2** – BLE (Bluetooth Low Energy) support with RxJava. &#x20;

- **wekaSTRIPPED.jar** – Weka library for machine learning algorithms (Customized for Mobile devices). &#x20;

- **mdslib-3.15.0-release.aar** – Support for Movesense related functions. &#x20;


---


### 🛠 Installation & Setup  

#### 📥 Clone the Repository  
1. Open a terminal and run the following command to clone the project:  
   ```bash
   git clone https://github.com/a11y-edu/A11y.git
   ```
2. Open **Android Studio**.  
3. Select **File > Open** and navigate to the cloned project directory.  
4. Click **Open** to load the project.  

#### 📌 Prerequisites  
- **Android Studio:** Latest stable version  
- **Java SDK:** 11 or higher  

#### 🚀 Build the Project  
- Sync Gradle by clicking **"Sync Project with Gradle Files"** in Android Studio.  
- Ensure dependencies are installed.  
- Click **Run** ▶️ to build and deploy the app on an emulator or connected device (Preferred).  


---

### 📂 Project Structure  

### Overview

- **/app/src/main/java/com/a11y/** – Java source files and modules 
- **/app/src/main/res/layout/** – XML layout files  
- **/libs/** – External JAR/AAR libraries  
- **/app/src/AndroidManifest.xml** - Configuration for the App

```
📂 app  
   └── 📂 src  
       ├── 📂 main  
       │   ├── AndroidManifest.xml
       │   ├── 📂 java/com/a11yEdu/a11y  
       │   │   ├── MainActivity.java  
       │   │   └── OtherFiles.java // Just for Representation
       │   └── 📂 res  
       │       ├── 📂 layout  
       │       └── 📂 drawable  
       └── 📂 libs
```

### - Important modules and files

#### 📄 `HomeActivity.java`

**Purpose:** Main dashboard for navigating app features and managing initial setup.  

**Key Functions:**  
- **UI Setup:** Initializes card views and sets click listeners.  
- **Directory Creation:** Creates folders for `rawData`, `trimmedData`, `models`, and `rawVideos`.  
- **File Cleanup:** Deletes unmatched files via `FilePairChecker` in a background thread.  
- **Navigation:** Opens respective activities based on card selection (e.g., `RecordActivity`, `BluetoothActivity`).  

#### 📄 `BluetoothActivity.java`  

**Purpose:** Manages Bluetooth permissions, device discovery, and connections to Movesense sensors.  

**Key Functions:**  
- **Permissions:** Requests necessary Bluetooth permissions if not granted.  
- **Device Management:** Stores previously connected Movesense sensors.  
- **Scanning:** Searches for new Bluetooth devices using the Movesense API.  
- **Connection:** Establishes and maintains communication with Movesense sensors.  

#### 📂 `KMeans` Module (`KMeans.java`, `KMeansObj.java`)  

**Purpose:** Performs K-means clustering on provided data using Weka libraries and analyzes results with LCSS (Longest Common Subsequence).  

**Key Functions:**  
- **Clustering:** Uses Weka’s K-means algorithm to group similar data points.  
- **LCSS Analysis:** Applies LCSS to compare patterns within clusters and generate results.  
- **Data Handling:** Processes input datasets and returns clustering outputs.  

#### 📂 `CSVEditor` Module (`CSVFile.java`)  

**Purpose:** Handles CSV file operations for the project.  

**Key Functions:**  
- **CSV Management:** Reads, writes, and modifies CSV files.  
- **Data Analysis:** Supports moving average calculations and data differentiation.  
- **Utility:** Provides reusable CSV functions for other modules.  

#### 📂 `VideoTrimmer` Module  

**Purpose:** Trims video and CSV data for synchronized analysis.  

**Key Functions:**  
- **Based on:** Modified version of K4LVideoTrimmer from GitHub (fixed compatibility issues).  
- **Video & Graph View:** Displays video and graph views side-by-side.  
- **CSV Trimming:** Trims corresponding CSV data alongside video segments.  
- **UI Improvements:** Enhanced interface for better usability.  

#### 📄 `RecordActivity.java`  

**Purpose:** Records video and sensor data simultaneously.  

**Key Functions:**  
- **Camera Recording:** Captures video using the device camera.  
- **Sensor Integration:** Records data from the connected sensor alongside video.  
- **Camera Flip:** Allows switching between front and rear cameras.  
- **File Storage:** Saves recordings as `.mp4` (video) and `.csv` (sensor data).  

#### 📄 `RawVideoActivity.java`  

**Purpose:** Displays and manages recorded video data.  

**Key Functions:**  
- **Data Viewing:** Lists all recorded videos with thumbnails.  
- **Management:** Supports deleting and renaming recordings.  
- **Performance:** Uses background threads to load videos and thumbnails quickly.  

#### 📄 `GestureCategoryActivity.java`  

**Purpose:** Manages gesture categories for the app.  

**Key Functions:**  
- **Category Creation:** Prompts for category name, speech text, and AAC ignore setting.  
- **Category List:** Displays all created categories.  
- **Detail View:** Opens a detailed activity for each category on selection.  

#### 📄 `GestureInstanceActivity.java`  

**Purpose:** Manages gesture instances within a category.  

**Key Functions:**  
- **TTS Playback:** Plays the category’s text using text-to-speech.  
- **Instance Creation:** Opens `VideoListActivity` to select a video, then launches `VideoTrimmer.java` for trimming. Creates a new instance after trimming.  
- **Category Management:** Allows renaming, deleting, or modifying category settings.  
- **Instance List:** Displays all gesture instances, which open detailed views on selection.  

#### 📄 `InstanceDataActivity.java`  

**Purpose:** Displays details of a specific gesture instance.  

**Key Functions:**  
- **Video Preview:** Shows a thumbnail of the trimmed video, plays it on click.  
- **IMU Data:** Displays a graph of the recorded IMU sensor data.  

#### 📄 `VisualizationActivity.java`  

**Purpose:** Compares gesture instances and analyzes their similarity using models and heatmaps.  

**Key Functions:**  
- **Instance Comparison:** Plays two selected gesture instances side-by-side or individually with IMU data animations.  
- **Model Creation:** Allows users to create, save, rename, or delete models by adjusting accelerometer, gyroscope, and accelerometer moving average (MA) weights.  
- **Similarity Check:** Calculates similarity between the two instances using the KMeans module.  
- **Heatmap Generation:** Produces heatmaps (instance-wise or category-wise) showing similarity based on the selected model.  

#### 📄 `AccessibleActivity.java`  

**Purpose:** Provides an AAC interface using gesture recognition.  

**Key Functions:**  
- **Speech Box:** Displays selected gestures with "Clear" and "Play" buttons. 
- **Log View:** Displays recognition results: cross-recognition in red and good recognition in green. 
- **Watchdog Timer:** Buttons to set time limits for gesture detection.  
- **Model Selection:** Dropdown to choose from previously created models.  
- **Gesture Recognition:** Toggle switch to enable recognition (requires sensor connection and atleast one model). Detected gestures are added to the speech box with their accuracy.  
- **Gesture Shortcuts:** Displays all non-ignored gestures, which can be tapped to add to the speech box.  

---

### 🛠️ How to Load Data (Side-Loading)  

To manually load or restore previous data into the A11y app, follow these steps:  

1. **Close the A11y App:**  
   - **Ensure the app is completely closed before proceeding.** Keeping the app open during this process may cause data corruption or errors.  

2. **Navigate to the App Directory:**  
   - On your Android device, open a file manager and go to:  
     **`Android > data > com.a11yEdu.a11y > Documents`**  

3. **Replace Files and Folders:**  
   - Delete the existing contents in the `Documents` folder.  
   - Copy and paste your backed-up files and folders into the `Documents` directory.  

4. **Restart the App:**  
   - Open the A11y app to view the side-loaded data, including raw videos, trimmed data, models, and gesture instances.  

⚠️ **Note:** Ensure the app remains closed during the entire transfer process to avoid issues.  

---

### Thank You
We ❤️ Accessibility



