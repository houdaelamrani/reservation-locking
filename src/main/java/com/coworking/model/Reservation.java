package com.coworking.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entité représentant une Réservation d'un espace de Coworking.
 * Intègre le mécanisme de Verrouillage Optimiste (Optimistic Locking)
 * via le champ annoté @Version.
 *
 * @author Houda El Amrani
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "espace_coworking", nullable = false, length = 100)
    private String espace;

    @Column(name = "reserve_par", nullable = false, length = 100)
    private String reservePar;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    @Column(name = "motif", length = 255)
    private String motif;

    @Column(name = "tarif_heure")
    private double tarifHeure;

    /**
     * Champ clé du verrouillage optimiste.
     * Hibernate incrémente automatiquement cette valeur à chaque mise à jour (UPDATE).
     * Si la version en base de données diffère de la version de l'entité lors du commit,
     * une exception OptimisticLockException est déclenchée.
     */
    @Version
    @Column(name = "version", nullable = false)
    private int version;

    // Constructeur sans argument obligatoire pour JPA
    public Reservation() {
    }

    // Constructeur pratique pour initialiser une réservation
    public Reservation(String espace, String reservePar, LocalDateTime dateDebut, LocalDateTime dateFin, String motif, double tarifHeure) {
        this.espace = espace;
        this.reservePar = reservePar;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motif = motif;
        this.tarifHeure = tarifHeure;
    }

    // Constructeur de compatibilité avec l'énoncé d'origine (3 paramètres)
    public Reservation(String espace, LocalDateTime dateDebut, LocalDateTime dateFin) {
        this(espace, "Anonyme", dateDebut, dateFin, "Session de travail", 50.0);
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEspace() {
        return espace;
    }

    public void setEspace(String espace) {
        this.espace = espace;
    }

    // Alias pour getSalle / setSalle pour rester compatible avec l'énoncé de base
    public String getSalle() {
        return espace;
    }

    public void setSalle(String salle) {
        this.espace = salle;
    }

    public String getReservePar() {
        return reservePar;
    }

    public void setReservePar(String reservePar) {
        this.reservePar = reservePar;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public double getTarifHeure() {
        return tarifHeure;
    }

    public void setTarifHeure(double tarifHeure) {
        this.tarifHeure = tarifHeure;
    }

    public int getVersion() {
        return version;
    }

    // Le setter de version est généralement réservé aux frameworks ou aux simulations
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return String.format(
            "Reservation[ID=%d | Espace='%s' | Réservé par='%s' | Du %s au %s | Tarif=%.1f DH/h | Version=@%d]",
            id,
            espace,
            reservePar,
            dateDebut != null ? dateDebut.format(FORMATTER) : "N/A",
            dateFin != null ? dateFin.format(FORMATTER) : "N/A",
            tarifHeure,
            version
        );
    }
}
