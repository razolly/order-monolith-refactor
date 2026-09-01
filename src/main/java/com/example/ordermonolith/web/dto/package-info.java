/**
 * Request/response types for the checkout endpoint, plus the conditional
 * card-details constraint.
 *
 * <p><b>Why these exist.</b> They are the typed replacement for the monolith's
 * {@code Map<String, Object>} request and {@code LinkedHashMap} response. Being
 * records with Jakarta Bean Validation annotations, they collapse the seven-level
 * {@code if/else} validation pyramid into declarations the framework enforces
 * before the controller runs. They are intentionally distinct from the JPA
 * entities: the wire contract and the database schema evolve independently.
 */
package com.example.ordermonolith.web.dto;
