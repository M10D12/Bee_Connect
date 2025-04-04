package com.example.beeconnect

import android.location.Geocoder
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@Composable
fun CreateApiaryScreen(navController: NavController) {
    var apiaryName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(TextFieldValue("")) }
    var selectedEnv by remember { mutableStateOf("Suburbano") }
    var latitude by remember { mutableStateOf("36.21367483") }
    var longitude by remember { mutableStateOf("-56.9846634") }
    var isMapExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = { BeeConnectTopBar(navController) },
        bottomBar = { BeeConnectBottomNavigation(navController) }
    ) { paddingValues ->
        if (isMapExpanded) {
            Box(modifier = Modifier.fillMaxSize()) {
                MapViewComposable(
                    latitude = latitude,
                    longitude = longitude,
                    onLocationChanged = { lat, lon ->
                        latitude = lat
                        longitude = lon
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Button(
                    onClick = { isMapExpanded = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
                ) {
                    Text("Concluir", color = Color.White)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text("Criação do Apiário", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = apiaryName,
                    onValueChange = { apiaryName = it },
                    label = { Text("Nome do Apiário") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Meio envolvente do apiário")
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val environments = listOf(
                        Triple("Rural", R.drawable.rural, selectedEnv == "Rural"),
                        Triple("Urbano", R.drawable.urbano, selectedEnv == "Urbano")
                    )

                    environments.forEach { (label, icon, isSelected) ->
                        EnvironmentOption(
                            label = label,
                            iconRes = icon,
                            selected = isSelected,
                            onClick = { selectedEnv = label }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Localização do apiário:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("Procura endereço...") },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFFC107), RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            try {
                                val results = geocoder.getFromLocationName(address.text, 1)
                                if (!results.isNullOrEmpty()) {
                                    val location = results[0]
                                    latitude = location.latitude.toString()
                                    longitude = location.longitude.toString()
                                    Toast.makeText(context, "Localização encontrada!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Endereço não encontrado", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Procurar", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    MapViewComposable(
                        latitude = latitude,
                        longitude = longitude,
                        onLocationChanged = { lat, lon ->
                            latitude = lat
                            longitude = lon
                        }
                    )

                    Button(
                        onClick = { isMapExpanded = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
                    ) {
                        Text("Maximizar", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val auth = Firebase.auth

                        if (apiaryName.isBlank() || address.text.isBlank() ||
                            latitude.toDoubleOrNull() == null || longitude.toDoubleOrNull() == null) {
                            Toast.makeText(context, "Preenche todos os campos corretamente.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val firestore = Firebase.firestore
                        val apiaryData = hashMapOf(
                            "nome" to apiaryName,
                            "localizacao" to address.text,
                            "meio" to selectedEnv,
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "owner_id" to auth.currentUser?.uid
                        )

                        firestore.collection("apiarios")
                            .add(apiaryData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Apiário criado com sucesso!", Toast.LENGTH_SHORT).show()
                                navController.navigate("home")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Erro ao criar apiário: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Criar Apiary", color = Color.White)
                }

            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun MapViewComposable(
    latitude: String,
    longitude: String,
    onLocationChanged: (String, String) -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .clip(RoundedCornerShape(12.dp))
        .pointerInteropFilter { event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> false
                else -> false
            }
        }
) {
    AndroidView(
        factory = { ctx ->
            val appContext = ctx.applicationContext
            val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext)
            Configuration.getInstance().load(appContext, sharedPrefs)

            MapView(appContext).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
            }
        },
        modifier = modifier,
        update = { mapView ->
            val lat = latitude.toDoubleOrNull()
            val lon = longitude.toDoubleOrNull()
            if (lat != null && lon != null) {
                val point = GeoPoint(lat, lon)
                mapView.controller.setCenter(point)
                mapView.overlays.clear()

                val marker = Marker(mapView).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Apiário"
                    isDraggable = true

                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDragStart(marker: Marker?) {}
                        override fun onMarkerDrag(marker: Marker?) {}
                        override fun onMarkerDragEnd(marker: Marker?) {
                            marker?.position?.let {
                                onLocationChanged(it.latitude.toString(), it.longitude.toString())
                            }
                            mapView.invalidate()
                        }
                    })
                }

                mapView.overlays.add(marker)
                mapView.invalidate()
            }
        }
    )
}

@Composable
fun EnvironmentOption(label: String, iconRes: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0xFFE0F7FA) else Color(0xFFF0F0F0))
                .padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(64.dp)
            )
        }
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
