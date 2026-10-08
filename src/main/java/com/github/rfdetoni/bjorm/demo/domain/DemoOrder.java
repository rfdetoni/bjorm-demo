package com.github.rfdetoni.bjorm.demo.domain;

import com.github.rfdetoni.bjorm.Children;
import com.github.rfdetoni.bjorm.Id;
import com.github.rfdetoni.bjorm.JoinType;
import com.github.rfdetoni.bjorm.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table("demo_orders")
public class DemoOrder {
    @Id private UUID id;
    private String customer;
    @Children(mappedBy = "orderId", type = JoinType.LEFT) private List<DemoOrderLine> lines = new ArrayList<>();

    public DemoOrder() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public List<DemoOrderLine> getLines() { return lines; }
    public void setLines(List<DemoOrderLine> lines) { this.lines = lines; }
}
