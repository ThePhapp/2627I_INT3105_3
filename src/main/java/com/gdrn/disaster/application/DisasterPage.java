package com.gdrn.disaster.application;

import com.gdrn.disaster.domain.Disaster;
import java.util.List;

public record DisasterPage(List<Disaster> items, int page, int size, long totalElements, int totalPages) {
    public DisasterPage {
        items = List.copyOf(items);
    }
}
