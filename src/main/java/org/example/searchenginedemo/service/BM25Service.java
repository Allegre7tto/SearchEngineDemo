package org.example.searchenginedemo.service;

import org.example.searchenginedemo.mapper.SearchMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BM25Service {
    @Autowired
    private SearchMapper searchMapper;

    // BM25参数
    private final double k1;
    private final double b;

    // 缓存
    private Double averageDocumentLength = null;
    private Integer totalDocuments = null;

    public BM25Service() {
        // 默认参数
        this.k1 = 1.2;
        this.b = 0.75;
    }

    public double score(String term, int docId, int termFrequency, int documentFrequency) {
        if (termFrequency == 0 || documentFrequency == 0) {
            return 0.0;
        }

        // 获取文档总数
        int N = getTotalDocuments();

        // 计算IDF: log((N - n + 0.5) / (n + 0.5)) + 1
        double idf = Math.log((N - documentFrequency + 0.5) / (documentFrequency + 0.5) + 1.0);

        // 获取文档长度和平均文档长度
        int docLength = searchMapper.getPageLength(docId);
        double avgDocLength = getAverageDocumentLength();

        // 计算文档长度归一化因子
        double normalizationFactor = 1.0 - b + b * (docLength / avgDocLength);

        // 计算BM25分数
        return idf * ((k1 + 1.0) * termFrequency) / (k1 * normalizationFactor + termFrequency);
    }

    public int getTotalDocuments() {
        if (totalDocuments == null) {
            totalDocuments = searchMapper.getTotalPageCount();
        }
        return totalDocuments;
    }

    public double getAverageDocumentLength() {
        if (averageDocumentLength == null) {
            averageDocumentLength = searchMapper.getAveragePageLength();
        }
        return averageDocumentLength;
    }

    public void clearCache() {
        averageDocumentLength = null;
        totalDocuments = null;
    }
}
