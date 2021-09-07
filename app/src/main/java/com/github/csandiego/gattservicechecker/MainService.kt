package com.github.csandiego.gattservicechecker

import android.app.Notification
import android.app.PendingIntent
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainService : LifecycleService() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private val SERVICE_UUID = UUID.fromString("EB9F9B9B-ED1E-4BC0-948F-84E488844E09")
    }

    private var gattServer: BluetoothGattServer? = null

    private val gattService = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (gattServer == null) {
            gattServer = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
                .openGattServer(this, object : BluetoothGattServerCallback() {}).apply {
                addService(gattService)
            }
            lifecycleScope.launch {
                while(true) {
                    delay(1000L)
                    val time = SimpleDateFormat("h:mm:ss").format(Date())
                    val found = if (gattServer?.getService(SERVICE_UUID) == null) " GONE" else " FOUND"
                    NotificationManagerCompat.from(this@MainService)
                        .notify(NOTIFICATION_ID, buildNotification(time + found))
                }
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification(SimpleDateFormat("h:mm:ss").format(Date())))

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        gattServer?.close()
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            0
        )
        return NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
            .setContentTitle("Gatt Service Checker")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}