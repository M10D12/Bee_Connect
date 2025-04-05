package com.example.beeconnect

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.util.*

@Composable
fun CreateHiveScreen(navController: NavController, apiaryId: String) {
    val context = LocalContext.current

    var hiveName by remember { mutableStateOf("") }
    var creationDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Select an item") }

    val typeOptions = listOf("Langstroth", "Lusitano", "Reversível", "Industrial (dadant)")

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                creationDate = "%02d/%02d/%04d".format(selectedDay, selectedMonth + 1, selectedYear)
            },
            year, month, day
        )
    }

    Scaffold(
        topBar = { BeeConnectTopBar(navController) },
        bottomBar = { BeeConnectBottomNavigation(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.Black
                )
            }
            Text("Criação da Colmeia", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            // Nome da colmeia (editável)
            OutlinedTextField(
                value = hiveName,
                onValueChange = { hiveName = it },
                label = { Text("Nome da Colmeia") },
                modifier = Modifier.fillMaxWidth()
            )

            // Tipo (dropdown)
            DropdownSelector("Tipo da colmeia", typeOptions, selectedType) { selectedType = it }

            // Data de criação (Date Picker)
            Text("Data de Criação")
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFC107), RoundedCornerShape(12.dp))
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
                    .padding(12.dp)
            ) {
                Text(text = if (creationDate.isEmpty()) "Selecionar data" else creationDate)
            }

            // Descrição
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val firestore = Firebase.firestore
                    val auth = Firebase.auth
                    val hiveData = hashMapOf(
                        "nome" to hiveName,
                        "tipo" to selectedType,
                        "apiario" to apiaryId,
                        "data_criacao" to creationDate,
                        "descricao" to description,
                        "owner_id" to auth.currentUser?.uid
                    )

                    firestore.collection("colmeia")
                        .add(hiveData)
                        .addOnSuccessListener { docRef ->
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