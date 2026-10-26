package com.luojiajun.chat.common;

import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回体，和 PageHelper 的 PageInfo 对接。
 */
@Data
@Schema(description = "分页结果")
public class CommonPage<T> implements Serializable {

    @Schema(description = "当前页")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;

    @Schema(description = "总页数")
    private Integer totalPage;

    @Schema(description = "总条数")
    private Long total;

    @Schema(description = "当前页数据")
    private List<T> list;

    public static <T> CommonPage<T> restPage(List<T> list) {
        PageInfo<T> pageInfo = new PageInfo<>(list);
        CommonPage<T> result = new CommonPage<>();
        result.setTotalPage(pageInfo.getPages());
        result.setTotal(pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        result.setList(pageInfo.getList());
        return result;
    }
}
