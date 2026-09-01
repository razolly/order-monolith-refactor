/**
 * Persistence layer: JPA entities ({@code entity}) and Spring Data repositories
 * ({@code repository}).
 *
 * <p><b>Why this package exists.</b> The monolith issued raw
 * {@code JdbcTemplate.queryForList} / {@code update} calls from inside the
 * controller, reading columns by upper-case {@code Map} key and re-selecting the
 * generated id with {@code ORDER BY id DESC LIMIT 1}. Persistence is now behind
 * repository interfaces returning typed entities. Entities never leave the
 * service layer &ndash; the web layer only sees DTOs &ndash; so a schema change
 * does not ripple out to the API.
 *
 * <p>{@link com.example.ordermonolith.persistence.repository.ProductRepository}
 * carries a conditional {@code UPDATE ... WHERE stock >= :qty} so concurrent
 * checkouts cannot oversell a product (the README's "safe against oversell"
 * point) without needing a schema change for an optimistic-lock column.
 */
package com.example.ordermonolith.persistence;
