package com.psachdev

enum class MessageType(private val messageType: String) {
    JOIN_ROOM("join_room"),
    LEAVE_ROOM("leave_room"),
    OFFER("offer"),
    ANSWER("answer"),
    CANDIDATE("candidate")
}