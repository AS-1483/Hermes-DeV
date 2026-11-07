package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ApiProviderEntity
import com.example.data.local.entity.ArtifactEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.ExecutionLogEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.TaskRunEntity
import com.example.data.local.entity.ToolDependencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    suspend fun getAllProjectsSync(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getSessionsForProject(projectId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE projectId = :projectId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestSessionForProject(projectId: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE projectId = :projectId ORDER BY updatedAt DESC")
    suspend fun getSessionsForProjectSync(projectId: String): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSessionMessages(sessionId: String)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_runs WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getTasksForSession(sessionId: String): Flow<List<TaskRunEntity>>

    @Query("SELECT * FROM task_runs ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskRunEntity>>

    @Query("SELECT * FROM task_runs ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestTaskSync(): TaskRunEntity?

    @Query("SELECT * FROM task_runs WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTask(task: TaskRunEntity)

    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getLogsForTask(taskId: String): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllRecentLogs(): Flow<List<ExecutionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLogEntity)
}

@Dao
interface ApiProviderDao {
    @Query("SELECT * FROM api_providers ORDER BY priority ASC")
    fun getAllProviders(): Flow<List<ApiProviderEntity>>

    @Query("SELECT * FROM api_providers WHERE isEnabled = 1 ORDER BY priority ASC")
    suspend fun getEnabledProvidersSync(): List<ApiProviderEntity>

    @Query("SELECT * FROM api_providers WHERE id = :id LIMIT 1")
    suspend fun getProviderById(id: String): ApiProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(provider: ApiProviderEntity)

    @Query("DELETE FROM api_providers WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY installedAt DESC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE isEnabled = 1")
    suspend fun getEnabledSkillsSync(): List<SkillEntity>

    @Query("SELECT * FROM skills WHERE id = :id LIMIT 1")
    suspend fun getSkillById(id: String): SkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(skill: SkillEntity)

    @Query("UPDATE skills SET isEnabled = :enabled WHERE id = :id")
    suspend fun setSkillEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkill(id: String)
}

@Dao
interface ToolDao {
    @Query("SELECT * FROM tool_dependencies ORDER BY name ASC")
    fun getAllTools(): Flow<List<ToolDependencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tool: ToolDependencyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tools: List<ToolDependencyEntity>)
}

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getArtifactsForProject(projectId: String): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM artifacts ORDER BY createdAt DESC")
    fun getAllArtifacts(): Flow<List<ArtifactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: ArtifactEntity)
}
