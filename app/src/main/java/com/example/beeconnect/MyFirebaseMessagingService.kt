package com.example.beeconnect

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Mensagem recebida: ${message.notification?.title} - ${message.notification?.body}")
        // Aqui podes exibir uma notificação local se quiseres
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Novo token: $token")
        // Podes guardar o token na Firestore se quiseres enviar mensagens específicas
    }
}
