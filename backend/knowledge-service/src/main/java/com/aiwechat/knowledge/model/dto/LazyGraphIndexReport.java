package com.aiwechat.knowledge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LazyGraphIndexReport {

    private int chunksScanned;

    private int chunksIndexed;

    private int conceptsCreated;

    private int conceptMentionsCreated;

    private int edgesCreated;
}
