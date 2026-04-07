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

    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId} ORDER BY created_at LIMIT 1")
    Text findLatestBySourceId(Long sourceId);

    @Insert("INSERT INTO t_text (source_id, title, content, char_count, difficulty) " +
            "VALUES (#{sourceId}, #{title}, #{content}, #{charCount}, #{difficulty})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Text text);

    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId} AND title = #{title} LIMIT 1")
    Text findBySourceIdAndTitle(Long sourceId, String title);
}
