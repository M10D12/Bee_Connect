package com.example.beeconnect

import android.annotation.SuppressLint
import android.util.Log
import android.widget.DatePicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
@Composable
fun StatisticsScreen(navController: NavController, apiaryId: String) {
    val db = Firebase.firestore
    val context = LocalContext.current

    // State variables
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var harvests by remember { mutableStateOf<List<HoneyHarvest>>(emptyList()) }
    var apiaryName by remember { mutableStateOf("") }
    var newHarvestAmount by remember { mutableStateOf("") }

    // Format date for display
    val formattedDate = remember(selectedDate) {
        SimpleDateFormat("yyyy-MM-dd").format(selectedDate)
    }

    // Load data
    LaunchedEffect(apiaryId) {
        val user = Firebase.auth.currentUser
        if (user == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }
        try {
            // 1. Get apiary info
            db.collection("apiarios").document(apiaryId)
                .get()
                .addOnSuccessListener { doc ->
                    apiaryName = doc.getString("nome") ?: "Unknown"

                    // 2. Get harvests for this apiary
                    db.collection("honey_harvests")
                        .whereEqualTo("apiaryId", apiaryId)
                        .get()
                        .addOnSuccessListener { result ->
                            harvests = result.map { doc ->
                                HoneyHarvest(
                                    apiaryId = doc.getString("apiaryId") ?: apiaryId,
                                    apiaryName = apiaryName,
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    date = doc.getTimestamp("date") ?: Timestamp.now()
                                )
                            }
                            isLoading = false
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Failed to load harvests: ${e.message}"
                            isLoading = false
                        }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Failed to load apiary: ${e.message}"
                    isLoading = false
                }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estatísticas de $apiaryName") },
                backgroundColor = Color(0xFFFFC107),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage ?: "Error", color = Color.Red)
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Voltar")
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    // Date and Add Harvest Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Data: $formattedDate", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.width(120.dp)
                        ) {
                            Text("Alterar Data")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Add Harvest Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newHarvestAmount,
                            onValueChange = { newHarvestAmount = it },
                            label = { Text("Quantidade (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val amount = newHarvestAmount.toDoubleOrNull() ?: 0.0
                                if (amount > 0) {
                                    val newHarvest = HoneyHarvest(
                                        apiaryId = apiaryId,
                                        apiaryName = apiaryName,
                                        amount = amount,
                                        date = Timestamp(selectedDate)
                                    )

                                    // Update local state
                                    harvests = harvests + newHarvest
                                    newHarvestAmount = ""

                                    // Add to Firestore
                                    db.collection("honey_harvests").add(
                                        mapOf(
                                            "apiaryId" to apiaryId,
                                            "apiaryName" to apiaryName,
                                            "amount" to amount,
                                            "date" to Timestamp(selectedDate)
                                        )
                                    )
                                }
                            }
                        ) {
                            Text("Adicionar")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Chart
                    Text("Produção de Mel por dia", fontWeight = FontWeight.Bold)
                    HoneyProductionChart(harvests = harvests)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Harvest List
                    Text("Registros de Colheita:", fontWeight = FontWeight.Bold)
                    LazyColumn {
                        items(harvests.sortedByDescending { it.date.toDate() }) { harvest ->
                            HarvestItem(harvest = harvest)
                            Divider()
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                selectedDate = calendar.time
                showDatePicker = false
            },
            initialDate = selectedDate
        )
    }
}

@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,
    initialDate: Date
) {
    val calendar = remember { Calendar.getInstance().apply { time = initialDate } }
    var tempYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var tempMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var tempDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancelar")
                }
                TextButton(onClick = {
                    onDateSelected(tempYear, tempMonth, tempDay)
                }) {
                    Text("OK")
                }
            }
        },
        text = {
            AndroidView(
                factory = { context ->
                    DatePicker(context).apply {
                        init(tempYear, tempMonth, tempDay) { _, y, m, d ->
                            tempYear = y
                            tempMonth = m
                            tempDay = d
                        }
                    }
                }
            )
        }
    )
}

@Composable
fun HarvestItem(harvest: HoneyHarvest) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(harvest.apiaryName, fontWeight = FontWeight.Bold)
            Text(
                SimpleDateFormat("dd/MM/yyyy").format(harvest.date.toDate()),
                fontSize = 12.sp
            )
        }
        Text("${harvest.amount} kg", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HoneyProductionChart(harvests: List<HoneyHarvest>) {
    val chartData by remember(harvests) {
        derivedStateOf {
            harvests
                .groupBy { harvest ->
                    SimpleDateFormat("dd/MM", Locale.getDefault())
                        .format(harvest.date.toDate())
                }
                .mapValues { (_, dailyHarvests) ->
                    dailyHarvests.sumOf { it.amount }
                }
                .toList()
                .sortedBy { (date, _) -> date }
        }
    }

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
            }
        },
        update = { chart ->
            val entries = chartData.mapIndexed { index, (_, amount) ->
                Entry(index.toFloat(), amount.toFloat())
            }

            val dataSet = LineDataSet(entries, "Mel (kg)").apply {
                color = Color(0xFFFFA000).toArgb()
                valueTextColor = Color.Black.toArgb()
                lineWidth = 2f
                setCircleColor(Color(0xFFFFC107).toArgb())
                circleRadius = 4f
            }

            chart.data = LineData(dataSet)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(chartData.map { it.first })
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

data class HoneyHarvest(
    val apiaryId: String,
    val apiaryName: String,
    val amount: Double,
    val date: Timestamp
)