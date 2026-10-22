package com.coworking.service;

import com.coworking.model.Reservation;

import javax.persistence.EntityManagerFactory;
import java.util.List;

/**
 * Interface de service pour les opérations de gestion des réservations
 * avec support des transactions et de la détection de concurrence optimiste.
 *
 * @author Houda El Amrani
 */
public interface ReservationService {

    /**
     * Enregistre une nouvelle réservation en base de données.
     * Sa version initiale sera initialisée à 0 par Hibernate.
     */
    Reservation creerReservation(Reservation reservation);

    /**
     * Met à jour une réservation existante.
     * Si l'entité passée est détachée (cas typique des architectures Web/REST/Client-Serveur),
     * JPA merge() vérifie le numéro de version avec celui en base de données.
     *
     * @param reservation L'entité contenant les nouvelles valeurs et sa version d'origine.
     * @return L'entité mise à jour et gérée par le contexte de persistance.
     * @throws javax.persistence.OptimisticLockException Si un conflit de version est détecté.
     */
    Reservation modifierReservation(Reservation reservation);

    /**
     * Recherche une réservation par son identifiant unique.
     */
    Reservation trouverParId(Long id);

    /**
     * Retourne la liste complète des réservations enregistrées.
     */
    List<Reservation> listerReservations();

    /**
     * Donne accès à l'EntityManagerFactory pour des simulations multi-thread avancées.
     */
    EntityManagerFactory getEntityManagerFactory();

    /**
     * Ferme l'EntityManagerFactory et libère les ressources.
     */
    void close();
}
