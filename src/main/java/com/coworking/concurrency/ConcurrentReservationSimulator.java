package com.coworking.concurrency;

import com.coworking.model.Reservation;
import com.coworking.service.ReservationService;
import com.coworking.service.ReservationServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulateur de conflits de concurrence démontrant le comportement
 * du Verrouillage Optimiste (@Version) avec JPA et Hibernate.
 *
 * Scénario 1 : Conflit via Entités Détachées (Pattern Web / API REST)
 * Scénario 2 : Conflit Multi-Thread Réel avec Threads et CountDownLatch
 *
 * @author Houda El Amrani
 */
public class ConcurrentReservationSimulator {

    public static void main(String[] args) throws InterruptedException {
        afficherBanniere();

        ReservationService service = new ReservationServiceImpl();

        try {
            // =========================================================================
            // SCÉNARIO 1 : Conflit par entités détachées (Cas classique Web / REST)
            // =========================================================================
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════════════════╗");
            System.out.println("║ SCÉNARIO 1 : Conflit Optimiste via Entités Détachées (Architecture Web/REST)  ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝\n");

            // 1. Création d'une réservation initiale
            Reservation resInitiale = new Reservation(
                    "Box Créatif - Coworking Centre",
                    "Houda El Amrani",
                    LocalDateTime.now().plusDays(2).withHour(9).withMinute(0),
                    LocalDateTime.now().plusDays(2).withHour(12).withMinute(0),
                    "Brainstorming Architecture Microservices",
                    80.0
            );
            resInitiale = service.creerReservation(resInitiale);
            System.out.println("✅ Réservation initiale créée :");
            System.out.println("   " + resInitiale);
            System.out.printf("   -> Numéro de version initial en base : @%d\n\n", resInitiale.getVersion());

            // 2. Deux utilisateurs (ou sessions web) chargent simultanément la même réservation
            System.out.println("📥 Chargement simultané par deux utilisateurs :");
            Reservation user1View = service.trouverParId(resInitiale.getId());
            Reservation user2View = service.trouverParId(resInitiale.getId());

            System.out.printf("   [Session 1 - Houda] Données chargées : Espace='%s', Version=@%d\n",
                    user1View.getEspace(), user1View.getVersion());
            System.out.printf("   [Session 2 - Karim] Données chargées : Espace='%s', Version=@%d\n\n",
                    user2View.getEspace(), user2View.getVersion());

            // 3. L'utilisateur 1 modifie l'espace et valide sa transaction en premier
            System.out.println("✍️  [Session 1 - Houda] Modifie l'espace vers 'Salle Turing (VIP)' et valide...");
            user1View.setEspace("Salle Turing (VIP)");
            user1View.setTarifHeure(120.0);
            Reservation updated1 = service.modifierReservation(user1View);

            System.out.println("✅ [Session 1 - Houda] Commit réussi !");
            System.out.printf("   -> Nouveau statut en base : Espace='%s', Version incrémentée à : @%d\n\n",
                    updated1.getEspace(), updated1.getVersion());

            // 4. L'utilisateur 2 tente de valider sa modification avec son entité basée sur l'ancienne version (@0)
            System.out.println("✍️  [Session 2 - Karim] Tente de modifier vers 'Open Space Collaboratif'...");
            System.out.printf("   -> Karim soumet une entité dont la version est toujours : @%d (périmée !)\n",
                    user2View.getVersion());

            user2View.setEspace("Open Space Collaboratif");
            user2View.setTarifHeure(45.0);

            try {
                service.modifierReservation(user2View);
                System.out.println("❌ ERREUR : La modification n'aurait pas dû réussir !");
            } catch (OptimisticLockException e) {
                System.out.println("\n🎯 CONFLIT DÉTECTÉ AVEC SUCCÈS PAR HIBERNATE !");
                System.out.println("   Type d'exception : " + e.getClass().getName());
                System.out.println("   Message d'erreur : " + e.getMessage());
                System.out.println("   Cause sous-jacente : La clause SQL 'WHERE id=? AND version=0' n'a affecté 0 ligne !");
                System.out.println("   Protection validée : Les modifications de Houda n'ont PAS été écrasées par Karim.");
            }

            // Vérification de l'état final en base
            Reservation etatFinal = service.trouverParId(resInitiale.getId());
            System.out.println("\n🔍 État actuel préservé en base de données :");
            System.out.println("   " + etatFinal);

            // =========================================================================
            // SCÉNARIO 2 : Conflit Réel Multi-Thread (2 Threads avec CountDownLatch)
            // =========================================================================
            System.out.println("\n\n╔═══════════════════════════════════════════════════════════════════════════════╗");
            System.out.println("║ SCÉNARIO 2 : Conflit Réel Multi-Thread (Synchronisation CountDownLatch)       ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝\n");

            simulerConflitMultiThread(service);

        } finally {
            service.close();
        }
    }

