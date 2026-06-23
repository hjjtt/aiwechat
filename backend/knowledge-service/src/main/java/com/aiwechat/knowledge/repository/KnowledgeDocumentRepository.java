package com.aiwechat.knowledge.repository;

import com.aiwechat.knowledge.model.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeDocumentRepository extends BaseMapper<KnowledgeDocument> {

    KnowledgeDocument selectById(@Param("id") String id);

    List<KnowledgeDocument> selectList();

    KnowledgeDocument selectByContentHash(@Param("contentHash") String contentHash);

    List<KnowledgeDocument> selectByBizType(@Param("bizType") String bizType);

    int insert(KnowledgeDocument document);

    int updateChunkCount(@Param("id") String id, @Param("chunkCount") int chunkCount);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    int deleteById(@Param("id") String id);
}
