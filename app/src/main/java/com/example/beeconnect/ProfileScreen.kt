package com.example.beeconnect

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    var showDialog by rememberSaveable { mutableStateOf(false) }

    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var existingImageBase64 by rememberSaveable { mutableStateOf(profilePicUrl) }

    LaunchedEffect(profilePicUrl) {
        existingImageBase64 = profilePicUrl
        selectedImageUri = null
    }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            existingImageBase64 = null
        }
    }

    if (showDialog) {
        EditProfileDialog(
            initialName = displayName,
            initialEmail = email,
            selectedImageUri = selectedImageUri,
            existingImageBase64 = existingImageBase64,
            onDismiss = { showDialog = false },
            onSave = { newName, newEmail, newPhotoUri ->
                val uid = Firebase.auth.currentUser?.uid
                val db = Firebase.firestore

                if (uid != null) {
                    val newPhotoBase64 = newPhotoUri?.let { encodeImageToBase64(it, context) }

                    db.collection("users").document(uid)
                        .update(
                            mapOf(
                                "username" to newName,
                                "email" to newEmail,
                                "profilePic" to newPhotoBase64
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(context, "Perfil atualizado!", Toast.LENGTH_SHORT).show()
                            onSaveUpdatedInfo(newName, newEmail, newPhotoBase64 ?: "")
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                        }
                }
                showDialog = false
            },
            onImageClick = { imagePickerLauncher.launch("image/*") }
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

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { showDialog = true },
                contentAlignment = Alignment.Center
            ) {
                when {
                    selectedImageUri != null -> {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Foto de perfil selecionada",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    !existingImageBase64.isNullOrBlank() -> {
                        val bitmap = decodeBase64ToImageBitmap(existingImageBase64)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto de perfil existente",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.bee_keeper),
                                contentDescription = "Foto padrão",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    else -> {
                        Image(
                            painter = painterResource(id = R.drawable.bee_keeper),
                            contentDescription = "Foto padrão",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
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

    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(uid, refreshTrigger) {  // Add refreshTrigger as a key
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
            refreshTrigger++
        }
    )
}

@Composable
fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    selectedImageUri: Uri?,
    existingImageBase64: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, Uri?) -> Unit,
    onImageClick: () -> Unit
) {
    var newName by remember { mutableStateOf(initialName) }
    var newEmail by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Clique na imagem para alterar",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0))
                        .clickable { onImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedImageUri != null -> {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Nova foto de perfil",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        !existingImageBase64.isNullOrBlank() -> {
                            val bitmap = decodeBase64ToImageBitmap(existingImageBase64)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto atual",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.bee_keeper),
                                    contentDescription = "Foto padrão",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        else -> {
                            Image(
                                painter = painterResource(id = R.drawable.bee_keeper),
                                contentDescription = "Adicionar foto",
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Name and email fields
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
            }
        },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onSave(newName, newEmail, selectedImageUri) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF03A9F4))
                ) {
                    Text("Salvar", color = Color.White)
                }
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

fun decodeBase64ToImageBitmap(base64: String?): android.graphics.Bitmap? {
    if (base64 == null) return null
    return try {
        val imageBytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        null
    }
}

