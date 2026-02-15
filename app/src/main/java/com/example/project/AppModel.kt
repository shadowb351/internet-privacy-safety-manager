package com.example.project

import android.graphics.drawable.Drawable

data class AppModel(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isSelected: Boolean = false
)
