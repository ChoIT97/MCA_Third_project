package com.chocho.mca_project_tablet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide.init
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * [대기 화면 - 앱 시작점]
 *
 * 로봇 얼굴(robot_2) 이미지만 보이는 화면. 로봇에 어떤 모듈이 끼워졌는지(NFC)를 기다린다.
 * - Firebase "NFC" = "Hotel"   → 로딩 → 호텔 모드(HotelMain)
 * - Firebase "NFC" = "Serving" → 로딩 → 서빙 모드(ServingMain)
 *
 * 들어올 때 이전 작업 값(QR, Sound)을 지우고 Start 를 "Null" 로 초기화한다.
 */
class Main : AppCompatActivity() {

    private val database = Firebase.database

    // Firebase Realtime DB 노드들 (로봇 하드웨어와 이 앱이 값을 주고받는 통로)
    private val nfc = database.reference.child("NFC")
    private val qr = database.reference.child("QR")
    private val sound = database.reference.child("Sound")
    private val hotelStart = database.reference.child("Start")

    private lateinit var robot : ConstraintLayout

    private lateinit var intentLoding : Intent

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        init()

    }

    private fun init(){

        // 화면 생성되면 뜨는 커스텀 토스트 메세지
        // 로딩 화면에서 "모듈 해제"로 돌아온 경우("string")와 처음 실행한 경우의 인사 토스트를 구분
        when(intent.getStringExtra("key1")){

            "string" -> customToastView("해제 완료 되었습니다.") //해제시

            else ->  customToastView("만나서 반갑습니다 !!") //처음 시작시

        }

        // 이전 작업 신호 초기화
        qr.removeValue()
        sound.removeValue()
        hotelStart.setValue("Null")

        //xml에서 가져오기
        robot = findViewById(R.id.Robot)

        //MainLoading 이동
        intentLoding = Intent(this@Main, MainLoading::class.java)

        //메인페이지 클릭시 토스트메세지
        // 대기 화면에서는 할 수 있는 동작이 없어 안내 토스트만 띄운다.
        robot.setOnClickListener { customToastView("     업데이트 된 화면이 없습니다.     ") }

        //nfc파이어베이스
        // 로봇에 끼운 모듈(NFC 태그)이 바뀌면 로딩 화면을 거쳐 해당 모드로 전환한다.
        // NFC 값: "None"=처음 화면, "Hotel"=호텔 모드, "Serving"=서빙 모드
        nfc.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val nfcValue = snapshot.value // nfcValue 값

                when (nfcValue) {

                    // 로딩_호텔(1)
                    "Hotel" -> {
                        intentLoding.putExtra("key1", "1")
                        startActivity(intentLoding)
                        finish()
                    }

                    // 로딩_서빙(2)
                    "Serving" -> {
                        intentLoding.putExtra("key1", "2")
                        startActivity(intentLoding)
                        finish()
                    }

                }

                Log.d("nfcValue", "Value is: $nfcValue") // nfcValue 값 확인

            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("nfcValue", "Failed to read value.", error.toException())
            }

        })
    }

    //커스텀 메세지
    /**
     * 화면 가운데에 커스텀 토스트(activity_custom_toast.xml)를 띄운다.
     * 주의: activity_custom_toast.xml 레이아웃이 저장소에 없어 그대로는 빌드되지 않는다.
     */
    private fun customToastView(text: String) {
        val inflater = layoutInflater
        val layout: View = inflater.inflate(R.layout.activity_custom_toast, findViewById(R.id.toast_layout_root))
        val textView = layout.findViewById<TextView>(R.id.textboard)
        textView.text = text

        val toastView = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
        toastView.setGravity(Gravity.CENTER, 0, 0)
        toastView.view = layout
        toastView.show()
    }

}