package com.chocho.mca_project_tablet

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.integration.android.IntentIntegrator
import java.util.*

/**
 * [호텔 모드 - 도착 후 QR 확인]
 *
 * 로봇 이동 중에는 얼굴 화면을 보여준다.
 * 목적지에 도착해 로봇이 Firebase QR="QR" 을 보내면 전면 카메라 QR 스캐너(zxing)를 띄운다.
 * 스캔 값이 HotelPage1 에서 입력한 호실("go")과 같으면 서랍 열기 화면(HotelPage2)으로,
 * 다르면 QR 신호를 다시 보내 재스캔한다.
 */
class HotelPage3 : AppCompatActivity() {

    private lateinit var integrator: IntentIntegrator

    private lateinit var intentHotelPage2: Intent



    //파이어베이스
    val database = FirebaseDatabase.getInstance()
    private val qr = database.reference.child("QR")
    private lateinit var qrListener: ValueEventListener




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        init()


    }

    private fun init() {

        // 로봇의 도착 신호(QR="QR")를 받으면 QR 스캔 시작
        qrListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == "QR") {
                    //큐얼코드 리더기 추가
                    integrator = IntentIntegrator(this@HotelPage3) //qr 코드 스캐너를 호출하는 액티비티 지정
                    integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE) //스캔할 바코드 형식을 설정 (QR코드)
                    integrator.setCameraId(1) //0후면 카메라 1 전면 카메라
                    integrator.setPrompt("Scan a QR code") //스캔 프롬프트 메세지 설정
                    integrator.setOrientationLocked(false) //스캔 사용자가 화면을 회전 시킬수 있습니다.
                    integrator.setBeepEnabled(true) //스캔시 음 발생 여부 설정
                    integrator.setBarcodeImageEnabled(true) //스캔된 바코드 이미지를 저장할 지 여부
                    integrator.initiateScan() //스캔 작업 시작
                }
            }


            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("MainActivity", "onCancelled: $databaseError")
            }
        }
        qr.addValueEventListener(qrListener)

    }

    //qr코드 주소 반환 시에
    // QR 스캔 결과 처리: 입력한 호실 번호와 비교
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {

            if (result.contents != null) {

                val scannedData = result.contents // 스캔한 QR 코드의 결과 값
                val qrIntentKey = intent.getStringExtra("go")


                if (scannedData == qrIntentKey) {

                    Log.d("호텔", scannedData)
                    customToastView("     호실이 확인 되었습니다.    ")

                    intentHotelPage2 = Intent(this@HotelPage3, HotelPage2::class.java)
                    startActivity(intentHotelPage2)
                    finish()

                } else {

                    customToastView("     Scan canceled    ")
                    // 실패한 경우 QR 코드 리더기 다시 시작
                    qr.removeValue()
                    qr.setValue("QR")
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }


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




    override fun onStop() {
        super.onStop()
        qr.removeEventListener(qrListener)

    }


}



