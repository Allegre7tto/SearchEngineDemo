package org.example.searchenginedemo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.searchenginedemo.entity.Context;

import java.util.List;

@Mapper
public interface PagesMapper {
    List<Context> selectTextFromPages(String table);
    void updateDicDone(String table);
}
