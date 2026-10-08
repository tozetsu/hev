package ai.hev.app.domain.provider

/** Validation and link resolution for the http(s) URLs providers are configured with. */
object Endpoints {
    private const val LABEL = "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"

    /** RFC 3986 characters only, so unfilled `{placeholders}` and spaces are rejected. */
    private val HttpUrl = Regex(
        """https?://(?:[^\s/?#@\[\]]*@)?(?:\[[0-9a-f:.]+]|$LABEL(?:\.$LABEL)*\.?)(?::\d*)?""" +
            """(?:[/?#][^\s{}|\\^`"<>]*)?""",
        RegexOption.IGNORE_CASE,
    )

    /** RFC 3986 appendix B: scheme, authority, path, query, fragment. */
    private val Reference = Regex("""(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\?([^#]*))?(?:#(.*))?""")

    /** An absolute http(s) URL with a host. */
    fun isValid(url: String): Boolean = HttpUrl.matches(url.trim())

    /**
     * [reference] resolved against [base] the way a browser resolves a link (RFC 3986 §5.2),
     * or null when either side is unusable. An absolute [reference] is returned as is.
     */
    fun resolve(base: String, reference: String): String? {
        val ref = reference.trim()
        if (isValid(ref)) return ref
        if (!isValid(base)) return null
        val b = parse(base.trim())
        val r = parse(ref)
        val target = when {
            r.scheme != null -> r.copy(path = removeDotSegments(r.path))
            r.authority != null -> r.copy(scheme = b.scheme, path = removeDotSegments(r.path))
            r.path.isEmpty() -> b.copy(query = r.query ?: b.query, fragment = r.fragment)
            r.path.startsWith("/") -> b.copy(path = removeDotSegments(r.path), query = r.query, fragment = r.fragment)
            else -> b.copy(path = removeDotSegments(merge(b.path, r.path)), query = r.query, fragment = r.fragment)
        }
        return target.toString().takeIf(::isValid)
    }

    private data class Parts(
        val scheme: String?,
        val authority: String?,
        val path: String,
        val query: String?,
        val fragment: String?,
    ) {
        /** An http(s) URL with an authority always has a path; an empty one means `/`. */
        override fun toString() = buildString {
            scheme?.let { append(it).append(':') }
            authority?.let { append("//").append(it) }
            append(path.ifEmpty { if (authority != null) "/" else "" })
            query?.let { append('?').append(it) }
            fragment?.let { append('#').append(it) }
        }
    }

    private fun parse(url: String): Parts {
        val groups = Reference.matchEntire(url)!!.groups
        return Parts(groups[1]?.value, groups[2]?.value, groups[3]?.value.orEmpty(), groups[4]?.value, groups[5]?.value)
    }

    private fun merge(basePath: String, path: String): String = basePath.substringBeforeLast('/', "") + "/" + path

    private fun removeDotSegments(path: String): String {
        val segments = path.split('/')
        val output = ArrayList<String>()
        segments.drop(if (path.startsWith("/")) 1 else 0).forEach { segment ->
            when (segment) {
                "." -> Unit
                ".." -> output.removeLastOrNull()
                else -> output += segment
            }
        }
        if (segments.last() == "." || segments.last() == "..") output += ""
        return (if (path.startsWith("/")) "/" else "") + output.joinToString("/")
    }
}
