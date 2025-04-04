package com.example.beeconnect.models

data class Apiary(
    val name: String,
    val location: String,
    val latitude: String,
    val longitude: String,
    val imageRes: Int? = null,
    val id: String = ""
)
