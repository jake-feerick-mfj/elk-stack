package io.mfj.uns

import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.http.BadRequestResponse
import io.mfj.uns.model.Event

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.Javalin
import io.mfj.uns.service.UnsService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UnsApiController {
    val logstashUrl = System.getenv("LOGSTASH_URL") ?: "http://logstash01:8080" // This is the HTTP input URL for Logstash, not monitoring (port 9600)
    val elkUrl = System.getenv("ELASTIC_URL") ?: "https://es01:9200"
    val elkUser = System.getenv("ELASTIC_USER") ?: "elastic"
    val elkPass = System.getenv("ELASTIC_PASSWORD") ?: "elk-mfj"

    val log: Logger = LoggerFactory.getLogger("UNS")
    val objectMapper = jacksonObjectMapper()
    val unsService = UnsService(elkUrl, logstashUrl, elkUser, elkPass)

    fun registerRoutes(app: Javalin) {

        // GET /events endpoint
        app.get("/events") { ctx ->
            try {
                val events = unsService.getAllEvents()
                ctx.json(events)
            } catch (e: Exception) {
                log.error("Failed to retrieve events: ${e.message}")
                ctx.status(500).json(mapOf("error" to "Failed to retrieve events"))
            }
        }

        // GET /events/{id} endpoint
        app.get("/events/{id}") { ctx ->
            val eventId = ctx.pathParam("id")
            try {
                val event = unsService.getEventById(eventId)
                if (event != null) {
                    ctx.json(event)
                } else {
                    ctx.status(404).json(mapOf("error" to "Event not found"))
                }
            } catch (e: Exception) {
                log.error("Failed to retrieve event with ID $eventId: ${e.message}")
                ctx.status(500).json(mapOf("error" to "Failed to retrieve event"))
            }
        }

        // POST /logs endpoint
        app.post("/events") { ctx ->
            val logEvent = try {
                objectMapper.readValue<Event>(ctx.body())
            } catch (e: Exception) {
                log.warn("Failed to parse log event: ${e.message}")
                throw BadRequestResponse("Malformed JSON or missing fields: ${e.message}")
            }
            try {
                unsService.createEvent(logEvent)
                ctx.json(logEvent)
            } catch (e: Exception) {
                log.error("Failed to create event: ${e.message}")
                ctx.status(500).json(mapOf("error" to "Failed to create event"))
                return@post
            }
        }
    }
}