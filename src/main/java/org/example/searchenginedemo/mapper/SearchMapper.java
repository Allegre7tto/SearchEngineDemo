package org.example.searchenginedemo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {
    int getTotalPageCount();

    List<Integer> getAllPageIds();

    String getPageContent(@Param("docId") int docId);

    int getPageFrequency(@Param("term") String term);

    Map<String, Object> getTermPositions(@Param("term") String term);

    List<Map<String, Object>> searchTerms(@Param("terms") List<String> terms);

    int getPageLength(@Param("docId") int docId);

    double getAveragePageLength();
}
