import sqlite3, os, sys

OUT = sys.argv[1]
if os.path.exists(OUT): os.remove(OUT)
IDENTITY = "b729c140d99c9e64b7ff2c977a7fa1bd"

TODAY = 20729  # 2026-10-03

# (id, name, targetDay, dueDay, colour, [(title, completed), ...])
LISTS = [
    ("l1", "Groceries",       None, TODAY + 2, "Butter", [
        ("Milk", 0), ("Bread", 0), ("Eggs", 0), ("Coffee beans", 0),
        ("Olive oil", 1), ("Lemons", 1)]),
    ("l2", "Weekend trip",    TODAY + 6, None, "None", [
        ("Book the train", 0), ("Pack a raincoat", 0), ("Charge the camera", 0),
        ("Find the tent", 1), ("Print the tickets", 1)]),
    ("l3", "Birthday party",  None, TODAY, "Rose", [
        ("Order the cake", 0), ("Send the invitations", 0), ("Borrow chairs", 0),
        ("Make a playlist", 0), ("Buy candles", 0),
        ("Book the room", 1)]),
    ("l4", "Reading list",    None, None, "None", [
        ("Piranesi", 0), ("The Overstory", 0), ("A Pale View of Hills", 0),
        ("Tokyo Ueno Station", 0)]),
    ("l5", "Apartment move",  None, TODAY + 9, "Sky", [
        ("Call the movers", 0), ("Change the address", 0), ("Return the keys", 0),
        ("Box up the kitchen", 0), ("Cancel the internet", 0), ("Measure the sofa", 0),
        ("Book the lift", 1)]),
    ("l6", "Guitar practice", TODAY + 1, None, "None", [
        ("Learn the bridge", 0),
        ("Restring it", 1), ("Tune by ear", 1), ("Chord chart", 1)]),
    ("l8", "Home office setup", None, None, "None", [
        ("Order the lamp", 1), ("Hang the shelf", 1), ("Route the cables", 1)]),
    ("l9", "Bike service",    None, None, "None", [
        ("New brake pads", 1), ("Straighten the wheel", 1)]),
]


BIG = [
    ("m1", "Garden jobs",     TODAY + 3, None, "Mint", [
        ("Repot the basil", 0), ("Buy compost", 0), ("Fix the water timer", 0),
        ("Prune the olive", 1)]),
    ("m2", "Tax paperwork",   None, TODAY + 14, "None", [
        ("Find last year's return", 0), ("Scan the receipts", 0),
        ("Email the accountant", 0)]),
    ("m3", "Sunday cooking",  TODAY + 4, None, "None", [
        ("Sourdough starter", 0), ("Roast the peppers", 0)]),
    ("m4", "Camera bag",      None, None, "Peach", [
        ("Spare batteries", 0), ("Lens cloth", 0), ("SD cards", 0), ("Rain cover", 0)]),
    ("m5", "Flat repairs",    None, TODAY + 21, "None", [
        ("Silicone the bath", 0), ("Bleed the radiators", 0), ("Draught strip", 0),
        ("Replace the fuse box cover", 0), ("Sand the door", 1)]),
    ("m6", "Language study",  TODAY + 2, None, "Lilac", [
        ("Chapter 7 exercises", 0), ("Twenty new words", 0)]),
    ("m7", "Winter clothes",  None, None, "None", [
        ("Wash the coats", 0), ("Reproof the shell", 0), ("Mend the gloves", 0)]),
    ("m8", "Passport renewal", None, None, "None", [
        ("Photo booth", 1), ("Fill the form", 1), ("Post the old one", 1)]),
    ("m9", "Loft clear-out",  None, None, "None", [
        ("Sort the boxes", 1), ("Book the tip run", 1), ("Sell the old desk", 1)]),
]

if "--big" in sys.argv:
    tail = LISTS[-2:]
    LISTS = LISTS[:-2] + BIG[:7] + tail + BIG[7:]
    LISTS[4] = ("l5", "Apartment move", None, TODAY + 9, "Sky", [
        ("Call the movers", 0), ("Change the address", 0), ("Return the keys", 0),
        ("Box up the kitchen", 0), ("Cancel the internet", 0), ("Measure the sofa", 0),
        ("Redirect the post", 0), ("Read the meters", 0), ("Defrost the freezer", 0),
        ("Label every box", 0), ("Hand back the parking fob", 0),
        ("Book the lift", 1), ("Buy tape", 1), ("Count the keys", 1)])

