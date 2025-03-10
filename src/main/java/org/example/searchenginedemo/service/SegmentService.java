package org.example.searchenginedemo.service;


import org.example.searchenginedemo.entity.Context;
import org.example.searchenginedemo.mapper.DictMapper;
import org.example.searchenginedemo.mapper.PagesMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SegmentService {
    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private PagesMapper pagesMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // Kafka主题名称
    private static final String WORD_SEGMENT_TOPIC = "word-segments";

    // 创建线程池，线程数可以根据CPU核心数调整
    private final ExecutorService threadPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
    );

    public void loadDict() {
        // 从数据库获取需要分词的文本列表
        for (int i = 0;i < 16;i++) {
            char c = 'a';
            if (i > 9)
                c = (char) ('a' + i - 10);
            else
                c = (char) ('0' + i);
            List<Context> textList = pagesMapper.selectTextFromPages("pages" + c);
            pagesMapper.updateDicDone("pages" + c);
            System.out.println("总共加载了 " + textList.size() + " 个文本进行分词处理");
            // 调用批量分词方法
            processSegmentationToKafka(textList);
        }
    }

    /**
     * 批量分词处理并将结果发送到Kafka
     * @param textList 待分词文本列表
     */
    public void processSegmentationToKafka(List<Context> textList) {
        if (textList == null || textList.isEmpty()) {
            return;
        }

        int totalTexts = textList.size();
        AtomicInteger processedCount = new AtomicInteger(0);

        // 提交分词任务到线程池
        for (int i = 0; i < totalTexts; i++) {
            final int pageId = textList.get(i).getId(); // 假设索引对应页面ID，实际应从数据库获取
            final String text = textList.get(i).getText();

            threadPool.submit(() -> {
                try {
                    // 执行分词，返回每个词及其位置信息
                    Map<String, List<Integer>> wordPositions = segmentTextWithPositions(text);

                    // 将分词结果直接发送到Kafka
                    sendToKafka(pageId, wordPositions);

                    // 更新进度
                    int completed = processedCount.incrementAndGet();
                    if (completed % 100 == 0) {
                        System.out.println("已处理: " + completed + "/" + totalTexts + " 文本");
                    }
                } catch (Exception e) {
                    System.err.println("处理文本 " + pageId + " 失败: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }

        // 注意：这里不再等待所有任务完成，而是直接返回
        System.out.println("所有分词任务已提交到线程池，结果将发送到Kafka");
    }

    /**
     * 将分词结果发送到Kafka
     * @param pageId 页面ID
     * @param wordPositions 词及其位置信息
     */
    private void sendToKafka(int pageId, Map<String, List<Integer>> wordPositions) {
        for (Map.Entry<String, List<Integer>> entry : wordPositions.entrySet()) {
            String word = entry.getKey();
            List<Integer> positions = entry.getValue();

            // 为每个词构建消息
            for (Integer position : positions) {
                // 构建消息格式：词|页面ID|位置
                String message = word + "|" + pageId + "|" + position;

                // 发送到Kafka
                kafkaTemplate.send(WORD_SEGMENT_TOPIC, message);
            }
        }
    }

    /**
     * 对文本进行分词
     * @param text 待分词文本
     * @return 分词结果
     */
    public String segmentText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 调用带位置信息的分词方法
        Map<String, List<Integer>> wordPositions = segmentTextWithPositions(text);

        // 将分词结果拼接成字符串
        StringBuilder result = new StringBuilder();
        for (String word : wordPositions.keySet()) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(word);
        }

        return result.toString();
    }

    /**
     * 对文本进行分词，返回每个词及其在文本中的位置
     * @param text 待分词文本
     * @return 词及其位置列表的映射
     */
    private Map<String, List<Integer>> segmentTextWithPositions(String text) {
        if (text == null || text.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, List<Integer>> result = new HashMap<>();

        // 这里使用简单的示例分词算法，实际应用中应使用专业分词库如jieba、HanLP等
        // 示例仅按空格分词，实际使用时替换为真实分词逻辑
        String[] words = text.split("\\s+");
        int position = 0;

        for (String word : words) {
            if (!word.isEmpty()) {
                // 记录词的位置
                if (!result.containsKey(word)) {
                    result.put(word, new ArrayList<>());
                }
                result.get(word).add(position);
                position++;
            }
        }

        return result;
    }

    // 关闭线程池的方法，应在应用关闭时调用
    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
