package com.example.beeconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Visita(
    val data: String = "",
    val colmeiaNome: String = "",
    val apiarioId: String = ""
)

@Composable
fun CalendarScreen(navController: NavController) {
    val today = LocalDate.now()
    val currentMonth = remember { mutableStateOf(today.withDayOfMonth(1)) }
    val visitas = remember { mutableStateListOf<Visita>() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val apiarioNomes = remember { mutableStateMapOf<String, String>() }

    // Firebase
    LaunchedEffect(true) {
        val db = FirebaseFirestore.getInstance()
        visitas.clear()

        val colmeias = db.collection("colmeia").get().await()
        for (colmeia in colmeias.documents) {
            val colmeiaId = colmeia.id
            val colmeiaNome = colmeia.getString("nome") ?: continue
            val apiarioId = colmeia.getString("apiario") ?: continue

            if (!apiarioNomes.contains(apiarioId)) {
                val apiarioDoc = db.collection("apiarios").document(apiarioId).get().await()
                val apiarioNome = apiarioDoc.getString("nome") ?: "Desconhecido"
                apiarioNomes[apiarioId] = apiarioNome
            }

            val inspecoes = db.collection("colmeia").document(colmeiaId).collection("inspecoes").get().await()
            for (inspecao in inspecoes.documents) {
                val data = inspecao.getString("proxima_visita") ?: continue
                try {
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    val localDate = LocalDate.parse(data, formatter)
                    visitas.add(Visita(localDate.toString(), colmeiaNome, apiarioId))
                } catch (_: Exception) { }
            }
        }
    }

    val visitDates = visitas.mapNotNull {
        try {
            LocalDate.parse(it.data)
        } catch (_: Exception) { null }
    }

    Scaffold(
        topBar = { BeeConnectTopBar(navController) },
        bottomBar = { BeeConnectBottomNavigation(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = currentMonth.value.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + currentMonth.value.year,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CalendarGrid(
                month = currentMonth.value,
                visitDates = visitDates,
                onDateClick = { clickedDate -> selectedDate = clickedDate }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    currentMonth.value = currentMonth.value.minusMonths(1)
                }) {
                    Text("Anterior")
                }
                Button(onClick = {
                    currentMonth.value = currentMonth.value.plusMonths(1)
                }) {
                    Text("Próximo")
                }
            }

            selectedDate?.let { date ->
                val visitasDoDia = visitas.filter { it.data == date.toString() }
                if (visitasDoDia.isNotEmpty()) {
                    AlertDialog(
                        onDismissRequest = { selectedDate = null },
                        confirmButton = {
                            TextButton(onClick = { selectedDate = null }) {
                                Text("Fechar")
                            }
                        },
                        title = {
                            Text("Visitas em ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                        },
                        text = {
                            Column {
                                visitasDoDia.forEach {
                                    val apiarioNome = apiarioNomes[it.apiarioId] ?: "Desconhecido"
                                    Text("Apiário: $apiarioNome")
                                    Text("Colmeia: ${it.colmeiaNome}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    month: LocalDate,
    visitDates: List<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val days = remember(month) {
        val firstDayOfMonth = month.withDayOfMonth(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
        val daysInMonth = month.lengthOfMonth()
        val totalCells = firstDayOfWeek + daysInMonth

        List(totalCells) { index ->
            if (index < firstDayOfWeek) null
            else month.withDayOfMonth(index - firstDayOfWeek + 1)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(days.size) { index ->
            val date = days[index]
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(
                        when {
                            date == LocalDate.now() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            date in visitDates -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                    )
                    .clickable(enabled = date != null && date in visitDates) {
                        date?.let { onDateClick(it) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date?.dayOfMonth?.toString() ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (date in visitDates) MaterialTheme.colorScheme.secondary else Color.Unspecified
                )
            }
        }
    }
}
