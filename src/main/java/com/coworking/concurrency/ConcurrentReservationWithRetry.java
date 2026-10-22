package com.coworking.concurrency;

import com.coworking.model.Reservation;
import com.coworking.service.ReservationService;
import com.coworking.service.ReservationServiceImpl;

import java.time.LocalDateTime;

/**
 * Démonstration et validation de la stratégie de résolution automatique
 * des conflits de concurrence avec {@link ReservationRetryHandler}.
 *
 * @author Houda El Amrani
 */
public class ConcurrentReservationWithRetry {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  TEST DU PATTERN RETRY : GESTION ROBUSTE DES CONFLITS OPTIMISTES");
        System.out.println("  Application : Gestion des Réservations d'Espaces Coworking");
        System.out.println("  Auteure     : Houda El Amrani");
        System.out.println("================================================================================\n");

        ReservationService service = new ReservationServiceImpl();
        ReservationRetryHandler retryHandler = new ReservationRetryHandler(service);

        try {
            // 1. Création de la réservation initiale
            Reservation reservation = new Reservation(
                    "Espace Coworking - Zone Zen",
                    "Houda El Amrani",
                    LocalDateTime.now().plusDays(3).withHour(10).withMinute(0),
                    LocalDateTime.now().plusDays(3).withHour(18).withMinute(0),
                    "Atelier Architecture Cloud",
                    65.0
            );
            reservation = service.creerReservation(reservation);
            System.out.println("1️⃣ Réservation initiale enregistrée :");
            System.out.println("   " + reservation + "\n");

            // 2. Deux transactions concurrentes lisent la même réservation (Version @0)
            System.out.println("2️⃣ Deux utilisateurs chargent la même version (@0) :");
            Reservation tx1 = service.trouverParId(reservation.getId());
            Reservation tx2 = service.trouverParId(reservation.getId());
            System.out.println("   -> Transaction 1 a chargé : Version=@" + tx1.getVersion());
            System.out.println("   -> Transaction 2 a chargé : Version=@" + tx2.getVersion() + "\n");

            // 3. Transaction 1 modifie l'espace et valide sa modification (Version passe à @1)
            System.out.println("3️⃣ Transaction 1 modifie l'espace vers 'Zone Silence & Focus' et commit :");
            tx1.setEspace("Zone Silence & Focus");
            tx1.setTarifHeure(75.0);
            service.modifierReservation(tx1);
            System.out.println("   ✅ Transaction 1 : Modification validée avec succès en base !");
            System.out.println("   -> Version en base désormais incrémentée à : @" + service.trouverParId(reservation.getId()).getVersion() + "\n");

            // 4. Transaction 2 tente d'appliquer sa modification en passant par le RetryHandler
            System.out.println("4️⃣ Transaction 2 tente de modifier vers 'Hub Événementiel & Démo' via le RetryHandler :");
            tx2.setEspace("Hub Événementiel & Démo");
            tx2.setTarifHeure(150.0);
            tx2.setMotif("Conférence Internationale DevOps");

            try {
                Reservation resultatFinal = retryHandler.modifierAvecRetry(tx2);

                System.out.println("\n🎉 RÉSULTAT FINAL APRÈS RETRY AUTOMATIQUE :");
                System.out.println("   ✅ Modification de la Transaction 2 validée avec succès !");
                System.out.println("   📌 Entité finale persistée : " + resultatFinal);
                System.out.printf("   🏆 Version finale en base : @%d (Incrémentée proprement de 0 -> 1 -> 2)\n",
                        resultatFinal.getVersion());

            } catch (Exception e) {
                System.err.println("❌ Échec inattendu après retries : " + e.getMessage());
                e.printStackTrace();
            }

            // 5. Validation de l'intégrité finale des données en base
            Reservation verification = service.trouverParId(reservation.getId());
            System.out.println("\n5️⃣ Vérification finale de l'état en base de données :");
            System.out.println("   " + verification);

        } finally {
            service.close();
        }
    }
}
