package com.gorilla.music.data.model

data class RadioStation(
    val id: Long,
    val name: String,
    val streamUrl: String,
    val homepage: String,
    val favicon: String,
    val tags: List<String>,
    val country: String,
    val countryCode: String = "",
    val bitrate: Int,
    val codec: String,
    val hls: Boolean,
    val votes: Int,
    val clickCount: Int,
)
