package com.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStockMovementResponse {

    private LocalDate date;
    @Builder.Default
    private long stockIn = 0;
    @Builder.Default
    private long stockOut = 0;
}
