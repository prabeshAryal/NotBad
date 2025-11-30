package notbad.prabe.sh.core.model

/**
 * Enumeration of detected file types that determine the viewing/editing mode.
 * Detection is based on content analysis, MIME type, and file extension.
 */
enum class DetectedFileType {
    /**
     * Plain text file - displayed in text editor
     */
    TEXT,

    /**
     * Markdown file - displayed with Markdown preview option
     */
    MARKDOWN,

    /**
     * Source code file - displayed with syntax highlighting
     */
    SOURCE_CODE,

    /**
     * Binary file - displayed in hex viewer
     */
    BINARY,

    /**
     * File type could not be determined - defaults to binary view
     */
    UNKNOWN;

    /**
     * Whether this file type supports text editing
     */
    val isEditable: Boolean
        get() = this in listOf(TEXT, MARKDOWN, SOURCE_CODE)

    /**
     * Whether this file type should be displayed as hexadecimal
     */
    val isHexView: Boolean
        get() = this == BINARY || this == UNKNOWN

    /**
     * Whether this file type supports preview mode
     */
    val hasPreview: Boolean
        get() = this == MARKDOWN

    companion object {
        /**
         * File extensions that are definitively text-based
         */
        val TEXT_EXTENSIONS = setOf(
            "txt", "text", "log", "ini", "cfg", "conf", "config",
            "env", "properties", "gitignore", "gitattributes",
            "dockerignore", "editorconfig", "htaccess"
        )

        /**
         * File extensions that are Markdown
         */
        val MARKDOWN_EXTENSIONS = setOf(
            "md", "markdown", "mdown", "mkdn", "mkd", "mdwn", "mdtxt", "mdtext"
        )

        /**
         * File extensions for source code files
         */
        val SOURCE_CODE_EXTENSIONS = setOf(
            // JVM
            "java", "kt", "kts", "groovy", "gradle", "scala",
            // Web
            "js", "jsx", "ts", "tsx", "html", "htm", "css", "scss", "sass", "less",
            "json", "xml", "yaml", "yml", "toml",
            // Systems
            "c", "cpp", "cc", "cxx", "h", "hpp", "hxx",
            "rs", "go", "swift", "m", "mm",
            // Scripting
            "py", "rb", "php", "pl", "lua", "sh", "bash", "zsh", "fish",
            "ps1", "psm1", "bat", "cmd",
            // Data & Query
            "sql", "graphql", "gql",
            // Mobile
            "dart", "flutter",
            // Other
            "r", "R", "jl", "ex", "exs", "erl", "hrl", "clj", "cljs",
            "fs", "fsx", "vb", "cs", "asm", "s", "vue", "svelte"
        )

        /**
         * File extensions that are definitively binary
         */
        val BINARY_EXTENSIONS = setOf(
            // Executables
            "exe", "dll", "so", "dylib", "bin", "apk", "aab", "ipa",
            // Archives
            "zip", "tar", "gz", "bz2", "xz", "7z", "rar", "jar", "war", "ear",
            // Images
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "webp", "svg", "tiff", "psd",
            // Audio/Video
            "mp3", "wav", "ogg", "flac", "mp4", "avi", "mkv", "mov", "webm",
            // Documents (binary)
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods",
            // Database
            "db", "sqlite", "sqlite3", "mdb",
            // Other
            "class", "dex", "o", "obj", "pyc", "pyo"
        )

        /**
         * MIME types that indicate text content
         */
        val TEXT_MIME_TYPES = setOf(
            "text/plain",
            "text/html",
            "text/css",
            "text/javascript",
            "text/xml",
            "text/csv",
            "text/markdown"
        )

        /**
         * MIME type patterns that indicate text content
         */
        fun isTextMimeType(mimeType: String?): Boolean {
            if (mimeType == null) return false
            return mimeType.startsWith("text/") ||
                    mimeType == "application/json" ||
                    mimeType == "application/xml" ||
                    mimeType == "application/javascript" ||
                    mimeType == "application/x-sh" ||
                    mimeType == "application/x-python" ||
                    mimeType.contains("+xml") ||
                    mimeType.contains("+json")
        }
    }
}
