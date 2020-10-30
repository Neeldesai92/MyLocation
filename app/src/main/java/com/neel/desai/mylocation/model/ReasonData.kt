package com.neel.desai.mylocation.model

class ReasonData {

    var Reason: String? = null
    var isSelected = false

    constructor(Reason: String?, isSelected: Boolean) {
        this.Reason = Reason
        this.isSelected = isSelected
    }
}