package com.example.beeconnect

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun ApiaryScreen(navController: NavController) {
    Scaffold(
        topBar = { BeeConnectTopBar() },
        bottomBar = { BeeConnectBottomNavigation() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Apiário Lamego", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Button(
                    onClick = { /* Navegar para CreateHiveScreen */ },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Text(" Colmeia", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weather Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF2196F3), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lamego", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Today, 12 September", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("29°", color = Color.White, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Wind 10 km/h  Hum 54%", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hive Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xFFFFC107)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Colmeia 50", fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Image(
                        painter = painterResource(id = R.drawable.apiario),
                        contentDescription = "Imagem Colmeia",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { /* Navegar para detalhes da colmeia */ },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
                    ) {
                        Text("Ver mais", color = Color.White)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewApiaryScreen() {
    MaterialTheme {
        ApiaryScreen(navController = rememberNavController())
    }
}
