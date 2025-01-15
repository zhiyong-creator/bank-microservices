package com.yuqixue.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "分页响应结果")
public class PageResultDto<T> {
    
    @Schema(description = "总记录数")
    private long total;
    
    @Schema(description = "当前页数据列表")
    private List<T> items;
}
