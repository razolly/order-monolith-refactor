package com.example.ordermonolith.persistence.repository;

import com.example.ordermonolith.persistence.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Atomically decrement stock only if enough is available.
     *
     * <p>The {@code AND p.stock >= :quantity} guard makes this a compare-and-set:
     * two concurrent checkouts for the last unit cannot both succeed, because the
     * database serialises the row update and the loser matches 0 rows. The caller
     * treats a 0 return as {@code InsufficientStockException} &ndash; no
     * read-modify-write window, no schema change for a {@code @Version} column.
     *
     * @return number of rows updated: 1 on success, 0 if stock was insufficient
     */
    @Modifying
    @Query("""
            update Product p
               set p.stock = p.stock - :quantity
             where p.id = :productId
               and p.stock >= :quantity
            """)
    int decrementStock(@Param("productId") long productId, @Param("quantity") int quantity);
}
