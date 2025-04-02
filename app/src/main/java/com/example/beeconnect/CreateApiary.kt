package com.example.beeconnect

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun CreateApiaryScreen(navController: NavController) {
    var apiaryName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(TextFieldValue("")) }
    var selectedEnv by remember { mutableStateOf("Suburbano") }
    var latitude by remember { mutableStateOf("36.21367483") }
    var longitude by remember { mutableStateOf("-56.9846634") }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = { BeeConnectTopBar() },
        bottomBar = { BeeConnectBottomNavigation() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Criação do Apiário", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Nome do Apiário")
            OutlinedTextField(
                value = apiaryName,
                onValueChange = { apiaryName = it },
                placeholder = { Text("Nome do apiário") },
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
            TextField(
                value = address,
                onValueChange = { address = it },
                placeholder = { Text("Procura endereço...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFC107), RoundedCornerShape(8.dp)),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

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
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFFC107),
                        unfocusedBorderColor = Color(0xFFFFC107)
                    )
                )

                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFFC107),
                        unfocusedBorderColor = Color(0xFFFFC107)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        try {
                            val appContext = ctx.applicationContext
                            val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext)
                            Configuration.getInstance().load(appContext, sharedPrefs)

                            // Cria o MapView corretamente
                            MapView(appContext).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(15.0)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Em caso de falha, devolve uma View vazia (evita crash)
                            View(ctx)
                        }
                    },
                    update = { mapView ->
                        if (mapView is MapView) {
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
                                }
                                mapView.overlays.add(marker)
                                mapView.invalidate()
                            }
                        }
                    }
                )


            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val auth = Firebase.auth
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
                            Toast.makeText(
                                context,
                                "Apiário criado com sucesso!",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.navigate("home")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "Erro ao criar apiário: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
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

@Composable
fun EnvironmentOption(label: String, iconRes: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
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
            modifier = Modifier.padding(top = 4.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCreateApiaryScreen() {
    MaterialTheme {
        CreateApiaryScreen(navController = rememberNavController())
    }
}