package com.aiwechat.knowledge.repository;

import com.aiwechat.knowledge.model.entity.KnowledgeConcept;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeConceptRepository extends BaseMapper<KnowledgeConcept> {

    KnowledgeConcept selectByNormalizedName(@Param("normalizedName") String normalizedName);

    List<KnowledgeConcept> selectByNormalizedNames(@Param("names") List<String> normalizedNames);

    List<KnowledgeConcept> selectByIds(@Param("ids") List<Long> ids);

    List<KnowledgeConcept> selectTop(@Param("limit") int limit);

    int insert(KnowledgeConcept concept);

    int incrementFrequency(@Param("id") Long id, @Param("delta") int delta);

    int deleteAll();
}
