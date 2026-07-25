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
    val updatedAt: Long = System.currentTimeMillis(),
)

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
}

@Database(
    entities = [NovelProject::class, Chapter::class, StoryItem::class],
    version = 1,
    exportSchema = false,
)
abstract class NovelDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun storyItemDao(): StoryItemDao

    companion object {
        fun create(context: Context): NovelDatabase = Room.databaseBuilder(
            context.applicationContext,
            NovelDatabase::class.java,
            "novelcraft.db",
        ).build()
    }
}
