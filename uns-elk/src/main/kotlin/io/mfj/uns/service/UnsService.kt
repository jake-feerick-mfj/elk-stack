package io.mfj.uns.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.http.BadRequestResponse
import io.mfj.uns.model.Event
import java.util.UUID
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory.getLogger

class UnsService(
    private val elasticUrl: String,
    private val logstashUrl: String,
    private val username: String,
    private val password: String
) {
    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()
    private val index = "logstash-*"
    private val log = getLogger(UnsService::class.java)

    fun getAllEvents(): List<Map<String, Any>> {
        val url = "$elasticUrl/$index/_search"
        // Could configure size differently
        val reqBody = """{"size":5000,"query":{"match_all":{}}}"""
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(reqBody)
            .addHeader("Authorization", Credentials.basic(username, password))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw Exception("Failed to query Elasticsearch: ${resp.code}")
            }
            val json = mapper.readTree(resp.body!!.string())
            return json["hits"]["hits"].map { it["_source"] }.map { mapper.convertValue(it, Map::class.java) as Map<String,Any> }
        }
    }

    fun getEventById(eventId: String): Event? {
        val url = "$elasticUrl/$index/_search"
        log.info("Querying for event with ID: $eventId")
        val reqBody = """
        {
            "query": { "term": { "id.keyword": "$eventId" } }
        }
    """.trimIndent().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(reqBody)
            .addHeader("Authorization", Credentials.basic(username, password))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("Failed to query ES: ${resp.code}")
            val json = mapper.readTree(resp.body!!.string())
            val hits = json["hits"]["hits"]
            if (hits.isEmpty()) return null
            return mapper.treeToValue(hits[0]["_source"], Event::class.java)
        }
    }

    fun createEvent(event: Event): Event {
        // Validate fields
        log.info("Sending log")
        val payload = event.payload
        log.info("Payload: $payload")
        if (!payload.containsKey("email") || !payload.containsKey("slack")) {
            throw BadRequestResponse("Payload must contain both 'email' and 'slack' fields.")
        }
        // Currently manually setting ideas to write logs directly to logstash, could consider
        // using elastic ID instead
        event.id = UUID.randomUUID().toString()
        log.info("Generated event: $event")
        val eventBody = mapper.writeValueAsString(event)
        // Forward to Logstash (assuming HTTP input plugin is enabled; otherwise, adapt as needed)
        val req = Request.Builder()
            .url("$logstashUrl")
            .post(eventBody.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            log.info("Processing response")
            if (!resp.isSuccessful) {
                log.error("Failed to forward log to logstash: ${resp.code} ${resp.body?.string()}")

            }
            log.info("Logstash response: ${resp.code} ${resp.body?.string()}")
        }
        return event
    }
}