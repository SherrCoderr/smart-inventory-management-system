package com.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String sku, int requested, int available) {
        super(String.format("Insufficient stock for product SKU '%s'. Requested: %d, Available: %d", sku, requested, available));
    }
}
