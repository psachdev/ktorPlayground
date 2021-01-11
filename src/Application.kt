package com.psachdev

import com.google.gson.Gson
import com.psachdev.JwtManager.Companion.tokenClaimId
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import java.time.Duration
import java.util.*

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
        val websocketConnections = Collections.synchronizedList(mutableListOf<WebSocketServerSession>())
        authenticate("jwt_authentication") {
            webSocket("/signal_server") {
                websocketConnections.add(this)
                val gson = Gson()
                print("\n $call.request.origin.host")
                send(Frame.Text("Hi from server.\n " +
                        "There are ${websocketConnections.size} connected users"))
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
                                        val incomingStr = frame.readText()
                                        val incomingText = try{
                                            gson.fromJson(incomingStr, IncomingData::class.java)
                                        }catch (e: Exception){
                                            ""
                                        }
                                        println("Gson " + incomingText)
                                        websocketServerConnection.outgoing.send(Frame.Text("Client said: " + incomingStr))
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
                print("HERE 4")
                call.respond(HttpStatusCode.InternalServerError, serverError)
            }else{
                call.respondText(token)
            }

        }
    }
}

