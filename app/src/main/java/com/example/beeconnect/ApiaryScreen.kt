package com.example.beeconnect

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            )

            {
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
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(contentPadding = PaddingValues(bottom = 70.dp)) {
                items(colmeias.size) { index ->
                    ColmeiaCard(colmeia = colmeias[index], navController)
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

            Log.d("API_WEATHER", "Resposta completa: $responseBody")

            val json = JSONObject(responseBody ?: "")

            if (json.has("main") && json.has("wind")) {
                val main = json.getJSONObject("main")
                val windObj = json.getJSONObject("wind")

                val temp = main.getDouble("temp").toInt()
                val hum = main.getInt("humidity")
                val wind = windObj.getDouble("speed").toInt()

                callback(temp, wind, hum)
            } else {
                Log.e("API_WEATHER", "Erro: Campo 'main' ou 'wind' não existe na resposta")
            }
        } catch (e: Exception) {
            Log.e("API_WEATHER", "Erro ao processar resposta da API: ${e.message}")
        }
    }.start()
}

@Composable
fun ColmeiaCard(colmeia: Colmeia, navController: NavController) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp,
        backgroundColor = Color(0xFFFFECB3),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(
                painter = painterResource(id = colmeia.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = colmeia.nome, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {navController.navigate("colmeiaScreen/${colmeia.id}")  },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black),
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Ver mais", color = Color.White)
            }
        }
    }
}

data class Colmeia(
    val nome: String,
    val imageRes: Int,
    val id: String
)

@Preview(showBackground = true)
@Composable
fun PreviewApiaryScreen() {
    MaterialTheme {
        ApiaryScreen(navController = rememberNavController(), apiaryId = "apiary123")
    }
}