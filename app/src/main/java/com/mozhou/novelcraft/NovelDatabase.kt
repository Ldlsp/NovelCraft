package com.mozhou.novelcraft

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class NovelProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val genre: String,
    val premise: String,
    @ColumnInfo(defaultValue = "''") val styleGuide: String = "",
    @ColumnInfo(defaultValue = "''") val outlineRevisionReport: String = "",
    @ColumnInfo(defaultValue = "''") val summary: String = "",
    @ColumnInfo(defaultValue = "''") val tags: String = "",
    @ColumnInfo(defaultValue = "''") val targetAudience: String = "",
    @ColumnInfo(defaultValue = "''") val protagonistName: String = "",
    @ColumnInfo(defaultValue = "''") val longFormBlueprint: String = "",
    @ColumnInfo(defaultValue = "0") val targetChapterCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val targetWordCount: Int = 0,
    @ColumnInfo(defaultValue = "'均衡'") val pacingProfile: String = "均衡",
    @ColumnInfo(defaultValue = "''") val forbiddenContent: String = "",
    @ColumnInfo(defaultValue = "'半自动'") val automationLevel: String = "半自动",
    @ColumnInfo(defaultValue = "3000") val targetChapterWordCount: Int = 3000,
    @ColumnInfo(defaultValue = "5000") val targetChapterWordCountMax: Int = 5000,
    @ColumnInfo(defaultValue = "''") val coverPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = NovelProject::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId"), Index("autoWriteRunId")],
)
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val number: Int,
    val title: String,
    val content: String = "",
    @ColumnInfo(defaultValue = "''") val outline: String = "",
    @ColumnInfo(defaultValue = "''") val beatSheet: String = "",
    @ColumnInfo(defaultValue = "0") val targetWordCount: Int = 0,
    @ColumnInfo(defaultValue = "'ready'") val qualityStatus: String = ChapterQualityStatus.READY,
    @ColumnInfo(defaultValue = "''") val qualityIssueSummary: String = "",
    @ColumnInfo(defaultValue = "'manual'") val lifecycleStatus: String = ChapterLifecycleStatus.MANUAL,
    @ColumnInfo(defaultValue = "''") val lifecycleDetail: String = "",
    @ColumnInfo(defaultValue = "0") val memoryUpdatedAt: Long = 0,
    @ColumnInfo(defaultValue = "0") val autoWriteRunId: Long = 0,
    @ColumnInfo(defaultValue = "0") val gateFailureCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val requiresHumanReview: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "chapter_revisions",
    foreignKeys = [
        ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Chapter::class, parentColumns = ["id"], childColumns = ["chapterId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("projectId"), Index("chapterId")],
)
data class ChapterRevision(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val chapterId: Long,
    val previousContent: String,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "auto_write_runs",
    foreignKeys = [ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class AutoWriteRun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val requestedCount: Int,
    val completedCount: Int = 0,
    val status: String = AutoWriteRunStatus.RUNNING,
    val detail: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "import_analysis_runs",
    foreignKeys = [ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
)
data class ImportAnalysisRun(
    @PrimaryKey val projectId: Long,
    val status: String = ImportAnalysisStatus.QUEUED,
    val stage: String = "等待开始",
    val progress: Int = 0,
    val detail: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

object ImportAnalysisStatus {
    const val QUEUED = "queued"
    const val RUNNING = "running"
    const val WAITING_FOR_CONFIG = "waiting_for_config"
    const val WAITING_FOR_NETWORK = "waiting_for_network"
    const val COMPLETED = "completed"
    const val FAILED = "failed"
    const val CANCELLED = "cancelled"
}

@Entity(
    tableName = "chapter_story_mentions",
    foreignKeys = [
        ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Chapter::class, parentColumns = ["id"], childColumns = ["chapterId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StoryItem::class, parentColumns = ["id"], childColumns = ["storyItemId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("projectId"), Index("chapterId"), Index("storyItemId")],
)
data class ChapterStoryMention(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val chapterId: Long,
    val storyItemId: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "research_notes",
    foreignKeys = [ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class ResearchNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val sourceUrl: String = "",
    val tags: String = "",
    val content: String,
    @ColumnInfo(defaultValue = "0") val rightsConfirmed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "editorial_reviews",
    foreignKeys = [
        ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Chapter::class, parentColumns = ["id"], childColumns = ["chapterId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("projectId"), Index("chapterId")],
)
data class EditorialReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val chapterId: Long,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A resumable, mandatory new-book brief before it becomes a project. */
@Entity(tableName = "ideation_drafts")
data class IdeationDraft(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val step: Int = 1,
    val title: String = "",
    val genre: String = "",
    val premise: String = "",
    val protagonist: String = "",
    val conflict: String = "",
    val promise: String = "",
    @ColumnInfo(defaultValue = "''") val targetAudience: String = "",
    @ColumnInfo(defaultValue = "''") val writingStyle: String = "",
    @ColumnInfo(defaultValue = "''") val forbiddenContent: String = "",
    @ColumnInfo(defaultValue = "'半自动'") val automationLevel: String = "半自动",
    @ColumnInfo(defaultValue = "3000") val targetChapterWordCount: Int = 3000,
    @ColumnInfo(defaultValue = "5000") val targetChapterWordCountMax: Int = 5000,
    @ColumnInfo(defaultValue = "0") val targetWordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "chapter_pacing_events",
    foreignKeys = [
        ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Chapter::class, parentColumns = ["id"], childColumns = ["chapterId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("projectId"), Index("chapterId")],
)
data class ChapterPacingEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val chapterId: Long,
    val chapterNumber: Int,
    val eventType: String,
    val pace: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "event_matrix_rules",
    foreignKeys = [ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class EventMatrixRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val ruleKey: String,
    val label: String,
    val cooldown: Int,
    val category: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "chapter_gate_reports",
    foreignKeys = [
        ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Chapter::class, parentColumns = ["id"], childColumns = ["chapterId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("projectId"), Index("chapterId"), Index("stage")],
)
data class ChapterGateReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val chapterId: Long,
    val stage: String,
    val passed: Boolean,
    val content: String,
    val contextSnapshot: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "style_profiles", indices = [Index("genre"), Index("sourceProjectId")])
data class StyleProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val genre: String = "",
    val guide: String,
    val sourceProjectId: Long = 0,
    @ColumnInfo(defaultValue = "''") val metrics: String = "",
    @ColumnInfo(defaultValue = "''") val keywords: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "batch_review_runs",
    foreignKeys = [ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class BatchReviewRun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val startChapter: Int,
    val endChapter: Int,
    val round: Int = 1,
    val report: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "review_issues",
    foreignKeys = [
        ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = BatchReviewRun::class, parentColumns = ["id"], childColumns = ["reviewRunId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("projectId"), Index("reviewRunId"), Index("status")],
)
data class ReviewIssue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = 0,
    val reviewRunId: Long = 0,
    val chapterNumber: Int = 0,
    val severity: String,
    val summary: String,
    val status: String = "open",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "rag_chunks", indices = [Index("projectId"), Index("chapterId")])
data class RagChunk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val chapterId: Long,
    val chapterNumber: Int,
    val ordinal: Int,
    val content: String,
    val terms: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "chapter_continuity_snapshots",
    foreignKeys = [
        ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Chapter::class, parentColumns = ["id"], childColumns = ["chapterId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("projectId"), Index("predecessorChapterId")],
)
data class ChapterContinuitySnapshot(
    @PrimaryKey val chapterId: Long,
    val projectId: Long,
    val predecessorChapterId: Long = 0,
    val predecessorTail: String = "",
    val contextPrompt: String,
    val confirmationStatus: String = ContinuitySnapshotStatus.CONFIRMED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

object ContinuitySnapshotStatus {
    const val PENDING = "pending"
    const val CONFIRMED = "confirmed"
}

@Entity(
    tableName = "chapter_lifecycle_jobs",
    foreignKeys = [
        ForeignKey(entity = NovelProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Chapter::class, parentColumns = ["id"], childColumns = ["chapterId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("projectId"), Index("status")],
)
data class ChapterLifecycleJob(
    @PrimaryKey val chapterId: Long,
    val projectId: Long,
    val contentFingerprint: String,
    val status: String = ChapterLifecycleJobStatus.QUEUED,
    val attempts: Int = 0,
    val detail: String = "等待后台闭环",
    @ColumnInfo(defaultValue = "''") val afterSuccessAction: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

object ChapterLifecycleJobStatus {
    const val QUEUED = "queued"
    const val RUNNING = "running"
    const val FAILED = "failed"
    const val COMPLETED = "completed"
}

object AutoWriteRunStatus {
    const val RUNNING = "running"
    const val PAUSED = "paused"
    const val COMPLETED = "completed"
}

object ChapterQualityStatus {
    const val READY = "ready"
    const val NEEDS_REPAIR = "needs_repair"
}

@Entity(
    tableName = "story_items",
    foreignKeys = [ForeignKey(
        entity = NovelProject::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class StoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val kind: String,
    val name: String,
    val detail: String,
    @ColumnInfo(defaultValue = "'活跃'") val status: String = StoryItemStatus.ACTIVE,
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val cascadePending: Boolean = false,
)

@Entity(
    tableName = "story_anchors",
    foreignKeys = [ForeignKey(
        entity = NovelProject::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class StoryAnchor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val startChapter: Int,
    val endChapter: Int,
    val title: String,
    val coreConflict: String,
    val allowedPlot: String = "",
    val forbiddenReveals: String = "",
    val mandatoryTension: String = "",
    @ColumnInfo(defaultValue = "0") val cascadePending: Boolean = false,
)

@Entity(
    tableName = "story_edges",
    foreignKeys = [ForeignKey(
        entity = NovelProject::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId"), Index("sourceItemId"), Index("targetItemId")],
)
data class StoryEdge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val sourceItemId: Long,
    val targetItemId: Long,
    val relation: String,
    val strength: Float = 0.5f,
    val description: String = "",
    val sinceChapter: Int = 1,
    @ColumnInfo(defaultValue = "0") val cascadePending: Boolean = false,
)

object StoryItemStatus {
    const val ACTIVE = "活跃"
    const val RESOLVED = "已回收"
    const val SECRET = "保密"
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NovelProject>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observe(projectId: Long): Flow<NovelProject?>

    @Insert
    suspend fun insert(project: NovelProject): Long

    @Update
    suspend fun update(project: NovelProject)

    @Query("UPDATE projects SET updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun touch(projectId: Long, updatedAt: Long)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteById(projectId: Long)

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun findById(projectId: Long): NovelProject?
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE projectId = :projectId ORDER BY number ASC")
    fun observeByProject(projectId: Long): Flow<List<Chapter>>

    @Insert
    suspend fun insert(chapter: Chapter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Update
    suspend fun update(chapter: Chapter)

    @Query("SELECT MAX(number) FROM chapters WHERE projectId = :projectId")
    suspend fun maxNumber(projectId: Long): Int?

    @Query("SELECT COUNT(*) FROM chapters WHERE projectId = :projectId")
    suspend fun countByProject(projectId: Long): Int

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun findById(chapterId: Long): Chapter?

    @Query("SELECT * FROM chapters WHERE projectId = :projectId ORDER BY number ASC")
    suspend fun listByProject(projectId: Long): List<Chapter>

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    suspend fun deleteById(chapterId: Long)

    @Query("SELECT COUNT(*) FROM chapters WHERE autoWriteRunId = :runId")
    suspend fun countByAutoWriteRun(runId: Long): Int

    @Query("UPDATE chapters SET lifecycleStatus = :lifecycleStatus, lifecycleDetail = :detail WHERE lifecycleStatus = 'processing'")
    suspend fun markInterruptedLifecycles(lifecycleStatus: String, detail: String)
}

@Dao
interface ChapterRevisionDao {
    @Query("SELECT * FROM chapter_revisions WHERE chapterId = :chapterId ORDER BY id DESC LIMIT 1")
    fun observeLatest(chapterId: Long): Flow<ChapterRevision?>

    @Insert
    suspend fun insert(revision: ChapterRevision): Long

    @Query("DELETE FROM chapter_revisions WHERE id = :revisionId")
    suspend fun deleteById(revisionId: Long)

    @Query("SELECT * FROM chapter_revisions WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<ChapterRevision>
}

@Dao
interface AutoWriteRunDao {
    @Query("SELECT * FROM auto_write_runs WHERE projectId = :projectId AND status = 'paused' ORDER BY updatedAt DESC LIMIT 1")
    fun observeResumable(projectId: Long): Flow<AutoWriteRun?>

    @Insert
    suspend fun insert(run: AutoWriteRun): Long

    @Update
    suspend fun update(run: AutoWriteRun)

    @Query("UPDATE auto_write_runs SET status = 'paused', detail = :detail, updatedAt = :updatedAt WHERE status = 'running'")
    suspend fun pauseInterruptedRuns(detail: String, updatedAt: Long)

    @Query("SELECT * FROM auto_write_runs WHERE status IN ('running', 'paused')")
    suspend fun findRecoverableRuns(): List<AutoWriteRun>

    @Query("SELECT * FROM auto_write_runs WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<AutoWriteRun>
}

@Dao
interface ImportAnalysisDao {
    @Query("SELECT * FROM import_analysis_runs WHERE projectId = :projectId")
    fun observe(projectId: Long): Flow<ImportAnalysisRun?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(run: ImportAnalysisRun)
}

@Dao
interface StoryItemDao {
    @Query("SELECT * FROM story_items WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeByProject(projectId: Long): Flow<List<StoryItem>>

    @Insert
    suspend fun insert(item: StoryItem): Long

    @Update
    suspend fun update(item: StoryItem)

    @Update
    suspend fun updateAll(items: List<StoryItem>)

    @Query("SELECT * FROM story_items WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<StoryItem>
}

@Dao
interface ChapterStoryMentionDao {
    @Query("SELECT * FROM chapter_story_mentions WHERE projectId = :projectId")
    fun observeByProject(projectId: Long): Flow<List<ChapterStoryMention>>

    @Query("DELETE FROM chapter_story_mentions WHERE chapterId = :chapterId")
    suspend fun deleteByChapter(chapterId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(mentions: List<ChapterStoryMention>)

    @Query("SELECT * FROM chapter_story_mentions WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<ChapterStoryMention>
}

@Dao
interface ResearchNoteDao {
    @Query("SELECT * FROM research_notes WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeByProject(projectId: Long): Flow<List<ResearchNote>>

    @Insert
    suspend fun insert(note: ResearchNote): Long

    @Update
    suspend fun update(note: ResearchNote)

    @Query("DELETE FROM research_notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("SELECT * FROM research_notes WHERE id = :noteId")
    suspend fun findById(noteId: Long): ResearchNote?

    @Query("SELECT * FROM research_notes WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<ResearchNote>
}

@Dao
interface EditorialReviewDao {
    @Query("SELECT * FROM editorial_reviews WHERE chapterId = :chapterId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(chapterId: Long): Flow<EditorialReview?>

    @Insert
    suspend fun insert(review: EditorialReview): Long

    @Query("SELECT * FROM editorial_reviews WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<EditorialReview>
}

@Dao
interface StoryAnchorDao {
    @Query("SELECT * FROM story_anchors WHERE projectId = :projectId ORDER BY startChapter ASC, endChapter ASC")
    fun observeByProject(projectId: Long): Flow<List<StoryAnchor>>

    @Insert
    suspend fun insert(anchor: StoryAnchor): Long

    @Update
    suspend fun updateAll(anchors: List<StoryAnchor>)

    @Query("SELECT * FROM story_anchors WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<StoryAnchor>
}

@Dao
interface StoryEdgeDao {
    @Query("SELECT * FROM story_edges WHERE projectId = :projectId ORDER BY sinceChapter ASC, id ASC")
    fun observeByProject(projectId: Long): Flow<List<StoryEdge>>

    @Insert
    suspend fun insert(edge: StoryEdge): Long

    @Update
    suspend fun updateAll(edges: List<StoryEdge>)

    @Query("SELECT * FROM story_edges WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<StoryEdge>
}

@Dao
interface IdeationDraftDao {
    @Query("SELECT * FROM ideation_drafts ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatest(): Flow<IdeationDraft?>
    @Insert
    suspend fun insert(draft: IdeationDraft): Long
    @Update
    suspend fun update(draft: IdeationDraft)
    @Query("DELETE FROM ideation_drafts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ChapterPacingEventDao {
    @Query("SELECT * FROM chapter_pacing_events WHERE projectId = :projectId ORDER BY chapterNumber ASC, id ASC")
    fun observeByProject(projectId: Long): Flow<List<ChapterPacingEvent>>
    @Query("SELECT * FROM chapter_pacing_events WHERE projectId = :projectId ORDER BY chapterNumber ASC, id ASC")
    suspend fun listByProject(projectId: Long): List<ChapterPacingEvent>
    @Query("DELETE FROM chapter_pacing_events WHERE chapterId = :chapterId")
    suspend fun deleteByChapter(chapterId: Long)
    @Insert
    suspend fun insert(event: ChapterPacingEvent): Long
}

@Dao
interface StyleProfileDao {
    @Query("SELECT * FROM style_profiles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StyleProfile>>
    @Query("SELECT * FROM style_profiles ORDER BY id ASC")
    suspend fun listAll(): List<StyleProfile>
    @Insert
    suspend fun insert(profile: StyleProfile): Long
    @Query("DELETE FROM style_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface BatchReviewDao {
    @Query("SELECT * FROM batch_review_runs WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeByProject(projectId: Long): Flow<List<BatchReviewRun>>
    @Query("SELECT * FROM batch_review_runs WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<BatchReviewRun>
    @Insert
    suspend fun insert(run: BatchReviewRun): Long
}

@Dao
interface ReviewIssueDao {
    @Query("SELECT * FROM review_issues WHERE projectId = :projectId ORDER BY status ASC, createdAt DESC")
    fun observeByProject(projectId: Long): Flow<List<ReviewIssue>>
    @Query("SELECT * FROM review_issues WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<ReviewIssue>
    @Insert
    suspend fun insertAll(issues: List<ReviewIssue>)
    @Query("UPDATE review_issues SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}

@Dao
interface EventMatrixRuleDao {
    @Query("SELECT * FROM event_matrix_rules WHERE projectId = :projectId ORDER BY id ASC")
    fun observeByProject(projectId: Long): Flow<List<EventMatrixRule>>
    @Query("SELECT * FROM event_matrix_rules WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun listByProject(projectId: Long): List<EventMatrixRule>
    @Insert
    suspend fun insertAll(rules: List<EventMatrixRule>)
    @Insert
    suspend fun insert(rule: EventMatrixRule): Long
    @Update
    suspend fun update(rule: EventMatrixRule)
    @Query("DELETE FROM event_matrix_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ChapterGateReportDao {
    @Query("SELECT * FROM chapter_gate_reports WHERE chapterId = :chapterId ORDER BY createdAt DESC, id DESC")
    fun observeByChapter(chapterId: Long): Flow<List<ChapterGateReport>>
    @Query("SELECT * FROM chapter_gate_reports WHERE chapterId = :chapterId ORDER BY createdAt DESC, id DESC")
    suspend fun listByChapter(chapterId: Long): List<ChapterGateReport>
    @Query("SELECT * FROM chapter_gate_reports WHERE projectId = :projectId ORDER BY createdAt ASC, id ASC")
    suspend fun listByProject(projectId: Long): List<ChapterGateReport>
    @Insert
    suspend fun insert(report: ChapterGateReport): Long
}

@Dao
interface RagChunkDao {
    @Query("SELECT * FROM rag_chunks WHERE projectId = :projectId ORDER BY chapterNumber ASC, ordinal ASC")
    fun observeByProject(projectId: Long): Flow<List<RagChunk>>
    @Query("DELETE FROM rag_chunks WHERE chapterId = :chapterId")
    suspend fun deleteByChapter(chapterId: Long)
    @Insert
    suspend fun insertAll(chunks: List<RagChunk>)
}

@Dao
interface ChapterContinuitySnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: ChapterContinuitySnapshot)

    @Query("UPDATE chapter_continuity_snapshots SET confirmationStatus = :status, updatedAt = :updatedAt WHERE predecessorChapterId = :predecessorChapterId")
    suspend fun updateForPredecessor(predecessorChapterId: Long, status: String, updatedAt: Long)
}

@Dao
interface ChapterLifecycleJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: ChapterLifecycleJob)

    @Query("SELECT * FROM chapter_lifecycle_jobs WHERE projectId = :projectId AND status = 'queued' ORDER BY updatedAt ASC LIMIT 1")
    suspend fun nextQueued(projectId: Long): ChapterLifecycleJob?

    @Query("SELECT * FROM chapter_lifecycle_jobs WHERE chapterId = :chapterId")
    suspend fun findByChapter(chapterId: Long): ChapterLifecycleJob?

    @Query("UPDATE chapter_lifecycle_jobs SET status = 'queued', detail = :detail, updatedAt = :updatedAt WHERE status = 'running'")
    suspend fun recoverRunning(detail: String, updatedAt: Long)
}

@Database(
    entities = [NovelProject::class, Chapter::class, ChapterRevision::class, AutoWriteRun::class, ImportAnalysisRun::class, StoryItem::class, ChapterStoryMention::class, ResearchNote::class, EditorialReview::class, StoryAnchor::class, StoryEdge::class, IdeationDraft::class, ChapterPacingEvent::class, EventMatrixRule::class, ChapterGateReport::class, StyleProfile::class, BatchReviewRun::class, ReviewIssue::class, RagChunk::class, ChapterContinuitySnapshot::class, ChapterLifecycleJob::class],
    version = 24,
    exportSchema = false,
)
abstract class NovelDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun chapterRevisionDao(): ChapterRevisionDao
    abstract fun autoWriteRunDao(): AutoWriteRunDao
    abstract fun importAnalysisDao(): ImportAnalysisDao
    abstract fun storyItemDao(): StoryItemDao
    abstract fun chapterStoryMentionDao(): ChapterStoryMentionDao
    abstract fun researchNoteDao(): ResearchNoteDao
    abstract fun editorialReviewDao(): EditorialReviewDao
    abstract fun storyAnchorDao(): StoryAnchorDao
    abstract fun storyEdgeDao(): StoryEdgeDao
    abstract fun ideationDraftDao(): IdeationDraftDao
    abstract fun chapterPacingEventDao(): ChapterPacingEventDao
    abstract fun styleProfileDao(): StyleProfileDao
    abstract fun batchReviewDao(): BatchReviewDao
    abstract fun reviewIssueDao(): ReviewIssueDao
    abstract fun eventMatrixRuleDao(): EventMatrixRuleDao
    abstract fun chapterGateReportDao(): ChapterGateReportDao
    abstract fun ragChunkDao(): RagChunkDao
    abstract fun chapterContinuitySnapshotDao(): ChapterContinuitySnapshotDao
    abstract fun chapterLifecycleJobDao(): ChapterLifecycleJobDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chapters ADD COLUMN outline TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE chapters ADD COLUMN targetWordCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE story_items ADD COLUMN status TEXT NOT NULL DEFAULT '活跃'")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS story_anchors (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        startChapter INTEGER NOT NULL,
                        endChapter INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        coreConflict TEXT NOT NULL,
                        allowedPlot TEXT NOT NULL,
                        forbiddenReveals TEXT NOT NULL,
                        mandatoryTension TEXT NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_story_anchors_projectId ON story_anchors(projectId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS story_edges (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        sourceItemId INTEGER NOT NULL,
                        targetItemId INTEGER NOT NULL,
                        relation TEXT NOT NULL,
                        strength REAL NOT NULL,
                        description TEXT NOT NULL,
                        sinceChapter INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_story_edges_projectId ON story_edges(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_story_edges_sourceItemId ON story_edges(sourceItemId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_story_edges_targetItemId ON story_edges(targetItemId)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chapters ADD COLUMN beatSheet TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN styleGuide TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chapters ADD COLUMN qualityStatus TEXT NOT NULL DEFAULT 'ready'")
                db.execSQL("ALTER TABLE chapters ADD COLUMN qualityIssueSummary TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN outlineRevisionReport TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE story_items ADD COLUMN cascadePending INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE story_anchors ADD COLUMN cascadePending INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE story_edges ADD COLUMN cascadePending INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN targetAudience TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN protagonistName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN coverPath TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chapters ADD COLUMN lifecycleStatus TEXT NOT NULL DEFAULT 'manual'")
                db.execSQL("ALTER TABLE chapters ADD COLUMN lifecycleDetail TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE chapters ADD COLUMN memoryUpdatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chapter_revisions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        chapterId INTEGER NOT NULL,
                        previousContent TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE,
                        FOREIGN KEY(chapterId) REFERENCES chapters(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_revisions_projectId ON chapter_revisions(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_revisions_chapterId ON chapter_revisions(chapterId)")
            }
        }
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS auto_write_runs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        requestedCount INTEGER NOT NULL,
                        completedCount INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        detail TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_write_runs_projectId ON auto_write_runs(projectId)")
            }
        }
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chapters ADD COLUMN autoWriteRunId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_autoWriteRunId ON chapters(autoWriteRunId)")
            }
        }
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chapter_story_mentions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        chapterId INTEGER NOT NULL,
                        storyItemId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE,
                        FOREIGN KEY(chapterId) REFERENCES chapters(id) ON DELETE CASCADE,
                        FOREIGN KEY(storyItemId) REFERENCES story_items(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_story_mentions_projectId ON chapter_story_mentions(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_story_mentions_chapterId ON chapter_story_mentions(chapterId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_story_mentions_storyItemId ON chapter_story_mentions(storyItemId)")
            }
        }
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN longFormBlueprint TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN targetChapterCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN targetWordCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN pacingProfile TEXT NOT NULL DEFAULT '均衡'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS research_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        sourceUrl TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_research_notes_projectId ON research_notes(projectId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS editorial_reviews (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        chapterId INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE,
                        FOREIGN KEY(chapterId) REFERENCES chapters(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_editorial_reviews_projectId ON editorial_reviews(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_editorial_reviews_chapterId ON editorial_reviews(chapterId)")
            }
        }
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE research_notes ADD COLUMN rightsConfirmed INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS ideation_drafts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, step INTEGER NOT NULL, title TEXT NOT NULL, genre TEXT NOT NULL, premise TEXT NOT NULL, protagonist TEXT NOT NULL, conflict TEXT NOT NULL, promise TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chapter_pacing_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, chapterId INTEGER NOT NULL, chapterNumber INTEGER NOT NULL, eventType TEXT NOT NULL, pace TEXT NOT NULL, note TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE, FOREIGN KEY(chapterId) REFERENCES chapters(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_pacing_events_projectId ON chapter_pacing_events(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_pacing_events_chapterId ON chapter_pacing_events(chapterId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS style_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, genre TEXT NOT NULL, guide TEXT NOT NULL, sourceProjectId INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_style_profiles_genre ON style_profiles(genre)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_style_profiles_sourceProjectId ON style_profiles(sourceProjectId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS batch_review_runs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, startChapter INTEGER NOT NULL, endChapter INTEGER NOT NULL, round INTEGER NOT NULL, report TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_batch_review_runs_projectId ON batch_review_runs(projectId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS review_issues (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, reviewRunId INTEGER NOT NULL, chapterNumber INTEGER NOT NULL, severity TEXT NOT NULL, summary TEXT NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE, FOREIGN KEY(reviewRunId) REFERENCES batch_review_runs(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_issues_projectId ON review_issues(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_issues_reviewRunId ON review_issues(reviewRunId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_issues_status ON review_issues(status)")
            }
        }
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE style_profiles ADD COLUMN metrics TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE style_profiles ADD COLUMN keywords TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_18_19 = object : Migration(18, 19) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS rag_chunks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, chapterId INTEGER NOT NULL, chapterNumber INTEGER NOT NULL, ordinal INTEGER NOT NULL, content TEXT NOT NULL, terms TEXT NOT NULL, updatedAt INTEGER NOT NULL)"); db.execSQL("CREATE INDEX IF NOT EXISTS index_rag_chunks_projectId ON rag_chunks(projectId)"); db.execSQL("CREATE INDEX IF NOT EXISTS index_rag_chunks_chapterId ON rag_chunks(chapterId)") } }
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN forbiddenContent TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN automationLevel TEXT NOT NULL DEFAULT '半自动'")
                db.execSQL("ALTER TABLE projects ADD COLUMN targetChapterWordCount INTEGER NOT NULL DEFAULT 2000")
                db.execSQL("ALTER TABLE chapters ADD COLUMN gateFailureCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chapters ADD COLUMN requiresHumanReview INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS event_matrix_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, ruleKey TEXT NOT NULL, label TEXT NOT NULL, cooldown INTEGER NOT NULL, category TEXT NOT NULL, enabled INTEGER NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_event_matrix_rules_projectId ON event_matrix_rules(projectId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chapter_gate_reports (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, chapterId INTEGER NOT NULL, stage TEXT NOT NULL, passed INTEGER NOT NULL, content TEXT NOT NULL, contextSnapshot TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE, FOREIGN KEY(chapterId) REFERENCES chapters(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_gate_reports_projectId ON chapter_gate_reports(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_gate_reports_chapterId ON chapter_gate_reports(chapterId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_gate_reports_stage ON chapter_gate_reports(stage)")
            }
        }
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ideation_drafts ADD COLUMN targetAudience TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ideation_drafts ADD COLUMN writingStyle TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ideation_drafts ADD COLUMN forbiddenContent TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ideation_drafts ADD COLUMN automationLevel TEXT NOT NULL DEFAULT '半自动'")
                db.execSQL("ALTER TABLE ideation_drafts ADD COLUMN targetChapterWordCount INTEGER NOT NULL DEFAULT 2000")
                db.execSQL("ALTER TABLE ideation_drafts ADD COLUMN targetWordCount INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS import_analysis_runs (projectId INTEGER NOT NULL, status TEXT NOT NULL, stage TEXT NOT NULL, progress INTEGER NOT NULL, detail TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(projectId), FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE)")
            }
        }
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS chapter_continuity_snapshots (chapterId INTEGER NOT NULL, projectId INTEGER NOT NULL, predecessorChapterId INTEGER NOT NULL, predecessorTail TEXT NOT NULL, contextPrompt TEXT NOT NULL, confirmationStatus TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(chapterId), FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE, FOREIGN KEY(chapterId) REFERENCES chapters(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_continuity_snapshots_projectId ON chapter_continuity_snapshots(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_continuity_snapshots_predecessorChapterId ON chapter_continuity_snapshots(predecessorChapterId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chapter_lifecycle_jobs (chapterId INTEGER NOT NULL, projectId INTEGER NOT NULL, contentFingerprint TEXT NOT NULL, status TEXT NOT NULL, attempts INTEGER NOT NULL, detail TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(chapterId), FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE, FOREIGN KEY(chapterId) REFERENCES chapters(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_lifecycle_jobs_projectId ON chapter_lifecycle_jobs(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_lifecycle_jobs_status ON chapter_lifecycle_jobs(status)")
            }
        }
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN targetChapterWordCountMax INTEGER NOT NULL DEFAULT 5000")
                db.execSQL("ALTER TABLE ideation_drafts ADD COLUMN targetChapterWordCountMax INTEGER NOT NULL DEFAULT 5000")
                db.execSQL("ALTER TABLE chapter_lifecycle_jobs ADD COLUMN afterSuccessAction TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE projects SET targetChapterWordCount = 3000 WHERE targetChapterWordCount < 3000")
                db.execSQL("UPDATE ideation_drafts SET targetChapterWordCount = 3000 WHERE targetChapterWordCount < 3000")
            }
        }

        fun create(context: Context): NovelDatabase = Room.databaseBuilder(
            context.applicationContext,
            NovelDatabase::class.java,
            "novelcraft.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24).build()
    }
}
