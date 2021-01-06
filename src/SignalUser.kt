package com.psachdev

import io.ktor.auth.Principal

data class SignalUser(val id: String, val username: String): Principal