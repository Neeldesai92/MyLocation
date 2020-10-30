package com.neel.desai.mylocation.model

import android.graphics.drawable.Drawable

class DashbordMenu {

    var name: String? = null
    var icon: Drawable? = null

    constructor(name: String?, icon: Drawable?) {
        this.name = name
        this.icon = icon
    }
}