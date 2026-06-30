package fr.agroscan.repository;

import java.time.Instant;

public interface PlotHealthRow {
    Long getPlotId();

    String getPlotName();

    long getTotal();

    long getHealthy();

    long getDiseased();

    Instant getLatestScanAt();
}
