# Bike Garage
This app has the goal to support quick overview of your bike garage including the bikes and their parts, your tools
and even your clothes.

## Tech

- [x] Erste Version ist eine installierte Android-App; PC- und Browser-Support sind nicht Teil dieser Version.
- [x] Mindestversion ist Android 12 (API 31).
- [x] App und alle Daten, einschließlich Bilder, arbeiten lokal auf Gerät. Starten, Navigieren, Anzeigen, Anlegen und Bearbeiten funktionieren ohne Internetverbindung.
- [x] Startseite ist `Garage overview`; Kacheln öffnen die bestehenden Bereiche wie in den Feature-Gruppen beschrieben.
- [x] Jede Seite zeigt oben links ein Home-Icon, das zu `Garage overview` führt.
- [~] Bedienung ist auf Touch-Geräte ausgelegt. Kartenraster passen sich verfügbarer Android-Bildschirmbreite an. — `Garage overview` verwendet fest zwei Spalten statt eines adaptiven Rasters.
- [x] App-Sprache ist Deutsch.
- [x] Bilder für Fahrräder können mit Kamera aufgenommen oder aus Fotomediathek gewählt werden.
- [x] Produkt-URLs öffnen Systembrowser. Ohne Internet zeigt Systembrowser möglichen Ladefehler.
- [~] Beim Verlassen eines Formulars mit ungespeicherten Änderungen fordert App Bestätigung zum Verwerfen. — `Shop creation` verwirft Änderungen beim Zurückgehen ohne Bestätigungsdialog.
- [x] Sicherung, Wiederherstellung und Synchronisierung zwischen Geräten sind nicht Teil erster Version. Android Auto Backup ist deaktiviert.
- [x] Erste Version wird als APK direkt installiert.

### Android stack

- [x] Kotlin mit Jetpack Compose und Material 3.
- [x] Navigation Compose verwaltet Navigation und Back-Stack.
- [x] Architektur ist MVVM mit ViewModel, Repository und Use-Cases.
- [x] Hilt übernimmt Dependency Injection.
- [x] Room über SQLite speichert strukturierte lokale Daten.
- [x] Bilder liegen im app-internen Speicher. Room speichert zugehörige Metadaten und Pfade.
- [x] Coil lädt und zeigt Bilder in Compose.
- [x] Unit-Tests prüfen Use-Cases und Repositorys; Compose-UI-Tests prüfen Kernflüsse.
- [ ] Bilder von Fahrrädern und Teilen sind zentriert.

# Feature groups
- [x] garage overview: have a basic selection buttons on the landing page to navigate to bikes, tools or clothes
- [x] bike overview: display all your bikes including a thumbnail image, an optional nickname, the brand and model
- [x] bike creation: create a new bike
- [x] bike edit: edit an existing bike
- [x] bike deletion: delete a bike and optionally its linked parts
- [x] bike details: display all details of a specific bike including more images, purchase date and shop and an overview of its equipment
- [x] bike equipment details: displays all the details of a bike part including the manufacturer number as well as multiple links to shops to buy from
- [x] bike equipment creation: create a bike part
- [x] bike equipment edit: edit a bike part
- [x] bike equipment deletion: delete a bike part
- [x] parts overview: display all parts, including parts without a bike assignment
- [x] part edit: edit a part from the parts overview
- [x] shop overview: displays all shops that are currently associated with parts with their logo and name
- [x] shop creation: create a new shop
- [x] shop edit: edit an existing shop
- [x] shop deletion: delete a shop
- [x] shop details: displays the name, the logo and all parts that are associated with the shop across bikes

## Garage overview

- [x] Startseite zeigt vier gleich große Kacheln für Bikes, Parts, Tools und Clothes.
- [x] Jede Kachel enthält Icon und Label, aber keine Bestands- oder Änderungsinfos.
- [x] Parts öffnet `Parts overview`.
- [x] Tools und Clothes führen vorerst zu leeren Platzhalterseiten.

## Bike overview

- [x] Fahrräder erscheinen in einem Kartenraster, das sich Android-Bildschirmbreite anpasst:
  - [x] Bei ausreichender Breite stehen Karten nebeneinander.
  - [x] Bei geringer Breite stehen Karten untereinander.
- [x] Jede Karte zeigt Thumbnail, optionalen Spitznamen sowie Marke und Modell.
- [x] Bei vorhandenen Fahrrädern öffnet ausschließlich Button `Neu` `Bike creation`.
- [x] Sortierung: zuletzt angelegt zuerst.
- [x] Ohne Fahrräder erscheint ein Hinweistext mit Button zu `Bike creation`.
- [x] Klick auf Karte öffnet `Bike details` für dieses Fahrrad.

