package com.mozhou.novelcraft

enum class GenerationTask(val label: String) {
    CONTINUATION("续写"),
    AUTO_WRITE("批量写作"),
    CHAPTER_PLAN("本章计划"),
    BEAT_SHEET("场景分镜"),
    STYLE_GUIDE("文风提取"),
    MEMORY_EXTRACTION("知识图谱"),
    REPAIR_PLAN("修复计划"),
}

class GenerationRequest {
    @Volatile
    var connection: java.net.HttpURLConnection? = null

    fun cancel() {
        connection?.disconnect()
    }
}
