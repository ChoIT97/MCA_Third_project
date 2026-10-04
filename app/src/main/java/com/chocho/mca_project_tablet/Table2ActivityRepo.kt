package com.chocho.mca_project_tablet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


/**
 * [주문 데이터 저장소]
 *
 * Firebase "master" 노드(결제가 끝난 주문 목록)를 실시간 구독해서 Meat 목록 LiveData 로 내보낸다.
 * master 는 MCA_Restaurant 앱의 PaymentListActivity 가 결제 승인 후 저장한다.
 */
class Table2ActivityRepo {

    fun getData(): LiveData<MutableList<Meat>> {

        val mutableData = MutableLiveData<MutableList<Meat>>()
        val database = Firebase.database
        val myRef = database.getReference("master")

        myRef.addValueEventListener(object : ValueEventListener {
            val listData: MutableList<Meat> = mutableListOf()

            override fun onDataChange(snapshot: DataSnapshot) {

                listData.clear()

                if (snapshot.exists()) {

                    for (meatSnapshot in snapshot.children) {

                        val getData = meatSnapshot.getValue(Meat::class.java)

                        listData.add(getData!!)

                        mutableData.value = listData

                    }
                } else {

                    listData.clear()

                    mutableData.value = listData

                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
        return mutableData
    }
}