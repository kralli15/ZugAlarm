# ZugAlarm Android – Prototyp 0.2

Native Android-App mit Kotlin und Jetpack Compose.

## In dieser Version
- Kompakte Darstellung jeder Zugfahrt in einer Karte
- Abfahrt mit Bahnhof, Gleis, Planzeit und Echtzeitfeld
- Ankunft mit Bahnhof, Gleis, Planzeit und Echtzeitfeld
- Mehrere Abschnitte als Reisekette
- Dialog zum manuellen Hinzufügen von Zugabschnitten
- Bereich für Meldungen und spätere Alternativen

## Wichtige Einschränkung
Die dargestellten Fahrten und Zeiten sind Beispieldaten. Es gibt noch keine Verbindung zu Live-Fahrplandaten, keine automatische Anschlussberechnung, keine Hintergrundüberwachung und keine Push-Nachrichten. Die Felder „Echtzeit“ zeigen im Prototyp zunächst denselben Demo-Wert wie die Planzeit.

## Öffnen
1. ZIP entpacken.
2. Ordner `ZugAlarm_Android` in Android Studio öffnen.
3. Gradle-Synchronisierung abwarten.
4. Emulator oder Android-Smartphone verbinden.
5. Auf „Run“ klicken.

APK erstellen: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## Technik
Kotlin, Jetpack Compose, Android API 26 oder neuer.
