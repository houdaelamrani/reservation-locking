package com.coworking.concurrency;

import com.coworking.model.Reservation;
import com.coworking.service.ReservationService;

import javax.persistence.OptimisticLockException;
import java.util.function.Consumer;

/**
 * Gestionnaire de résilience implémentant le pattern "Optimistic Locking Retry".
 * Lors d'un conflit de concurrence (OptimisticLockException), ce composant :
 * 1. Détecte le conflit sans bloquer le système.
 * 2. Recharge l'état le plus récent de l'entité en base (avec le nouveau numéro de version).
 * 3. Réapplique la modification voulue par le client sur cette version fraîche.
 * 4. Retente le commit avec un backoff progressif (jusqu'à MAX_RETRIES).
 *
 * @author Houda El Amrani
 */
public class ReservationRetryHandler {

    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_BASE_MS = 60; // Délai d'attente progressif

    private final ReservationService service;

    public ReservationRetryHandler(ReservationService service) {
        this.service = service;
    }

    /**
     * Tente de modifier une réservation passée sous forme d'entité détachée.
     * En cas de conflit, conserve les valeurs souhaitées par l'utilisateur,
     * recharge l'entité et réapplique les modifications.
     *
     * @param reservationSouhaitee L'objet contenant les modifications demandées.
     * @return La réservation persistée avec son nouveau numéro de version.
     */
    public Reservation modifierAvecRetry(Reservation reservationSouhaitee) {
        int tentative = 0;
        Reservation entiteEnCours = reservationSouhaitee;

        // Mémorisation des modifications souhaitées par l'utilisateur
        final String espaceVoulu = reservationSouhaitee.getEspace();
        final String reserveParVoulu = reservationSouhaitee.getReservePar();
        final String motifVoulu = reservationSouhaitee.getMotif();
        final double tarifVoulu = reservationSouhaitee.getTarifHeure();

        while (tentative < MAX_RETRIES) {
            try {
                tentative++;
                System.out.printf("  [RetryHandler] 🚀 Tentative %d/%d pour réservation ID=%d (Version=@%d)...\n",
                        tentative, MAX_RETRIES, entiteEnCours.getId(), entiteEnCours.getVersion());

                Reservation resultat = service.modifierReservation(entiteEnCours);
                System.out.printf("  [RetryHandler] ✅ Succès à la tentative %d ! Nouvelle version=@%d.\n",
                        tentative, resultat.getVersion());
                return resultat;

            } catch (OptimisticLockException e) {
                System.out.printf("  [RetryHandler] ⚠️ Conflit détecté à la tentative %d : La réservation a été modifiée en parallèle !\n", tentative);

                if (tentative >= MAX_RETRIES) {
                    System.err.printf("  [RetryHandler] ❌ Échec définitif après %d tentatives. Conflit non résolu.\n", MAX_RETRIES);
                    throw new OptimisticLockException(
                        String.format("Échec de mise à jour après %d tentatives pour la réservation ID=%d", MAX_RETRIES, reservationSouhaitee.getId()),
                        e
                    );
                }

                // Pause avec backoff exponentiel pour réduire la contention
                attendreBackoff(tentative);

                // Étape clé : Recharger l'entité depuis la base pour récupérer le DERNIER numéro de version
                System.out.printf("  [RetryHandler] 🔄 Rechargement de l'entité fraîche en base (ID=%d)...\n", reservationSouhaitee.getId());
                entiteEnCours = service.trouverParId(reservationSouhaitee.getId());

                if (entiteEnCours == null) {
                    throw new IllegalStateException("La réservation ID=" + reservationSouhaitee.getId() + " n'existe plus en base.");
                }

                System.out.printf("  [RetryHandler] 📌 Version actuelle en base : @%d (modification concurrente prise en compte).\n", entiteEnCours.getVersion());

                // Réapplication des modifications métier de l'utilisateur
                entiteEnCours.setEspace(espaceVoulu);
                entiteEnCours.setReservePar(reserveParVoulu);
                entiteEnCours.setMotif(motifVoulu);
                entiteEnCours.setTarifHeure(tarifVoulu);
                if (reservationSouhaitee.getDateDebut() != null) {
                    entiteEnCours.setDateDebut(reservationSouhaitee.getDateDebut());
                }
                if (reservationSouhaitee.getDateFin() != null) {
                    entiteEnCours.setDateFin(reservationSouhaitee.getDateFin());
                }
            }
        }

        throw new OptimisticLockException("Échec après " + MAX_RETRIES + " tentatives.");
    }

    /**
     * Variante fonctionnelle (Pattern moderne) : applique une fonction de modification sur l'entité fraîche.
     */
    public Reservation modifierAvecRetryFonctionnel(Long id, Consumer<Reservation> modificateurMetier) {
        int tentative = 0;

        while (tentative < MAX_RETRIES) {
            tentative++;
            Reservation res = service.trouverParId(id);
            if (res == null) {
                throw new IllegalArgumentException("Réservation non trouvée : " + id);
            }

            // Applique la logique métier
            modificateurMetier.accept(res);

            try {
                return service.modifierReservation(res);
            } catch (OptimisticLockException e) {
                System.out.printf("  [RetryHandler-Lambda] Conflit à la tentative %d, relance...\n", tentative);
                attendreBackoff(tentative);
            }
        }
        throw new OptimisticLockException("Échec après " + MAX_RETRIES + " tentatives");
    }

    private void attendreBackoff(int tentative) {
        try {
            long sleepTime = BACKOFF_BASE_MS * tentative + (long)(Math.random() * 20);
            Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
