package com.example.beeconnect

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun CreateHiveScreen(navController: NavController, apiaryId: String) {
    val context = LocalContext.current

    var hiveName by remember { mutableStateOf("Example hive name") }
    var selectedType by remember { mutableStateOf("Select an item") }

    val typeOptions = listOf("Langstroth", "Lusitano", "Reversível", "Industrial (dadant)")

    Scaffold(
        topBar = { BeeConnectTopBar() },
        bottomBar = { BeeConnectBottomNavigation() }

    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Criação da Colmeia", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Welcome, UserX!", fontSize = 14.sp)

            // Nome da Colmeia
            Text("Nome da Colmeia:")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFFFC107), RoundedCornerShape(12.dp))
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(hiveName, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    // Aqui poderias abrir um dialog para editar, por simplicidade fizemos diretamente
                    hiveName = "Nova colmeia"
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit hive name")
                }
            }

            // Dropdown para tipo de colmeia
            DropdownSelector("Selecione o tipo de colmeia:", typeOptions, selectedType) { selectedType = it }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val firestore = Firebase.firestore
                    val auth = Firebase.auth
                    val hiveData = hashMapOf(
                        "nome" to hiveName,
                        "tipo" to selectedType,
                        "apiario" to apiaryId,
                        "owner_id" to auth.currentUser?.uid
                    )

                    firestore.collection("colmeia")
                        .add(hiveData)
                        .addOnSuccessListener { docRef ->
                            // Atualiza o apiário para incluir a colmeia criada
                            firestore.collection("apiarios").document(apiaryId)
                                .update("colmeias", FieldValue.arrayUnion(docRef.id))
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Colmeia criada com sucesso!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Erro ao atualizar apiário: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Erro ao criar colmeia: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
            ) {
                Text("Next", color = Color.White)
            }
        }
    }
}

@Composable
fun DropdownSelector(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label)
        Box(
            modifier = Modifier
                .background(Color(0xFFFFC107), RoundedCornerShape(12.dp))
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Text(selected)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onSelect(option)
                    expanded = false
                }) {
                    Text(option)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCreateHiveScreen() {
    MaterialTheme {
        CreateHiveScreen(navController = rememberNavController(), apiaryId = "apiary123")
    }
}