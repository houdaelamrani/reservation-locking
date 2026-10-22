package com.coworking.service;

import com.coworking.model.Reservation;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;
import java.util.List;

/**
 * Implémentation du service de gestion des réservations.
 * Gère le cycle de vie JPA et le verrouillage optimiste via EntityManager.
 *
 * @author Houda El Amrani
 */
public class ReservationServiceImpl implements ReservationService {

    private final EntityManagerFactory emf;

    public ReservationServiceImpl() {
        this("coworkingReservationPU");
    }

    public ReservationServiceImpl(String persistenceUnitName) {
        this.emf = Persistence.createEntityManagerFactory(persistenceUnitName);
    }

    @Override
    public Reservation creerReservation(Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(reservation);
            em.getTransaction().commit();
            return reservation;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Modification d'une réservation avec gestion du verrouillage optimiste.
     * En utilisant em.merge(reservation) sur une entité détachée portant un numéro de version,
     * Hibernate compare le champ @Version de l'entité avec la valeur actuelle en base de données.
     *
     * Requête SQL exécutée par Hibernate :
     * UPDATE reservations SET espace=?, reserve_par=?, date_debut=?, date_fin=?, motif=?, tarif_heure=?, version=?
     * WHERE id=? AND version=?
     *
     * Si aucune ligne n'est mise à jour (car un commit concurrent a déjà incrémenté la version),
     * JPA lève immédiatement une OptimisticLockException.
     */
    @Override
    public Reservation modifierReservation(Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // em.merge réattache l'entité détachée et valide la cohérence du champ @Version
            Reservation managed = em.merge(reservation);

            // em.flush force l'exécution immédiate de l'instruction SQL UPDATE
            em.flush();

            em.getTransaction().commit();
            return managed;
        } catch (OptimisticLockException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            // Propager l'exception spécifique pour permettre une stratégie de Retry
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public Reservation trouverParId(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Reservation.class, id);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Reservation> listerReservations() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT r FROM Reservation r ORDER BY r.id ASC", Reservation.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return this.emf;
    }

    @Override
    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
