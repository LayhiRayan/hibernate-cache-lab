package com.example.service;

import org.hibernate.stat.Statistics;

public class Mesure {
    public final String scenario;
    public final int auteurs;
    public final int livres;
    public final long requetesSql;
    public final long hitsL2;
    public final long missesL2;
    public final long putsL2;
    public final long hitsRequetes;
    public final double millisecondes;

    public Mesure(String scenario, int auteurs, int livres, long nanos, Statistics stats) {
        this.scenario = scenario;
        this.auteurs = auteurs;
        this.livres = livres;
        this.requetesSql = stats.getPrepareStatementCount();
        this.hitsL2 = stats.getSecondLevelCacheHitCount();
        this.missesL2 = stats.getSecondLevelCacheMissCount();
        this.putsL2 = stats.getSecondLevelCachePutCount();
        this.hitsRequetes = stats.getQueryCacheHitCount();
        this.millisecondes = nanos / 1_000_000.0;
    }
    @Override public String toString() {
        return String.format(java.util.Locale.ROOT,
                "%s : %d auteurs, %d livres | SQL=%d | L2 hits=%d misses=%d puts=%d | Query hits=%d | %.3f ms",
                scenario, auteurs, livres, requetesSql, hitsL2, missesL2, putsL2, hitsRequetes, millisecondes);
    }
}
