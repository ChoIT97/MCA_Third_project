package com.chocho.mca_project_tablet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * [서빙 주문 ViewModel]
 *
 * Table1/Table2ActivityRepo 의 LiveData 를 서빙 화면에 전달한다.
 * (두 Repo 모두 같은 master 노드를 읽는다. table2Data 는 현재 사용되지 않음)
 */
class SubViewModel : ViewModel() {

    private val repo = Table1ActivityRepo()
    private val repo2 = Table2ActivityRepo()


    fun table1Data(): LiveData<MutableList<Meat>>{

        val mutableData = MutableLiveData<MutableList<Meat>>()

        repo.getData().observeForever{ mutableData.value = it }

        return mutableData
    }

    fun table2Data() : LiveData<MutableList<Meat>>{

        val mutableData = MutableLiveData<MutableList<Meat>>()

        repo2.getData().observeForever{ mutableData.value = it }

        return mutableData
    }

}