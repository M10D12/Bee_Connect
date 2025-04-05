package com.example.beeconnect


import android.os.Bundle
import android.util.Base64
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.beeconnect.models.Apiary
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import androidx.compose.material3.Scaffold
import androidx.compose.material.AlertDialog
import androidx.compose.material.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        FirebaseApp.initializeApp(this)
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "login") {
                composable("home") {
                    BeeConnectApp(navController)
                }
                composable(
                    route = "createApiary?apiaryId={apiaryId}",
                    arguments = listOf(
                        navArgument("apiaryId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val apiaryId = backStackEntry.arguments?.getString("apiaryId")
                    CreateApiaryScreen(navController, apiaryId)
                }

                composable("editHive/{colmeiaId}") { backStackEntry ->
                    val colmeiaId = backStackEntry.arguments?.getString("colmeiaId") ?: ""
                    EditHiveScreen(navController, colmeiaId)
                }

                composable("login") {
                    LoginScreen(navController)
                }
                composable("register") {
                    RegisterScreen(navController)
                }
                composable("createHive/{apiaryId}") { backStackEntry ->
                    val apiaryId = backStackEntry.arguments?.getString("apiaryId") ?: ""
                    CreateHiveScreen(navController, apiaryId)
                }
                composable("apiaryScreen/{apiaryId}") { backStackEntry ->
                    val apiaryId = backStackEntry.arguments?.getString("apiaryId") ?: ""
                    ApiaryScreen(navController, apiaryId)
                }

                composable("my_apiaries_map") {
                    MyApiariesMapScreen(navController)
                }
                composable("colmeiaScreen/{colmeiaId}") { backStackEntry ->
                    val colmeiaId = backStackEntry.arguments?.getString("colmeiaId") ?: ""
                    ColmeiaScreen(navController, colmeiaId)
                }
                composable("profile"){
                    RealProfileScreen(navController = navController)
                }
            }
        }
    }
}

