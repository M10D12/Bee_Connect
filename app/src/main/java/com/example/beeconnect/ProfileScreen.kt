package com.example.beeconnect

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.Composable
import com.google.firebase.firestore.ktx.firestore
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun ProfileScreen(
    navController: NavController,
    displayName: String,
    email: String,
    showLogout: Boolean = true
) {
    val context = LocalContext.current

    Scaffold(
        topBar = { BeeConnectTopBar(navController) },
        bottomBar = { BeeConnectBottomNavigation(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.apiario),
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = email,
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Funcionalidade em desenvolvimento", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF03A9F4))
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Perfil", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showLogout) {
                Button(
                    onClick = {
                        Firebase.auth.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", color = Color.White)
                }
            }
        }
    }
}
@Composable
fun RealProfileScreen(navController: NavController) {
    val user = Firebase.auth.currentUser
    val context = LocalContext.current

    // ✅ Tipos declarados explicitamente
    var username by remember { mutableStateOf("Carregando...") }
    var email by remember { mutableStateOf(user?.email ?: "email@exemplo.com") }

    val uid = user?.uid

    // ✅ Correto uso de LaunchedEffect + Firebase Firestore
    LaunchedEffect(uid) {
        if (uid != null) {
            val db = Firebase.firestore
            db.collection("utilizadores").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        username = doc.getString("username") ?: "Utilizador"
                        email = doc.getString("email") ?: email
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Erro ao carregar perfil: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // ✅ Passa os dados para a UI desacoplada
    ProfileScreen(
        navController = navController,
        displayName = username,
        email = email
    )
}



@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    val fakeNavController = rememberNavController()
    ProfileScreen(
        navController = fakeNavController,
        displayName = "Maria Abelha",
        email = "maria@colmeia.com",
        showLogout = false
    )
}