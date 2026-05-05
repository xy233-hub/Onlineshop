package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_messages (session_id, role, content) VALUES (#{sessionId}, #{role}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    @Select("SELECT * FROM chat_messages WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<ChatMessage> findBySessionId(Integer sessionId);

    @Select("SELECT * FROM chat_messages WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ChatMessage> findRecentBySessionId(@Param("sessionId") Integer sessionId, @Param("limit") int limit);

    @Delete("DELETE FROM chat_messages WHERE session_id = #{sessionId}")
    int deleteBySessionId(Integer sessionId);

    @Select("SELECT COUNT(*) FROM chat_messages WHERE session_id = #{sessionId}")
    int countBySessionId(Integer sessionId);
}