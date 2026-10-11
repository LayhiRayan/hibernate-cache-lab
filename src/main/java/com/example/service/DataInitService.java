package com.example.service;

import com.example.model.Auteur;
import com.example.model.Livre;
import javax.persistence.*;

public class DataInitService {
    private final EntityManagerFactory emf;
    public DataInitService(EntityManagerFactory emf) { this.emf = emf; }
    public void initData() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            for (int i = 1; i <= 10; i++) {
                Auteur auteur = new Auteur("Auteur " + i);
                for (int j = 1; j <= 5; j++) auteur.ajouterLivre(new Livre("Livre " + j + " de l'auteur " + i));
                em.persist(auteur);
            }
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }
}
