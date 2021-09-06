package com.yourapp.seetuorganizer.model

import java.io.Serializable

class SeetuModel : Serializable {
    var amount: Int? = null
    var beet : Int? = null
    var months: Int? = null
    var name: String? = null
    var year: Int? = null

    // Empty constructor needed for Firestore serialization
    constructor()

    constructor(amount: Int?, beet : Int, months: Int?, name : String?, year: Int?) {
        this.amount = amount
        this.beet = beet
        this.months = months
        this.name = name
        this.year = year
    }
}