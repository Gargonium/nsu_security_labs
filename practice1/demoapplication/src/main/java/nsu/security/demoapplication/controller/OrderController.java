package nsu.security.demoapplication.controller;

import nsu.security.demoapplication.model.Order;
import nsu.security.demoapplication.repository.OrderRepository;
import nsu.security.demoapplication.repository.OrderJdbcRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderJdbcRepository orderJdbcRepository;

    public OrderController(OrderRepository orderRepository, OrderJdbcRepository orderJdbcRepository) {
        this.orderRepository = orderRepository;
        this.orderJdbcRepository = orderJdbcRepository;
    }
    
    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/orders")
    public Long createOrder(@RequestBody Order order) {
        Order saved = orderRepository.save(order);
        return saved.getId();
    }

    @PostMapping("/ordersql")
    public Long createOrderSql(@RequestBody Order order) {
        Order saved = orderRepository.insertOrder(order.getProductType().name(), order.getQuantity());
        return saved.getId();
    }

    @GetMapping("/orders/jdbc/{id}")
    public List<Order> getOrdersJdbc(@PathVariable String id) {
        return orderJdbcRepository.getOrders(id);
    }
}