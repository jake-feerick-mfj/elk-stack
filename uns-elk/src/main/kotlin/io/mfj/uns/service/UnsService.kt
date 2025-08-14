package io.mfj.uns.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.mfj.uns.model.Event
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

    fun createEvent(logEvent: Event, ctx: Context) {
        // Validate fields
        log.info("Sending log")
        val payload = logEvent.payload
        log.info("Payload: $payload")
        if (!payload.containsKey("email") || !payload.containsKey("slack")) {
            throw BadRequestResponse("Payload must contain both 'email' and 'slack' fields.")
        }
        // Forward to Logstash (assuming HTTP input plugin is enabled; otherwise, adapt as needed)
        val req = Request.Builder()
            .url("$logstashUrl")
            .post(ctx.body().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            log.info("Processing response")
            if (!resp.isSuccessful) {
                log.error("Failed to forward log to logstash: ${resp.code} ${resp.body?.string()}")
                ctx.status(502).result("Failed to forward log to logstash")

            }
            log.info("Logstash response: ${resp.code} ${resp.body?.string()}")
        }
        ctx.status(201).json(mapOf("message" to "Log accepted"))
    }
}