    /**
     * Démontre un conflit de concurrence multi-thread réel où deux threads
     * ouvrent chacun leur propre EntityManager et s'affrontent sur le commit.
     */
    private static void simulerConflitMultiThread(ReservationService service) throws InterruptedException {
        // Création d'une nouvelle réservation test
        Reservation res = new Reservation(
                "Salle Ada Lovelace",
                "Direction R&D",
                LocalDateTime.now().plusDays(1).withHour(14).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(17).withMinute(0),
                "Workshop Concurrence & JPA",
                100.0
        );
        res = service.creerReservation(res);
        final Long reservationId = res.getId();

        System.out.println("📌 Réservation créée pour le test multi-thread : ID=" + reservationId + " (Version=@" + res.getVersion() + ")");

        EntityManagerFactory emf = service.getEntityManagerFactory();

        CountDownLatch deuxThreadsPrets = new CountDownLatch(2);
        CountDownLatch signalDepart = new CountDownLatch(1);
        CountDownLatch commitThread1Termine = new CountDownLatch(1);

        AtomicBoolean thread2ConflitAttrape = new AtomicBoolean(false);

        // Thread 1 : Utilisateur A
        Thread threadA = new Thread(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                Reservation r1 = em.find(Reservation.class, reservationId);
                System.out.printf("  [Thread-A] Entité lue : Version=@%d\n", r1.getVersion());

                deuxThreadsPrets.countDown();
                signalDepart.await(); // Attend que les deux threads soient synchronisés

                // Thread A effectue sa mise à jour
                r1.setEspace("Salle Ada Lovelace - Réservée par Thread-A");
                em.getTransaction().commit();
                System.out.printf("  [Thread-A] ✅ Commit validé ! Version en base devenue @%d\n", r1.getVersion());

                commitThread1Termine.countDown(); // Notifie le thread B qu'il peut tenter son commit
            } catch (Exception ex) {
                System.err.println("  [Thread-A] Erreur inattendue : " + ex.getMessage());
            } finally {
                em.close();
            }
        }, "Thread-Utilisateur-A");

        // Thread 2 : Utilisateur B
        Thread threadB = new Thread(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                Reservation r2 = em.find(Reservation.class, reservationId);
                System.out.printf("  [Thread-B] Entité lue : Version=@%d\n", r2.getVersion());

                deuxThreadsPrets.countDown();
                signalDepart.await();

                // Attend que le Thread-A ait commit en premier pour garantir le conflit
                commitThread1Termine.await();

                // Thread B tente d'appliquer sa modification sur son instance chargée avant le commit de A
                r2.setEspace("Salle Ada Lovelace - Tentative par Thread-B");
                System.out.println("  [Thread-B] ⏳ Tentative de commit de l'entité avec version obsolète...");

                em.getTransaction().commit();
                System.err.println("  [Thread-B] ❌ Échec du test : Le commit aurait dû être rejeté !");

            } catch (OptimisticLockException | javax.persistence.RollbackException e) {
                thread2ConflitAttrape.set(true);
                System.out.println("  [Thread-B] 🎯 Conflit Optimiste Intercepté avec succès !");
                System.out.println("  [Thread-B]    Message : " + e.getMessage());
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } catch (Exception ex) {
                System.err.println("  [Thread-B] Autre exception : " + ex.getMessage());
            } finally {
                em.close();
            }
        }, "Thread-Utilisateur-B");

        // Démarrage des threads
        threadA.start();
        threadB.start();

        // Attend que les deux threads aient lu l'entité
        deuxThreadsPrets.await();
        // Donne le signal de départ
        signalDepart.countDown();

        threadA.join();
        threadB.join();

        System.out.println("\n📊 Bilan de la simulation Multi-Thread :");
        if (thread2ConflitAttrape.get()) {
            System.out.println("✅ SUCCÈS : Le Verrouillage Optimiste protège parfaitement l'intégrité face aux threads concurrents !");
        } else {
            System.out.println("⚠️ Attention : Conflit non intercepté.");
        }
    }

    private static void afficherBanniere() {
        System.out.println("================================================================================");
        System.out.println("  TP JPA & HIBERNATE : VERROUILLAGE OPTIMISTE (@Version) ET CONCURRENCE");
        System.out.println("  Application : Gestion des Réservations d'Espaces Coworking");
        System.out.println("  Auteure     : Houda El Amrani");
        System.out.println("================================================================================");
    }
}
