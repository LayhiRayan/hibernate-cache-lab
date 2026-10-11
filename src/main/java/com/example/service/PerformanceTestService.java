package com.example.service;

import com.example.model.Auteur;
import com.example.model.Livre;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import javax.persistence.*;
import java.util.List;

/** Mesures sequentielles : les statistiques sont globales a la factory. */
public class PerformanceTestService {
    private final EntityManagerFactory emf;
    private final SessionFactory sf;
    public PerformanceTestService(EntityManagerFactory emf) {
        this.emf = emf;
        this.sf = emf.unwrap(SessionFactory.class);
    }
    public void viderCache() { sf.getCache().evictAllRegions(); }
    public Mesure testPerformanceSansCache() {
        if (sf.getSessionFactoryOptions().isSecondLevelCacheEnabled())
            throw new IllegalStateException("Utiliser une factory avec le cache desactive");
        return mesurer("Sans cache - N+1", false, false, false);
    }
    public Mesure testPerformanceAvecJoinFetch() { return mesurer("JOIN FETCH", true, false, false); }
    public Mesure testPerformanceAvecEntityGraph() { return mesurer("Entity Graph", false, true, false); }
    public Mesure testRequeteCacheable() { return mesurer("Requete cacheable", false, false, true); }
    private Mesure mesurer(String nom, boolean fetch, boolean graph, boolean cacheable) {
        Statistics stats = sf.getStatistics();
        stats.clear();
        long start = System.nanoTime();
        // Nouveau contexte a chaque appel : aucune reutilisation du cache de premier niveau.
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            String jpql = fetch ? "SELECT DISTINCT a FROM Auteur a LEFT JOIN FETCH a.livres ORDER BY a.id"
                    : "SELECT DISTINCT a FROM Auteur a ORDER BY a.id";
            TypedQuery<Auteur> query = em.createQuery(jpql, Auteur.class);
            if (graph) query.setHint("javax.persistence.fetchgraph", em.getEntityGraph("Auteur.livres"));
            query.setHint("org.hibernate.cacheable", cacheable);
            List<Auteur> auteurs = query.getResultList();
            int livres = 0;
            for (Auteur auteur : auteurs) {
                // Acceder aussi aux titres pour verifier le chargement des entites Livre.
                for (Livre livre : auteur.getLivres()) {
                    if (livre.getTitre() == null) throw new IllegalStateException("Titre manquant");
                    livres++;
                }
            }
            em.getTransaction().commit();
            return new Mesure(nom, auteurs.size(), livres, System.nanoTime() - start, stats);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }
    public Mesure lireAuteur(Long id) {
        Statistics stats = sf.getStatistics();
        stats.clear();
        long start = System.nanoTime();
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Auteur auteur = em.find(Auteur.class, id);
            if (auteur == null) throw new IllegalArgumentException("Auteur inconnu");
            int livres = 0;
            for (Livre livre : auteur.getLivres()) {
                if (livre.getTitre() == null) throw new IllegalStateException("Titre manquant");
                livres++;
            }
            em.getTransaction().commit();
            return new Mesure("find Auteur + livres", 1, livres, System.nanoTime() - start, stats);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }
    public void testPerformanceAvecCache() {
        viderCache();
        System.out.println("Lecture 1 (cache vide) : " + lireAuteur(1L));
        System.out.println("Lecture 2 (nouvel EntityManager) : " + lireAuteur(1L));
        viderCache();
        System.out.println("JPQL cacheable - lecture 1 : " + testRequeteCacheable());
        System.out.println("JPQL cacheable - lecture 2 : " + testRequeteCacheable());
    }
}
