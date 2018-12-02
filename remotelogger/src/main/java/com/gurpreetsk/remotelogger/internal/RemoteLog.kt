package com.gurpreetsk.remotelogger.internal

data class RemoteLog(
    val id: Int,
    val userUUID: String,
    val timestamp: Long,
    val osName: String,
    val osVersion: String,
    val appVersion: String,
    val logTag: String,
    val logLevel: String,
    val message: String,
    val stackTrace: String
)
