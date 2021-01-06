package com.psachdev

class PreAuthorizedUsers {
    private val signalUsers = listOf(
        SignalUser("a1","alice"),
        SignalUser("a2","bob"))
        .associateBy(SignalUser::id)
    fun findUserById(id: String): SignalUser = signalUsers.getValue(id)
}