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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.graphics.Color

@Composable
fun HiveSelectionScreen(navController: NavController) {
    val hives = remember { mutableStateListOf<Colmeia>() }
    val db = Firebase.firestore
    val auth = Firebase.auth
    val userId = auth.currentUser?.uid

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("colmeia")
                .whereEqualTo("owner_id", userId) // Make sure this matches your Firestore structure
                .get()
                .addOnSuccessListener { result ->
                    hives.clear()
                    hives.addAll(result.map { doc ->
                        Colmeia(
                            nome = doc.getString("nome") ?: "Unknown",
                            imageRes = R.drawable.logo_beeconnect,
                            id = doc.id
                        )
                    })
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecionar Colmeia") },
                backgroundColor = Color(0xFFFFC107),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(hives) { hive ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            navController.navigate("statistics/${hive.id}")
                        },
                    elevation = 4.dp
                ) {
                    Text(
                        text = hive.nome,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
