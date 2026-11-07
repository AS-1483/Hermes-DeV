package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ApiProviderDao
import com.example.data.local.dao.ArtifactDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.SkillDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.ToolDao
import com.example.data.local.entity.ApiProviderEntity
import com.example.data.local.entity.ArtifactEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.ExecutionLogEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.TaskRunEntity
import com.example.data.local.entity.ToolDependencyEntity

@Database(
    entities = [
        ProjectEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        TaskRunEntity::class,
        ExecutionLogEntity::class,
        ApiProviderEntity::class,
        SkillEntity::class,
        ToolDependencyEntity::class,
        ArtifactEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun chatDao(): ChatDao
    abstract fun taskDao(): TaskDao
    abstract fun apiProviderDao(): ApiProviderDao
    abstract fun skillDao(): SkillDao
    abstract fun toolDao(): ToolDao
    abstract fun artifactDao(): ArtifactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hermes_dev_agent.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
