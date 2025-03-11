package org.example.searchenginedemo.service;

import org.example.searchenginedemo.mapper.DictMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {
    private static final Logger logger = LoggerFactory.getLogger(IndexService.class);

    @Autowired
    private DictMapper dictMapper;

    // 批处理大小
    private static final int BATCH_SIZE = 50;
    // 批处理最大等待时间(毫秒)
    private static final long BATCH_TIMEOUT_MS = 500;

    // 存储接收到的消息的队列
    private final BlockingQueue<Map<String, String>> messageQueue = new LinkedBlockingQueue<>();
    // 调度执行器，用于定时批量处理
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

    @PostConstruct
    public void init() {
        // 启动批处理任务，每BATCH_TIMEOUT_MS毫秒检查一次是否有数据需要处理
        scheduler.scheduleWithFixedDelay(
                this::processBatch,
                BATCH_TIMEOUT_MS,
                BATCH_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
        );

        logger.info("Kafka消费者服务已启动，等待消息...");
    }

    @KafkaListener(topics = "word-segments", groupId = "search-engine-group")
    public void listen(String message) {
        try {
            // 解析消息
            String[] parts = message.split("\\|");
            if (parts.length >= 3) {
                String word = parts[0];
                String position = parts[1] + ":" + parts[2];

                // 创建参数映射并加入队列
                Map<String, String> params = new HashMap<>();
                params.put("word", word);
                params.put("position", position);
                messageQueue.add(params);

                // 如果队列中的消息数量达到批处理大小，立即触发处理
                if (messageQueue.size() >= BATCH_SIZE) {
                    processBatch();
                }
            }
        } catch (Exception e) {
            logger.error("处理消息失败: {}", message, e);
        }
    }

    @Transactional
    public void processBatch() {
        List<Map<String, String>> batch = new ArrayList<>();

        // 从队列中取出消息，最多取BATCH_SIZE条
        messageQueue.drainTo(batch, BATCH_SIZE);

        if (!batch.isEmpty()) {
            try {
                // 批量写入数据库
                dictMapper.batchUpdateDict(batch);
                logger.info("成功批量写入 {} 条记录到数据库", batch.size());
            } catch (Exception e) {
                logger.error("批量写入数据库失败", e);
                // 事务回滚会由Spring管理
                throw e; // 重新抛出异常以触发事务回滚
            }
        }
    }

    public void shutdown() {
        // 处理剩余的消息
        processBatch();
        // 关闭调度器
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
