package com.github.rfdetoni.bjorm.demo.api;

import com.github.rfdetoni.bjorm.Bjorm;
import com.github.rfdetoni.bjorm.demo.domain.DemoOrder;
import com.github.rfdetoni.bjorm.demo.domain.DemoOrderLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Demonstrates native UUID v7, upsert and transactional persistence graphs. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final Bjorm db;
    public OrderController(Bjorm db) { this.db = db; }

    public record LineInput(UUID id, @NotBlank String sku, @Min(1) int quantity) {}
    public record OrderInput(@NotBlank String customer, @NotEmpty List<@Valid LineInput> lines) {}
    public record OrderLineView(UUID id, UUID orderId, String sku, int quantity) {}
    public record OrderView(UUID id, String customer, List<OrderLineView> lines) {}

    private static DemoOrder fromInput(UUID id, OrderInput input) {
        DemoOrder order = new DemoOrder();
        order.setId(id);
        order.setCustomer(input.customer());
        for (LineInput item : input.lines()) {
            DemoOrderLine line = new DemoOrderLine();
            line.setId(item.id());
            line.setSku(item.sku());
            line.setQuantity(item.quantity());
            order.getLines().add(line);
        }
        return order;
    }

    private static OrderView view(DemoOrder order) {
        return new OrderView(order.getId(), order.getCustomer(), order.getLines().stream()
            .map(item -> new OrderLineView(item.getId(), item.getOrderId(), item.getSku(), item.getQuantity()))
            .toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView create(@Valid @RequestBody OrderInput input) {
        DemoOrder order = fromInput(null, input);
        db.insert(order); // one transaction: generates parent UUID v7 and FK for every child
        return view(order);
    }

    @GetMapping("/{id}")
    public OrderView find(@PathVariable UUID id) {
        DemoOrder order = db.find(DemoOrder.class, id);
        if (order == null) throw new NoSuchElementException("Pedido não encontrado");
        // @Children is fetched with the parent in one joined SQL statement.
        return view(order);
    }

    @PutMapping("/{id}/upsert")
    public OrderView upsert(@PathVariable UUID id, @Valid @RequestBody OrderInput input) {
        // Upsert inserts/updates supplied children, but does not reconcile omitted lines.
        DemoOrder order = fromInput(id, input);
        db.upsert(order);
        return find(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        DemoOrder order = db.find(DemoOrder.class, id);
        if (order == null) throw new NoSuchElementException("Pedido não encontrado");
        db.delete(order); // remove persisted lines first, even with unloaded children
    }
}
