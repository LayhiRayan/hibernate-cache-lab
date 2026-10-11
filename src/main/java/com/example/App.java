package com.example;

import com.example.service.DataInitService;
import com.example.service.PerformanceTestService;
import com.example.util.JpaUtil;
import javax.persistence.EntityManagerFactory;

public class App {
    public static void main(String[] args) {
        System.out.println("=== 1. Sans cache de second niveau : 10 auteurs et 50 livres ===");
        EntityManagerFactory sansCache = JpaUtil.creerFactory(false, "sans_cache", true);
        try {
            new DataInitService(sansCache).initData();
            PerformanceTestService tests = new PerformanceTestService(sansCache);
            System.out.println(tests.testPerformanceSansCache());
            System.out.println(tests.testPerformanceAvecJoinFetch());
            System.out.println(tests.testPerformanceAvecEntityGraph());
        } finally { sansCache.close(); }

        System.out.println("\n=== 2. Cache de second niveau et cache de requetes ===");
        EntityManagerFactory avecCache = JpaUtil.creerFactory(true, "avec_cache", true);
        try {
            new DataInitService(avecCache).initData();
            PerformanceTestService tests = new PerformanceTestService(avecCache);
            tests.testPerformanceAvecCache();
            System.out.println("JPQL sans cache de requete, meme apres rechauffement : " + tests.testPerformanceAvecJoinFetch());
        } finally { avecCache.close(); }
        System.out.println("Les durees sont indicatives ; comparer surtout SQL et les hits de cache.");
    }
}
