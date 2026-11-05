# ElPiFon – das einfache Telefon für Senioren

![Logo](store/icon_512.png)

Android-App (Kotlin, Jetpack Compose), die das Handy in ein übersichtliches Senioren-Telefon
verwandelt. Alles ist groß, kontrastreich und mit einem Fingertipp erreichbar:

- Oben der geschwungene Schriftzug **„ElPiFon“**, rechts daneben klein „by Frank“
- **Große Kontakt-Kacheln** mit Foto oder Anfangsbuchstaben – einmal antippen, und der
  Anruf startet sofort
- **Roter Notruf-Knopf**: 2 Sekunden gedrückt halten (verhindert versehentliches Auslösen)
  - SMS an alle Notfallkontakte, auf Wunsch mit Standort-Link (Google Maps)
  - ist der bekannte Standort älter als 10 Minuten, wird ein aktueller gesucht und nachgeschickt
  - danach Anruf beim obersten Notfallkontakt
  - ohne eingerichteten Notfallkontakt: Anruf bei der 112 (ist ElPiFon nicht die Telefon-App,
    öffnet sich die normale Telefon-App mit vorgewählter 112)
- **Einstellungen für Angehörige** (unten „⚙ Einstellungen“ lange drücken), optional mit PIN:
  Kontakte anlegen (auch direkt aus dem Telefonbuch, inkl. Foto), sortieren, bearbeiten,
  löschen; Notruf-Text; Standort an/aus
- **Eigener Anruf-Bildschirm** (wenn ElPiFon als Telefon-App gewählt ist):
  - eingehend: Foto und „Name ruft an“ (unbekannt: die Nummer), zwei riesige Tasten **Annehmen** (grün)
    und **Ablehnen** (rot) – antippen statt wischen, auch auf dem Sperrbildschirm
  - im Gespräch: nur Foto, Name und eine große Taste **Auflegen**; am Ohr schaltet sich
    der Bildschirm ab (Näherungssensor)
  - bewusst ohne Anklopfen, Lautsprecher und Stummschalten: Wer während eines Gesprächs
    anruft, hört „besetzt“
  - optional: unbekannte Nummern klingeln nicht
  - kein Wählfeld: Wird in einer anderen App (z. B. Browser) eine Telefonnummer angetippt,
    ruft ElPiFon sie direkt an
  - läuft ein Gespräch im Hintergrund, führt ein grüner Balken auf dem Startbildschirm zurück
- **Startbildschirm-Modus**: ElPiFon ersetzt auf Wunsch den normalen Startbildschirm. Die
  Home-Taste führt dann immer zu den Kacheln, „Zurück“ verlässt die App nicht. Abschalten
  jederzeit in den Einstellungen („Anderen Startbildschirm wählen“).

## Einrichtung durch Angehörige

1. APK installieren (siehe unten) und ElPiFon öffnen.
2. Die Fragen nach **Telefonanrufen, SMS und Standort** mit „Zulassen“ beantworten.
   Ohne Anruf-Berechtigung öffnet sich beim Antippen nur die Wähl-App (dann grünen Hörer tippen).
3. Unten „⚙ Einstellungen“ **lange drücken** → „Kontakt hinzufügen“.
   Tipp: „Aus dem Telefonbuch übernehmen“ füllt Name, Nummer und Foto aus.
4. Mindestens einen Kontakt als **Notfallkontakt** markieren. Der oberste Notfallkontakt wird
   beim Notruf angerufen – mit den Pfeilen nach oben sortieren.
5. Optional: PIN vergeben, damit nichts versehentlich verstellt wird.
6. Empfohlen: „ElPiFon als Telefon-App verwenden“ antippen und bestätigen. Erst dann
   erscheinen eingehende Anrufe im großen ElPiFon-Bildschirm.
   **Ab Android 15** (z. B. Moto G85) sperrt Android das für Apps, die nicht aus dem Play Store
   kommen. Einmalig freischalten:
   1. Einmal versuchen, ElPiFon als Telefon-App zu wählen (Einstellungen → Apps →
      Standard-Apps → Telefon-App). Die Meldung „Eingeschränkte Einstellung“ wegtippen.
   2. Einstellungen → Apps → ElPiFon → oben rechts ⋮ → „Eingeschränkte Einstellungen
      zulassen“ und mit PIN/Fingerabdruck bestätigen.
   3. ElPiFon erneut als Telefon-App wählen.
7. Optional: „ElPiFon als Startbildschirm“ einschalten und in der Android-Auswahl ElPiFon
   wählen („Immer“).
8. **Notruf und einen eingehenden Anruf einmal ausprobieren** (vorher den Notfallkontakt informieren).

Hinweise:
- Die SMS kostet je nach Tarif Geld. Bei Dual-SIM wird die Standard-SIM für SMS verwendet.
- Als Telefon-App ersetzt ElPiFon die normale Telefon-App. Anrufliste und Mailbox-Abfrage
  bietet ElPiFon nicht; dafür bei Bedarf in den Einstellungen wieder zurückstellen
  („Wieder andere Telefon-App verwenden“).
