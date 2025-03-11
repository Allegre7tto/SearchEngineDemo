package org.example.searchenginedemo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {
    public int getTotalPageCount();

    public List<Integer> getAllPageIds();

    public String getPageContent(@Param("docId") int docId);

    public int getPageFrequency(@Param("term") String term);

    public Map<String, Object> getTermPositions(@Param("term") String term);

    public List<Map<String, Object>> searchTerms(@Param("terms") List<String> terms);

    public int getPageLength(@Param("docId") int docId);

    public double getAveragePageLength();
}
