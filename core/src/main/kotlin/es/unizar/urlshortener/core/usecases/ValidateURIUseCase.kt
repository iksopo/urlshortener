package es.unizar.urlshortener.core.usecases

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.unizar.urlshortener.core.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.*


enum class ValidateURIUseCaseResponse {
    OK,
    NOT_REACHABLE,
    UNSAFE
}

@Value("\${google.API.value}")
const val APIKEY : String = "AIzaSyBS26eLBuGZEmscRx9AmUVG8O_YaiwgDu0"
const val FINDURL : String = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=$APIKEY"
const val CLIENTNAME : String = "URIShortenerTestApp"
const val CLIENTVERSION : String = "0.1"

interface ValidateURIUseCase {
    fun ValidateURI(uri: String):ValidateURIUseCaseResponse
    fun ReachableURI(uri: String):ValidateURIUseCaseResponse
    fun SafeURI(uri: String):ValidateURIUseCaseResponse
}

class ValidateURIUseCaseImpl(
) : ValidateURIUseCase {

    @Autowired
    lateinit var restTemplate: RestTemplate

    override fun ValidateURI(uri: String): ValidateURIUseCaseResponse {
        val resp1 = SafeURI(uri)
        return if(resp1 == ValidateURIUseCaseResponse.OK) ReachableURI(uri) else resp1
    }

    override fun ReachableURI(uri: String): ValidateURIUseCaseResponse {
        return try {
            val response = restTemplate.getForEntity(uri, String::class.java)
            if (response.statusCode.is2xxSuccessful) ValidateURIUseCaseResponse.OK else ValidateURIUseCaseResponse.NOT_REACHABLE
        }catch (ex: Exception){
            ValidateURIUseCaseResponse.NOT_REACHABLE
        }

    }

    override fun SafeURI(uri: String): ValidateURIUseCaseResponse {
        val mapper = jacksonObjectMapper()

        val safeRequest = ThreatMatchesFindRequest(
            ClientInfo(CLIENTNAME, CLIENTVERSION),
            ThreatInfo(
                listOf(ThreatType.MALWARE,ThreatType.POTENTIALLY_HARMFUL_APPLICATION,ThreatType.UNWANTED_SOFTWARE),
                listOf(PlatformType.WINDOWS),
                listOf(ThreatEntryType.URL),
                listOf(ThreatEntry(uri,ThreatEntryRequestType.URL))
            )
        )
        val serialized = mapper.writeValueAsString(safeRequest)
        val httpResponse = restTemplate.postForObject(URI(FINDURL), HttpEntity(serialized),ThreatMatchesFindResponse::class.java)
        if(!httpResponse?.matches.isNullOrEmpty()){
            return ValidateURIUseCaseResponse.UNSAFE
        }

        return ValidateURIUseCaseResponse.OK
    }
}

