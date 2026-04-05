package com.example.unifront.ui.components

import android.content.Context
import android.graphics.*
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.unifront.data.BusStop
import com.example.unifront.data.BusData
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import org.osmdroid.util.GeoPoint

data class FleetBus(
    val id: String,
    val routeId: String,
    val location: GeoPoint,
    val occupancy: Int
)

@Composable
fun MapboxMapView(
    modifier: Modifier = Modifier,
    busLocation: GeoPoint? = null,
    userLocation: GeoPoint? = null,
    passengerLocations: List<GeoPoint> = emptyList(),
    stops: List<BusStop> = emptyList(),
    stopPassengerCounts: Map<String, Int> = emptyMap(),
    busOccupancy: Int = 0,
    totalSeats: Int = 60,
    routePoints: List<Point> = emptyList(),
    driverName: String? = null,
    recenterTrigger: Int = 0,
    fleetBuses: List<FleetBus> = emptyList(),
    onBusClick: (String) -> Unit = {},
    onStopClick: (String) -> Unit = {},
    onMapClick: (GeoPoint) -> Unit = {}
) {
    val context = LocalContext.current
    var mapboxMap by remember { mutableStateOf<com.mapbox.maps.MapboxMap?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var polylineAnnotationManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }

    // Improved Bus Shape Marker
    val busBitmap = remember { createBusMarker(context, Color.BLACK, Color.YELLOW) }
    val userBitmap = remember { createModernMarker(context, Color.parseColor("#276EF1"), Color.WHITE, "ME") }
    val stopBitmap = remember { createModernMarker(context, Color.parseColor("#05A357"), Color.WHITE, "●") }
    val passengerBitmap = remember { createModernMarker(context, Color.parseColor("#FFC107"), Color.BLACK, "👤") }
    val fleetBitmap = remember { createBusMarker(context, Color.BLACK, Color.YELLOW) }
    val univBitmap = remember { createModernMarker(context, Color.parseColor("#6650a4"), Color.WHITE, "🏫") }

    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            val target = busLocation ?: userLocation ?: (if(fleetBuses.isNotEmpty()) fleetBuses[0].location else null)
            target?.let {
                mapboxMap?.flyTo(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(it.longitude, it.latitude))
                        .zoom(15.0)
                        .build(),
                    MapAnimationOptions.Builder().duration(1500).build()
                )
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                this@apply.mapboxMap.also { mapboxMap = it }
                val annotationApi = this.annotations
                pointAnnotationManager = annotationApi.createPointAnnotationManager().apply {
                    addClickListener(OnPointAnnotationClickListener { annotation ->
                        val data = annotation.getData()?.asJsonObject
                        val busId = data?.get("busId")?.asString
                        val stopName = data?.get("stopName")?.asString
                        
                        if (busId != null) onBusClick(busId)
                        if (stopName != null) onStopClick(stopName)
                        true
                    })
                }
                polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
                
                mapboxMap?.loadStyle(Style.MAPBOX_STREETS)
                this.gestures.rotateEnabled = false
                
                // ADD MAP CLICK LISTENER
                mapboxMap?.addOnMapClickListener { point ->
                    onMapClick(GeoPoint(point.latitude(), point.longitude()))
                    true
                }
            }
        },
        update = { _ ->
            pointAnnotationManager?.let { manager ->
                manager.deleteAll()
                val annotations = mutableListOf<PointAnnotationOptions>()

                // 1. University Destination
                annotations.add(
                    PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(BusData.UNIVERSITY_LOCATION.longitude, BusData.UNIVERSITY_LOCATION.latitude))
                        .withIconImage(univBitmap)
                        .withTextField("K.R. Mangalam University")
                        .withTextSize(12.0)
                        .withTextOffset(listOf(0.0, 2.5))
                        .withTextColor(Color.parseColor("#6650a4"))
                        .withTextHaloColor(Color.WHITE)
                        .withTextHaloWidth(2.0)
                )

                // 2. Bus Marker
                busLocation?.let {
                    val remaining = (totalSeats - busOccupancy).coerceAtLeast(0)
                    annotations.add(
                        PointAnnotationOptions()
                            .withPoint(Point.fromLngLat(it.longitude, it.latitude))
                            .withIconImage(busBitmap)
                            .withTextField("${driverName ?: "Bus"}\n$remaining Seats")
                            .withTextSize(12.0)
                            .withTextOffset(listOf(0.0, 2.8))
                            .withTextColor(if (remaining < 5) Color.RED else Color.BLACK)
                            .withTextHaloColor(Color.WHITE)
                            .withTextHaloWidth(2.0)
                    )
                }

                // 3. Fleet Buses
                fleetBuses.forEach { bus ->
                    val data = com.google.gson.JsonObject().apply {
                        addProperty("busId", bus.id)
                    }
                    annotations.add(
                        PointAnnotationOptions()
                            .withPoint(Point.fromLngLat(bus.location.longitude, bus.location.latitude))
                            .withIconImage(fleetBitmap)
                            .withTextField(bus.routeId)
                            .withTextSize(14.0)
                            .withTextOffset(listOf(0.0, 2.0))
                            .withTextColor(Color.BLACK)
                            .withTextHaloColor(Color.WHITE)
                            .withTextHaloWidth(1.0)
                            .withData(data)
                    )
                }

                // 4. User Marker
                userLocation?.let {
                    annotations.add(
                        PointAnnotationOptions()
                            .withPoint(Point.fromLngLat(it.longitude, it.latitude))
                            .withIconImage(userBitmap)
                            .withIconSize(0.8)
                    )
                }

                // 5. Passenger Markers
                passengerLocations.forEach { loc ->
                    annotations.add(
                        PointAnnotationOptions()
                            .withPoint(Point.fromLngLat(loc.longitude, loc.latitude))
                            .withIconImage(passengerBitmap)
                            .withIconSize(0.7)
                    )
                }

                // 6. Stop Markers
                stops.forEach { stop ->
                    val count = stopPassengerCounts[stop.name] ?: 0
                    val data = com.google.gson.JsonObject().apply {
                        addProperty("stopName", stop.name)
                    }
                    annotations.add(
                        PointAnnotationOptions()
                            .withPoint(Point.fromLngLat(stop.location.longitude, stop.location.latitude))
                            .withIconImage(stopBitmap)
                            .withIconSize(0.6)
                            .withTextField("${stop.name}${if(count > 0) "\n($count waiting)" else ""}\n${stop.scheduledTime}")
                            .withTextSize(10.0)
                            .withTextOffset(listOf(0.0, 2.2))
                            .withTextColor(if (count > 0) Color.RED else Color.DKGRAY)
                            .withData(data)
                    )
                }

                manager.create(annotations)
            }

            polylineAnnotationManager?.let { manager ->
                manager.deleteAll()
                if (routePoints.isNotEmpty()) {
                    manager.create(
                        PolylineAnnotationOptions()
                            .withPoints(routePoints)
                            .withLineColor("#276EF1")
                            .withLineWidth(5.0)
                            .withLineOpacity(0.8)
                            .withLineJoin(LineJoin.ROUND)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Creates a bitmap that looks like a simplified bus
 */
private fun createBusMarker(context: Context, strokeColor: Int, bodyColor: Int): Bitmap {
    val width = 120
    val height = 80
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Shadow
    paint.color = Color.parseColor("#40000000")
    canvas.drawRoundRect(10f, 10f, width.toFloat() - 6f, height.toFloat() - 6f, 15f, 15f, paint)

    // Main Body
    paint.color = bodyColor
    canvas.drawRoundRect(4f, 4f, width.toFloat() - 10f, height.toFloat() - 10f, 15f, 15f, paint)
    
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = strokeColor
    canvas.drawRoundRect(4f, 4f, width.toFloat() - 10f, height.toFloat() - 10f, 15f, 15f, paint)

    // Windows
    paint.style = Paint.Style.FILL
    paint.color = Color.parseColor("#ADD8E6") // Light Blue
    canvas.drawRect(15f, 15f, 45f, 45f, paint)
    canvas.drawRect(55f, 15f, 85f, 45f, paint)
    canvas.drawRect(95f, 15f, 105f, 45f, paint)

    // Wheels
    paint.color = strokeColor
    canvas.drawCircle(30f, height.toFloat() - 10f, 10f, paint)
    canvas.drawCircle(width.toFloat() - 40f, height.toFloat() - 10f, 10f, paint)

    return bitmap
}

private fun createModernMarker(context: Context, @ColorInt bgColor: Int, @ColorInt iconColor: Int, text: String): Bitmap {
    val size = 120
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.parseColor("#40000000")
    canvas.drawCircle(size / 2f, size / 2f + 4f, size / 2.2f, paint)
    paint.color = Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint)
    paint.color = bgColor
    canvas.drawCircle(size / 2f, size / 2f, size / 2.6f, paint)
    paint.color = iconColor
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = if (text.length > 2) 24f else 40f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val fontMetrics = paint.fontMetrics
    val y = (size / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(text, size / 2f, y, paint)
    return bitmap
}
