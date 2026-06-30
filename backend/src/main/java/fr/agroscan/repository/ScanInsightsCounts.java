package fr.agroscan.repository;

public record ScanInsightsCounts(
        long total,
        long uploaded,
        long pending,
        long running,
        long analyzed,
        long failed,
        long healthy,
        long diseased
) {
}
