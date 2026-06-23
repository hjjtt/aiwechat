package com.aiwechat.knowledge.repository;

import com.aiwechat.knowledge.model.entity.KnowledgeConceptChunk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeConceptChunkRepository extends BaseMapper<KnowledgeConceptChunk> {

    int batchInsert(@Param("list") List<KnowledgeConceptChunk> chunks);

    List<KnowledgeConceptChunk> selectByConceptIds(@Param("conceptIds") List<Long> conceptIds,
                                                   @Param("limit") int limit);

    int deleteAll();
}
