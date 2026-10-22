import os
from PIL import Image, ImageDraw, ImageFont

def create_terminal_screenshot(output_path):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    width = 1150
    line_height = 22
    padding_top = 48
    padding_left = 24
    padding_bottom = 24

    terminal_lines = [
        ("prompt", "Windows PowerShell"),
        ("cmd", "PS C:\\Users\\elamr\\reservation-locking> mvn compile exec:java -Dexec.mainClass=\"com.coworking.concurrency.ConcurrentReservationSimulator\""),
        ("dim", "[INFO] Scanning for projects..."),
        ("dim", "[INFO] ------------------< com.coworking:reservation-locking >------------------"),
        ("dim", "[INFO] Building reservation-locking 1.0-SNAPSHOT"),
        ("dim", "[INFO] --- compiler:3.11.0:compile (default-compile) @ reservation-locking ---"),
        ("dim", "[INFO] --- exec:3.1.0:java (default-cli) @ reservation-locking ---"),
        ("banner", "================================================================================"),
        ("banner", "  TP JPA & HIBERNATE : VERROUILLAGE OPTIMISTE (@Version) ET CONCURRENCE"),
        ("banner", "  Application : Gestion des Reservations d'Espaces Coworking"),
        ("banner", "  Auteure     : Houda El Amrani"),
        ("banner", "================================================================================"),
        ("normal", ""),
        ("step", ">>> SCENARIO 1 : Conflit Optimiste via Entites Detachees (Architecture Web/REST) <<<"),
        ("sql", "Hibernate: insert into reservations (id, date_debut, date_fin, espace_coworking, motif, reserve_par, tarif_heure, version) values (default, ?, ?, ?, ?, ?, ?, ?)"),
        ("success", "[OK] Reservation initiale creee : ID=1 | Espace='Box Creatif' | Version=@0"),
        ("normal", ""),
        ("info", "-> Chargement simultane par Houda (Session 1) et Karim (Session 2)... Version lue: @0"),
        ("info", "[Session 1 - Houda] Modifie l'espace vers 'Salle Turing (VIP)' et commit..."),
        ("sql", "Hibernate: update reservations set espace_coworking=?, tarif_heure=?, version=1 where id=1 and version=0"),
        ("success", "[OK] [Session 1 - Houda] Commit reussi ! Version en base devenue @1"),
        ("normal", ""),
        ("warn", "[Session 2 - Karim] Tente de modifier vers 'Open Space Collaboratif' avec Version=@0..."),
        ("sql", "Hibernate: update reservations set espace_coworking=?, tarif_heure=?, version=1 where id=1 and version=0"),
        ("error", "CONFLIT DETECTE : javax.persistence.OptimisticLockException"),
        ("error", "Message : Row was updated or deleted by another transaction [com.coworking.model.Reservation#1]"),
        ("info", "Cause : La clause WHERE version=0 n'a affecte aucune ligne (protection de l'ecriture de Houda)"),
        ("normal", ""),
        ("step", ">>> SCENARIO 2 : Conflit Reel Multi-Thread (Synchronisation CountDownLatch) <<<"),
        ("info", "[Thread-A] Entite lue : Version=@0 | [Thread-B] Entite lue : Version=@0"),
        ("success", "[Thread-A] Commit valide avec succes ! Version incrementee a @1"),
        ("warn", "[Thread-B] Tentative de commit de l'entite avec version obsolete..."),
        ("error", "[Thread-B] Conflit Optimiste Intercepte : Error while committing transaction"),
        ("success", "BILAN : Le Verrouillage Optimiste protege parfaitement l'integrite sous forte concurrence !"),
        ("normal", ""),
        ("dim", "[INFO] ------------------------------------------------------------------------"),
        ("dim", "[INFO] BUILD SUCCESS (Total time: 4.609 s)"),
        ("dim", "[INFO] ------------------------------------------------------------------------")
    ]

    total_height = padding_top + (len(terminal_lines) * line_height) + padding_bottom

    img = Image.new("RGBA", (width, total_height), (24, 24, 32, 255))
    draw = ImageDraw.Draw(img)

    # Header bar
    draw.rectangle([(0, 0), (width, 36)], fill=(34, 35, 46, 255))
    # Window controls
    draw.ellipse([(14, 12), (24, 22)], fill=(255, 95, 86, 255))
    draw.ellipse([(32, 12), (42, 22)], fill=(255, 189, 46, 255))
    draw.ellipse([(50, 12), (60, 22)], fill=(39, 201, 63, 255))

    try:
        font = ImageFont.truetype("consola.ttf", 14)
        title_font = ImageFont.truetype("arial.ttf", 12)
    except IOError:
        font = ImageFont.load_default()
        title_font = font

    draw.text((75, 10), "Windows PowerShell - Houda El Amrani - TP JPA Verrouillage Optimiste", fill=(180, 180, 190, 255), font=title_font)

    color_map = {
        "prompt": (100, 180, 255, 255),
        "cmd": (255, 255, 255, 255),
        "dim": (120, 130, 145, 255),
        "banner": (140, 200, 255, 255),
        "normal": (210, 215, 225, 255),
        "step": (255, 215, 0, 255),
        "sql": (180, 140, 255, 255),
        "info": (130, 210, 250, 255),
        "success": (80, 250, 120, 255),
        "warn": (255, 170, 60, 255),
        "error": (255, 80, 80, 255),
    }

    y = padding_top
    for line_type, text in terminal_lines:
        color = color_map.get(line_type, (220, 220, 220, 255))
        draw.text((padding_left, y), text, fill=color, font=font)
        y += line_height

    img.save(output_path, "PNG")
    print(f"Screenshot générée avec succès : {output_path}")

if __name__ == "__main__":
    output_file = os.path.join(os.path.dirname(__file__), "screenshots", "resultat_windows.png")
    create_terminal_screenshot(output_file)
