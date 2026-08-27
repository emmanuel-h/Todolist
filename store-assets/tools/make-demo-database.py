import sqlite3, os, sys

OUT = sys.argv[1]
if os.path.exists(OUT): os.remove(OUT)
IDENTITY = "31ef59047f6e20e6911bd945838e6b43"

TODAY = 20692  # 2026-08-27

# (id, name, targetDay, dueDay, [(title, completed), ...])
LISTS = [
    ("l1", "Groceries",       None, TODAY + 2, [
        ("Milk", 0), ("Bread", 0), ("Eggs", 0), ("Coffee beans", 0),
        ("Olive oil", 1), ("Lemons", 1)]),
    ("l2", "Weekend trip",    TODAY + 6, None, [
        ("Book the train", 0), ("Pack a raincoat", 0), ("Charge the camera", 0),
        ("Find the tent", 1), ("Print the tickets", 1)]),
    ("l3", "Birthday party",  None, TODAY, [
        ("Order the cake", 0), ("Send the invitations", 0), ("Borrow chairs", 0),
        ("Make a playlist", 0), ("Buy candles", 0),
        ("Book the room", 1)]),
    ("l4", "Reading list",    None, None, [
        ("Piranesi", 0), ("The Overstory", 0), ("A Pale View of Hills", 0),
        ("Tokyo Ueno Station", 0)]),
    ("l5", "Apartment move",  None, TODAY + 9, [
        ("Call the movers", 0), ("Change the address", 0), ("Return the keys", 0),
        ("Box up the kitchen", 0), ("Cancel the internet", 0), ("Measure the sofa", 0),
        ("Book the lift", 1)]),
    ("l6", "Guitar practice", TODAY + 1, None, [
        ("Learn the bridge", 0),
        ("Restring it", 1), ("Tune by ear", 1), ("Chord chart", 1)]),
    ("l8", "Home office setup", None, None, [
        ("Order the lamp", 1), ("Hang the shelf", 1), ("Route the cables", 1)]),
    ("l9", "Bike service",    None, None, [
        ("New brake pads", 1), ("Straighten the wheel", 1)]),
]


BIG = [
    ("m1", "Garden jobs",     TODAY + 3, None, [
        ("Repot the basil", 0), ("Buy compost", 0), ("Fix the water timer", 0),
        ("Prune the olive", 1)]),
    ("m2", "Tax paperwork",   None, TODAY + 14, [
        ("Find last year's return", 0), ("Scan the receipts", 0),
        ("Email the accountant", 0)]),
    ("m3", "Sunday cooking",  TODAY + 4, None, [
        ("Sourdough starter", 0), ("Roast the peppers", 0)]),
    ("m4", "Camera bag",      None, None, [
        ("Spare batteries", 0), ("Lens cloth", 0), ("SD cards", 0), ("Rain cover", 0)]),
    ("m5", "Flat repairs",    None, TODAY + 21, [
        ("Silicone the bath", 0), ("Bleed the radiators", 0), ("Draught strip", 0),
        ("Replace the fuse box cover", 0), ("Sand the door", 1)]),
    ("m6", "Language study",  TODAY + 2, None, [
        ("Chapter 7 exercises", 0), ("Twenty new words", 0)]),
    ("m7", "Winter clothes",  None, None, [
        ("Wash the coats", 0), ("Reproof the shell", 0), ("Mend the gloves", 0)]),
    ("m8", "Passport renewal", None, None, [
        ("Photo booth", 1), ("Fill the form", 1), ("Post the old one", 1)]),
    ("m9", "Loft clear-out",  None, None, [
        ("Sort the boxes", 1), ("Book the tip run", 1), ("Sell the old desk", 1)]),
]

if "--big" in sys.argv:
    tail = LISTS[-2:]
    LISTS = LISTS[:-2] + BIG[:7] + tail + BIG[7:]
    LISTS[4] = ("l5", "Apartment move", None, TODAY + 9, [
        ("Call the movers", 0), ("Change the address", 0), ("Return the keys", 0),
        ("Box up the kitchen", 0), ("Cancel the internet", 0), ("Measure the sofa", 0),
        ("Redirect the post", 0), ("Read the meters", 0), ("Defrost the freezer", 0),
        ("Label every box", 0), ("Hand back the parking fob", 0),
        ("Book the lift", 1), ("Buy tape", 1), ("Count the keys", 1)])

db = sqlite3.connect(OUT)
c = db.cursor()
c.execute("CREATE TABLE IF NOT EXISTS `todo_lists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL, `targetDate` INTEGER, `dueDate` INTEGER, PRIMARY KEY(`id`))")
c.execute("CREATE TABLE IF NOT EXISTS `todo_items` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `listId` TEXT NOT NULL, `completed` INTEGER NOT NULL, `completedAt` INTEGER, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`listId`) REFERENCES `todo_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
c.execute("CREATE INDEX IF NOT EXISTS `index_todo_items_listId` ON `todo_items` (`listId`)")
c.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
c.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)", (IDENTITY,))

done_at = 1787000000000
for pos, (lid, name, target, due, items) in enumerate(LISTS):
    c.execute("INSERT INTO todo_lists VALUES (?,?,?,?,?)", (lid, name, pos, target, due))
    for i, (title, completed) in enumerate(items):
        done_at += 60000
        c.execute("INSERT INTO todo_items VALUES (?,?,?,?,?,?)",
                  (f"{lid}-i{i}", title, lid, completed, done_at if completed else None, i))

db.commit()
c.execute("PRAGMA user_version = 7")
c.execute("PRAGMA journal_mode = TRUNCATE")
db.commit()
db.close()
print("wrote", OUT, os.path.getsize(OUT), "bytes")
