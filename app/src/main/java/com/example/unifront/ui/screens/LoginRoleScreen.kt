package com.example.unifront.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifront.ui.theme.*

@Composable
fun LoginRoleScreen(
    onDriverSelected: () -> Unit,
    onPassengerSelected: () -> Unit,
    onAdminSelected: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Circuit Board Pattern Background
        CircuitPattern()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // App Logo
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(20.dp, CircleShape),
                color = UberWhite,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DirectionsBus,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = UberBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "UniRide",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = UberWhite,
                    letterSpacing = 2.sp
                )
            )
            
            Text(
                text = "Smart Campus Transportation",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = UberWhite.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "How are you travelling today?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = UberWhite
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Role Cards
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ModernRoleCard(
                    title = "Passenger",
                    description = "Track buses & reserve your seat",
                    icon = Icons.Default.Person,
                    startColor = PassengerGradientStart,
                    endColor = PassengerGradientEnd,
                    onClick = onPassengerSelected
                )

                ModernRoleCard(
                    title = "Driver",
                    description = "Start your route & share location",
                    icon = Icons.Default.DirectionsBus,
                    startColor = DriverGradientStart,
                    endColor = DriverGradientEnd,
                    onClick = onDriverSelected
                )

                ModernRoleCard(
                    title = "Administrator",
                    description = "Manage fleet & driver profiles",
                    icon = Icons.Default.Apartment,
                    startColor = AdminGradientStart,
                    endColor = AdminGradientEnd,
                    onClick = onAdminSelected
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Developer Credits
            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Developed by",
                    style = MaterialTheme.typography.labelSmall,
                    color = UberWhite.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Yash Kumar Gautam • Ankush Sangwan • Sanyam Sharma • Netra Prakash",
                    style = MaterialTheme.typography.labelSmall,
                    color = UberWhite.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By continuing, you agree to our Terms of Service",
                    style = MaterialTheme.typography.labelSmall,
                    color = UberWhite.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ModernRoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    startColor: Color,
    endColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(startColor, endColor)
                    )
                )
        ) {
            // Background Watermark Icon (Gear)
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp)
                    .size(140.dp)
                    .alpha(0.1f)
                    .rotate(30f),
                tint = UberWhite
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Container
                Surface(
                    modifier = Modifier.size(60.dp),
                    color = UberWhite.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = UberWhite
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = UberWhite,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = UberWhite.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = UberWhite
                )
            }
        }
    }
}

@Composable
fun CircuitPattern() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
        val path = Path()
        val color = Color(0xFF00FFCC)
        val stroke = 1.dp.toPx()

        // Draw some "circuit" lines
        // Line 1
        path.moveTo(0f, size.height * 0.2f)
        path.lineTo(size.width * 0.3f, size.height * 0.2f)
        path.lineTo(size.width * 0.4f, size.height * 0.3f)
        path.lineTo(size.width * 0.4f, size.height * 0.5f)
        
        // Line 2
        path.moveTo(size.width, size.height * 0.1f)
        path.lineTo(size.width * 0.7f, size.height * 0.1f)
        path.lineTo(size.width * 0.6f, size.height * 0.2f)
        path.lineTo(size.width * 0.6f, size.height * 0.4f)
        path.lineTo(size.width * 0.8f, size.height * 0.6f)

        // Line 3
        path.moveTo(0f, size.height * 0.8f)
        path.lineTo(size.width * 0.2f, size.height * 0.8f)
        path.lineTo(size.width * 0.3f, size.height * 0.7f)
        path.lineTo(size.width * 0.5f, size.height * 0.7f)

        drawPath(path, color, style = Stroke(width = stroke))
        
        // Draw some small circles at junctions
        drawCircle(color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.4f, size.height * 0.3f))
        drawCircle(color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.2f))
        drawCircle(color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.7f))
    }
}
