package com.example.ceris.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CerisMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        PushNotifications.get(this).registered(installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Sem HTTP neste callback: valida e publica dentro do tempo do FCM.
        PushNotifications.get(this).receive(message.data)
    }
}
