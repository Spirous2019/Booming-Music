# 📦 Release Build Guide (How to Generate the Installation APK)

Follow these exact steps to build a **Release APK** of the app using Android Studio. This guide uses the release signing keys backed up inside this folder, ensuring the resulting APK is signed properly and can seamlessly update your existing installation.

---

## 🔑 Signing Credentials Summary

The project contains a pre-configured keystore. Here are the credentials to use:

*   **Keystore File:** `my-release-key.jks` (located in the root folder of this project)
*   **Keystore Password:** `booming123`
*   **Key Alias:** `key0`
*   **Key Password:** `booming123`

> [!TIP]
> The keystore file (`my-release-key.jks`) is tracked and committed inside this repository, making it immediately available when you clone this project on another device.

---

## 🛠️ Step-by-Step Build Instructions

### 1. Open the Project in Android Studio
Make sure you have opened the `BoomingMusic` project directory in Android Studio and that all Gradle syncs have completed successfully.

### 2. Open the Generate Signed APK Dialog
In the top menu bar of Android Studio, click:
**Build** ➡️ **Generate Signed Bundle / APK...**

### 3. Select APK
1. In the dialog, select the **APK** option (not Android App Bundle).
2. Click **Next**.

### 4. Configure Keystore Paths & Credentials
1. **Key store path:** Click the folder icon on the right, navigate to this project's root folder, and select the `my-release-key.jks` file.
2. **Key store password:** Type `booming123`.
3. **Key alias:** Select `key0` from the drop-down menu (if it doesn't auto-fill).
4. **Key password:** Type `booming123`.
5. Check the box for **Remember passwords** so you don't have to type them next time.
6. Click **Next**.

### 5. Choose Build Variant & Signatures
1. **Build Variants:** Select **`normalRelease`**.
   * *Do NOT select `fdroidRelease` or any `debug` variants.*
2. **Signature Versions:** Ensure both are checked:
   * [x] **V1 (Jar Signature)**
   * [x] **V2 (Full APK Signature)**
3. Click **Create** (or **Finish**).

### 6. Locate the Generated APK
1. Wait for Android Studio to compile the app. You can track progress in the status bar at the bottom right.
2. Once complete, a notification popup will appear in Android Studio saying: **"APK(s) generated successfully..."**
3. Click the **Locate** link inside that notification to open the folder containing the signed APK files.
4. You will see several APK files. Select the correct one for your device:
   * **For Samsung Galaxy A16 (and modern phones):** Choose **`BoomingMusic-1.3.0-normal-arm64-v8a.apk`**. This is optimized specifically for your phone's processor.
   * **Fallback:** Choose **`BoomingMusic-1.3.0-normal-universal.apk`**. It works on any device but has a larger file size.

---

## 🔄 Updating your Existing App

To install this update on your phone without losing any settings:
1. Transfer your chosen APK (preferably **`BoomingMusic-1.3.0-normal-arm64-v8a.apk`**) to your phone.
2. Open the file on your device and tap **Update**.

### ⚠️ Troubleshooting "App not installed" or "Package conflicts"
If your device displays a signature conflict or install failure:
1. **Check if you have a Debug version installed:** If you previously installed a debug version of the app (built directly via Android Studio's play button, or via "Build APK" without signing), you must uninstall it first. Debug builds use a different signature key.
2. **Check the Keystore:** Ensure you used the exact `my-release-key.jks` file provided in this folder, and selected the `normalRelease` build variant.

