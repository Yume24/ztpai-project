package com.rendezvous.location.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record LocationBatchRequest(@NotNull @Size(min = 1, max = 50) List<@Valid LocationPointRequest> points) {
}
