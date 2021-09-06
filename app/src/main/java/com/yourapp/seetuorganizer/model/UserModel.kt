package com.yourapp.seetuorganizer.model

import java.io.Serializable

class UserModel : Serializable{
    var locality: String? = null
    var name : String? = null
    var pending: Int? = null
    var phone: String? = null

    // Empty constructor needed for Firestore serialization
    constructor()

    constructor(locality : String?, name : String?, pending : Int?, phone : String?) {
        this.locality = locality
        this.name = name
        this.pending = pending
        this.phone = phone
    }
}