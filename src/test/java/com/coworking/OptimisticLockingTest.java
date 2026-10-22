package com.coworking;

import com.coworking.concurrency.ReservationRetryHandler;
import com.coworking.model.Reservation;
import com.coworking.service.ReservationService;
import com.coworking.service.ReservationServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Tests unitaires automatisés validant le mécanisme de Verrouillage Optimiste (@Version)
 * et la stratégie de Retry avec JPA / Hibernate.
 *
 * @author Houda El Amrani
 */
public class OptimisticLockingTest {

    private ReservationService service;
    private ReservationRetryHandler retryHandler;

    @Before
    public void setUp() {
        service = new ReservationServiceImpl();
        retryHandler = new ReservationRetryHandler(service);
    }

    @After
    public void tearDown() {
        if (service != null) {
            service.close();
        }
    }

    @Test
    public void testVersionInitialiseeAZeroEtIncremente() {
        Reservation res = new Reservation(
                "Bureau Privé Alpha",
                "Houda El Amrani",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(4),
                "Réunion Projet",
                90.0
        );

        res = service.creerReservation(res);
        assertNotNull("L'identifiant généré ne doit pas être null", res.getId());
        assertEquals("La version initiale doit être à 0", 0, res.getVersion());

        // Première modification
        res.setEspace("Bureau Privé Alpha (Équipé Vidéo)");
        Reservation maj1 = service.modifierReservation(res);
        assertEquals("Après une mise à jour, la version doit s'incrémenter à 1", 1, maj1.getVersion());

        // Seconde modification
        maj1.setTarifHeure(95.0);
        Reservation maj2 = service.modifierReservation(maj1);
        assertEquals("Après une deuxième mise à jour, la version doit être à 2", 2, maj2.getVersion());
    }

    @Test(expected = OptimisticLockException.class)
    public void testConflitOptimisteDeclencheException() {
        // 1. Création initiale
        Reservation res = new Reservation(
                "Salle de Réunion Atlas",
                "Houda El Amrani",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(2),
                "Kick-off Sprint",
                70.0
        );
        res = service.creerReservation(res);
        Long id = res.getId();

        // 2. Deux entités détachées représentant deux lectures concurrentes
        Reservation tx1 = service.trouverParId(id);
        Reservation tx2 = service.trouverParId(id);

        assertEquals(0, tx1.getVersion());
        assertEquals(0, tx2.getVersion());

        // 3. tx1 valide sa mise à jour -> DB version devient 1
        tx1.setEspace("Salle Atlas - Modifié par Tx1");
        service.modifierReservation(tx1);

        // 4. tx2 tente de modifier avec sa version 0 obsolète -> DOIT lever OptimisticLockException
        tx2.setEspace("Salle Atlas - Modifié par Tx2");
        service.modifierReservation(tx2);
    }

    @Test
    public void testRetryHandlerResoutLeConflitAvecSucces() {
        Reservation res = new Reservation(
                "Espace Coworking Oasis",
                "Houda El Amrani",
                LocalDateTime.now().plusDays(4),
                LocalDateTime.now().plusDays(4).plusHours(3),
                "Formation Cloud",
                60.0
        );
        res = service.creerReservation(res);
        Long id = res.getId();

        Reservation tx1 = service.trouverParId(id);
        Reservation tx2 = service.trouverParId(id);

        // tx1 commit avec succès (version = 1)
        tx1.setEspace("Espace Oasis - Option Pro");
        service.modifierReservation(tx1);
        assertEquals(1, service.trouverParId(id).getVersion());

        // tx2 utilise le retryHandler pour appliquer sa modification malgré le conflit
        tx2.setEspace("Espace Oasis - Option Entreprise");
        tx2.setTarifHeure(110.0);

        Reservation resultat = retryHandler.modifierAvecRetry(tx2);

        assertNotNull(resultat);
        assertEquals("Espace Oasis - Option Entreprise", resultat.getEspace());
        assertEquals(110.0, resultat.getTarifHeure(), 0.001);
        assertEquals("La version finale doit être passée à 2", 2, resultat.getVersion());
    }

    @Test
    public void testConflitMultiThreadAvecCountDownLatch() throws InterruptedException {
        Reservation res = new Reservation(
                "Pod Acoustique",
                "Houda El Amrani",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                "Call Client",
                35.0
        );
        res = service.creerReservation(res);
        final Long resId = res.getId();

        EntityManagerFactory emf = service.getEntityManagerFactory();
        CountDownLatch latchPrets = new CountDownLatch(2);
        CountDownLatch latchDepart = new CountDownLatch(1);
        CountDownLatch latchThread1Done = new CountDownLatch(1);

        AtomicBoolean conflictDetected = new AtomicBoolean(false);

        Thread t1 = new Thread(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                Reservation r1 = em.find(Reservation.class, resId);
                latchPrets.countDown();
                latchDepart.await();

                r1.setEspace("Pod Acoustique - Thread 1");
                em.getTransaction().commit();
                latchThread1Done.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                em.close();
            }
        });

        Thread t2 = new Thread(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                Reservation r2 = em.find(Reservation.class, resId);
                latchPrets.countDown();
                latchDepart.await();

                latchThread1Done.await(); // Attend que T1 ait fait son commit
                r2.setEspace("Pod Acoustique - Thread 2");
                em.getTransaction().commit();
            } catch (OptimisticLockException | javax.persistence.RollbackException e) {
                conflictDetected.set(true);
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                em.close();
            }
        });

        t1.start();
        t2.start();

        latchPrets.await();
        latchDepart.countDown();

        t1.join();
        t2.join();

        assertTrue("Le conflit optimiste doit être détecté lors de la collision de threads", conflictDetected.get());
    }
}
