package com.typetype.text.mapper;

import com.typetype.text.entity.Text;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TextMapper {

    @Select("SELECT * FROM t_text WHERE id = #{id}")
    Text findById(Long id);

    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId}")
    List<Text> findBySourceId(Long sourceId);

    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId} ORDER BY RAND() LIMIT 1")
    Text findRandomBySourceId(Long sourceId);

    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId} ORDER BY created_at DESC LIMIT 1")
    Text findLatestBySourceId(Long sourceId);

    @Insert("INSERT INTO t_text (source_id, title, content, char_count, difficulty, client_text_id) " +
            "VALUES (#{sourceId}, #{title}, #{content}, #{charCount}, #{difficulty}, #{clientTextId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Text text);

    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId} AND title = #{title} LIMIT 1")
    Text findBySourceIdAndTitle(Long sourceId, String title);

    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId} AND content = #{content} LIMIT 1")
    Text findBySourceIdAndContent(Long sourceId, String content);

    @Select("SELECT * FROM t_text WHERE client_text_id = #{clientTextId} LIMIT 1")
    Text findByClientTextId(Long clientTextId);

    @Select("SELECT id, title, char_count, created_at FROM t_text WHERE source_id = #{sourceId} ORDER BY created_at DESC")
    List<Text> findBySourceIdSummary(Long sourceId);
}
