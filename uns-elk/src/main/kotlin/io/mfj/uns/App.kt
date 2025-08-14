package io.mfj.uns

import io.javalin.Javalin

fun main() {
    val app = Javalin.create().start(8000)
    val controller = UnsApiController()
    controller.registerRoutes(app)
}