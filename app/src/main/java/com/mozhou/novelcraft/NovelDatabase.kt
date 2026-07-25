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
    val targetWordCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

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
}

@Dao
interface StoryItemDao {
    @Query("SELECT * FROM story_items WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeByProject(projectId: Long): Flow<List<StoryItem>>

    @Insert
    suspend fun insert(item: StoryItem): Long

    @Update
    suspend fun update(item: StoryItem)
}

@Dao
interface StoryAnchorDao {
    @Query("SELECT * FROM story_anchors WHERE projectId = :projectId ORDER BY startChapter ASC, endChapter ASC")
    fun observeByProject(projectId: Long): Flow<List<StoryAnchor>>

    @Insert
    suspend fun insert(anchor: StoryAnchor): Long
}

@Dao
interface StoryEdgeDao {
    @Query("SELECT * FROM story_edges WHERE projectId = :projectId ORDER BY sinceChapter ASC, id ASC")
    fun observeByProject(projectId: Long): Flow<List<StoryEdge>>

    @Insert
    suspend fun insert(edge: StoryEdge): Long
}

@Database(
    entities = [NovelProject::class, Chapter::class, StoryItem::class, StoryAnchor::class, StoryEdge::class],
    version = 3,
    exportSchema = false,
)
abstract class NovelDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun storyItemDao(): StoryItemDao
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

        fun create(context: Context): NovelDatabase = Room.databaseBuilder(
            context.applicationContext,
            NovelDatabase::class.java,
            "novelcraft.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
