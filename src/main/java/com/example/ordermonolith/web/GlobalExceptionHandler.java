package com.example.ordermonolith.web;

import com.example.ordermonolith.error.InsufficientStockException;
import com.example.ordermonolith.error.InvalidCheckoutException;
import com.example.ordermonolith.error.PaymentGatewayException;
import com.example.ordermonolith.error.ProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * The one place HTTP status codes are decided.
 *
 * <p><b>Why this class exists.</b> It replaces the controller's catch-all
 * {@code catch (Exception e) -> 500 "Error: " + message}, which flattened bad
 * input, missing products and gateway outages into the same opaque response.
 * Each domain exception now maps to a deliberate status and an RFC&nbsp;7807
 * {@link ProblemDetail} body. Infrastructure failures (no handler below) keep
 * Spring's default 500 &ndash; we do not dress them up.
 *
 * <table>
 *   <caption>Mapping</caption>
 *   <tr><th>Condition</th><th>Status</th></tr>
 *   <tr><td>Bean-validation / malformed body</td><td>400</td></tr>
 *   <tr><td>Unknown product</td><td>404</td></tr>
 *   <tr><td>Out of stock / invalid instrument</td><td>422</td></tr>
 *   <tr><td>Payment provider down</td><td>502</td></tr>
 * </table>
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail handleProductNotFound(ProductNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Product not found", e.getMessage());
    }

    @ExceptionHandler({InsufficientStockException.class, InvalidCheckoutException.class})
    ProblemDetail handleUnprocessable(RuntimeException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Checkout rejected", e.getMessage());
    }

    @ExceptionHandler(PaymentGatewayException.class)
    ProblemDetail handlePaymentGateway(PaymentGatewayException e) {
        log.error("Payment gateway failure", e);
        return problem(HttpStatus.BAD_GATEWAY, "Payment provider unavailable", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String globalErrors = e.getBindingResult().getGlobalErrors().stream()
                .map(oe -> oe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (!globalErrors.isBlank()) {
            detail = detail.isBlank() ? globalErrors : detail + "; " + globalErrors;
        }
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body",
                "Request body is missing or not valid JSON for this endpoint");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
