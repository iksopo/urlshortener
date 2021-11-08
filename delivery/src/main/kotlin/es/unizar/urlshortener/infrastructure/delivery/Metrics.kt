package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

import java.lang.Runtime

@Controller
class Metrics {
	@GetMapping("/metrics")
	fun metrics(model: MutableMap<String, Any>): String {
		var rt = Runtime.getRuntime()
		model["uptime"] = "TODO"
		model["redTime"] = "TODO"
		model["memUsed"] = ((rt.totalMemory()-rt.freeMemory())/1000000).toString() + "MB"
		model["memFree"] = (rt.freeMemory()/1000000).toString() + "MB"
		return "metrics"
	}
}
