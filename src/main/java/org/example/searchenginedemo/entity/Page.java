package org.example.searchenginedemo.entity;

import java.util.HashMap;
import java.util.Map;

public class Page {
    private int id;
    private String content;
    private Map<String, Integer> termFrequencies;
    private int length;

    public Page(int id, String content) {
        this.id = id;
        this.content = content;
        this.termFrequencies = new HashMap<>();
        calculateTermFrequencies();
    }

    private void calculateTermFrequencies() {
        // 简单分词，以空格分隔
        String[] terms = content.toLowerCase().split("\\s+");
        this.length = terms.length;

        for (String term : terms) {
            termFrequencies.put(term, termFrequencies.getOrDefault(term, 0) + 1);
        }
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Integer> getTermFrequencies() {
        return termFrequencies;
    }

    public int getLength() {
        return length;
    }

    public int getTermFrequency(String term) {
        return termFrequencies.getOrDefault(term.toLowerCase(), 0);
    }
}
