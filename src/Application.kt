package com.psachdev

import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.receive
import io.ktor.request.uri
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.time.*

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@KtorExperimentalAPI
@ExperimentalCoroutinesApi
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val jwtManager = JwtManager().also {
        it.init(environment)
    }
    val preAuthorizedUsers = PreAuthorizedUsers()

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(CallLogging)

    install(Authentication){
        jwt(name = "jwt_authentication") {
            println("HERE 3")
            verifier(jwtManager.verifier)
            validate { credential ->
                credential.payload.getClaim("id").asString()?.let(preAuthorizedUsers::findUserById)
            }
        }
    }

    intercept(ApplicationCallPipeline.Call) {
        if (call.request.uri == "/signal_server") {
            println("Token " + call.request.headers["Authorization"])
        }
    }
    routing {
        val websocketConnections = mutableListOf<WebSocketServerSession>()
        authenticate("jwt_authentication") {
            webSocket("/signal_server") {
                websocketConnections.add(this)
                print("\n $call.request.origin.host")
                send(Frame.Text("Hi from server"))
                while (true) {
                    if(incoming.isClosedForReceive){
                        websocketConnections.remove(this)
                        cancel()
                    }else {
                        val frame = incoming.receive()
                        if (frame is Frame.Text) {
                            for(websocketServerConnection in websocketConnections) {
                                if (!websocketServerConnection.outgoing.isClosedForSend) {
                                    try {
                                        websocketServerConnection.outgoing.send(Frame.Text("Client said: " + frame.readText()))
                                    }catch (e: ClosedReceiveChannelException){
                                        websocketConnections.remove(this)
                                        cancel()
                                    }
                                } else {
                                    websocketConnections.remove(this)
                                    cancel()
                                }
                            }
                        }
                    }
                }//end while
            }//end websocket
        }

        webSocket("/dummy"){
            send(Frame.Text("Hi from dummy"))
        }

        post("/login") {
            val signalUser = call.receive<SignalUser>()
            val user = preAuthorizedUsers.findUserById(signalUser.id)
            val token = jwtManager.makeToken(user)
            call.respondText(token)
        }

    }
}

