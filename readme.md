# UniRide - Smart Campus Transportation System

UniRide is a comprehensive transportation management solution designed for university campuses. It synchronizes Admins, Drivers, Conductors, and Passengers (Students/Staff) in real-time to provide a seamless and professional commute experience.

## 🚀 Features

### 🏢 Admin Panel
- **Fleet Management**: Add and manage Drivers and Conductors. Smart merging of staff assigned to the same route.
- **Passenger Management**: Unified dashboard for Students and Staff/Faculty.
- **Access Control**: Approve or reject new registration requests.
- **Live Fleet Map**: Real-time tracking of all active buses with detailed occupancy and staff info.

### 🚛 Driver Dashboard
- **Service Control**: Start and end transit services with a single tap.
- **Map-based Stop Addition**: Add new stops by simply tapping on the map.
- **Passenger Insights**: View a detailed list of waiting passengers at each stop, including photos and identification.
- **Live Sync**: Automatic occupancy updates and location sharing.


### 👤 Passenger App
- **Real-time Tracking**: Track assigned buses (e.g., BH1, DW3) on a live map.
- **Staff Visibility**: View photos and contact details for both the Driver and Conductor.
- **Boarding Status**: Auto-detection of boarding and seat reservation features.
- **Role-based Profiles**: Specific fields for Students (Roll No) and Staff (Employee Code & Department).

## 🛠️ Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Modern, declarative UI)
- **Database**: Firebase Realtime Database (for instant synchronization)
- **Maps**: Mapbox SDK & OSMDroid
- **Image Loading**: Coil
- **Location Services**: Google Play Services Location

## 📦 Setup & Installation

1. **Clone the repository**:
   ```bash
   git clone 

2. **Firebase Setup**:
   - created a firebase project .
   - Added an Android App with package name `com.uniride`.
   - Download `google-services.json` and placed it in the `app/` directory.
   - Enable **Realtime Database** and **Firebase Auth**.

3. **Mapbox Setup**:
   - have a map box acces token and API key added in the mapbox 
   - Added  token to project  environment variables.

4. **Build**:
   - Open the project in Android Studio.
   - Sync Gradle and run the `app` module.

## 📄 Environment Variables
 (see `.env.` for reference) .

## 👥 Developers
- **Yash Kumar Gautam**
- **Ankush Sangwan**
- **Sanyam sharma**
- **NETRA PARKASH**
