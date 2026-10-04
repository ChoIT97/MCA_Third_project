package com.chocho.mca_project_tablet

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*


/**
 * [호텔 모드 - 호실 입력 & 출발]
 *
 * - 키패드로 3자리 호실 번호 입력 (← 는 한 글자 지우기)
 * - 왼쪽: 서랍 3칸의 잠금 상태를 Firebase Hotel_Motor 에서 실시간 표시
 * - 🔓 버튼: 바텀시트(BottomSheetFragment)에서 서랍 칸별 잠금/해제
 * - 상단: 시각, 모듈 결속 상태(체인 아이콘, 누르면 Module_Motor="Open"), 배터리
 * - START: Start="Question" 으로 로봇에 출발 가능 여부를 묻고
 *     "Fail"    → "문을 닫아 주세요"
 *     "Success" → 서랍 3칸 모두 잠금, Hotel/go 에 호실 저장, QR 확인 화면(HotelPage3)으로
 */
class HotelPage1 : AppCompatActivity() {

    private lateinit var startListener: ValueEventListener
    private lateinit var motorListener: ValueEventListener
    private lateinit var moduleListener: ValueEventListener

    private val database = Firebase.database
    private val nfc = database.reference.child("NFC") //모듈 변경을 위한 nfc
    private val module = database.reference.child("Module_Motor") //모듈 잠금 장치
    private val motor = database.reference.child("Hotel_Motor") //서랍 잠금 장치
    private val move = database.reference.child("Start") //출발 신호 보내기
    private val hotel = database.reference.child("Hotel") //저장값 저장하기


    // 잠금 상태 이미지: img_lock2 = 잠김, img_lock7 = 열림
    private val imgLock7 = R.drawable.img_lock7
    private val imgLock2 = R.drawable.img_lock2

    private lateinit var textHome: TextView
    private lateinit var moduleTx: TextView

    private lateinit var moduleImg: ImageFilterView

    // 주의: 이 화면에서는 초기화하지 않아서 NFC 값이 바뀌면 UninitializedPropertyAccessException 으로 종료된다.
    private lateinit var intentLoding: Intent

