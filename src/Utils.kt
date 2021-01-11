package com.psachdev

suspend fun handleUserLogin(incomingUser: User,
                    service: UserService,
                    jwtManager: JwtManager,
                    respondWithToken: (String)-> Unit,
                    respondWithError: () -> Unit){

}