package com.mozhou.novelcraft

internal fun ModelConfig.hasTextGenerationConfiguration(): Boolean =
    baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
