package com.sential.discordbubbles

import net.dv8tion.jda.api.entities.User

data class Message(val author: User, val body: String, val channel: String)
