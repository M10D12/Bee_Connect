package com.example.beeconnect

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import com.example.beeconnect.models.Apiary
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.navigation.NavController

@Composable
fun MyApiariesMapScreen(navController: NavController) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var apiaries by remember { mutableStateOf(listOf<Apiary>()) }

    LaunchedEffect(Unit) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            Firebase.firestore.collection("apiarios")
                .whereEqualTo("owner_id", user.uid)
                .get()
                .addOnSuccessListener { documents ->
                    apiaries = documents.mapNotNull { doc ->
                        val name = doc.getString("nome") ?: return@mapNotNull null
                        val lat = doc.getString("latitude") ?: return@mapNotNull null
                        val lon = doc.getString("longitude") ?: return@mapNotNull null
                        val location = doc.getString("localizacao") ?: ""
                        Apiary(
                            name = name,
                            location = location,
                            latitude = lat,
                            longitude = lon,
                            imageRes = null,
                            id = doc.id
                        )
                    }
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Erro ao buscar apiários: ${it.message}")
                }
        }
    }

    Scaffold(
        topBar = { BeeConnectTopBar() },
        bottomBar = { BeeConnectBottomNavigation(navController) }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            AndroidView(
                factory = { ctx ->
                    val appContext = ctx.applicationContext
                    val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext)
                    Configuration.getInstance().load(appContext, sharedPrefs)

                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(5.0)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    if (apiaries.isNotEmpty()) {
                        map.overlays.clear()

                        val geoPoints = apiaries.map {
                            GeoPoint(it.latitude.toDouble(), it.longitude.toDouble())
                        }

                        geoPoints.forEachIndexed { index, point ->
                            val marker = Marker(map).apply {
                                position = point
                                title = apiaries[index].name
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            map.overlays.add(marker)
                        }

                        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPointsSafe(geoPoints)

                        map.zoomToBoundingBox(boundingBox, false) // <– false para aplicar zoom depois
                        map.controller.setCenter(boundingBox.centerWithDateLine)
                        map.controller.setZoom(map.zoomLevelDouble - 1.5)

                        map.invalidate()
                    }
                }


            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyApiariesMapScreen() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Preview não disponível para MapView",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
