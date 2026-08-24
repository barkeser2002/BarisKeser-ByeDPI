package com.bariskeser.byedpi.data

const val STARTED_BROADCAST = "com.bariskeser.byedpi.STARTED"
const val STOPPED_BROADCAST = "com.bariskeser.byedpi.STOPPED"
const val FAILED_BROADCAST = "com.bariskeser.byedpi.FAILED"

const val SENDER = "sender"

enum class Sender(val senderName: String) {
    Proxy("Proxy"),
    VPN("VPN")
}
