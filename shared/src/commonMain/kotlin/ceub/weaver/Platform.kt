package ceub.weaver

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform