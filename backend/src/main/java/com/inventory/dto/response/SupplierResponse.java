package com.inventory.dto.response;

import com.inventory.entity.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupplierResponse fromEntity(Supplier supplier) {
        if (supplier == null) return null;
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .active(supplier.isActive())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
