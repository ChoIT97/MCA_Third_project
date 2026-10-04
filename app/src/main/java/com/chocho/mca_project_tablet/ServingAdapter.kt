package com.chocho.mca_project_tablet

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * [서빙 테이블 카드 어댑터]
 *
 * 카드 한 장(activity_serving_item.xml): 테이블 이름 / 주문 목록(안쪽 RecyclerView) / [이 동] 버튼
 * [이 동] → Firebase Start="Serving_Start" 로 로봇 출발 신호를 보내고 ServingMain(얼굴 화면)으로 이동.
 *
 * 현재는 모든 카드가 같은 master 전체 목록을 보여준다. (2번 테이블 분기는 주석 처리됨)
 */
class ServingAdapter(private val items: List<String>,private val context: Context) : RecyclerView.Adapter<ServingAdapter.ViewHolder>() {
    private val database = Firebase.database
    private val move = database.reference.child("Start")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_serving_item, parent, false)

        //파이어베이스 어뎁터 추가
        val holder = ViewHolder(view)
        val innerLayoutManager = LinearLayoutManager(parent.context)


        holder.recyclerView2.layoutManager = innerLayoutManager
        holder.recyclerView2.adapter = ServingAdapterFirebase(parent.context)
        holder.recyclerView2.setHasFixedSize(true)
//        Log.d("확인용4",items[0])
//        Log.d("확인용5",items[1])
        // 첫 키가 "0" 이면 master 전체 주문을 구독해서 안쪽 목록에 표시
        if (items[0] =="0"){
            val viewModel = ViewModelProvider(parent.context as ServingPage1, ViewModelProvider.AndroidViewModelFactory.getInstance((parent.context as ServingPage1).application)).get(SubViewModel::class.java)
            viewModel.table1Data().observe(parent.context as ServingPage1, Observer {
                holder.recyclerView2.adapter?.let { adapter ->
                    if (adapter is ServingAdapterFirebase) {

                        adapter.setListData(it)
                        Log.d("사살", adapter.setListData(it).toString())
                        adapter.notifyDataSetChanged()
                    }
                }
            })
        }

        // [이 동] 버튼: 로봇 출발 신호
        holder.button2.setOnClickListener {
            move.setValue("Serving_Start")
            val intent = Intent(context, ServingMain::class.java)
            context.startActivity(intent)

        }
//        else if(items[1] =="2번 테이블"){
//            val viewModel = ViewModelProvider(parent.context as ServingPage1, ViewModelProvider.AndroidViewModelFactory.getInstance((parent.context as ServingPage1).application)).get(SubViewModel::class.java)
//            viewModel.table2Data().observe(parent.context as ServingPage1, Observer {
//                holder.recyclerView2.adapter?.let { adapter ->
//                    if (adapter is ServingAdapterFirebase) {
//                        adapter.setListData(it)
//                        Log.d("사살", adapter.setListData(it).toString())
//                        adapter.notifyDataSetChanged()
//                    }
//                }
//            })
//        }





        return ViewHolder(view)
    }

    // 뷰 홀더에 데이터 바인딩
    // 키("0","1","2")를 "1번/2번/3번 테이블" 이름으로 바꿔 표시
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val itemTable = when(item){
            "0" -> "1번 테이블"
            "1" -> "2번 테이블"
            "2" -> "3번 테이블"
            else -> "준비중"
        }
        holder.textView.text = itemTable

    }

    // 아이템 개수 반환
    override fun getItemCount(): Int {
        return items.size
    }

    // 뷰 홀더 클래스
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val recyclerView2: RecyclerView = itemView.findViewById(R.id.recyclerView2)
        val button2: Button =itemView.findViewById(R.id.button2)


    }



}