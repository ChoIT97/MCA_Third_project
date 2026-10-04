package com.chocho.mca_project_tablet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.NonDisposableHandle.parent

/**
 * [테이블 카드 안쪽 주문 목록 어댑터]
 *
 * "메뉴 이름 / 수량 : n" 형식으로 한 줄씩 표시한다.
 */
class ServingAdapterFirebase(private val context: Context): RecyclerView.Adapter<ServingAdapterFirebase.ViewHolder>() {

    private var meatList = mutableListOf<Meat>()



    fun setListData(data: MutableList<Meat>) {
        meatList = data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_serving_firebase_item, parent, false)

        return ViewHolder(view)

    }

    // 뷰 홀더에 데이터 바인딩
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {


        val meat: Meat = meatList[position]
        val count = meat.meatNumber

        holder.textName.text = "${meat.meatMenu} / 수량 : $count"

    }

    // 아이템 개수 반환
    override fun getItemCount(): Int {
        return meatList.size
    }

    // 뷰 홀더 클래스
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.textName)
    }


}