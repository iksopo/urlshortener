package es.unizar.urlshortener.core.usecases

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.unizar.urlshortener.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.web.client.RestTemplate
import java.net.URI


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
    suspend fun ValidateURI(uri: String):ValidateURIUseCaseResponse
    suspend fun ReachableURI(uri: String):ValidateURIUseCaseResponse
    suspend fun SafeURI(uri: String):ValidateURIUseCaseResponse
}

class ValidateURIUseCaseImpl(
) : ValidateURIUseCase {

    @Autowired
    lateinit var restTemplate: RestTemplate

    override suspend fun ValidateURI(uri: String): ValidateURIUseCaseResponse = coroutineScope  {
        val resp1 = async { SafeURI(uri) }
        val resp2 = async { ReachableURI(uri) }
        if(resp1.await() == ValidateURIUseCaseResponse.OK) resp2.await() else resp1.await()
    }

    override suspend fun ReachableURI(uri: String): ValidateURIUseCaseResponse {
        return try {
            val response = restTemplate.getForEntity(uri, String::class.java)
            println("Reachable")
            if (response.statusCode.is2xxSuccessful) ValidateURIUseCaseResponse.OK else ValidateURIUseCaseResponse.NOT_REACHABLE
        }catch (ex: Exception){
            ValidateURIUseCaseResponse.NOT_REACHABLE
        }

    }

    override suspend fun SafeURI(uri: String): ValidateURIUseCaseResponse {
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
        delay(10000)
        if(!httpResponse?.matches.isNullOrEmpty()){
            println("Unsafe")
            return ValidateURIUseCaseResponse.UNSAFE
        }

        println("Safe")


        return ValidateURIUseCaseResponse.OK
    }
}

