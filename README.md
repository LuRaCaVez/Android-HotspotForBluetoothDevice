# Hotspot For Bluetooth Device

A specialized Android utility that automatically toggles your phone's Wi-Fi hotspot based on the connection status of a specific device identified by its HMAC (MAC Address).

## 📱 Compatibility
- **Primary Test Environment:** Realme 8 (Android 13).
- **Other Devices:** While designed for the Realme 8, users on other devices or Android versions are welcome to try it. Note that hotspot APIs can be manufacturer-specific.

## 🚀 How It Works
* **On Connection:** When the app detects that the device with the configured HMAC has connected to your phone, it automatically enables the Hotspot.
* **On Disconnection:** As soon as that specific device disconnects, the Hotspot is disabled.

## ⚠️ Mandatory Permissions
For this app to function, **every requested permission must be granted.** Android's security model is very strict regarding system settings:
* **Modify System Settings:** Needed to toggle the Hotspot.
* **Battery Optimization:** You **must** set this app to "Don't Optimize" in your phone settings, or Android 13 will kill the process while it's waiting in the background.

## 🛠 Setup
1. Enter the **HMAC/MAC Address** of the client