## Bike creation

- [x] Pflichtfelder: Marke, Modell und mindestens ein Bild.
- [x] Optionale Felder: Spitzname, Kaufdatum und Shop.
- [x] Marke bietet beim Tippen passende Vorschläge aus bekannter Markenliste direkt unter Eingabefeld in gleicher Breite sowie Freitexteingabe für nicht gelistete Marken.
- [x] Kaufdatum wird über Kalender-DatePicker gewählt.
- [x] Mehrere Bilder sind möglich; Nutzer wählt Thumbnail.
- [x] Nach Speichern öffnet App `Bike details` für neues Fahrrad.

## Bike edit

- [x] Enthält dieselben Felder wie `Bike creation`.
- [x] Bilder können hinzugefügt und entfernt werden; Nutzer wählt Thumbnail.
- [x] Nach Speichern öffnet App `Bike details` für dieses Fahrrad.

## Bike deletion

- [x] Löschen erfordert Bestätigung.
- [x] Dialog zeigt verknüpfte Parts; Nutzer kann null bis alle zusätzlich zum Fahrrad zum Löschen auswählen.
- [x] Nicht ausgewählte Parts bleiben erhalten. Ihre Verknüpfung zum gelöschten Fahrrad wird entfernt.

## Bike details

- [x] Ein Bearbeiten-Button öffnet `Bike edit` für dieses Fahrrad.
- [x] Bearbeiten und Löschen stehen als Icons rechts oben in App-Bar, nicht als große Buttons im Inhalt.
- [x] Bilder erscheinen in horizontaler Galerie.
- [x] Unter Bildern stehen Name, Marke, Modell, Kaufdatum und Kaufort.
- [ ] Kaufort steht vollständig in einer Zeile im Format `Kaufort: <Shop>`.
- [x] Kaufort verweist auf bestehenden Shop; bei Bedarf öffnet ein Link `Shop creation`.
- [x] Darunter steht vollständige Ausrüstung als nach Kategorie und Teiltyp gruppierte Kartenliste mit allen diesem Fahrrad zugeordneten Teilen.
  - [x] Beispiel: Kategorie Antrieb mit Kette und Kurbel.
  - [x] Kategorieüberschrift, darunter Teiltypüberschrift und darunter die Teile; kanonische, kategoriekonsistente Teiltypen kommen aus `PartTechnicalFields`, fehlende, unbekannte oder ungültige Typen erscheinen im Fallback.
  - [x] Jede Teilekarte zeigt nur Namen und den ersten vorhandenen technischen Wert, etwa die Übersetzung eines Kettenblatts; der Teiltyp wird nicht in der Karte wiederholt.
  - [x] Weitere Attribute wie Shop oder Anzahl Kettenglieder stehen nur in `Bike equipment details`.
- [x] Kategorien sind fest vorgegeben und später erweiterbar:
  - [x] Antrieb
  - [x] Bremsen
  - [x] Laufräder
  - [x] Cockpit
  - [x] Sattel
  - [x] Fahrwerk
  - [x] Zubehör
- [x] Solange keine Ausrüstung vorhanden ist, steht unter Überschrift `Ausrüstung` großer Button `Teil hinzufügen`. Nach erstem Teil ersetzt kleiner `+`-Button auf gleicher Höhe wie Überschrift diesen Button.

## Bike equipment details

- [x] Pflichtfelder: Kategorie und Name.
- [x] Optionale Felder: Hersteller, Artikelnummer und Beschreibung.
- [ ] Optionales Feld `Anzahl` zeigt vorhandenen Inventarbestand, zum Beispiel von Bremsbelägen.
- [x] Shop-Angaben sind optional:
  - [x] Jede Angabe verweist auf bestehenden Shop oder öffnet bei Bedarf `Shop creation`.
  - [x] Jede Angabe speichert Produkt-URL.
  - [x] Ein Teil darf beliebig viele Shop- und Produkt-URL-Angaben haben.
- [x] Teiltypen haben feste optionale technische Felder:
  - [x] Kettenblatt: Übersetzung
  - [x] Kette: Anzahl Glieder, Anzahl Gänge
  - [x] Kassette: Anzahl Ritzel, Übersetzung, Freilauf
  - [x] Felge: Anzahl Speichen
  - [x] Lenker: Onepiece, Durchmesser
- [x] Klick auf Teilezeile in Ausrüstungstabelle öffnet diese Seite.
- [x] Ein Teil kann keinem, einem oder mehreren Fahrrädern zugeordnet sein.
- [x] Bearbeiten und Löschen stehen als Icons rechts oben in App-Bar, nicht als große Buttons im Inhalt.
- [x] Zugeordnete Fahrräder erscheinen als anklickbare Pillen; Klick öffnet jeweiliges `Bike details`.
- [x] Fahrradpillen verlaufen zeilenweise und brechen bei Bedarf in weitere Zeilen um.

