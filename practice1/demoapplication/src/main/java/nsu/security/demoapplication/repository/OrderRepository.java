package nsu.security.demoapplication.repository;

import nsu.security.demoapplication.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Transactional
    @Query(value = "INSERT INTO orders (product_type, quantity) VALUES (:productType, :quantity) RETURNING *",
            nativeQuery = true)
    Order insertOrder(@Param("productType") String productType, @Param("quantity") String quantity);
}