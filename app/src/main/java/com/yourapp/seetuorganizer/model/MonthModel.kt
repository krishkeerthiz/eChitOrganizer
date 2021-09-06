package com.yourapp.seetuorganizer.model

import java.io.Serializable

class MonthModel : Serializable {
    var month: Int? = null
    var pending : Int? = null
    var toPay: Int? = null
    var yelam: Int? = null

    // Empty constructor needed for Firestore serialization
    constructor()

    constructor(month: Int?, pending : Int, toPay: Int?, yelam: Int?) {
        this.month = month
        this.pending = pending
        this.toPay = toPay
        this.yelam = yelam
    }
}