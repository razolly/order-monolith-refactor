package com.example.ordermonolith.persistence.repository;

import com.example.ordermonolith.persistence.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
