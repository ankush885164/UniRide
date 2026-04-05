package com.example.unifront.data

import org.osmdroid.util.GeoPoint

data class BusStop(val name: String, val location: GeoPoint, val scheduledTime: String = "")
data class BusRoute(val id: String, val name: String, val stops: List<BusStop>, val universityArrivalTime: String = "9:00 AM")

object BusData {
    val UNIVERSITY_LOCATION = GeoPoint(28.271817976694052, 77.06967209900854) // K.R. Mangalam University, Sohna
    
    val ROUTES = listOf(
        BusRoute("BH1", "BH1 - Uni", listOf(
            BusStop("BH1 Central", GeoPoint(28.2015, 76.8218), "7:05 AM"),
            BusStop("Vatika Stop", GeoPoint(28.3900, 77.0600), "7:25 AM"),
            BusStop("Sohna Cross", GeoPoint(28.2450, 77.0500), "7:45 AM"),
            BusStop("CD Chowk", GeoPoint(28.4120, 77.0420), "8:05 AM")
        )),
        BusRoute("BH2", "BH2 - Uni", listOf(
            BusStop("Sec 1 Point", GeoPoint(28.174540, 76.824739), "7:15 AM"),
            BusStop("Phase II Ent", GeoPoint(28.192850, 76.813914), "7:28 AM"),
            BusStop("Bhw Junction", GeoPoint(28.201515, 76.821858), "7:36 AM")
        )),
        BusRoute("DW3", "DW3 - Uni", listOf(
            BusStop("DW Sec 10", GeoPoint(28.5810, 77.0590), "7:10 AM"),
            BusStop("DW Sec 21", GeoPoint(28.5520, 77.0200), "7:22 AM"),
            BusStop("DW Sec 12", GeoPoint(28.5921, 77.0460), "7:35 AM"),
            BusStop("Palam Fly", GeoPoint(28.5845, 77.0832), "7:48 AM"),
            BusStop("Subhash Chowk", GeoPoint(28.4100, 77.0400), "8:10 AM"),
            BusStop("HH Chowk", GeoPoint(28.4200, 77.0100), "8:25 AM")
        )),
        BusRoute("G8", "G8 - Uni", listOf(
            BusStop("IFFCO Stop", GeoPoint(28.4700, 77.0700), "7:15 AM"),
            BusStop("Sig Tower", GeoPoint(28.4600, 77.0600), "7:28 AM"),
            BusStop("GDG Cross", GeoPoint(28.2250, 77.0650), "7:50 AM"),
            BusStop("Rajiv Chowk", GeoPoint(28.4500, 77.0300), "8:15 AM")
        ))
    )
}
