package com.example.first

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.first.ActivityTransitionData.TRANSITIONS_EXTRA
import com.example.first.ActivityTransitionData.TRANSITIONS_RECEIVER_ACTION
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

// 브로드캐스트 리시버 클래스: 활동 전환 이벤트를 수신하고 처리하는 역할
class ActivityTransitionsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ActivityTransitionReceiver", "Transition Event Received")

        when (intent.action) {
            TRANSITIONS_RECEIVER_ACTION -> {
                // 인텐트에서 활동 전환 결과를 추출
                if (ActivityTransitionResult.hasResult(intent)) {
                    val result: ActivityTransitionResult = ActivityTransitionResult.extractResult(intent) ?: return

                    // 결과에 있는 각 활동 전환 이벤트를 처리
                    for (event in result.transitionEvents) {
                        // 전환 정보를 가져와서 처리
                        val transitionInfo = getTransitionInfo(event)
                        // 전환 정보를 메인 액티비티로 전달
                        sendTransitionInfo(transitionInfo, context)
                    }
                }
            }
        }
    }

    // 이벤트에서 활동 유형과 전환 유형을 문자열로 변환하여 전환 정보를 생성하는 메서드
    private fun getTransitionInfo(event: ActivityTransitionEvent): String {
        val activityType = activityType(event.activityType) // 활동 유형 변환
        val transitionType = transitionType(event.transitionType) // 전환 유형 변환
        Log.d("ActivityRecognition", "Activity Type: $activityType, Transition Type: $transitionType") // 로그 추가
        // 활동 유형, 전환 유형, 현재 시간을 문자열로 결합
        if(transitionType == "시작"){
            return when(activityType){
                "차량 이동" -> "현재 차량을 타고 있습니다."
                "자전거 이동" -> "현재 자전거를 타고 있습니다."
                "뛰기" -> "현재 뛰고 있습니다."
                "걷기" -> "현재 걷고 있습니다."
                "정지" -> "현재 정지에 있습니다."
                else -> "알 수 없음"
            }
        }
        else if(transitionType == "종료") {
            return when (activityType) {
                "차량 이동" -> "현재 차량에서 내렸습니다."
                "자전거 이동" -> "현재 자전거에서 내렸습니다."
                "뛰기" -> "현재 뛰는 걸 멈췄습니다."
                "걷기" -> "현재 걷는 걸 멈췄습니다."
                "정지" -> "현재 움직임이 감지되었습니다."
                else -> "알 수 없음"
            }
        }
        return "($activityType)"
    }

    // 전환 정보를 메인 액티비티로 전달하는 메서드
    private fun sendTransitionInfo(transitionInfo: String, context: Context) {
        // 메인 액티비티로 인텐트를 생성하고 전환 정보를 전달
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(TRANSITIONS_EXTRA, transitionInfo)
        // 기존 액티비티를 다시 열지 않고 같은 액티비티로 인텐트를 전달
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent) // 메인 액티비티 시작
    }


    private fun activityType(activityType: Int): String {
        return when (activityType) {
            DetectedActivity.IN_VEHICLE -> "차량 이동"
            DetectedActivity.ON_BICYCLE -> "자전거 이동"
            DetectedActivity.RUNNING -> "뛰기"
            DetectedActivity.STILL -> "정지"
            DetectedActivity.WALKING -> "걷기"
            else -> "알 수 없음"
        }
    }

    private fun transitionType(transitionType: Int): String {
        return when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "시작"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "종료"
            else -> "알 수 없음"
        }
    }

}

