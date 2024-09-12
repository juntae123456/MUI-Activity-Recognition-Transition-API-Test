package com.example.first

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.first.ActivityTransitionData.TRANSITIONS_EXTRA
import com.example.first.ActivityTransitionData.TRANSITIONS_RECEIVER_ACTION
import com.example.first.ui.theme.FirstTheme
import com.example.first.ui.theme.blue
import com.example.first.ui.theme.gray
import com.example.first.ui.theme.green
import com.example.first.ui.theme.line
import com.example.first.ui.theme.red
import com.example.first.ui.theme.topcolor
import com.example.first.ui.theme.yellow
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransitionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var activityClient: ActivityRecognitionClient
    private lateinit var request: ActivityTransitionRequest
    private lateinit var pendingIntent: PendingIntent

    private val activityTransitionReceiver by lazy { ActivityTransitionsReceiver() }

    // 현재 상태 변수
    private var currentActivity by mutableStateOf("실행 되었습니다.")
    private var currentTime by mutableStateOf(getCurrentFormattedTime())
    private val statusList = mutableStateListOf<String>() // 활동 상태 리스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FordegroundService()

        activityClient = ActivityRecognition.getClient(this)

        // 활동 인식 권한 확인 및 요청
        if (!checkRecognitionPermissionIfLaterVersionQ()) {
            requestRecognitionPermission()
        }

        setContent {
            FirstTheme {
                StatusScreen(
                    currentActivity = currentActivity,
                    currentTime = currentTime,
                    statusList = statusList,
                    getCurrentTime = { getCurrentFormattedTime() }
                )
            }
        }

        // PedingIntent 설정 및 활동 전환 요청
        initPendingIntent()

        if (checkRecognitionPermissionIfLaterVersionQ()) {
            registerActivityTransitionUpdates()
        }
    }

    private fun FordegroundService() {
        Intent(this, ActivityTransitionService::class.java).run {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) startForegroundService(this)
            else startService(this)
            Log.d("ActivityTransition", "Foreground Service 시작됨 (Android O 이상)")
        }
    }
    // 브로드 캐스터 리시버 등록
    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(TRANSITIONS_RECEIVER_ACTION)

        registerReceiver(
            activityTransitionReceiver,
            intentFilter,
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(activityTransitionReceiver)
    }

    private fun checkRecognitionPermissionIfLaterVersionQ(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val activityRecognitionGranted = PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)

            val locationPermissionGranted = PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

            activityRecognitionGranted && locationPermissionGranted
        } else {
            true
        }
    }

    private fun requestRecognitionPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            0
        )
    }

    private fun initPendingIntent() {
        val intent = Intent(ActivityTransitionData.TRANSITIONS_RECEIVER_ACTION)
        intent.setPackage(this.packageName)

        pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun registerActivityTransitionUpdates() {
        request = ActivityTransitionRequest(ActivityTransitionData.getActivityTransitionList())
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        activityClient
            .requestActivityTransitionUpdates(request, pendingIntent)
            .addOnSuccessListener {
                addItemToList("활동 감지가 시작되었습니다.")
            }
            .addOnFailureListener { exception ->
                addItemToList(exception.localizedMessage ?: "예외가 발생하였습니다.")
            }
    }

    private fun addItemToList(activityTransition: String) {
        statusList.add("$activityTransition")
    }

    // From ActivityTransitionReceiver
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("Activity Recognition", "onNewIntent")
        checkIntentData(intent)
    }

    private fun checkIntentData(intent: Intent) {
        val activityTransition = intent.getStringExtra(TRANSITIONS_EXTRA)


        if (activityTransition != null) {
            // 상단 텍스트 뷰 업데이트
            currentActivity = activityTransition
            currentTime = getCurrentFormattedTime()
            addItemToList(activityTransition)
        }
    }

    // 시간 출력 메서드
    private fun getCurrentFormattedTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    currentActivity: String,
    currentTime: String,
    statusList: List<String>,
    getCurrentTime: () -> String // 시간 함수 전달
) {
    // 시간과 상태 리스트의 변경 가능 변수
    var time by remember { mutableStateOf(getCurrentTime()) }
    val mutableStatusList = remember { mutableStateListOf(*statusList.toTypedArray()) }

    // LazyColumn의 스크롤 상태 저장
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val current = "$currentActivity"

    // 상태가 변경될 때마다 상태 리스트에 추가하고 자동 스크롤
    LaunchedEffect(mutableStatusList.size) {
        coroutineScope.launch {
            listState.scrollToItem(mutableStatusList.size - 1) // 자동으로 마지막 항목으로 스크롤
        }
    }

    // 1초마다 시간과 상태 리스트 업데이트
    LaunchedEffect(currentActivity) {
        while (true) {
            time = getCurrentTime()

            if("$currentActivity"=="현재 차량을 타고 있습니다.")
            {
                mutableStatusList.add("차량에 탄 상태입니다.")
            }
            else if("$currentActivity"=="현재 자전거를 타고 있습니다.")
            {
                mutableStatusList.add("자전거에 탄 상태입니다.")
            }
            else if("$currentActivity"=="현재 뛰고 있습니다.")
            {
                mutableStatusList.add("뛰는 상태입니다.")
            }
            else if("$currentActivity"=="현재 걷고 있습니다.")
            {
                mutableStatusList.add("걷는 상태입니다.")
            }
            else if("$currentActivity"=="현재 정지에 있습니다.")
            {
                mutableStatusList.add("정지 상태입니다.")
            }
            else{
                mutableStatusList.add("$currentActivity") // 로그를 추가
            }
            delay(1000L) // 1초마다 갱신
        }
    }

    Scaffold(
        topBar = {
            CustomLargeTopAppBarWithCenteredTitleAndTime(currentActivity, time)
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 상태 리스트 표시
                StatusList(mutableStatusList,  listState, getCurrentTime)
            }
        }
    )

}

