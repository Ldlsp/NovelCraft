package com.mozhou.novelcraft

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.IOException

class ImportAnalysisWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val projectId = inputData.getLong(PROJECT_ID, 0L)
        if (projectId == 0L) return Result.failure()

        val database = NovelDatabase.create(applicationContext)
        val analysisDao = database.importAnalysisDao()
        val project = database.projectDao().findById(projectId) ?: return Result.failure()
        val config = ModelPreferences(applicationContext).load()
        if (config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            updateStatus(analysisDao, projectId, ImportAnalysisStatus.WAITING_FOR_CONFIG, "等待模型配置", 0, "请在“我的”中完成云端文本模型配置")
            return Result.success()
        }

        return try {
            setForeground(foregroundInfo(project.title, "正在准备导入分析"))
            val chapters = database.chapterDao().listByProject(projectId)
            updateStatus(analysisDao, projectId, ImportAnalysisStatus.RUNNING, "整理正文样本", 12, "正在读取 ${chapters.size} 章并建立分析样本", project.title)
            val samples = (chapters.take(3) + chapters.takeLast(2))
                .distinctBy { it.id }
                .joinToString("\n\n") { chapter -> "【第${chapter.number}章 ${chapter.title}】\n${chapter.content.take(2_000)}" }
                .take(9_000)
            require(samples.isNotBlank()) { "导入正文为空，无法分析" }

            updateStatus(analysisDao, projectId, ImportAnalysisStatus.RUNNING, "提炼作品资料", 38, "AI 正在识别题材、简介、标签和主角", project.title)
            val profileContext = buildString {
                appendLine("书名：${project.title}")
                appendLine("已导入章节数：${chapters.size}")
                appendLine("以下为来自开篇、中段或结尾的正文样本，请仅据此提炼资料：")
                append(samples)
            }
            val profile = parseProfile(OpenAiCompatibleClient().generateProjectProfile(config, profileContext).getOrThrow())
            val updatedProject = database.projectDao().findById(projectId) ?: return Result.failure()
            database.projectDao().update(
                updatedProject.copy(
                    genre = profile.genre.ifBlank { "待分类" },
                    premise = profile.premise,
                    summary = profile.summary,
                    tags = profile.tags,
                    targetAudience = profile.targetAudience,
                    protagonistName = profile.protagonistName,
                    updatedAt = System.currentTimeMillis(),
                ),
            )

            updateStatus(analysisDao, projectId, ImportAnalysisStatus.RUNNING, "归纳文风", 76, "AI 正在提取叙事节奏、语言和视角特征", project.title)
            val styleGuide = OpenAiCompatibleClient().extractStyleGuide(config, samples.take(8_000)).getOrThrow()
            val latestProject = database.projectDao().findById(projectId) ?: return Result.failure()
            database.projectDao().update(latestProject.copy(styleGuide = styleGuide, updatedAt = System.currentTimeMillis()))
            updateStatus(analysisDao, projectId, ImportAnalysisStatus.COMPLETED, "分析完成", 100, "作品资料、文风和本地索引均已就绪", project.title)
            Result.success()
        } catch (cancelled: CancellationException) {
            updateStatus(analysisDao, projectId, ImportAnalysisStatus.CANCELLED, "已取消", 0, "导入分析已取消")
            throw cancelled
        } catch (networkError: IOException) {
            val failure = ImportAnalysisFailureClassifier.classify(networkError, hasValidatedNetwork())
            updateStatus(analysisDao, projectId, failure.status, failure.stage, 0, failure.detail, project.title)
            if (failure.shouldRetry) Result.retry() else Result.failure()
        } catch (error: Exception) {
            updateStatus(analysisDao, projectId, ImportAnalysisStatus.FAILED, "分析失败", 0, error.message ?: "模型返回异常", project.title)
            Result.failure()
        }
    }

    private suspend fun updateStatus(
        dao: ImportAnalysisDao,
        projectId: Long,
        status: String,
        stage: String,
        progress: Int,
        detail: String,
        title: String = "",
    ) {
        dao.upsert(ImportAnalysisRun(projectId, status, stage, progress.coerceIn(0, 100), detail))
        setProgress(workDataOf("progress" to progress, "stage" to stage))
        if (title.isNotBlank()) notificationManager().notify(notificationId, notification(title, "$stage · $progress%"))
    }

    private fun foregroundInfo(title: String, detail: String): ForegroundInfo {
        val notification = notification(title, detail)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun notification(title: String, detail: String): android.app.Notification {
        notificationManager()
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("墨舟正在分析《$title》")
            .setContentText(detail)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun notificationManager(): NotificationManager = applicationContext.getSystemService(NotificationManager::class.java).also { manager ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "导入分析", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun hasValidatedNetwork(): Boolean {
        val connectivity = applicationContext.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private val notificationId: Int get() = NOTIFICATION_ID + id.hashCode()

    private fun parseProfile(raw: String): ProjectProfileSuggestion {
        val json = JSONObject(raw.trim().removePrefix("```json").removeSuffix("```").trim())
        return ProjectProfileSuggestion(
            title = json.optString("title").trim(),
            genre = json.optString("genre").trim(),
            premise = json.optString("premise").trim(),
            summary = json.optString("summary").trim(),
            tags = json.optString("tags").trim(),
            targetAudience = json.optString("targetAudience").trim(),
            protagonistName = json.optString("protagonistName").trim(),
            conflict = json.optString("conflict").trim(),
            promise = json.optString("promise").trim(),
            writingStyle = json.optString("writingStyle").trim(),
            forbiddenContent = json.optString("forbiddenContent").trim(),
        )
    }

    companion object {
        const val PROJECT_ID = "project_id"
        private const val CHANNEL_ID = "import_analysis"
        private const val NOTIFICATION_ID = 9_200
    }
}

object ImportAnalysisScheduler {
    fun enqueue(context: Context, projectId: Long) {
        val request = OneTimeWorkRequestBuilder<ImportAnalysisWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(ImportAnalysisWorker.PROJECT_ID to projectId))
            .addTag("import-analysis-$projectId")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("import-analysis-$projectId", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, projectId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("import-analysis-$projectId")
    }
}
