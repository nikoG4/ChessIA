package org.nko.chessia

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform