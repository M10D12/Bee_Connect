package com.example.beeconnect

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Scaffold

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
                composable("createApiary") {
                    CreateApiaryScreen(navController)
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

                        apiaries.add(
                            Apiary(
                                name = nome,
                                location = localizacao,
                                imageRes = R.drawable.apiario,
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
        topBar = { BeeConnectTopBar() },
        bottomBar = { BeeConnectBottomNavigation() },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AddApiaryTopButton(navController)
            Spacer(modifier = Modifier.height(8.dp))
            ApiaryList(apiaries,navController)
        }
    }
}

@Composable
fun BeeConnectTopBar() {
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
            IconButton(onClick = { /* Ação para abrir perfil */ }) {
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
fun ApiaryList(apiaries: List<Apiary>,navcontroller: NavController) {
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
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (apiary.imageRes != null) {
                Image(
                    painter = painterResource(id = apiary.imageRes),
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

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                RoundedBlackButton(text = "Ver mais") {
                    navController.navigate("apiaryScreen/${apiary.id}")
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
fun BeeConnectBottomNavigation() {
    BottomNavigation(
        backgroundColor = Color(0xFFFFC107)
    ) {
        BottomNavigationItem(
            selected = true,
            onClick = { /* Navegar para apiários */ },
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
            onClick = { /* Navegar para estatísticas */ },
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
            onClick = { /* Navegar para mapa */ },
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

data class Apiary(
    val name: String,
    val location: String,
    val imageRes: Int? = null,
    val id: String = "" // útil para futuras ações (editar, apagar)
)

@Preview(showBackground = true)
@Composable
fun PreviewBeeConnectApp() {
    val navController = rememberNavController()
    BeeConnectApp(navController)
}
