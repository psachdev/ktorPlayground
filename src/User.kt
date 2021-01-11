package com.psachdev

import io.ktor.auth.Principal

data class User(val id: String, val username: String): Principal