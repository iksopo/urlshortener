package es.unizar.urlshortener.core

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

enum class ThreatType {
    THREAT_TYPE_UNSPECIFIED,
    MALWARE,
    SOCIAL_ENGINEERING,
    UNWANTED_SOFTWARE,
    POTENTIALLY_HARMFUL_APPLICATION
}

enum class PlatformType  {
    PLATFORM_TYPE_UNSPECIFIED,
    WINDOWS,
    LINUX,
    ANDROID,
    OSX,
    IOS,
    ANY_PLATFORM,
    ALL_PLATFORMS,
    CHROME
}

enum class ThreatEntryType   {
    THREAT_ENTRY_TYPE_UNSPECIFIED,
    URL,
    EXECUTABLE
}

enum class ThreatEntryRequestType   {
    HASH,
    URL,
    DIGEST
}

@JsonInclude(JsonInclude.Include.NON_NULL)
class ThreatEntry(value: String, type: ThreatEntryRequestType)    {
    @JsonProperty("hash")
    var hash: String? = null
    @JsonProperty("url")
    var url: String? = null
    @JsonProperty("digest")
    var digest: String? = null

    init {
        setOneValue(value, type)
    }

    private fun setOneValue(value: String, type: ThreatEntryRequestType){
        if (type==ThreatEntryRequestType.DIGEST){
            digest = value;
        }
        if (type==ThreatEntryRequestType.URL){
            url = value;
        }
        if (type==ThreatEntryRequestType.HASH){
            hash = value;
        }
        arrayOf<ThreatEntryRequestType>(ThreatEntryRequestType.HASH, ThreatEntryRequestType.HASH)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
class ThreatInfo (
    @JsonProperty("threatTypes")
    var threatTypes : List<ThreatType>? = null,
    @JsonProperty("platformTypes")
    var platformTypes: List<PlatformType>? = null,
    @JsonProperty("threatEntryTypes")
    var threatEntryTypes: List<ThreatEntryType>? = null,
    @JsonProperty("threatEntries")
    var threatEntries: List<ThreatEntry>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
class ClientInfo (
    @JsonProperty("clientId")
    var clientId : String? = null,
    @JsonProperty("clientVersion")
    var clientVersion: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
class ThreatMatchesFindRequest (
    @JsonProperty("client")
    var client : ClientInfo? = null,
    @JsonProperty("threatInfo")
    var threatInfo: ThreatInfo? = null
)