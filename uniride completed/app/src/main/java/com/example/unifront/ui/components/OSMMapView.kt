package com.example.unifront.ui.components

import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapController(val mapView: MapView) {
    fun recenter(point: GeoPoint, zoomLevel: Double = 17.0) {
        mapView.controller.animateTo(point, zoomLevel, 1000L)
    }
    
    fun addMarker(point: GeoPoint, title: String, snippet: String = "", id: String = "", icon: Drawable? = null) {
        // Find existing marker with same ID
        val existingMarker = mapView.overlays.filterIsInstance<Marker>().find { it.id == id }
        if (existingMarker != null) {
            existingMarker.position = point
            existingMarker.title = title
            existingMarker.snippet = snippet
            if (icon != null) existingMarker.icon = icon
        } else {
            val marker = Marker(mapView).apply {
                this.id = id
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                this.title = title
                this.snippet = snippet
                if (icon != null) this.icon = icon
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    fun removeMarker(id: String) {
        mapView.overlays.filterIsInstance<Marker>().filter { it.id == id }.forEach { 
            mapView.overlays.remove(it) 
        }
        mapView.invalidate()
    }

    fun drawRoute(points: List<GeoPoint>, color: Int = android.graphics.Color.parseColor("#6200EE"), id: String = "route") {
        mapView.overlays.filterIsInstance<Polyline>().filter { it.id == id }.forEach { 
            mapView.overlays.remove(it) 
        }
        
        if (points.size >= 2) {
            val polyline = Polyline(mapView).apply {
                this.id = id
                setPoints(points)
                outlinePaint.color = color
                outlinePaint.strokeWidth = 12f
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            }
            mapView.overlays.add(polyline)
        }
        mapView.invalidate()
    }

    fun fitMarkers(points: List<GeoPoint>, padding: Int = 100) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            recenter(points[0])
            return
        }

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        for (point in points) {
            minLat = minOf(minLat, point.latitude)
            maxLat = maxOf(maxLat, point.latitude)
            minLon = minOf(minLon, point.longitude)
            maxLon = maxOf(maxLon, point.longitude)
        }

        val boundingBox = BoundingBox(maxLat, maxLon, minLat, minLon)
        mapView.zoomToBoundingBox(boundingBox, true, padding)
    }
}

@Composable
fun OSMMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(28.2393, 77.0658),
    zoom: Double = 16.0,
    showMyLocation: Boolean = true,
    onMapReady: (MapController) -> Unit = {}
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        Configuration.getInstance().load(context, prefs)
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(zoom)
            controller.setCenter(center)
            
            overlays.add(ScaleBarOverlay(this).apply { setCentred(true) })
            overlays.add(RotationGestureOverlay(this).apply { isEnabled = true })
            
            if (showMyLocation) {
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                locationOverlay.enableMyLocation()
                // Use a custom icon for my location if needed, but MyLocationNewOverlay handles it
                overlays.add(locationOverlay)
            }
        }
    }

    val mapController = remember(mapView) { MapController(mapView) }

    DisposableEffect(Unit) {
        onMapReady(mapController)
        onDispose { mapView.onDetach() }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
