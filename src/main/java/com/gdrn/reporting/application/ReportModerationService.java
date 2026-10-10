package com.gdrn.reporting.application;

import com.gdrn.reporting.application.contract.LinkedDisasterState;
import com.gdrn.reporting.application.port.DisasterLookup;
import com.gdrn.reporting.application.port.ReportStore;
import com.gdrn.reporting.domain.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

public final class ReportModerationService {
    private final ReportStore reports;
    private final DisasterLookup disasters;
    private final Clock clock;

    public ReportModerationService(ReportStore reports, DisasterLookup disasters, Clock clock) {
        this.reports = reports;
        this.disasters = disasters;
        this.clock = clock;
    }

    public record Decision(ReportStatus status, UUID disasterId, String rejectionReason) {
        public Decision {
            if (status == ReportStatus.VERIFIED) {
                if (disasterId == null || rejectionReason != null) throw new InvalidReport("disasterId");
            } else if (status == ReportStatus.REJECTED) {
                if (disasterId != null) throw new InvalidReport("disasterId");
                rejectionReason = Report.reason(rejectionReason);
            } else throw new InvalidReport("status");
        }
    }

    public Report decide(ReportActor actor, UUID id, Decision decision) {
        actor.requireAuthority();
        Report current = reports.findById(id).orElseThrow(ReportNotFound::new);
        current.requirePending();
        if (decision.status() == ReportStatus.VERIFIED) {
            var disaster = disasters.findByIds(Set.of(decision.disasterId())).get(decision.disasterId());
            if (disaster == null) throw new ReportNotFound();
            if (disaster.status() != LinkedDisasterState.ACTIVE) throw new DisasterNotActive();
        }
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Report changed = decision.status() == ReportStatus.VERIFIED
                ? current.verify(decision.disasterId(), now) : current.reject(decision.rejectionReason(), now);
        persist(current, changed);
        return changed;
    }

    public void withdraw(ReportActor actor, UUID id) {
        actor.requireCitizen();
        Report current = reports.findById(id).orElseThrow(ReportNotFound::new);
        if (!current.reporterId().equals(actor.id())) throw new ReportNotFound();
        persist(current, current.withdraw(clock.instant().truncatedTo(ChronoUnit.MICROS)));
    }

    private void persist(Report current, Report changed) {
        if (!reports.updatePending(changed, current.version())) throw new InvalidReportTransition();
    }
}
