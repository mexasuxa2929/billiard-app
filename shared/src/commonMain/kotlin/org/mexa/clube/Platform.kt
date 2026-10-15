package org.mexa.clube

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform