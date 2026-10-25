package id.gustyxganteng.agent.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import id.gustyxganteng.agent.core.database.dao.ChatSessionDao
import id.gustyxganteng.agent.core.database.dao.MessageDao
import id.gustyxganteng.agent.core.database.dao.UserRuleDao
import id.gustyxganteng.agent.core.database.dao.WorkspaceDao
import id.gustyxganteng.agent.core.database.entity.ChatSessionEntity
import id.gustyxganteng.agent.core.database.entity.MessageEntity
import id.gustyxganteng.agent.core.database.entity.UserRuleEntity
import id.gustyxganteng.agent.core.database.entity.WorkspaceEntity

@Database(
    entities = [
        ChatSessionEntity::class,
        MessageEntity::class,
        WorkspaceEntity::class,
        UserRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GGAgentDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun messageDao(): MessageDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun userRuleDao(): UserRuleDao
}
