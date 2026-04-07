package com.typetype.text.mapper;

import com.typetype.text.entity.TextSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TextSourceMapper {

    @Select("SELECT * FROM t_text_source WHERE is_active = 1")
    List<TextSource> findAllActive();

    @Select("SELECT * FROM t_text_source WHERE source_key = #{sourceKey}")
    TextSource findBySourceKey(String sourceKey);
}
