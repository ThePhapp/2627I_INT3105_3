package com.gdrn.disaster.application;

import com.gdrn.disaster.domain.DisasterStatus;
import com.gdrn.disaster.domain.DisasterType;
import java.util.Optional;

public record DisasterSearch(int page, int size, Sort sort, Optional<DisasterStatus> status,
                             Optional<DisasterType> type) {
    public DisasterSearch {
        if (page < 0 || page > 1_000_000) throw new InvalidDisasterInput("page");
        if (size < 1 || size > 100) throw new InvalidDisasterInput("size");
        if (sort == null) throw new InvalidDisasterInput("sort");
        status = status == null ? Optional.empty() : status;
        type = type == null ? Optional.empty() : type;
    }
    public enum Sort { CREATED_AT_ASC, CREATED_AT_DESC }
}
