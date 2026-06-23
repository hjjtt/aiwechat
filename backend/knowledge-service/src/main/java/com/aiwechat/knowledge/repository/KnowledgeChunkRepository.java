package com.aiwechat.knowledge.repository;

import com.aiwechat.knowledge.model.entity.KnowledgeChunk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeChunkRepository extends BaseMapper<KnowledgeChunk> {

    KnowledgeChunk selectById(@Param("id") String id);

    List<KnowledgeChunk> selectByDocumentId(@Param("documentId") String documentId);

    List<KnowledgeChunk> searchByKeywords(@Param("keyword") String keyword, @Param("limit") int limit);

    int insert(KnowledgeChunk chunk);

    int batchInsert(@Param("list") List<KnowledgeChunk> chunks);

    int deleteByDocumentId(@Param("documentId") String documentId);

    int deleteById(@Param("id") String id);
}
