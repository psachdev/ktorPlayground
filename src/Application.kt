package com.psachdev

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.time.*

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@ExperimentalCoroutinesApi
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD 2!", contentType = ContentType.Text.Plain)
        }

        val websocketConnections = mutableListOf<WebSocketServerSession>()
        webSocket("/myws/echo") {
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
}

