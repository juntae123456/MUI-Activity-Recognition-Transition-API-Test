package com.example.first

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient


class ActivityTransitionService : Service() {

    private lateinit var activityClient: ActivityRecognitionClient
    private lateinit var pendingIntent: PendingIntent
    private val notificationChannelId = "ActivityTransitionChannel"
    private val notificationId = 1

    override fun onCreate() {
        super.onCreate()

        // ActivityRecognitionClient 초기화
        activityClient = ActivityRecognition.getClient(this)

        // PendingIntent 초기화
        initPendingIntent()

        // Foreground Service 시작
        startForegroundService("활동 감지 중", "백그라운드에서 활동을 감지하고 있습니다.")


    }

    // Foreground Service 시작
    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(notificationChannelId, "Activity Transition Detection", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // 아이콘 설정
            .setContentIntent(pendingIntent)
            .build()

        startForeground(notificationId, notification)  // Foreground 서비스 시작
    }


    // PendingIntent 초기화
    private fun initPendingIntent() {
        val intent = Intent(this, ActivityTransitionsReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}
