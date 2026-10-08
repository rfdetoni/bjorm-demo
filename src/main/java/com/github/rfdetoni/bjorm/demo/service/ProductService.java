package com.github.rfdetoni.bjorm.demo.service;

import com.github.rfdetoni.bjorm.Bjorm;
import com.github.rfdetoni.bjorm.spring.BjormPages;
import com.github.rfdetoni.bjorm.spring.SeekSlice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import com.github.rfdetoni.bjorm.OptimisticLockException;
import com.github.rfdetoni.bjorm.SqlOrder;
import com.github.rfdetoni.bjorm.SqlPredicate;
import com.github.rfdetoni.bjorm.demo.api.PageResult;
import com.github.rfdetoni.bjorm.demo.api.ProductInput;
import com.github.rfdetoni.bjorm.demo.domain.Product;
import com.github.rfdetoni.bjorm.demo.domain.Product_;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

@Service
public class ProductService {
    private final Bjorm db;
    private final BjormPages pages;

    public ProductService(Bjorm db, BjormPages pages) { this.db = db; this.pages = pages; }

    /** Typed predicate reused by Page and Slice; sorting is validated by generated property metadata. */
    private static SqlPredicate nameFilter(String search) {
        return search == null || search.isBlank() ? null :
            Product_.name.like("%" + search.strip().replace("%", "\\%").replace("_", "\\_") + "%");
    }

    public Page<Product> springPage(String search, Pageable pageable) {
        return pages.page(Product.class, nameFilter(search), pageable);
    }

    public Slice<Product> springSlice(String search, Pageable pageable) {
        return pages.slice(Product.class, nameFilter(search), pageable);
    }

    /** ID-keyset pagination for very large tables; no COUNT(*) and no OFFSET. */
    public SeekSlice<Product> seek(String search, UUID after, int size, boolean descending) {
        return pages.seekSlice(Product.class, nameFilter(search), after, size, descending);
    }

    public Product find(UUID id) {
        return db.findOne(Product.class, Product_.id.eq(id))
            .orElseThrow(() -> new NoSuchElementException("Produto não encontrado"));
    }

    public Product create(ProductInput input) {
        Product product = new Product(null, input.name().trim(), input.description(),
            input.price(), input.stock(), input.active(), 0);
        return db.insertReturning(product);
    }

    public Product update(UUID id, ProductInput input) {
        if (input.version() == null) throw new IllegalArgumentException("Versão obrigatória para editar");
        Product updated = new Product(id, input.name().trim(), input.description(),
            input.price(), input.stock(), input.active(), input.version());
        db.update(updated); // BJORM uses id+version in UPDATE WHERE; throws on conflict
        return find(id); // reload incremented version from DB
    }

    public void delete(UUID id, int version) {
        Product candidate = new Product(id, "", null, BigDecimal.ZERO, 0, false, version);
        db.delete(candidate); // uses id+version; stale write becomes HTTP 409
    }

    public PageResult<?> page(int page, int size, String search, String sort, boolean descending, boolean compact) {
        if (page < 0) throw new IllegalArgumentException("Página deve ser não-negativa");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Tamanho deve estar entre 1 e 100");
        if ((long) page * size > pages.maxOffset())
            throw new IllegalArgumentException("OFFSET excede bjorm.pagination.max-offset=" + pages.maxOffset() + "; utilize /api/products/cursor");
        SqlPredicate where = search == null || search.isBlank() ? null :
            Product_.name.like("%" + search.strip().replace("%", "\\%").replace("_", "\\_") + "%");
        SqlOrder ordering = order(sort, descending);
        var select = db.select(Product.class).orderBy(ordering, Product_.id.asc()).limit(size).offset(page * size);
        if (where != null) select.where(where);
        List<?> items = compact
            ? select.fields("id", "name", "price", "stock", "version").fetch()
            : select.fetch();
        long total = count(search);
        return new PageResult<>(items, total, page, size, (int) ((total + size - 1) / size));
    }

    public long count(String search) {
        if (search == null || search.isBlank()) {
            Long count = db.one("SELECT COUNT(*) FROM products", rs -> rs.getLong(1));
            return count == null ? 0 : count;
        }
        String pattern = "%" + search.strip().replace("%", "\\%").replace("_", "\\_") + "%";
        Long count = db.one("SELECT COUNT(*) FROM products WHERE name LIKE ?", ps -> ps.setString(1, pattern), rs -> rs.getLong(1));
        return count == null ? 0 : count;
    }

    public int seed(int count) {
        if (count < 1 || count > 2000) throw new IllegalArgumentException("Quantidade deve estar entre 1 e 2000");
        var items = new ArrayList<Product>(count);
        long seed = System.nanoTime();
        for (int i = 0; i < count; i++) items.add(new Product(null,
            "Exemplo " + seed + "-" + (i + 1), "Produto gerado para testar paginação",
            new BigDecimal("19.90").add(BigDecimal.valueOf(i % 50)), i % 100, true, 0));
        db.batchInsertReturning(items);
        return count;
    }

    private static SqlOrder order(String name, boolean descending) {
        var field = switch (name == null ? "name" : name) {
            case "name" -> Product_.name;
            case "price" -> Product_.price;
            case "stock" -> Product_.stock;
            case "version" -> Product_.version;
            default -> throw new IllegalArgumentException("Ordenação inválida");
        };
        return descending ? field.desc() : field.asc();
    }
}
