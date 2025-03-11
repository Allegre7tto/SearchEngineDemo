package org.example.searchenginedemo;

import org.example.searchenginedemo.service.SegmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;

@SpringBootTest
public class IndexTest {
    @Autowired
    private SegmentService segmentService;

    @Test
    void testIndex() {
        System.out.println("Test Index");
        segmentService.loadDict();
    }

    @Test
    void testIndex2() {

    }
}
