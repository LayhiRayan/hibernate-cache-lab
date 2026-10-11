package com.example;

import com.example.model.Auteur;
import com.example.model.Livre;
import com.example.service.DataInitService;
import com.example.service.Mesure;
import com.example.service.PerformanceTestService;
import com.example.util.JpaUtil;
import org.junit.After;
import org.junit.Test;
import javax.persistence.*;
import java.util.UUID;
import java.util.function.Consumer;
import static org.junit.Assert.*;

public class CacheLabTest {
    private EntityManagerFactory emf;
    private PerformanceTestService tests;

    private void initialiser(boolean cache) {
        emf = JpaUtil.creerFactory(cache, "test_" + UUID.randomUUID(), false);
        new DataInitService(emf).initData();
        tests = new PerformanceTestService(emf);
        tests.viderCache();
    }
    @After public void fermer() { if (emf != null) emf.close(); }
    private void verifierDonnees(Mesure mesure) {
        assertEquals(10, mesure.auteurs);
        assertEquals(50, mesure.livres);
    }
    private void ecrire(Consumer<EntityManager> action) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            action.accept(em);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }
    @Test public void sansCacheProduitOnzeRequetesMemeALaSecondeLecture() {
        initialiser(false);
        for (int i = 0; i < 2; i++) {
            Mesure mesure = tests.testPerformanceSansCache();
            verifierDonnees(mesure);
            assertEquals(11, mesure.requetesSql);
            assertEquals(0, mesure.hitsL2);
        }
    }
    @Test public void joinFetchChargeToutEnUneRequeteSansDoublons() {
        initialiser(false);
        Mesure mesure = tests.testPerformanceAvecJoinFetch();
        verifierDonnees(mesure);
        assertEquals(1, mesure.requetesSql);
        ecrire(em -> em.persist(new Auteur("Sans livre")));
        Mesure avecAuteurVide = tests.testPerformanceAvecJoinFetch();
        assertEquals(11, avecAuteurVide.auteurs);
        assertEquals(50, avecAuteurVide.livres);
        assertEquals(1, avecAuteurVide.requetesSql);
    }
    @Test public void entityGraphChargeToutEnUneRequeteSansDoublons() {
        initialiser(false);
        Mesure mesure = tests.testPerformanceAvecEntityGraph();
        verifierDonnees(mesure);
        assertEquals(1, mesure.requetesSql);
    }
    @Test public void cacheL2PartageEntreContextesEtMisAJourApresEcriture() {
        initialiser(true);
        Mesure froide = tests.lireAuteur(1L);
        assertEquals(2, froide.requetesSql);
        assertTrue(froide.putsL2 > 0);
        Mesure chaude = tests.lireAuteur(1L);
        assertEquals(0, chaude.requetesSql);
        assertEquals(5, chaude.livres);
        assertTrue(chaude.hitsL2 >= 7); // Auteur, collection et cinq Livre.
        ecrire(em -> em.find(Auteur.class, 1L).setNom("Nom modifie"));
        EntityManager em = emf.createEntityManager();
        try { assertEquals("Nom modifie", em.find(Auteur.class, 1L).getNom()); }
        finally { em.close(); }
    }
    @Test public void cacheRequeteEviteSQLMaisJPQLNonCacheableExecuteEncoreSQL() {
        initialiser(true);
        Mesure froide = tests.testRequeteCacheable();
        verifierDonnees(froide);
        assertEquals(11, froide.requetesSql);
        Mesure chaude = tests.testRequeteCacheable();
        verifierDonnees(chaude);
        assertEquals(0, chaude.requetesSql);
        assertEquals(1, chaude.hitsRequetes);
        assertTrue(chaude.hitsL2 > 0);
        assertEquals(1, tests.testPerformanceAvecJoinFetch().requetesSql);
    }
    @Test public void insertionInvalideLesResultatsDeRequeteEnCache() {
        initialiser(true);
        tests.testRequeteCacheable();
        assertEquals(0, tests.testRequeteCacheable().requetesSql);
        ecrire(em -> em.persist(new Auteur("Nouvel auteur")));
        Mesure nouvelle = tests.testRequeteCacheable();
        assertEquals(11, nouvelle.auteurs);
        assertEquals(50, nouvelle.livres);
        assertTrue(nouvelle.requetesSql > 0);
        assertEquals(0, nouvelle.hitsRequetes);
    }
    @Test public void suppressionOrphelineActualiseLeCacheDeCollection() {
        initialiser(true);
        tests.lireAuteur(1L);
        final Long[] livreSupprime = new Long[1];
        ecrire(em -> {
            Auteur auteur = em.find(Auteur.class, 1L);
            Livre livre = auteur.getLivres().get(0);
            livreSupprime[0] = livre.getId();
            auteur.retirerLivre(livre);
            assertNull(livre.getAuteur());
        });
        assertEquals(4, tests.lireAuteur(1L).livres);
        Mesure chaude = tests.lireAuteur(1L);
        assertEquals(4, chaude.livres);
        assertEquals(0, chaude.requetesSql);
        EntityManager em = emf.createEntityManager();
        try { assertNull(em.find(Livre.class, livreSupprime[0])); }
        finally { em.close(); }
    }
}
