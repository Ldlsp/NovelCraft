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
    fun anchors(projectId: Long): Flow<List<StoryAnchor>> = database.storyAnchorDao().observeByProject(projectId)
    fun edges(projectId: Long): Flow<List<StoryEdge>> = database.storyEdgeDao().observeByProject(projectId)

    suspend fun createProject(title: String, genre: String, premise: String): Long {
        val projectId = database.projectDao().insert(
            NovelProject(title = title.trim(), genre = genre.trim(), premise = premise.trim()),
        )
        database.chapterDao().insert(Chapter(projectId = projectId, number = 1, title = "开篇"))
        return projectId
    }

    suspend fun importProject(context: Context, uri: Uri, title: String): Long {
        val text = DocumentTextExtractor.read(context, uri)
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

    suspend fun updateProjectStyle(project: NovelProject, styleGuide: String) {
        database.projectDao().update(project.copy(styleGuide = styleGuide.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun renameChapter(chapter: Chapter, title: String) {
        val timestamp = System.currentTimeMillis()
        database.chapterDao().update(chapter.copy(title = title.trim().ifBlank { "第${chapter.number}章" }, updatedAt = timestamp))
        database.projectDao().touch(chapter.projectId, timestamp)
    }

    suspend fun updateChapterPlan(chapter: Chapter, outline: String, targetWordCount: Int) {
        val timestamp = System.currentTimeMillis()
        database.chapterDao().update(
            chapter.copy(outline = outline.trim(), targetWordCount = targetWordCount.coerceAtLeast(0), updatedAt = timestamp),
        )
        database.projectDao().touch(chapter.projectId, timestamp)
    }

    suspend fun updateChapterBeatSheet(chapter: Chapter, beatSheet: String) {
        val timestamp = System.currentTimeMillis()
        database.chapterDao().update(chapter.copy(beatSheet = beatSheet.trim(), updatedAt = timestamp))
        database.projectDao().touch(chapter.projectId, timestamp)
    }

    suspend fun addChapter(projectId: Long): Long {
        val number = (database.chapterDao().maxNumber(projectId) ?: 0) + 1
        val timestamp = System.currentTimeMillis()
        val id = database.chapterDao().insert(Chapter(projectId = projectId, number = number, title = "第${number}章", updatedAt = timestamp))
        database.projectDao().touch(projectId, timestamp)
        return id
    }

    suspend fun addStoryItem(projectId: Long, kind: String, name: String, detail: String, status: String): Long {
        return database.storyItemDao().insert(
            StoryItem(projectId = projectId, kind = kind, name = name, detail = detail, status = status),
        )
    }

    suspend fun updateStoryItem(item: StoryItem, kind: String, name: String, detail: String, status: String) {
        database.storyItemDao().update(item.copy(kind = kind, name = name, detail = detail, status = status, updatedAt = System.currentTimeMillis()))
    }

    suspend fun addAnchor(
        projectId: Long,
        startChapter: Int,
        endChapter: Int,
        title: String,
        coreConflict: String,
        allowedPlot: String,
        forbiddenReveals: String,
        mandatoryTension: String,
    ) {
        database.storyAnchorDao().insert(
            StoryAnchor(
                projectId = projectId,
                startChapter = startChapter.coerceAtLeast(1),
                endChapter = endChapter.coerceAtLeast(startChapter.coerceAtLeast(1)),
                title = title.trim(),
                coreConflict = coreConflict.trim(),
                allowedPlot = allowedPlot.trim(),
                forbiddenReveals = forbiddenReveals.trim(),
                mandatoryTension = mandatoryTension.trim(),
            ),
        )
    }

    suspend fun addEdge(projectId: Long, sourceItemId: Long, targetItemId: Long, relation: String, description: String, sinceChapter: Int): Long {
        require(sourceItemId != targetItemId) { "关系的两端不能是同一资料卡" }
        return database.storyEdgeDao().insert(
            StoryEdge(
                projectId = projectId,
                sourceItemId = sourceItemId,
                targetItemId = targetItemId,
                relation = relation.trim(),
                description = description.trim(),
                sinceChapter = sinceChapter.coerceAtLeast(1),
            ),
        )
    }
}
