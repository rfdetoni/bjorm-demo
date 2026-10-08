package com.github.rfdetoni.bjorm.demo.api;

import com.github.rfdetoni.bjorm.demo.service.BenchmarkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {
    private final BenchmarkService service;
    public BenchmarkController(BenchmarkService service) { this.service = service; }

    @PostMapping
    public List<BenchmarkResult> compare(@Valid @RequestBody BenchmarkRequest request) {
        return service.compare(request);
    }
}
