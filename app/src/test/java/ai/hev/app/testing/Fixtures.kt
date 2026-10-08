package ai.hev.app.testing

/** Reads JSON fixtures from `src/test/resources/fixtures/`. */
object Fixtures {
    fun read(path: String): String {
        val resource = requireNotNull(javaClass.classLoader?.getResource("fixtures/$path")) {
            "Missing fixture: $path"
        }
        return resource.readText()
    }
}
