package com.example.unifront.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Driver(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val assignedBusId: String = "None",
    val manualRoute: String = "",
    val password: String = "",
    val plateNumber: String = "",
    val photoUrl: String = ""
)

@IgnoreExtraProperties
data class Conductor(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val assignedBusId: String = "None",
    val photoUrl: String = ""
)

@IgnoreExtraProperties
data class StudentRequest(
    val id: String = "",
    val name: String = "",
    val rollNo: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val preferredRoute: String = "",
    val status: String = "pending",
    val isBoarded: Boolean = false,
    val boardedAt: Long = 0L,
    val userType: String = "Student", // "Student", "Staff", "Faculty"
    val department: String = "",
    val employeeCode: String = "",
    val designation: String = "",
    val profilePhotoUrl: String = ""
)

@IgnoreExtraProperties
data class BusLiveInfo(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val speed: Double = 0.0,
    val status: String = "offline",
    val occupancy: Int = 0,
    val serviceStart: Long = 0L,
    val serviceEnd: Long = 0L
)
