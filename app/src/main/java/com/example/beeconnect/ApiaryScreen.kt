package com.example.beeconnect

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*

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

@Composable
fun ApiaryScreen(navController: NavController, apiaryId: String) {
    val context = LocalContext.current
    val colmeias = remember { mutableStateListOf<Colmeia>() }
    var apiaryName by remember { mutableStateOf("...") }

    LaunchedEffect(apiaryId) {
        val db = Firebase.firestore

        db.collection("apiarios").document(apiaryId)
            .get()
            .addOnSuccessListener { document ->
                apiaryName = document.getString("nome") ?: "Sem nome"
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }

        db.collection("colmeia")
            .whereEqualTo("apiario", apiaryId)
            .get()
            .addOnSuccessListener { result ->
                colmeias.clear()
                for (document in result) {
                    val nome = document.getString("nome") ?: "Sem nome"
                    val imageRes = R.drawable.apiario
                    colmeias.add(Colmeia(nome, imageRes))
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = { BeeConnectTopBar() },
        bottomBar = { BeeConnectBottomNavigation() },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
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
                    Text("Today, 12 September", fontSize = 14.sp)
                    Text("29°", fontSize = 32.sp)
                    Text("Wind 10km/h  Hum 54%", fontSize = 14.sp)
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
                onClick = { /* ação Ver mais */ },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black),
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Ver mais", color = Color.White)
            }
        }
    }
}

data class Colmeia(val nome: String, val imageRes: Int)

@Preview(showBackground = true)
@Composable
fun PreviewApiaryScreen() {
    MaterialTheme {
        ApiaryScreen(navController = rememberNavController(), apiaryId = "apiary123")
    }
}