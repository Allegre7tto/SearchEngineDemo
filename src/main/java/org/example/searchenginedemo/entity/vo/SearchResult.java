package org.example.searchenginedemo.entity.vo;

public class SearchResult implements Comparable<SearchResult> {
    private int pageId;
    private String content;
    private double score;

    public SearchResult(int pageId, String content, double score) {
        this.pageId = pageId;
        this.content = content;
        this.score = score;
    }

    public int getPageId() {
        return pageId;
    }

    public String getContent() {
        return content;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(SearchResult other) {
        // 按得分降序排列
        return Double.compare(other.score, this.score);
    }
}