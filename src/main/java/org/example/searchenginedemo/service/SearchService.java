package org.example.searchenginedemo.service;

import org.example.searchenginedemo.mapper.SearchMapper;
import org.example.searchenginedemo.entity.vo.SearchResult;
import org.example.searchenginedemo.util.PositionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private static final String SEARCH_TERMS_KEY = "search:terms:count";

    @Autowired
    private SearchMapper searchMapper;

    @Autowired
    private BM25Service bm25Service;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void init() {
        logger.info("初始化搜索服务...");
        // 预热缓存
        bm25Service.getTotalDocuments();
        bm25Service.getAverageDocumentLength();
    }

    /**
     * 执行搜索
     * @param query 查询字符串
     * @param topK 返回前K个结果
     * @return 排序后的搜索结果
     */
    public List<SearchResult> search(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 对查询进行分词(简单实现为按空格分隔)
        String[] queryTerms = query.toLowerCase().trim().split("\\s+");
        List<String> terms = Arrays.asList(queryTerms);

        logger.info("搜索查询: '{}', 分词为: {}", query, terms);

        // 增加每个查询词的计数
        incrementTermCounts(terms);

        // 执行数据库查询，获取包含查询词的文档
        List<Map<String, Object>> searchResults = searchMapper.searchTerms(terms);

        // 构建文档得分映射
        Map<Integer, Double> documentScores = new HashMap<>();

        // 处理每个查询词的结果
        for (Map<String, Object> result : searchResults) {
            String term = (String) result.get("name");
            String positionsString = (String) result.get("positions");

            // 解析位置信息
            Map<Integer, List<Integer>> positions = PositionParser.parsePositions(positionsString);

            // 计算该词的文档频率
            int documentFrequency = positions.size();

            // 计算每个文档中该词的词频
            for (Map.Entry<Integer, List<Integer>> entry : positions.entrySet()) {
                int docId = entry.getKey();
                List<Integer> termPositions = entry.getValue();
                int termFrequency = termPositions.size();

                // 计算该文档对该词的得分
                double score = bm25Service.score(term, docId, termFrequency, documentFrequency);

                // 累加到文档总得分
                documentScores.put(docId, documentScores.getOrDefault(docId, 0.0) + score);
            }
        }

        // 创建搜索结果列表
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : documentScores.entrySet()) {
            int docId = entry.getKey();
            double score = entry.getValue();
            String content = searchMapper.getPageContent(docId);

            results.add(new SearchResult(docId, content, score));
        }

        // 按得分排序
        Collections.sort(results);

        // 限制返回结果数量
        if (results.size() > topK) {
            results = results.subList(0, topK);
        }

        logger.info("查询 '{}' 返回 {} 个结果", query, results.size());
        return results;
    }

    /**
     * 增加查询词在Redis中的计数
     * @param terms 查询词列表
     */
    private void incrementTermCounts(List<String> terms) {
        try {
            for (String term : terms) {
                redisTemplate.opsForHash().increment(SEARCH_TERMS_KEY, term, 1);
                logger.debug("增加查询词计数: {}", term);
            }
        } catch (Exception e) {
            logger.error("Redis增加查询词计数失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取查询词的搜索次数
     * @param term 查询词
     * @return 搜索次数
     */
    public long getTermSearchCount(String term) {
        try {
            Object count = redisTemplate.opsForHash().get(SEARCH_TERMS_KEY, term);
            return count != null ? Long.parseLong(count.toString()) : 0;
        } catch (Exception e) {
            logger.error("获取查询词计数失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 获取所有查询词的搜索次数
     * @return 查询词及其搜索次数的映射
     */
    public Map<String, Long> getAllTermSearchCounts() {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(SEARCH_TERMS_KEY);
            Map<String, Long> result = new HashMap<>();

            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                result.put(entry.getKey().toString(),
                        Long.parseLong(entry.getValue().toString()));
            }

            return result;
        } catch (Exception e) {
            logger.error("获取所有查询词计数失败: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取前N个热门查询词
     * @param n 数量
     * @return 热门查询词列表
     */
    public List<Map.Entry<String, Long>> getTopSearchTerms(int n) {
        Map<String, Long> allCounts = getAllTermSearchCounts();

        List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(allCounts.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        return sortedEntries.size() <= n ? sortedEntries : sortedEntries.subList(0, n);
    }

    /**
     * 获取搜索引擎统计信息
     */
    public Map<String, Object> getSearchStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", bm25Service.getTotalDocuments());
        stats.put("averageDocumentLength", bm25Service.getAverageDocumentLength());
        // 添加热门查询词统计
        stats.put("topSearchTerms", getTopSearchTerms(10));
        return stats;
    }

    /**
     * 刷新统计信息缓存
     */
    public void refreshStats() {
        bm25Service.clearCache();
        logger.info("已刷新搜索统计信息缓存");
    }

    /**
     * 重置某个查询词的计数
     * @param term 查询词
     */
    public void resetTermCount(String term) {
        try {
            redisTemplate.opsForHash().delete(SEARCH_TERMS_KEY, term);
            logger.info("已重置查询词 '{}' 的计数", term);
        } catch (Exception e) {
            logger.error("重置查询词计数失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 重置所有查询词计数
     */
    public void resetAllTermCounts() {
        try {
            redisTemplate.delete(SEARCH_TERMS_KEY);
            logger.info("已重置所有查询词计数");
        } catch (Exception e) {
            logger.error("重置所有查询词计数失败: {}", e.getMessage(), e);
        }
    }
}