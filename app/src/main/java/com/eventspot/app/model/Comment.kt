package com.eventspot.app.model

// class representing a user comment on a book
class Comment() {

    var userId: String = ""       // ID of the user who posted the comment
    var username: String = ""     // Display name of the user
    var text: String = ""         // The comment content
    var timestamp: Long = 0L      // Time the comment was posted (in millis)

    // Secondary constructor for initializing all fields
    constructor(userId: String, username: String, text: String, timestamp: Long) : this() {
        this.userId = userId
        this.username = username
        this.text = text
        this.timestamp = timestamp
    }
}
