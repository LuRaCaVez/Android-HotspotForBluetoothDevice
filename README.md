# Hotspot Proximity Trigger (BLE)

A specialized Android utility that automatically toggles your phone's Wi-Fi hotspot based on the physical distance to a specific device, calculated via a **BLE (Bluetooth Low Energy)** service.

## 📱 Compatibility
- **Primary Test Environment:** Realme 8 (Android 13).
- **Other Devices:** Designed for Android 10 and above. Note that hotspot APIs and BLE scanning behaviors are often manufacturer-specific.

## 🚀 How It Works
Unlike standard Bluetooth utilities that trigger on "Connected/Disconnected" states, this app uses a proximity logic:
* **Distance Calculation:** The app monitors the signal strength (RSSI) of a specific BLE service to estimate the distance between your phone and the target device.
* **Proximity Activation:** When the device is detected within a specific range, the Hotspot is automatically enabled.
* **Auto-Shutdown:** If the device moves out of range or the BLE signal is lost, the Hotspot is disabled to conserve battery.

## ⚠️ Mandatory Permissions
For this app to function in the background, **every requested permission must be granted**:
* **Modify System Settings:** Needed to toggle the Hotspot state.
* **Location Services (Fine Location):** Android requires location access to perform BLE scans.
* **Battery Optimization:** You **must** set this app to "Don't Optimize" in your phone settings. Without this, Android will kill the background BLE service when the screen is off.

## 🛠 Setup
1. Enter the **MAC Address** or **BLE Service UUID** of the trigger device.
2. **Start the Service**