## Bike equipment creation

- [x] Button in Ausrüstungstabelle von `Bike details` oder `Parts overview` öffnet Seite.
- [x] Bei `Teil hinzufügen` aus `Bike details` wählt Nutzer zuerst: neues Teil anlegen oder vorhandenes Teil suchen und mit Fahrrad verknüpfen.
- [x] Bei Aufruf aus `Bike details` ist zugehöriges Fahrrad vorgewählt.
- [x] Bei Aufruf aus `Parts overview` bleibt Fahrrad optional.
- [x] Nutzer wählt zuerst Kategorie, dann Teiltyp.
- [x] Formular zeigt passende technische Felder.
- [x] Formular enthält optionales Feld `Anzahl` für Inventarbestand.
- [x] Nach Speichern kehrt App zur aufrufenden Übersicht zurück:
  - [x] Aus `Bike details` zurück zu diesem Fahrrad.
  - [x] Aus `Parts overview` zurück zu `Parts overview`.

## Bike equipment edit

- [x] Bearbeiten-Button in `Bike equipment details` öffnet Seite.
- [x] Kategorie und Teiltyp dürfen geändert werden.
- [x] Nach Änderung von Kategorie oder Teiltyp zeigt Formular passende technische Felder und speichert deren Werte.
- [x] Fahrradzuordnungen können per Mehrfachauswahl gesetzt und geändert werden.
- [x] Fahrrad-Mehrfachauswahl zeigt pro Fahrrad eine umschaltbare Pille statt Checkliste.
- [x] Fahrradpillen verlaufen zeilenweise und brechen bei Bedarf in weitere Zeilen um.
- [x] Formular bearbeitet optionales Feld `Anzahl` für Inventarbestand.
- [x] Nach Speichern öffnet App `Bike equipment details` für dieses Teil.

## Bike equipment deletion

- [x] Löschen erfordert Bestätigung.
- [x] App entfernt alle Fahrradzuordnungen des Parts.
- [x] Fahrräder bleiben immer erhalten.

## Parts overview

- [x] Zeigt alle Teile, auch Teile ohne Zuordnung zu einem Fahrrad.
- [x] Button öffnet `Bike equipment creation`; Fahrradzuordnung bleibt optional.
- [~] Teile erscheinen in gruppierter Tabelle nach Kategorie und Teiltyp. — Als gruppierte Kartenliste umgesetzt, nicht als Tabelle.
- [x] Jede Zeile zeigt Teilname und alle Fahrradzuordnungen.
- [x] Klick auf Teilezeile öffnet direkt `Bike equipment details`, analog zu `Bike overview`.

## Part edit

- [x] Nutzer öffnet aus `Parts overview` direkt `Bike equipment details` und wählt dort Bearbeiten.
- [x] Formular bearbeitet alle Teilfelder und Fahrradzuordnungen.
- [x] Nach Speichern öffnet App `Bike equipment details` für dieses Teil.

## Shop overview

- [x] Shops erscheinen in Kartenraster, das sich Android-Bildschirmbreite anpasst, mit Logo und Name.
- [x] Alle angelegten Shops erscheinen, auch wenn sie nur Kaufort eines Fahrrads sind.
- [x] Klick auf Karte öffnet `Shop details` für diesen Shop.

## Shop creation

- [x] Pflichtfelder: Name und Logo.
- [x] Aufruf aus Fahrrad- oder Teileformular kehrt nach Speichern dorthin zurück.
- [x] Neuer Shop ist dort automatisch ausgewählt.

## Shop edit

- [x] Bearbeiten-Button in `Shop details` öffnet Seite.
- [x] Formular bearbeitet Name und Logo.
- [x] Nach Speichern öffnet App `Shop details` für diesen Shop.

## Shop deletion

- [x] Löschen erfordert Bestätigung.
- [x] Shop mit Kaufort- oder Produkt-URL-Verknüpfungen kann erst gelöscht werden, nachdem Nutzer diese Verknüpfungen manuell entfernt hat.

## Shop details

- [x] Zeigt oben Fahrräder, die bei Shop gekauft wurden.
- [x] Darunter stehen Teile, die Shop zugeordnet sind.
- [x] Fahrradeintrag öffnet `Bike details`.
- [x] Teileintrag öffnet `Bike equipment details`.
- [x] Teileliste zeigt Teilname und zugehörige Fahrräder.
