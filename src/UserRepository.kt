package com.psachdev

class UserRepository {
    private val userMap: MutableMap<String, User> = HashMap()

    /**
     * Saves a new user
     * Replaces an existing user
     *
     * @param user
     */
    fun saveUser(user: User){
        userMap[user.id] = user
    }

    /**
     * Return an existing user if present
     * Return null for a missing id
     *
     * @param id
     * @return [User]
     */
    fun getUser(id: String): User?{
        return userMap[id]
    }

    /**
     * @param id
     * @return [Boolean]
     */
    fun isUserPresent(id: String): Boolean{
        return userMap.containsKey(id)
    }
}