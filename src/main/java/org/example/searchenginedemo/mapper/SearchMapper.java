package org.example.searchenginedemo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {
    /**
     * 获取文档总数
     */
    int getTotalPageCount();

    /**
     * 获取所有文档ID列表
     */
    List<Integer> getAllPageIds();

    /**
     * 获取文档内容
     */
    String getPageContent(@Param("docId") int docId);

    /**
     * 获取包含指定词的文档数量(文档频率)
     */
    int getPageFrequency(@Param("term") String term);

    /**
     * 获取词在各文档中的位置信息
     */
    Map<String, Object> getTermPositions(@Param("term") String term);

    /**
     * 搜索包含指定词的文档及其位置信息
     */
    List<Map<String, Object>> searchTerms(@Param("terms") List<String> terms);

    /**
     * 获取文档长度
     */
    int getPageLength(@Param("docId") int docId);

    /**
     * 获取平均文档长度
     */
    double getAveragePageLength();
}
