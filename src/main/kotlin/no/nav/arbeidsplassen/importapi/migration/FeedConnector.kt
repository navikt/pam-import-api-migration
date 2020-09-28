package no.nav.arbeidsplassen.importapi.migration

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.uri.UriTemplate
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FeedConnector(val objectMapper: ObjectMapper,
                    @Client("/") val client: RxHttpClient,
                    @Value("\${feed.pagesize}") val pageSize: Int = 200) {

    companion object {
        private val LOG = LoggerFactory.getLogger(FeedConnector::class.java)
    }

    @Throws(IOException::class)
    private fun <T> fetchWithURI(uri: String, type: Class<T>): List<T> {
        val items: MutableList<T> = mutableListOf()
        val json: String = retrieve(uri)
        val javaType: JavaType = objectMapper.typeFactory.constructParametricType(FeedTransport::class.java, type)
        val feedPage: FeedTransport<T> = objectMapper.readValue(json,javaType)
        items.addAll(feedPage.content)
        return items
    }

    private fun retrieve(uri: String): String {
        LOG.info("Connecting to feed: $uri")
        val json: String = client
                .retrieve(uri)
                .blockingFirst()
        return json
    }

    private fun buildURI(uri: String, updated: LocalDateTime): String {
        val template = if (uri.indexOf("?")>0) "$uri{&updated,size,sort}" else "$uri{?updated,size,sort}"
        val arguments: MutableMap<String, Any> = HashMap()
        arguments["size"] = pageSize
        arguments["updated"] = updated
        arguments["sort"] = "updated,asc"
        val uriTemplate = UriTemplate(template)
        return uriTemplate.expand(arguments)
    }

    @Throws(IOException::class)
    fun <T> fetchContentList(uri: String, updatedSince: LocalDateTime, type: Class<T>): List<T> {
        return fetchWithURI(buildURI(uri, updatedSince), type)
    }


}
