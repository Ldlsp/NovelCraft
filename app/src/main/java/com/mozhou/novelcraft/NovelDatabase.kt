package com.mozhou.novelcraft

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
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
    val styleGuide: String = "",
    val outlineRevisionReport: String = "",
    val summary: String = "",
    val tags: String = "",
    val targetAudience: String = "",
    val protagonistName: String = "",
    val longFormBlueprint: String = "",
    val targetChapterCount: Int = 0,
    val targetWordCount: Int = 0,
    val pacingProfile: String = "均衡",
    val coverPath: String = "",
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
    indices = [Index("projectId")],
)
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val number: Int,
    val title: String,
    val content: String = "",
    val outline: String = "",
    val beatSheet: String = "",
    val targetWordCount: Int = 0,
    val qualityStatus: String = ChapterQualityStatus.READY,
    val qualityIssueSummary: String = "",
    val lifecycleStatus: String = ChapterLifecycleStatus.MANUAL,
    val lifecycleDetail: String = "",
    val memoryUpdatedAt: Long = 0,
    val autoWriteRunId: Long = 0,
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
    val status: String = StoryItemStatus.ACTIVE,
    val updatedAt: Long = System.currentTimeMillis(),
    val cascadePending: Boolean = false,
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
    val cascadePending: Boolean = false,
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
    val cascadePending: Boolean = false,
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
}

@Dao
interface ChapterStoryMentionDao {
    @Query("SELECT * FROM chapter_story_mentions WHERE projectId = :projectId")
    fun observeByProject(projectId: Long): Flow<List<ChapterStoryMention>>

    @Query("DELETE FROM chapter_story_mentions WHERE chapterId = :chapterId")
    suspend fun deleteByChapter(chapterId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(mentions: List<ChapterStoryMention>)
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
}

@Dao
interface EditorialReviewDao {
    @Query("SELECT * FROM editorial_reviews WHERE chapterId = :chapterId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(chapterId: Long): Flow<EditorialReview?>

    @Insert
    suspend fun insert(review: EditorialReview): Long
}

@Dao
interface StoryAnchorDao {
    @Query("SELECT * FROM story_anchors WHERE projectId = :projectId ORDER BY startChapter ASC, endChapter ASC")
    fun observeByProject(projectId: Long): Flow<List<StoryAnchor>>

    @Insert
    suspend fun insert(anchor: StoryAnchor): Long

    @Update
    suspend fun updateAll(anchors: List<StoryAnchor>)
}

@Dao
interface StoryEdgeDao {
    @Query("SELECT * FROM story_edges WHERE projectId = :projectId ORDER BY sinceChapter ASC, id ASC")
    fun observeByProject(projectId: Long): Flow<List<StoryEdge>>

    @Insert
    suspend fun insert(edge: StoryEdge): Long

    @Update
    suspend fun updateAll(edges: List<StoryEdge>)
}

@Database(
    entities = [NovelProject::class, Chapter::class, ChapterRevision::class, AutoWriteRun::class, StoryItem::class, ChapterStoryMention::class, ResearchNote::class, EditorialReview::class, StoryAnchor::class, StoryEdge::class],
    version = 15,
    exportSchema = false,
)
abstract class NovelDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun chapterRevisionDao(): ChapterRevisionDao
    abstract fun autoWriteRunDao(): AutoWriteRunDao
    abstract fun storyItemDao(): StoryItemDao
    abstract fun chapterStoryMentionDao(): ChapterStoryMentionDao
    abstract fun researchNoteDao(): ResearchNoteDao
    abstract fun editorialReviewDao(): EditorialReviewDao
    abstract fun storyAnchorDao(): StoryAnchorDao
    abstract fun storyEdgeDao(): StoryEdgeDao

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

        fun create(context: Context): NovelDatabase = Room.databaseBuilder(
            context.applicationContext,
            NovelDatabase::class.java,
            "novelcraft.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15).build()
    }
}
