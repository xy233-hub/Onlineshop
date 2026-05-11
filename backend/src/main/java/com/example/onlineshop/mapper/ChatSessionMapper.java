package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.ChatSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    @Insert("INSERT INTO chat_sessions (user_id, session_name) VALUES (#{userId}, #{sessionName})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSession session);

    @Update("UPDATE chat_sessions SET session_name = #{sessionName} WHERE id = #{id}")
    int updateName(@Param("id") Integer id, @Param("sessionName") String sessionName);

    @Delete("DELETE FROM chat_sessions WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("SELECT * FROM chat_sessions WHERE user_id = #{userId} ORDER BY updated_at DESC")
    List<ChatSession> findByUserId(String userId);

    @Select("SELECT * FROM chat_sessions WHERE id = #{id}")
    ChatSession findById(Integer id);

    @Select("SELECT * FROM chat_sessions WHERE user_id = #{userId} ORDER BY updated_at DESC LIMIT 1")
    ChatSession findLatestByUserId(String userId);
}