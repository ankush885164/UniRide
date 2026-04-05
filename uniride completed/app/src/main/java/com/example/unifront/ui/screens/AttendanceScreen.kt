package com.example.unifront.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifront.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(busId: String, onBack: () -> Unit) {
    // 60 seats total: 1-6 reserved for teachers, 7-60 for students
    val totalSeats = 60
    val teacherSeatsCount = 6
    
    // State to track occupied seats and their type
    var occupiedSeats by remember { mutableStateOf(mutableMapOf<Int, String>()) }

    val studentCount = occupiedSeats.values.count { it == "Student" }
    val teacherCount = occupiedSeats.values.count { it == "Teacher" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance: $busId", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UberWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(UberWhite)
                .padding(16.dp)
        ) {
            // Stats Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "Students",
                    count = studentCount,
                    icon = Icons.Default.Person,
                    color = UberBlack
                )
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "Teachers",
                    count = teacherCount,
                    icon = Icons.Default.School,
                    color = UberGreen
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Select Seat to Mark Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("Teacher Seat", UberGreen)
                LegendItem("Student Seat", UberBlack)
                LegendItem("Available", UberGray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seat Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(totalSeats) { index ->
                    val seatNumber = index + 1
                    val isTeacherSeat = seatNumber <= teacherSeatsCount
                    val isOccupied = occupiedSeats.containsKey(seatNumber)
                    
                    SeatItem(
                        seatNumber = seatNumber,
                        isTeacherSeat = isTeacherSeat,
                        isOccupied = isOccupied,
                        onClick = {
                            val newMap = occupiedSeats.toMutableMap()
                            if (isOccupied) {
                                newMap.remove(seatNumber)
                            } else {
                                newMap[seatNumber] = if (isTeacherSeat) "Teacher" else "Student"
                            }
                            occupiedSeats = newMap
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatBox(modifier: Modifier, label: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color)
            Text(count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, fontSize = 11.sp, color = UberDarkGray)
    }
}

@Composable
fun SeatItem(seatNumber: Int, isTeacherSeat: Boolean, isOccupied: Boolean, onClick: () -> Unit) {
    val backgroundColor = when {
        isOccupied && isTeacherSeat -> UberGreen
        isOccupied -> UberBlack
        else -> UberGray.copy(alpha = 0.3f)
    }
    val contentColor = if (isOccupied) Color.White else UberBlack

    Surface(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isTeacherSeat && !isOccupied) {
                    Icon(Icons.Default.School, null, modifier = Modifier.size(14.dp), tint = UberDarkGray)
                }
                Text(
                    text = seatNumber.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}
