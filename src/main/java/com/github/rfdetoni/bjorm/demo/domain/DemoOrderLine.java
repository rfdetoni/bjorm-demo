package com.github.rfdetoni.bjorm.demo.domain;

import com.github.rfdetoni.bjorm.Id;
import com.github.rfdetoni.bjorm.Table;
import java.util.UUID;

@Table("demo_order_lines")
public class DemoOrderLine {
    @Id(uuidV7 = true) private UUID id;
    private UUID orderId;
    private String sku;
    private int quantity;

    public DemoOrderLine() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
