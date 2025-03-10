package org.example.searchenginedemo.mapper;

import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface DictMapper {
    // 批量更新方法
    void batchUpdateDict(List<Map<String, Object>> paramsList);

}
