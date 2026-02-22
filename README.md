# UniRide Bus Tracker - Android Frontend

A comprehensive university bus tracking system with separate interfaces for drivers and passengers.

## 📱 Features

### Driver Features
- 🔐 Secure driver login with credentials
- 📍 Real-time GPS location tracking
- 🚌 Route assignment and management
- ⏱️ ETA updates to next stop
- 👥 Bus occupancy reporting
- 📡 Live location broadcasting

### Passenger Features
- 🗺️ Interactive route selection
- 📍 Real-time bus location tracking
- ⏰ Live ETA countdown
- 🚏 Stop markers on map
- 📊 Bus occupancy information
- 🔔 Arrival notifications

## 📂 Project Structure

```
BusTracker/
│
├── activities/
│   ├── RoleSelectActivity.kt          # Main entry - role selection
│   ├── DriverLoginActivity.kt         # Driver authentication
│   ├── DriverMapActivity.kt           # Driver's map & controls
│   ├── PassengerRouteActivity.kt      # Route selection for passengers
│   └── PassengerMapActivity.kt        # Passenger's tracking map
│
├── layouts/
│   ├── activity_role_select.xml       # Role selection UI
│   ├── activity_driver_login.xml      # Driver login UI
│   ├── activity_driver_map.xml        # Driver map UI
│   ├── activity_passenger_route.xml   # Route selection UI
│   ├── activity_passenger_map.xml     # Passenger map UI
│   └── item_route_card.xml            # Route card for RecyclerView
│
├── model/
│   └── BusRoute.kt                    # Data models
│
├── res/
│   └── values/
│       ├── colors.xml                 # Color resources
│       ├── strings.xml                # String resources
│       └── styles.xml                 # Theme & styles
│
└── AndroidManifest.xml                # App configuration
```

## 🚀 Setup Instructions

### 1. Prerequisites
- Android Studio Arctic Fox or later
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 33 (Android 13)
- Google Play Services

### 2. Google Maps API Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable **Maps SDK for Android** and **Geolocation API**
4. Create API credentials (API Key)
5. Add the API key to `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_ACTUAL_API_KEY_HERE" />
   ```

### 3. Dependencies (build.gradle)
Add these dependencies to your `app/build.gradle`:

```gradle
dependencies {
    // AndroidX
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.coordinatorlayout:coordinatorlayout:1.2.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    
    // Material Design
    implementation 'com.google.android.material:material:1.11.0'
    
    // Google Maps & Location
    implementation 'com.google.android.gms:play-services-maps:18.2.0'
    implementation 'com.google.android.gms:play-services-location:21.1.0'
    
    // Kotlin Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // Lifecycle
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    
    // Parcelize
    implementation 'org.jetbrains.kotlin:kotlin-parcelize-runtime:1.9.0'
}
```

### 4. Permissions
The app requires these permissions (already in AndroidManifest.xml):
- `ACCESS_FINE_LOCATION` - For GPS tracking
- `ACCESS_COARSE_LOCATION` - For network location
- `INTERNET` - For map tiles and API calls
- `FOREGROUND_SERVICE` - For background location tracking

### 5. File Structure Setup
Copy all files to your Android project:
```
app/src/main/
├── java/com/university/bustracker/
│   ├── activities/
│   │   ├── RoleSelectActivity.kt
│   │   ├── DriverLoginActivity.kt
│   │   ├── DriverMapActivity.kt
│   │   ├── PassengerRouteActivity.kt
│   │   └── PassengerMapActivity.kt
│   └── model/
│       └── BusRoute.kt
│
├── res/
│   ├── layout/
│   │   ├── activity_role_select.xml
│   │   ├── activity_driver_login.xml
│   │   ├── activity_driver_map.xml
│   │   ├── activity_passenger_route.xml
│   │   ├── activity_passenger_map.xml
│   │   └── item_route_card.xml
│   └── values/
│       ├── colors.xml
│       ├── strings.xml
│       └── styles.xml
│
└── AndroidManifest.xml
```

## 🎨 Design Features

- **Modern Material Design** with custom themes
- **Gradient backgrounds** and card elevations
- **Smooth animations** for transitions
- **Responsive layouts** for different screen sizes
- **Dark status bar** with transparent overlays

## 🔐 Demo Credentials

For testing the driver login:
- **Driver ID:** D001
- **Password:** 1234

Other valid credentials:
- D002 / 5678
- D003 / abcd

## 🗺️ Mock Data

The app includes mock data for 4 routes:
1. **Route 1:** Main Campus → North Dorms (6 stops)
2. **Route 2:** Main Campus → South Dorms (5 stops)
3. **Route 3:** Library → Sports Complex (4 stops)
4. **Route 4:** Downtown Express (inactive)

## 📡 Backend Integration

Currently uses mock data. To integrate with a real backend:

1. **Driver Location Updates** (DriverMapActivity.kt):
   ```kotlin
   // Replace this comment in updateLocationOnMap()
   // sendLocationToServer(location.latitude, location.longitude)
   ```

2. **Passenger Bus Tracking** (PassengerMapActivity.kt):
   ```kotlin
   // Replace mock animation with real-time location from backend
   // fetchBusLocationFromServer(busId)
   ```

3. **ETA Updates** (DriverMapActivity.kt):
   ```kotlin
   // Replace this comment in broadcastUpdate()
   // updateBackend(eta, nextStop, occupancy)
   ```

## 🎯 Key Components

### RoleSelectActivity
- Entry point of the app
- Choose between Driver or Passenger mode

### DriverLoginActivity
- Validates driver credentials
- Route assignment
- Session management

### DriverMapActivity
- Real-time GPS tracking
- Location broadcasting
- ETA and status updates
- Shift management

### PassengerRouteActivity
- Displays available routes in RecyclerView
- Route cards with status indicators
- ETA display

### PassengerMapActivity
- Shows bus location on Google Maps
- Route polyline visualization
- Stop markers
- Live ETA countdown
- User location tracking

## 🎨 Drawable Resources Needed

Create these drawable XML files in `res/drawable/`:

- `ic_bus.xml` - Bus icon
- `ic_driver.xml` - Driver icon
- `ic_passenger.xml` - Passenger icon
- `ic_arrow_forward.xml` - Arrow icon
- `button_primary.xml` - Primary button background
- `button_secondary.xml` - Secondary button background
- `gradient_driver.xml` - Driver card gradient
- `gradient_passenger.xml` - Passenger card gradient
- `spinner_background.xml` - Spinner background
- `error_background.xml` - Error message background
- `status_background_active.xml` - Active status badge
- `bottom_sheet_background.xml` - Bottom sheet background
- `drag_handle.xml` - Bottom sheet drag handle

## 📱 Testing

1. Run on physical device for accurate GPS testing
2. Enable location services
3. Grant location permissions
4. Test both driver and passenger flows

## 🔄 Future Enhancements

- Real-time backend integration
- Push notifications for bus arrivals
- Chat between driver and passengers
- Route history and analytics
- Offline mode support
- Multiple language support

## 📄 License

This is a demonstration project for educational purposes.

## 🤝 Contributing

This is a frontend template. Customize for your university's requirements.

---

**Built with ❤️ for University Campus Transportation**
