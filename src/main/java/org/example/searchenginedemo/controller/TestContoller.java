package org.example.searchenginedemo.controller;

import org.example.searchenginedemo.service.IndexService;
import org.example.searchenginedemo.service.SegmentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestContoller {

    private final SegmentService segmentService;
    private final IndexService indexService;

    public TestContoller(SegmentService segmentService, IndexService indexService) {
        this.segmentService = segmentService;
        this.indexService = indexService;
    }

    @PostMapping("/test")
    public String test(){
        segmentService.loadDict();
        return "success";
    }

}
