// Imports permanecem os mesmos
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.beeconnect.BeeConnectBottomNavigation
import com.example.beeconnect.BeeConnectTopBar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColmeiaScreen(navController: NavController, colmeiaId: String) {
    val context = LocalContext.current
    val db = Firebase.firestore

    var nome by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }

    var dataInspecao by remember { mutableStateOf("") }
    var alimentacao by remember { mutableStateOf("") }
    var tratamentos by remember { mutableStateOf("") }
    var problemas by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var proximaVisita by remember { mutableStateOf("") }

    val historico = remember { mutableStateListOf<Map<String, String>>() }
    var showInspecaoForm by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            dataInspecao = "%02d/%02d/%04d".format(day, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val visitaDateTimePicker = {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, hour, minute ->
                proximaVisita = "%02d/%02d/%04d %02d:%02d".format(d, m + 1, y, hour, minute)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    var expanded by remember { mutableStateOf(false) }
    val typeOptions = listOf("Langstroth", "Lusitano", "Reversível", "Industrial (dadant)")

    // Load data
    LaunchedEffect(colmeiaId) {
        try {
            val doc = db.collection("colmeia").document(colmeiaId).get().await()
            nome = doc.getString("nome") ?: ""
            tipo = doc.getString("tipo") ?: ""
            descricao = doc.getString("descricao") ?: ""

            val result = db.collection("colmeia").document(colmeiaId)
                .collection("inspecoes")
                .orderBy("data")
                .get()
                .await()

            historico.clear()
            for (inspecao in result) {
                historico.add(inspecao.data.mapValues { it.value.toString() })
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = { BeeConnectTopBar() },
        bottomBar = { BeeConnectBottomNavigation(navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Gestão da Colmeia", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())

                Text("Tipo da Colmeia")

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selecionar tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        typeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    tipo = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        db.collection("colmeia").document(colmeiaId).update(
                            mapOf(
                                "nome" to nome,
                                "tipo" to tipo,
                                "descricao" to descricao
                            )
                        ).addOnSuccessListener {
                            Toast.makeText(context, "Informações atualizadas!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Salvar", color = Color.White)
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Histórico de Inspeções", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    TextButton(onClick = { showInspecaoForm = !showInspecaoForm }) {
                        Text(if (showInspecaoForm) "Fechar" else "+ Nova Inspeção")
                    }
                }

                if (showInspecaoForm) {
                    OutlinedTextField(
                        value = dataInspecao,
                        onValueChange = {},
                        label = { Text("Data da Inspeção") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
                        enabled = false
                    )

                    OutlinedTextField(value = alimentacao, onValueChange = { alimentacao = it }, label = { Text("Alimentação") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tratamentos, onValueChange = { tratamentos = it }, label = { Text("Tratamentos") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = problemas, onValueChange = { problemas = it }, label = { Text("Problemas / Doenças") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = observacoes, onValueChange = { observacoes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(
                        value = proximaVisita,
                        onValueChange = {},
                        label = { Text("Programar próxima visita (opcional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { visitaDateTimePicker() },
                        enabled = false
                    )

                    Button(
                        onClick = {
                            val inspecao = mutableMapOf(
                                "data" to dataInspecao,
                                "alimentacao" to alimentacao,
                                "tratamentos" to tratamentos,
                                "problemas" to problemas,
                                "observacoes" to observacoes
                            )

                            if (proximaVisita.isNotBlank()) {
                                inspecao["proxima_visita"] = proximaVisita
                            }

                            db.collection("colmeia").document(colmeiaId)
                                .collection("inspecoes")
                                .add(inspecao)
                                .addOnSuccessListener {
                                    historico.add(0, inspecao)
                                    dataInspecao = ""
                                    alimentacao = ""
                                    tratamentos = ""
                                    problemas = ""
                                    observacoes = ""
                                    proximaVisita = ""
                                    showInspecaoForm = false
                                    Toast.makeText(context, "Inspeção adicionada!", Toast.LENGTH_SHORT).show()
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text("Adicionar Inspeção", color = Color.White)
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            items(historico.size) { i ->
                val inspecao = historico[i]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📅 ${inspecao["data"]}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("🍯 Alimentação: ${inspecao["alimentacao"]}")
                        Text("🧪 Tratamentos: ${inspecao["tratamentos"]}")
                        Text("🐝 Problemas: ${inspecao["problemas"]}")
                        Text("📝 Observações: ${inspecao["observacoes"]}")
                        inspecao["proxima_visita"]?.let {
                            Text("📌 Próxima visita: $it", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
