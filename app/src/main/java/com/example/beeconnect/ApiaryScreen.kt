package com.example.beeconnect

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ApiaryScreen(navController: NavController, apiaryId: String) {
    val context = LocalContext.current
    val colmeias = remember { mutableStateListOf<Colmeia>() }
    var apiaryName by remember { mutableStateOf("...") }
    var temperature by remember { mutableStateOf("--") }
    var weatherInfo by remember { mutableStateOf("A obter...") }
    var forecast by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(apiaryId) {
        val db = Firebase.firestore

        db.collection("apiarios").document(apiaryId)
            .get()
            .addOnSuccessListener { document ->
                apiaryName = document.getString("nome") ?: "Sem nome"
                val latitude = document.getString("latitude")?.toDoubleOrNull()
                val longitude = document.getString("longitude")?.toDoubleOrNull()

                if (latitude != null && longitude != null) {
                    fetchWeather(latitude, longitude) { temp, wind, hum ->
                        temperature = "$temp°"
                        weatherInfo = "Vento ${wind}km/h  Hum ${hum}%"
                    }

                    fetchForecast(latitude, longitude) {
                        forecast = it
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao buscar apiário", Toast.LENGTH_SHORT).show()
            }

        db.collection("colmeia")
            .whereEqualTo("apiario", apiaryId)
            .get()
            .addOnSuccessListener { result ->
                colmeias.clear()
                for (document in result) {
                    val nome = document.getString("nome") ?: "Sem nome"
                    val imageRes = R.drawable.apiario
                    val id = document.id
                    colmeias.add(Colmeia(nome, imageRes, id))
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao buscar colmeias", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = { BeeConnectTopBar(navController) },
        bottomBar = { BeeConnectBottomNavigation(navController) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Apiário $apiaryName", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                RoundedBlackButton(text = "+ Colmeia") {
                    navController.navigate("createHive/${apiaryId}")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color(0xFFe0f7fa),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val dateStr = SimpleDateFormat("dd MMMM", Locale("pt", "PT")).format(Date())
                    Text("Hoje, $dateStr", fontSize = 14.sp)
                    Text(temperature, fontSize = 32.sp)
                    Text(weatherInfo, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray, thickness = 1.dp)

                    Text(
                        "☀️ Previsão para os próximos dias",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(forecast.size) { index ->
                            ForecastCard(forecast[index])
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(contentPadding = PaddingValues(bottom = 70.dp)) {
                items(colmeias.size) { index ->
                    ColmeiaCard(
                        colmeia = colmeias[index],
                        navController = navController,
                        apiaryId = apiaryId
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

fun fetchWeather(lat: Double, lon: Double, callback: (Int, Int, Int) -> Unit) {
    val apiKey = "f907eff41b9ba822e28fcdf74b6c537c"
    val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=$apiKey&lang=pt"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    Thread {
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            val json = JSONObject(responseBody ?: "")

            if (json.has("main") && json.has("wind")) {
                val main = json.getJSONObject("main")
                val windObj = json.getJSONObject("wind")

                val temp = main.getDouble("temp").toInt()
                val hum = main.getInt("humidity")
                val wind = windObj.getDouble("speed").toInt()

                callback(temp, wind, hum)
            }
        } catch (e: Exception) {
            Log.e("API_WEATHER", "Erro: ${e.message}")
        }
    }.start()
}

fun fetchForecast(lat: Double, lon: Double, callback: (List<String>) -> Unit) {
    val apiKey = "f907eff41b9ba822e28fcdf74b6c537c"
    val url = "https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&units=metric&appid=$apiKey&lang=pt"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    Thread {
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val json = JSONObject(responseBody ?: "")
            val list = json.getJSONArray("list")

            val result = mutableListOf<String>()
            val formatterIn = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formatterOut = SimpleDateFormat("EEEE, dd/MM", Locale("pt", "PT"))
            val seenDays = mutableSetOf<String>()

            for (i in 0 until list.length()) {
                val entry = list.getJSONObject(i)
                val dtTxt = entry.getString("dt_txt")
                val date = formatterIn.parse(dtTxt)
                val dayKey = formatterOut.format(date ?: continue)

                if (dtTxt.contains("12:00:00") && !seenDays.contains(dayKey)) {
                    val main = entry.getJSONObject("main")
                    val weatherArray = entry.getJSONArray("weather")
                    val description = weatherArray.getJSONObject(0).getString("description")
                    val temp = main.getDouble("temp").toInt()

                    result.add("$dayKey: $temp°C, $description")
                    seenDays.add(dayKey)

                    if (result.size == 5) break
                }
            }

            callback(result)
        } catch (e: Exception) {
            Log.e("API_FORECAST", "Erro: ${e.message}")
        }
    }.start()
}

@Composable
fun ForecastCard(dayForecast: String) {
    val parts = dayForecast.split(":")
    val day = parts.getOrNull(0)?.trim() ?: ""
    val rest = parts.getOrNull(1)?.trim() ?: ""

    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFFE1F5FE),
        elevation = 2.dp,
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(day, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(rest, fontSize = 12.sp, maxLines = 2)
        }
    }
}

@Composable
fun ColmeiaCard(colmeia: Colmeia, navController: NavController, apiaryId: String) {
    val context = LocalContext.current
    val db = Firebase.firestore
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar exclusão") },
            text = { Text("Tem certeza que deseja excluir esta colmeia?") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("colmeia").document(colmeia.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Colmeia excluída com sucesso",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Erro ao excluir colmeia: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        backgroundColor = Color(0xFFFFF3E0),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Image(
                painter = painterResource(id = colmeia.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(colmeia.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        // Navigate to edit screen (you'll need to create this)
                        navController.navigate("editHive/${colmeia.id}")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color.Black
                    )
                }

                Button(
                    onClick = { navController.navigate("colmeiaScreen/${colmeia.id}") },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.width(120.dp)
                ) {
                    Text("Ver mais", color = Color.White)
                }

                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

data class Colmeia(val nome: String, val imageRes: Int, val id: String)

@Preview(showBackground = true)
@Composable
fun PreviewApiaryScreen() {
    MaterialTheme {
        ApiaryScreen(navController = rememberNavController(), apiaryId = "apiary123")
    }
}