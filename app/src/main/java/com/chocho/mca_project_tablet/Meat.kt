package com.chocho.mca_project_tablet

/**
 * [주문 항목 데이터] - MCA_Restaurant 의 DataClassMeat 와 같은 구조
 *
 * @property meatMenu   메뉴 이름
 * @property meatNumber 수량
 * @property meatValue  합계 금액
 * Firebase 변환을 위해 모든 필드에 기본값을 둔다.
 */
data class Meat(

    val meatMenu: String = "오렌지 에이드",
    val meatNumber: Int = 1,
    val meatValue: Int ?= null

)