    private lateinit var lockImgIds: List<Int>
    private lateinit var buttonIds: List<Int>
    private lateinit var imageButtons: Array<ImageButton>
    private lateinit var lockImg: Array<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page1)

        init()


    }

    private fun init() {

        //버튼 변수 리스트로 정리
        // 키패드 버튼 id. 인덱스 0~9 = 숫자, 10 = 지우기, 11 = START, 12 = 서랍 잠금 바텀시트
        buttonIds = listOf(
            R.id.imageButton0,
            R.id.imageButton1,
            R.id.imageButton2,
            R.id.imageButton3,
            R.id.imageButton4,
            R.id.imageButton5,
            R.id.imageButton6,
            R.id.imageButton7,
            R.id.imageButton8,
            R.id.imageButton9,
            R.id.imageButtonBack,
            R.id.imageButtonStart,
            R.id.imageButtonLock

        )
        imageButtons = Array(buttonIds.size) { i -> findViewById(buttonIds[i]) }

        //서랍 잠금 장치 변수 리스트로 정리
        lockImgIds = listOf(R.id.lock1, R.id.lock2, R.id.lock3)
        lockImg = Array(lockImgIds.size) { o -> findViewById(lockImgIds[o]) }


        moduleImg = findViewById(R.id.chain) //모듈 연결 상태 이미지
        moduleTx = findViewById(R.id.chain_tx) //모듈 연결 상태 텍스트


        // 이전 배달 정보 초기화
        hotel.removeValue() //저장값 초기화


        // 체인 아이콘 터치 → 모듈 결속 해제 요청. 상태가 바뀌면 아래 리스너가 아이콘/텍스트를 갱신한다.
        moduleImg.setOnClickListener { module.setValue("Open") }  //모듈 잠금장치

        //모듈 잠금 장치 값 읽고 이미지
        moduleListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val moduleValue = snapshot.value
                if (moduleValue == "Open") {
                    moduleImg.setImageResource(R.drawable.chain_broken)
                    moduleTx.text = "해제"
                } else {
                    moduleImg.setImageResource(R.drawable.chain2)
                    moduleTx.text = "연결"
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }

        }
        module.addValueEventListener(moduleListener)

        textHome = findViewById(R.id.text_home)

        //버튼 클릭 함수
        // 키패드 입력: "del" 이면 마지막 글자 삭제, 숫자면 최대 3자리까지 이어 붙인다.
        fun buttonClick(num: String) {
            with(textHome) {
                setTextColor(ContextCompat.getColor(this@HotelPage1, R.color.purple_CACAE1))
                if (num == "del") {
                    if (text.isNotEmpty()) text = text.substring(0, text.length - 1)
                } else {
                    val setNum = text.toString() + num
                    if (setNum.length <= 3) text = setNum
                }
            }
        }

        val button = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "del")

        for (i in button.indices) {
            imageButtons[i].setOnClickListener { buttonClick(button[i]) }
        }


        //로봇 이동 start버튼
        // START 버튼: 3자리 호실이 입력됐으면 로봇에 출발 요청(Start="Question")을 보내고 응답을 기다린다.
        imageButtons[11].setOnClickListener {

            move.setValue("Question")

            val length = textHome.text.length
            when {
                length < 3 -> {
                    customToastView("호실을 지정해주시기 바랍니다.")
                    move.setValue("Null")
                }
                length == 3 -> {
                    startListener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val startValue = snapshot.value
                            val lock1 = motor.child("Hotel_Motor1")
                            val lock2 = motor.child("Hotel_Motor2")
                            val lock3 = motor.child("Hotel_Motor3")

                            Log.d("startValue", "Value is: $startValue")

                            if (startValue == "Fail") {

                                customToastView("  문을 닫아 주세요  ")


                            } else if (startValue == "Success") {


                                // 출발 후 서랍이 열리면(…_Unlock) 어느 칸이 열렸는지 Hotel/Lock1~3 에 기록 → HotelPage2 에서 사용
                                motorListener = object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {

                                        val hotelMotor1 = snapshot.child("Hotel_Motor1").value.toString()
                                        val hotelMotor2 = snapshot.child("Hotel_Motor2").value.toString()
                                        val hotelMotor3 = snapshot.child("Hotel_Motor3").value.toString()

                                        if (hotelMotor1 == "First_Unlock") {
                                            hotel.child("Lock1").setValue("First_Unlock")
                                        }
                                        if (hotelMotor2 == "Second_Unlock") {
                                            hotel.child("Lock2").setValue("Second_Unlock")

                                        }
                                        if (hotelMotor3 == "Third_Unlock") {
                                            hotel.child("Lock3").setValue("Third_Unlock")

                                        }

                                    }

                                    override fun onCancelled(error: DatabaseError) {

                                    }
                                }
                                motor.addValueEventListener(motorListener)


                                lock1.setValue("First_Lock")
                                lock2.setValue("Second_Lock")
                                lock3.setValue("Third_Lock")
                                hotel.child("go").setValue(textHome.text)

                                customToastView("  ${textHome.text}호로  출발 합니다.  ")


                                val intentAmenityPage3 = Intent(this@HotelPage1, HotelPage3::class.java)
                                intentAmenityPage3.putExtra("go", textHome.text)
                                startActivity(intentAmenityPage3)
                                finish()


                            }




                        }


                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }




                    }
                    move.addValueEventListener(startListener)

                }
            }

        }
        //nfc 동작
        // 로봇에 끼운 모듈(NFC 태그)이 바뀌면 로딩 화면을 거쳐 해당 모드로 전환한다.
        // NFC 값: "None"=처음 화면, "Hotel"=호텔 모드, "Serving"=서빙 모드
        nfc.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nfcValue = snapshot.value

                when (nfcValue) {

                    // 로딩_메인(0)
                    "None" -> {

                        intentLoding.putExtra("key1", "0")

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

                Log.d("AmenityMain_nfc", "Value is: $nfcValue")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("파이어", "Failed to read value.", error.toException())
            }
        })


        //잠금장치 클릭시 바텀 시트
        // 🔓 버튼: 서랍 칸별 잠금/해제 바텀시트 열기
        imageButtons[12].setOnClickListener {
            val bottomSheet = BottomSheetFragment()
            bottomSheet.show(supportFragmentManager, BottomSheetFragment.TAG)


        }


        //잠금장치 이미지 넣기 위한
        // 서랍 3칸 잠금 상태를 왼쪽 자물쇠 이미지에 실시간 반영
        motorListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val locks = arrayOf(
                    snapshot.child("Hotel_Motor1").value as String?,
                    snapshot.child("Hotel_Motor2").value as String?,
                    snapshot.child("Hotel_Motor3").value as String?
                )

                val lockImages = arrayOf(imgLock2, imgLock7)

                locks.forEachIndexed { index, lock ->
                    val imageResource = if (lock == "First_Lock" || lock == "Second_Lock" || lock == "Third_Lock") {
                        lockImages[0]
                    } else {
                        lockImages[1]
                    }
                    lockImg[index].setImageResource(imageResource)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("파이어", "Failed to read value.", error.toException())
            }

        }
        motor.addValueEventListener(motorListener)

        //Thread 이용하여 매초마다 업데이트 되는 방식
        val handler = Handler(Looper.getMainLooper())
        // 상태바(시간·배터리)를 계속 갱신하는 반복 작업. (10ms 간격이라 실제로는 매우 자주 호출된다)
        val runnable = object : Runnable {
            override fun run() {
                updateUI()
                handler.postDelayed(this, 10)
            }
        }
        handler.post(runnable)

    }

    //배터리&시간
    // 상단 상태바 갱신: 현재 시각(오전/오후 hh:mm:ss)과 배터리 잔량(%)
    private fun updateUI() {
        // Set the time
        val textTime = findViewById<TextView>(R.id.text_time)
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("a hh:mm:ss", Locale.getDefault())
        val getTime = dateFormat.format(now)
        textTime.text = getTime

        // Set the battery level
        val textBattery: TextView = findViewById<TextView>(R.id.text_battery)
        val batteryPct = getBatteryLevel()
        textBattery.text = "${batteryPct}%"
        someFunction(batteryPct.toString())
    }

    //배터리 계산
    // ACTION_BATTERY_CHANGED 브로드캐스트에서 배터리 잔량(0~100)을 읽는다. 실패하면 -1
    private fun getBatteryLevel(): Int {
        return try {
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = registerReceiver(null, batteryFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            (level / scale.toFloat() * 100).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    //
    // 배터리 잔량에 맞는 배터리 아이콘으로 바꾼다. (100 / 70↑ / 50↑ / 그 외)
    fun someFunction(returnData: String) {
        val Image_battery = findViewById<ImageFilterView>(R.id.Image_battery)
        when {
            returnData.toInt() == 100 -> Image_battery.setImageResource(R.drawable.battery_full)
            returnData.toInt() >= 70 -> Image_battery.setImageResource(R.drawable.battery_threequarter)
            returnData.toInt() >= 50 -> Image_battery.setImageResource(R.drawable.battery_half)
            returnData.toInt() >= 30 -> Image_battery.setImageResource(R.drawable.battery_low)
            else -> Image_battery.setImageResource(R.drawable.battery_low)
        }
    }

    /**
     * 화면 가운데에 커스텀 토스트(activity_custom_toast.xml)를 띄운다.
     * 주의: activity_custom_toast.xml 레이아웃이 저장소에 없어 그대로는 빌드되지 않는다.
     */
    private fun customToastView(text: String) {
        val inflater = layoutInflater
        val layout: View = inflater.inflate(R.layout.activity_custom_toast, findViewById<ViewGroup>(R.id.toast_layout_root))
        val textView = layout.findViewById<TextView>(R.id.textboard)
        textView.text = text

        val toastView = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
        toastView.setGravity(Gravity.CENTER, 0, 0)
        toastView.view = layout
        toastView.show()
    }




    // 화면을 떠날 때 Firebase 리스너 해제 (주의: START 를 누르지 않았다면 startListener 가 초기화되지 않아 종료될 수 있다)
    override fun onStop() {
        super.onStop()
        move.removeEventListener(startListener)
        motor.removeEventListener(motorListener)
        module.removeEventListener(moduleListener)

    }
}


