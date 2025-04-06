package com.example.beeconnect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.Alignment

@Composable
fun ApiarySelectionScreen(navController: NavController) {
    val apiaries = remember { mutableStateListOf<Apiary>() }
    val db = Firebase.firestore
    val auth = Firebase.auth
    val userId = auth.currentUser?.uid
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("apiarios")
                .whereEqualTo("owner_id", userId)
                .get()
                .addOnSuccessListener { result ->
                    apiaries.clear()
                    result.forEach { doc ->
                        apiaries.add(
                            Apiary(
                                name = doc.getString("nome") ?: "Unknown",
                                location = doc.getString("localizacao") ?: "",
                                latitude = doc.getString("latitude") ?: "",
                                longitude = doc.getString("longitude") ?: "",
                                id = doc.id
                            )
                        )
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    errorMessage = "Failed to load apiaries: ${e.message}"
                    isLoading = false
                }
        } else {
            errorMessage = "User not authenticated"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecionar Apiário") },
                backgroundColor = Color(0xFFFFC107),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage ?: "Error", color = Color.Red)
                }
            }
            apiaries.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ainda não tens nenhum apiário registado 🐝",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(apiaries) { apiary ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable {
                                    navController.navigate("statistics/${apiary.id}")
                                },
                            elevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = apiary.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(text = apiary.location, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Apiary(
    val name: String,
    val location: String,
    val latitude: String,
    val longitude: String,
    val id: String
)