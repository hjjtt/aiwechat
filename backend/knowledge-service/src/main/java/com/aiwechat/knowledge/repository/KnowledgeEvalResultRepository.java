package com.aiwechat.knowledge.repository;

import com.aiwechat.knowledge.model.entity.KnowledgeEvalResult;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeEvalResultRepository extends BaseMapper<KnowledgeEvalResult> {

    List<KnowledgeEvalResult> selectLatest(@Param("limit") int limit);

    int insert(KnowledgeEvalResult result);

    int deleteAll();
}
