package com.psachdev

interface UserService {
    /**
     * Saves a new user
     * Replaces an existing user
     *
     * @param user
     */
    fun saveUser(user: User)

    /**
     * Return an existing user if present
     * Return null for a missing id
     *
     * @param id
     * @return [User]
     */
    fun getUser(id: String): User?

    /**
     * @param id
     * @return [Boolean]
     */
    fun isUserPresent(id: String): Boolean
}

class UserServiceImpl(private val userRepository: UserRepository) : UserService{
    override fun saveUser(user: User) {
        userRepository.saveUser(user)
    }

    override fun getUser(id: String): User? {
        return userRepository.getUser(id)
    }

    override fun isUserPresent(id: String): Boolean {
        return userRepository.isUserPresent(id)
    }
}