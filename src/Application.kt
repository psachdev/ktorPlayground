package com.psachdev

import com.psachdev.JwtManager.Companion.tokenClaimId
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.timeout
import io.ktor.request.receive
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.websocket.WebSocketServerSession
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@KtorExperimentalAPI
@ExperimentalCoroutinesApi
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val jwtManager = JwtManager().also {
        it.init(environment)
    }

    install(Koin) {
        modules(userAppModule)
    }
    val service by inject<UserService>()

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
            verifier(jwtManager.verifier)
            validate { credential ->
                credential.payload.getClaim(tokenClaimId).asString()?.let(service::getUser)
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

        val serverError = "Server Error"
        post("/login") {
            val incomingUser = call.receive<User>()
            val chatUser = if(service.isUserPresent(incomingUser.id)){
                service.getUser(incomingUser.id)
            }else{
                service.saveUser(incomingUser)
                incomingUser
            }

            val token = if(chatUser != null){
                jwtManager.makeToken(chatUser)
            }else{
                null
            }

            if(token == null){
                call.respond(HttpStatusCode.InternalServerError, serverError)
            }else{
                call.respondText(token)
            }
        }

    }
}