@Composable
fun CustomLargeTopAppBarWithCenteredTitleAndTime(status: String, currentTime: String) {
    Surface(
        color = topcolor,
        modifier = Modifier
            .fillMaxWidth()
            .height(147.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(147.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = status,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentTime,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 20.sp
                ),
                color = gray
            )
        }
    }
}

@Composable
fun StatusList(statusList: List<String>, listState: LazyListState, getCurrentTime: () -> String) {
    LazyColumn(
        state = listState, // 스크롤 상태
        reverseLayout = false, // 아래로 항목 추가
        modifier = Modifier.fillMaxSize()
    ) {
        items(statusList.size) { index ->
            StatusItem(statusList[index], getCurrentTime)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = line
            )
        }
    }
}

@Composable
fun StatusItem(status: String, getCurrentTime: () -> String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = getCurrentTime(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterVertically)
        )

        Spacer(modifier = Modifier.weight(1f))

        // "정지"라는 단어만 회색으로 표시
        val annotatedStatus = buildAnnotatedString {
            if (status.contains("정지")) {
                // "정지" 부분만 회색으로 스타일 적용
                val parts = status.split("정지")
                append(parts[0])
                withStyle(style = SpanStyle(color = gray)) {
                    append("정지")
                }
                append(parts[1]) // 나머지 부분
            }
            else if (status.contains("걷는")) {
                // "걷는" 부분만 파란색으로 스타일 적용
                val parts = status.split("걷는")
                append(parts[0])
                withStyle(style = SpanStyle(color = blue)) {
                    append("걷는")
                }
                append(parts[1]) // 나머지 부분
            }
            else if (status.contains("뛰는")) {
                // "걷는" 부분만 파란색으로 스타일 적용
                val parts = status.split("뛰는")
                append(parts[0])
                withStyle(style = SpanStyle(color = yellow)) {
                    append("뛰는")
                }
                append(parts[1]) // 나머지 부분
            }
            else if (status.contains("자전거")) {
                // "걷는" 부분만 파란색으로 스타일 적용
                val parts = status.split("자전거")
                append(parts[0])
                withStyle(style = SpanStyle(color = green)) {
                    append("자전거")
                }
                append(parts[1]) // 나머지 부분
            }
            else if (status.contains("자동차")) {
                // "걷는" 부분만 파란색으로 스타일 적용
                val parts = status.split("자동차")
                append(parts[0])
                withStyle(style = SpanStyle(color = red)) {
                    append("자동차")
                }
                append(parts[1]) // 나머지 부분
            }
            else {
                append(status) // "정지"가 없으면 기본 텍스트로
            }
        }

        Text(
            text = annotatedStatus,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}




fun getCurrentFormattedTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}

@Preview(showBackground = true)
@Composable
fun PreviewStatusScreen() {
    FirstTheme {
        StatusScreen(
            currentActivity = "정지 상태 입니다",
            currentTime = getCurrentFormattedTime(),
            statusList = listOf("정지 상태 입니다", "걷는 상태 입니다"),
            getCurrentTime = { getCurrentFormattedTime() }
        )
    }
}
