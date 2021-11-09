package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

import java.lang.Runtime
import kotlin.time.toDuration

@Controller
class Metrics {

	var startTime = System.currentTimeMillis()

	@GetMapping("/metrics")
	fun metrics(model: MutableMap<String, Any>): String {
		var rt = Runtime.getRuntime()
		var uts = (System.currentTimeMillis()-startTime)/1000.0
		model["uptime"] = ((uts/3600)%24).toInt().toString() + ":" + ((uts/60)%60).toInt().toString() + ":" + (uts%60).toInt().toString()
		model["redTime"] = "TODO"
		model["memUsed"] = ((rt.totalMemory()-rt.freeMemory())/1000000).toString() + "MB"
		model["memFree"] = (rt.freeMemory()/1000000).toString() + "MB"
		return "metrics"
	}
}
