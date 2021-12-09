package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.annotation.Async

@Component @EnableScheduling
class Metrics {

	var uptime: Double = 0.0
	var redTime: Double = 0.0
	var memUsed: Long = 0
	var memFree: Long = 0

	val startTime = System.currentTimeMillis()
	val runTime = Runtime.getRuntime()


	@Async
	@Scheduled(fixedRate = 10000)
	fun GetMetrics() {
		uptime = (System.currentTimeMillis()-startTime)/1000.0
		memFree = runTime.freeMemory()
		memUsed = runTime.totalMemory()-memFree
		println("### METRICS ###")
		println("Uptime: " + uptime + "s")
		println("Memory Used: " + memUsed/1000000 + "MB")
		println("Memory Free: " + memFree/1000000 + "MB")
		println("Redirection Time: " + redTime + "s")
		println("###############")
	}
}


