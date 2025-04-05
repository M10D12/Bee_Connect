package com.example.beeconnect

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun ProfileScreen(
    navController: NavController,
    displayName: String,
    email: String,
    profilePicUrl: String?,
    showLogout: Boolean = true,
    onSaveUpdatedInfo: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        EditProfileDialog(
            initialName = displayName,
            initialEmail = email,
            initialPhoto = profilePicUrl ?: "",
            onDismiss = { showDialog = false },
            onSave = { newName, newEmail, newPhotoUrl ->
                val uid = Firebase.auth.currentUser?.uid
                val db = Firebase.firestore

                if (uid != null) {
                    db.collection("users").document(uid)
                        .update(
                            mapOf(
                                "username" to newName,
                                "email" to newEmail,
                                "profilePic" to newPhotoUrl
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(context, "Perfil atualizado!", Toast.LENGTH_SHORT).show()
                            onSaveUpdatedInfo(newName, newEmail, newPhotoUrl)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                        }
                }
                showDialog = false
            }
        )
    }

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
            Spacer(modifier = Modifier.height(32.dp))

            if (profilePicUrl.isNullOrBlank()) {
                Image(
                    painter = painterResource(id = R.drawable.bee_keeper),
                    contentDescription = "Foto padrão",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
            } else {
                AsyncImage(
                    model = profilePicUrl,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = email, fontSize = 16.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showDialog = true },
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

    var username by remember { mutableStateOf("Carregando...") }
    var email by remember { mutableStateOf(user?.email ?: "email@exemplo.com") }
    var profilePicUrl by remember { mutableStateOf("") }

    val uid = user?.uid

    LaunchedEffect(uid) {
        if (uid != null) {
            val db = Firebase.firestore
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        username = doc.getString("username") ?: "Utilizador"
                        email = doc.getString("email") ?: email
                        profilePicUrl = doc.getString("profilePic") ?: ""
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Erro ao carregar perfil: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    ProfileScreen(
        navController = navController,
        displayName = username,
        email = email,
        profilePicUrl = profilePicUrl,
        onSaveUpdatedInfo = { updatedName, updatedEmail, updatedPhotoUrl ->
            username = updatedName
            email = updatedEmail
            profilePicUrl = updatedPhotoUrl
        }
    )
}

@Composable
fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    initialPhoto: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var newName by remember { mutableStateOf(initialName) }
    var newEmail by remember { mutableStateOf(initialEmail) }
    var newPhotoUrl by remember { mutableStateOf(initialPhoto) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPhotoUrl,
                    onValueChange = { newPhotoUrl = it },
                    label = { Text("URL da Foto de Perfil") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(newName, newEmail, newPhotoUrl) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
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
        profilePicUrl = "",
        showLogout = false,
        onSaveUpdatedInfo = { _, _, _ -> }
    )
}
