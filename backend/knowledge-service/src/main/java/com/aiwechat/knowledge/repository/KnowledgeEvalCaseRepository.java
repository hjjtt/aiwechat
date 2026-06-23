package com.aiwechat.knowledge.repository;

import com.aiwechat.knowledge.model.entity.KnowledgeEvalCase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeEvalCaseRepository extends BaseMapper<KnowledgeEvalCase> {

    List<KnowledgeEvalCase> selectAll();

    List<KnowledgeEvalCase> selectByCategory(@Param("category") String category);

    int insert(KnowledgeEvalCase evalCase);

    int deleteById(@Param("id") Long id);
}
