package es.unizar.urlshortener.infrastructure.delivery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import org.springframework.web.socket.*
import org.springframework.web.socket.config.annotation.*
import org.springframework.web.socket.handler.TextWebSocketHandler

import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

import java.lang.Runtime
import kotlin.time.toDuration

class Data(val uptime: String, val redTime: String, val memUsed: String, val memFree: String)

@Controller
class Metrics {

	@GetMapping("/metrics")
	fun wsTest(): String {
		return "metrics"
	}

}

@Component @EnableScheduling
class WSHandler : TextWebSocketHandler() {

	val sessionList = ArrayList<WebSocketSession>()
	var startTime = System.currentTimeMillis()

	override fun afterConnectionClosed(session: WebSocketSession, staus: CloseStatus) {
		//println("Client disconected")
		sessionList -= session
	}

	public override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
		//println("Message received: " + message)
		if (!sessionList.contains(session)) {
			//println("Client connected")
			sessionList += session
		}
		sendMetrics(session)
	}

	@Scheduled(fixedRate = 1000)
	fun broadcastMetrics() {
		//println("Broadcasting to " + sessionList.size + " clients")
		sessionList.forEach {
			sendMetrics(it)
		}
	}

	fun sendMetrics(session: WebSocketSession) {
		var rt = Runtime.getRuntime()
		var uts = (System.currentTimeMillis()-startTime)/1000.0
		var ut = ((uts/86400)).toInt().toString() + " days, " + ((uts/3600)%24).toInt().toString() + " hours, " + ((uts/60)%60).toInt().toString() + " minutes and " + (uts%60).toInt().toString() + " seconds"
		var mu = ((rt.totalMemory()-rt.freeMemory())/1000000).toString() + "MB"
		var mf = (rt.freeMemory()/1000000).toString() + "MB"
		var data = jacksonObjectMapper().writeValueAsString(Data(ut, "TODO", mu, mf))
		println("Sending: " + data)
		session.sendMessage(TextMessage(data))
	}

}

@Configuration @EnableWebSocket
class WSConfig : WebSocketConfigurer {

	@Autowired
	lateinit var socketHandler: WSHandler

	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry.addHandler(socketHandler, "/ws").withSockJS()
	}

}
