package es.unizar.urlshortener.infrastructure.delivery

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class Metrics {
	@GetMapping("/metrics")
	fun metrics(model: MutableMap<String, Any>): String {
		model["uptime"] = 1
		model["redTime"] = 2
		model["memUsed"] = 3
		model["memFree"] = 4
		return "metrics"
	}
}
