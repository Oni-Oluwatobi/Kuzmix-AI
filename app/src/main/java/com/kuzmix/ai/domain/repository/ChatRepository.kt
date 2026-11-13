package com.kuzmix.ai.domain.repository

import com.kuzmix.ai.data.local.dao.ConversationDao
import com.kuzmix.ai.data.local.dao.MessageDao
import com.kuzmix.ai.data.local.entity.ConversationEntity
import com.kuzmix.ai.data.local.entity.MessageEntity
import com.kuzmix.ai.domain.model.ChatMessage
import com.kuzmix.ai.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        messageDao.observeForConversation(conversationId).map { list ->
            list.map {
                ChatMessage(
                    id = it.id,
                    conversationId = it.conversationId,
                    role = if (it.role == "user") ChatMessage.Role.USER else ChatMessage.Role.ASSISTANT,
                    content = it.content,
                    timestamp = it.timestamp
                )
            }
        }

    suspend fun createConversation(title: String, isCodeMode: Boolean = false): Long {
        return conversationDao.insert(
            ConversationEntity(title = title, isCodeMode = isCodeMode)
        )
    }

    suspend fun getConversation(id: Long): Conversation? =
        conversationDao.getById(id)?.toDomain()

    suspend fun getMessages(conversationId: Long): List<ChatMessage> =
        messageDao.getForConversation(conversationId).map {
            ChatMessage(
                id = it.id,
                conversationId = it.conversationId,
                role = if (it.role == "user") ChatMessage.Role.USER else ChatMessage.Role.ASSISTANT,
                content = it.content,
                timestamp = it.timestamp
            )
        }

    suspend fun addMessage(conversationId: Long, role: ChatMessage.Role, content: String): Long {
        val id = messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                role = if (role == ChatMessage.Role.USER) "user" else "assistant",
                content = content
            )
        )
        conversationDao.touch(conversationId)
        return id
    }

    suspend fun renameConversation(id: Long, title: String) {
        conversationDao.getById(id)?.let {
            conversationDao.update(it.copy(title = title, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteConversation(id: Long) {
        messageDao.deleteForConversation(id)
        conversationDao.delete(id)
    }

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isCodeMode = isCodeMode
    )
}
