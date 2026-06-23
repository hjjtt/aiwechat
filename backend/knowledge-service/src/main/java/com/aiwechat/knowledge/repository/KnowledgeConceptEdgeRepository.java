package com.aiwechat.knowledge.repository;

import com.aiwechat.knowledge.model.entity.KnowledgeConceptEdge;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeConceptEdgeRepository extends BaseMapper<KnowledgeConceptEdge> {

    int batchInsert(@Param("list") List<KnowledgeConceptEdge> edges);

    List<KnowledgeConceptEdge> selectNeighbors(@Param("conceptIds") List<Long> conceptIds,
                                               @Param("limit") int limit);

    int deleteAll();
}