FRENCH = {
    "Groceries": "Courses", "Milk": "Lait", "Bread": "Pain", "Eggs": "Œufs",
    "Coffee beans": "Café en grains", "Olive oil": "Huile d'olive", "Lemons": "Citrons",
    "Weekend trip": "Week-end", "Book the train": "Réserver le train",
    "Pack a raincoat": "Prendre un imper", "Charge the camera": "Charger l'appareil",
    "Find the tent": "Trouver la tente", "Print the tickets": "Imprimer les billets",
    "Birthday party": "Anniversaire", "Order the cake": "Commander le gâteau",
    "Send the invitations": "Envoyer les invitations", "Borrow chairs": "Emprunter des chaises",
    "Make a playlist": "Faire une playlist", "Buy candles": "Acheter des bougies",
    "Book the room": "Réserver la salle",
    "Reading list": "À lire", "Piranesi": "Piranesi", "The Overstory": "L'Arbre-monde",
    "A Pale View of Hills": "Lumière pâle sur les collines",
    "Tokyo Ueno Station": "Sous le ciel de Tokyo",
    "Apartment move": "Déménagement", "Call the movers": "Appeler les déménageurs",
    "Change the address": "Changer d'adresse", "Return the keys": "Rendre les clés",
    "Box up the kitchen": "Emballer la cuisine", "Cancel the internet": "Résilier la box",
    "Measure the sofa": "Mesurer le canapé", "Redirect the post": "Faire suivre le courrier",
    "Read the meters": "Relever les compteurs", "Defrost the freezer": "Dégivrer le congélateur",
    "Label every box": "Étiqueter les cartons", "Hand back the parking fob": "Rendre le badge du parking",
    "Book the lift": "Réserver l'ascenseur", "Buy tape": "Acheter du scotch",
    "Count the keys": "Compter les clés",
    "Guitar practice": "Guitare", "Learn the bridge": "Apprendre le pont",
    "Restring it": "Changer les cordes", "Tune by ear": "Accorder à l'oreille",
    "Chord chart": "Grille d'accords",
    "Home office setup": "Bureau à la maison", "Order the lamp": "Commander la lampe",
    "Hang the shelf": "Poser l'étagère", "Route the cables": "Ranger les câbles",
    "Bike service": "Révision du vélo", "New brake pads": "Plaquettes de frein",
    "Straighten the wheel": "Dévoiler la roue",
    "Garden jobs": "Jardin", "Repot the basil": "Rempoter le basilic",
    "Buy compost": "Acheter du terreau", "Fix the water timer": "Réparer le programmateur",
    "Prune the olive": "Tailler l'olivier",
    "Tax paperwork": "Impôts", "Find last year's return": "Retrouver la déclaration",
    "Scan the receipts": "Scanner les reçus", "Email the accountant": "Écrire au comptable",
    "Sunday cooking": "Cuisine du dimanche", "Sourdough starter": "Levain",
    "Roast the peppers": "Griller les poivrons",
    "Camera bag": "Sac photo", "Spare batteries": "Batteries de rechange",
    "Lens cloth": "Chiffon", "SD cards": "Cartes SD", "Rain cover": "Housse de pluie",
    "Flat repairs": "Travaux", "Silicone the bath": "Refaire le joint de la baignoire",
    "Bleed the radiators": "Purger les radiateurs", "Draught strip": "Joint de porte",
    "Replace the fuse box cover": "Changer le capot du tableau", "Sand the door": "Poncer la porte",
    "Language study": "Cours d'italien", "Chapter 7 exercises": "Exercices du chapitre 7",
    "Twenty new words": "Vingt mots nouveaux",
    "Winter clothes": "Vêtements d'hiver", "Wash the coats": "Laver les manteaux",
    "Reproof the shell": "Réimperméabiliser la veste", "Mend the gloves": "Repriser les gants",
    "Passport renewal": "Passeport", "Photo booth": "Photomaton",
    "Fill the form": "Remplir le formulaire", "Post the old one": "Envoyer l'ancien",
    "Loft clear-out": "Grenier", "Sort the boxes": "Trier les cartons",
    "Book the tip run": "Aller à la déchetterie", "Sell the old desk": "Vendre le vieux bureau",
}

if "--fr" in sys.argv:
    LISTS = [(lid, FRENCH[name], target, due, colour,
              [(FRENCH[title], completed) for title, completed in items])
             for lid, name, target, due, colour, items in LISTS]

db = sqlite3.connect(OUT)
c = db.cursor()
c.execute("CREATE TABLE IF NOT EXISTS `todo_lists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL, `targetDate` INTEGER, `dueDate` INTEGER, `reminderMinute` INTEGER, `colour` TEXT NOT NULL, PRIMARY KEY(`id`))")
c.execute("CREATE TABLE IF NOT EXISTS `todo_items` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `listId` TEXT NOT NULL, `completed` INTEGER NOT NULL, `completedAt` INTEGER, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`listId`) REFERENCES `todo_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
c.execute("CREATE INDEX IF NOT EXISTS `index_todo_items_listId` ON `todo_items` (`listId`)")
c.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
c.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)", (IDENTITY,))

done_at = 1787000000000
for pos, (lid, name, target, due, colour, items) in enumerate(LISTS):
    c.execute("INSERT INTO todo_lists (id, name, position, targetDate, dueDate, colour) VALUES (?,?,?,?,?,?)", (lid, name, pos, target, due, colour))
    for i, (title, completed) in enumerate(items):
        done_at += 60000
        c.execute("INSERT INTO todo_items VALUES (?,?,?,?,?,?)",
                  (f"{lid}-i{i}", title, lid, completed, done_at if completed else None, i))

db.commit()
c.execute("PRAGMA user_version = 9")
c.execute("PRAGMA journal_mode = TRUNCATE")
db.commit()
db.close()
print("wrote", OUT, os.path.getsize(OUT), "bytes")
