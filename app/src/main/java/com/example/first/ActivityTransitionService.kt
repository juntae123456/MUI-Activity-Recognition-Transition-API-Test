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
import com.example.first.ActivityTransitionsReceiver
import com.example.first.MainActivity
import com.example.first.R
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

        // Android O 이상에서는 Notification Channel 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Activity Transition Detection",
                NotificationManager.IMPORTANCE_HIGH  // 중요도를 높게 설정
            ).apply {
                description = "사용자 활동을 감지하는 서비스입니다."
                setShowBadge(false)  // 알림 배지 표시 안함
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        // 영구적 알림 생성
        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // 알림 아이콘
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // 알림 중요도 높게 설정
            .setOngoing(true)  // 사용자가 알림을 삭제하지 못하도록 설정
            .build()

        startForeground(notificationId, notification)  // Foreground 서비스 시작
    }
    // 알림을 업데이트하는 메서드
    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // 아이콘 설정
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)  // 알림 업데이트
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

    // 활동이 변경될 때 호출되는 메서드 (예시로 추가한 메서드)
    fun onActivityTransitionChanged(activityName: String) {
        // 활동 변경 시 알림을 업데이트
        updateNotification("활동 변경 감지", "현재 활동: $activityName")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
