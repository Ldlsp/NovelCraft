package com.mozhou.novelcraft

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class NovelRepository(private val database: NovelDatabase) {
    fun projects(): Flow<List<NovelProject>> = database.projectDao().observeAll()
    fun project(projectId: Long): Flow<NovelProject?> = database.projectDao().observe(projectId)
    fun chapters(projectId: Long): Flow<List<Chapter>> = database.chapterDao().observeByProject(projectId)
    fun storyItems(projectId: Long): Flow<List<StoryItem>> = database.storyItemDao().observeByProject(projectId)

    suspend fun createProject(title: String, genre: String, premise: String): Long {
        val projectId = database.projectDao().insert(
            NovelProject(title = title.trim(), genre = genre.trim(), premise = premise.trim()),
        )
        database.chapterDao().insert(Chapter(projectId = projectId, number = 1, title = "开篇"))
        return projectId
    }

    suspend fun importProject(context: Context, uri: Uri, title: String): Long {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("无法读取导入文件")
        return database.withTransaction {
            val projectId = database.projectDao().insert(
                NovelProject(title = title.ifBlank { "导入作品" }, genre = "待分类", premise = "从已有正文导入"),
            )
            database.chapterDao().insertAll(
                ChapterImporter.parse(text).map {
                    Chapter(projectId = projectId, number = it.number, title = it.title, content = it.content)
                },
            )
            projectId
        }
    }

    suspend fun updateChapter(chapter: Chapter, content: String) {
        database.withTransaction {
            val timestamp = System.currentTimeMillis()
            database.chapterDao().update(chapter.copy(content = content, updatedAt = timestamp))
            database.projectDao().touch(chapter.projectId, timestamp)
        }
    }

    suspend fun addStoryItem(projectId: Long, kind: String, name: String, detail: String) {
        database.storyItemDao().insert(
            StoryItem(projectId = projectId, kind = kind, name = name, detail = detail),
        )
    }
}
