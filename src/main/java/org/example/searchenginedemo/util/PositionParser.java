package org.example.searchenginedemo.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PositionParser {

    /**
     * 解析位置字符串，格式为: "1:3,2:5,3:2"
     * 返回格式: {docId1: [pos1, pos2...], docId2: [pos1, pos2...]}
     */
    public static Map<Integer, List<Integer>> parsePositions(String positionsString) {
        Map<Integer, List<Integer>> result = new HashMap<>();

        if (positionsString == null || positionsString.isEmpty()) {
            return result;
        }

        // 移除前导逗号（如果有）
        if (positionsString.startsWith(",")) {
            positionsString = positionsString.substring(1);
        }

        String[] positions = positionsString.split(",");

        for (String position : positions) {
            if (position.isEmpty()) {
                continue;
            }

            String[] parts = position.split(":");
            if (parts.length == 2) {
                try {
                    int docId = Integer.parseInt(parts[0].trim());
                    int pos = Integer.parseInt(parts[1].trim());

                    if (!result.containsKey(docId)) {
                        result.put(docId, new ArrayList<>());
                    }
                    result.get(docId).add(pos);
                } catch (NumberFormatException e) {
                    // 忽略无效格式
                }
            }
        }

        return result;
    }

    /**
     * 计算给定位置信息中该词在每个文档中的频率
     */
    public static Map<Integer, Integer> calculateTermFrequencies(Map<Integer, List<Integer>> positions) {
        Map<Integer, Integer> result = new HashMap<>();

        for (Map.Entry<Integer, List<Integer>> entry : positions.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }

        return result;
    }

    /**
     * 计算包含该词的文档总数
     */
    public static int calculateDocumentFrequency(Map<Integer, List<Integer>> positions) {
        return positions.size();
    }
}