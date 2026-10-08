package com.github.rfdetoni.bjorm.demo.api;

import com.github.rfdetoni.bjorm.demo.domain.Product;
import com.github.rfdetoni.bjorm.demo.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public PageResult<?> list(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(defaultValue = "") String search,
                              @RequestParam(defaultValue = "name") String sort,
                              @RequestParam(defaultValue = "false") boolean descending,
                              @RequestParam(defaultValue = "false") boolean compact) {
        return service.page(page, size, search, sort, descending, compact);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable UUID id) { return service.find(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductInput input) { return service.create(input); }

    @PutMapping("/{id}")
    public Product update(@PathVariable UUID id, @Valid @RequestBody ProductInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam int version) {
        if (version < 0) throw new IllegalArgumentException("Versão inválida");
        service.delete(id, version);
    }

    @PostMapping("/seed")
    public Map<String, Integer> seed(@RequestParam(defaultValue = "250") int count) {
        return Map.of("inserted", service.seed(count));
    }
}