- Eine App ersetzt keinen Hausnotruf-Dienst.

## Schriftzug

Der Schriftzug „ElPiFon“ ist als Vektorpfad eingebettet (`res/drawable/elpi_wordmark.xml`) und
stammt aus der Schrift **Dancing Script** (SIL Open Font License 1.1, Google Fonts).

## Datenschutz

ElPiFon hat **keine Internet-Berechtigung**. Das Telefonbuch wird nur gelesen, um Anrufer-Namen anzuzeigen. Kontakte, Fotos und Einstellungen bleiben auf dem
Gerät (DataStore bzw. interner App-Speicher). Der Standort verlässt das Handy nur in der
Notruf-SMS an die eingetragenen Notfallkontakte.

## Bauen

1. Ordner in **Android Studio** (Ladybug oder neuer) öffnen → *Open*.
2. Gradle-Sync abwarten (lädt Android SDK 35 und Abhängigkeiten).
3. Gerät/Emulator wählen → *Run*.

Kommandozeile: `./gradlew assembleDebug` → APK unter `app/build/outputs/apk/debug/`.

Voraussetzungen: JDK 17, Android SDK 35, minSdk 26 (Android 8.0).

## Projektstruktur

```
app/src/main/java/de/elpifon/app/
├── MainActivity.kt           Einstieg, Berechtigungen, Navigation, PIN-Abfrage
├── call/
│   ├── ElInCallService.kt    Von Android gemeldete Anrufe (nur als Telefon-App)
│   ├── CallManager.kt        Zustand des Gesprächs, Annehmen/Ablehnen/Auflegen
│   ├── CallActivity.kt       Anruf-Vollbild, Sperrbildschirm, Näherungssensor
│   └── DialerActivity.kt     Unsichtbar: Wähl-Anfragen anderer Apps
├── data/
│   ├── CallerLookup.kt       Anrufer-Name/Foto zur Nummer finden
│   ├── Models.kt             Kontakt, Einstellungen
│   ├── ContactStore.kt       Kontakte + Einstellungen dauerhaft speichern (DataStore)
│   ├── Photos.kt             Fotos verkleinern, drehen, intern ablegen
│   └── PhoneActions.kt       Anrufen, Notruf-SMS, Standort, Startbildschirm-Modus
└── ui/
    ├── MainViewModel.kt      Zustand, aktuelle Ansicht
    ├── HomeScreen.kt         Kacheln, Notruf-Knopf
    ├── CallScreen.kt         Anruf-Bildschirm
    ├── SettingsScreen.kt     Einstellungen für Angehörige
    ├── ContactEditScreen.kt  Kontakt anlegen/bearbeiten
    ├── ContactAvatar.kt      Foto oder Anfangsbuchstabe
    ├── PinDialog.kt
    └── Theme.kt              Kontrastreiche Farben (hell/dunkel)
```

## Über GitHub bauen und als APK verteilen

Der Workflow `.github/workflows/android.yml` baut die APK automatisch:

| Auslöser | Ergebnis |
|---|---|
| Push auf `main` oder Pull Request | APK unter **Actions → Lauf → Artifacts** (ZIP mit der APK) |
| Neue Nummer in `version.txt` auf `main` oder Tag `v…` | zusätzlich ein **GitHub-Release** mit der APK zum Herunterladen |
| „Run workflow“ im Actions-Tab | manueller Build |

### Einmalig: Signaturschlüssel hinterlegen

Ohne Schlüssel baut der Workflow nur eine Debug-APK, die bei jedem Lauf anders signiert
ist – Updates schlagen dann fehl („App nicht installiert“). Deshalb unter
**Settings → Secrets and variables → Actions** vier Secrets anlegen:
`KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

Eigenen Schlüssel erzeugen (falls nicht schon vorhanden):

```bash
keytool -genkeypair -keystore elpifon-release.jks -storetype PKCS12 \
  -alias elpifon -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 elpifon-release.jks   # Ausgabe = KEYSTORE_BASE64
```

Schlüssel und Passwort gut aufbewahren und **nie** ins Repository legen.

### Neue Version veröffentlichen

Am einfachsten: In `version.txt` die neue Versionsnummer eintragen (z. B. `1.2.0`) und nach
`main` übernehmen. Der Build legt dann automatisch das Release `v1.2.0` mit der APK an.
Bleibt die Nummer gleich, entsteht kein neues Release.

Alternativ geht weiterhin ein Tag:

```bash
git tag v1.2.0
git push origin v1.2.0
```

Nach wenigen Minuten liegt `ElPiFon-1.2.0.apk` unter **Releases**. Diesen Link kannst
du weitergeben. Auf dem Handy muss einmalig „Installation aus unbekannten Quellen“ für den
Browser erlaubt werden.