@Composable
fun BeeConnectApp(navController: NavController) {
    val apiaries = remember { mutableStateListOf<Apiary>() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        val userId = Firebase.auth.currentUser?.uid

        if (userId != null) {
            db.collection("apiarios")
                .whereEqualTo("owner_id", userId)
                .get()
                .addOnSuccessListener { result ->
                    apiaries.clear()
                    for (document in result) {
                        val nome = document.getString("nome") ?: "Sem nome"
                        val localizacao = document.getString("localizacao") ?: "Sem localização"
                        val latitude = document.getString("latitude") ?: ""
                        val longitude = document.getString("longitude") ?: ""
                        val imageBase64 = document.getString("imageBase64")

                        apiaries.add(
                            Apiary(
                                name = nome,
                                location = localizacao,
                                latitude = latitude,
                                longitude = longitude,
                                imageBase64 = imageBase64,
                                id = document.id
                            )
                        )
                    }

                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Erro ao buscar apiários: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } else {
            Toast.makeText(
                context,
                "Usuário não autenticado.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        topBar = { BeeConnectTopBar(navController) },
        bottomBar = { BeeConnectBottomNavigation(navController = navController) },
        content = { paddingValues ->  // Add this content parameter
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AddApiaryTopButton(navController)
                Spacer(modifier = Modifier.height(8.dp))
                ApiaryList(apiaries, navController)
            }
        }
    )
}

@Composable
fun BeeConnectTopBar(navController: NavController) {
    TopAppBar(
        title = { Text("BeeConnect") },
        backgroundColor = Color(0xFFFFC107),
        contentColor = Color.Black,
        navigationIcon = {
            IconButton(onClick = { /* Ação ao clicar no logo */ }) {
                Image(
                    painter = painterResource(id = R.drawable.logo_beeconnect),
                    contentDescription = "Logo BeeConnect",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        actions = {
            IconButton(onClick = { navController.navigate("profile")}) {
                Icon(Icons.Default.Person, contentDescription = "Perfil")
            }
        }
    )
}

@Composable
fun AddApiaryTopButton(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        RoundedBlackButton(text = "+ Apiário") {
            navController.navigate("createApiary")
        }
    }
}

@Composable
fun ApiaryList(apiaries: List<Apiary>, navcontroller: NavController) {
    if (apiaries.isEmpty()) {
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
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(apiaries.size) { index ->
                ApiaryCard(apiary = apiaries[index], navController = navcontroller)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ApiaryCard(apiary: Apiary, navController: NavController) {
    val context = LocalContext.current
    val db = Firebase.firestore
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleted by remember { mutableStateOf(false) }

    if (isDeleted) return // Skip rendering if deleted

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar exclusão") },
            text = { Text("Tem certeza que deseja excluir este apiário?") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("apiarios").document(apiary.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Apiário excluído com sucesso",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isDeleted = true
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Erro ao excluir apiário: ${e.message}",
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
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!apiary.imageBase64.isNullOrBlank()) {
                val imageBytes = Base64.decode(apiary.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Imagem do apiário",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sem imagem", color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = apiary.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = apiary.location, fontSize = 16.sp, color = Color.DarkGray)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { navController.navigate("createApiary?apiaryId=${apiary.id}") }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }

                RoundedBlackButton(text = "Ver mais") {
                    navController.navigate("apiaryScreen/${apiary.id}")
                }

                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun RoundedBlackButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Black,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth(0.5f)
    ) {
        Text(text, color = Color.White)
    }
}

@Composable
fun BeeConnectBottomNavigation(navController: NavController) {
    BottomNavigation(
        backgroundColor = Color(0xFFFFC107)
    ) {
        BottomNavigationItem(
            selected = true,
            onClick = { navController.navigate("home") },
            icon = {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Apiários"
                )
            },
            label = null,
            alwaysShowLabel = false
        )
        BottomNavigationItem(
            selected = false,
            onClick = { /* Navegar para Estatísticas */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Estatísticas"
                )
            },
            label = null,
            alwaysShowLabel = false
        )
        BottomNavigationItem(
            selected = false,
            onClick = { navController.navigate("my_apiaries_map") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Mapa"
                )
            },
            label = null,
            alwaysShowLabel = false
        )
        BottomNavigationItem(
            selected = false,
            onClick = { /* Navegar para configurações */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configurações"
                )
            },
            label = null,
            alwaysShowLabel = false
        )
    }
}

@Composable
fun EditHiveScreen(navController: NavController, colmeiaId: String) {
    var hiveName by remember { mutableStateOf("") }
    var hiveType by remember { mutableStateOf("Langstroth") }
    var hiveStatus by remember { mutableStateOf("Ativa") }
    var installationDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val context = LocalContext.current
    val db = Firebase.firestore

    // Load existing hive data
    LaunchedEffect(colmeiaId) {
        db.collection("colmeia").document(colmeiaId).get()
            .addOnSuccessListener { document ->
                hiveName = document.getString("nome") ?: ""
                hiveType = document.getString("tipo") ?: "Langstroth"
                hiveStatus = document.getString("estado") ?: "Ativa"
                installationDate = document.getString("data_instalacao") ?: ""
                notes = document.getString("notas") ?: ""
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Erro ao carregar colmeia: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Colmeia") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                backgroundColor = Color(0xFFFFC107),
                contentColor = Color.Black
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = hiveName,
                onValueChange = { hiveName = it },
                label = { Text("Nome da Colmeia") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Tipo de Colmeia", fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                val types = listOf("Langstroth", "Top Bar", "Warre", "Outro")
                types.forEach { type ->
                    FilterChip(
                        selected = hiveType == type,
                        onClick = { hiveType = type },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(type)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Estado da Colmeia", fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                val statuses = listOf("Ativa", "Inativa", "Em observação")
                statuses.forEach { status ->
                    FilterChip(
                        selected = hiveStatus == status,
                        onClick = { hiveStatus = status },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(status)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = installationDate,
                onValueChange = { installationDate = it },
                label = { Text("Data de Instalação (DD/MM/AAAA)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (hiveName.isBlank()) {
                        Toast.makeText(
                            context,
                            "Por favor, insira um nome para a colmeia",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    val hiveData = hashMapOf(
                        "nome" to hiveName,
                        "tipo" to hiveType,
                        "estado" to hiveStatus,
                        "data_instalacao" to installationDate,
                        "notas" to notes
                    )

                    db.collection("colmeia").document(colmeiaId)
                        .update(hiveData as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Colmeia atualizada com sucesso!",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "Erro ao atualizar colmeia: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Atualizar Colmeia", color = Color.White)
            }
        }
    }
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        color = when {
            selected -> Color(0xFFE0F7FA)
            else -> Color.LightGray
        },
        contentColor = Color.Black,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                selected -> Color(0xFF00838F)
                else -> Color.LightGray
            }
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEditHiveScreen() {
    EditHiveScreen(navController = rememberNavController(), colmeiaId = "test123")
}

@Preview(showBackground = true)
@Composable
fun PreviewBeeConnectApp() {
    val navController = rememberNavController()
    BeeConnectApp(navController)
}
