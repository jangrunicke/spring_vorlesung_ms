# Hinweise zum Programmierbeispiel

<Juergen.Zimmermann@HS-Karlsruhe.de>

> Diese Datei ist in Markdown geschrieben und kann z.B. mit IntelliJ IDEA
> oder NetBeans gelesen werden. Näheres zu Markdown gibt es in einem
> [Wiki](http://bit.ly/Markdown-Cheatsheet)

## Powershell

Überprüfung, ob sich Skripte (s.u.) starten lassen:

```CMD
    Get-ExecutionPolicy -list
```

`CurrentUser` muss das Recht `RemoteSigned` haben. Ggf. muss das Ausführungsrecht ergänzt werden:

```CMD
    Set-ExecutionPolicy RemoteSigned CurrentUser
```

## Falls die Speichereinstellung für Gradle zu großzügig ist

In `gradle.properties` bei `org.gradle.jvmargs` den voreingestellten Wert
(1 GB) ggf. reduzieren.

## MongoDB starten und beenden

Durch Aufruf der .bat-Datei:

````CMD
    .\mongodb.ps1
````

bzw.

````CMD
    .\mongodb.ps1 stop
````

## Mailserver

_FakeSMTP_ wird durch die .ps1-Datei `mailserver` gestartet und läuft auf Port 25000.

## Config-Server starten

Siehe `ReadMe.md` im Beispiel `config`.

Wenn der Config-Server gestartet ist, können die Properties für den Server _kunde_
mit dem Profil _dev_ überprüft werden, indem in einem Webbrowser durch
`https://localhost:8888/kunde/dev` aufgerufen wird.
Der Benutzername ist `admin`, und das Passwort ist `p`. 

## Übersetzung und Ausführung

### Public und Private Key für HTTPS

In einer Powershell werden der Keystore `keystore.p12` und der Truststore `truststore.p12`
im Format PKCS#12 durch das nachfolgende Kommando im Unterverzeichnis `src\main\resources
erzeugt.

```CMD
    .\create-keystore.ps1
```

### Start des Servers in der Kommandozeile

In einer Powershell wird der Server mit der Möglichkeit für einen
_Restart_ gestartet, falls es geänderte Dateien gibt:

```CMD
    .\kunde.ps1
```

### Start des Servers innerhalb von IntelliJ IDEA

Im Auswahlmenü rechts oben, d.h. dort wo _Application_ steht, die erste Option
_Edit Configurations ..._ auswählen. Danach beim Abschnitt _Environment_
im Unterabschnitt _VM options_ den Wert
`-Djavax.net.ssl.trustStore=C:/Users/MEINE_KENNUNG/IdeaProjects/kunde/src/main/resources/truststore.p12 -Djavax.net.ssl.trustStorePassword=zimmermann`
eintragen, wobei `MEINE_KENNUNG` durch die eigene Benutzerkennung zu ersetzen ist.
Nun beim Abschnitt _Spring Boot_ im Unterabschnitt _Active Profiles_ den Wert
`dev` eintragen und mit dem Button _OK_ abspeichern.

Von nun an kann man durch Anklicken des grünen Dreiecks rechts oben die
_Application_ bzw. den Microservice starten.

### Evtl. Probleme mit dem Kotlin Daemon

Falls der _Kotlin Daemon_ beim Übersetzen nicht mehr reagiert, dann muss man
alte Dateien im Verzeichnis `%USERPROFILE%\AppData\Local\kotlin\daemon` löschen.

### Kontinuierliches Monitoring von Dateiänderungen

In einer zweiten Powershell überwachen, ob es Änderungen gibt, so dass
die Dateien für den Server neu bereitgestellt werden müssen; dazu gehören die
übersetzten .class-Dateien und auch Konfigurationsdateien. Damit nicht bei jeder
Änderung der Server neu gestartet wird und man ständig warten muss, gibt es eine
"Trigger-Datei". Wenn die Datei `restart.txt` im Verzeichnis
`src\main\resources` geändert wird, dann wird ein _Neustart des Servers_
ausgelöst und nur dann.

Die Powershell, um kontinuierlich geänderte Dateien für den Server
bereitzustellen, kann auch innerhalb der IDE geöffnet werden (z.B. als
_Terminal_ bei IntelliJ).

```CMD
    .\gradlew classes -t
```

### Eventuelle Probleme mit Windows

_Nur_ falls es mit Windows Probleme gibt, weil der CLASSPATH zu lang ist und
deshalb `java.exe` nicht gestartet werden kann, dann kann man auf die beiden
folgenden Kommandos absetzen:

```CMD
    .\gradlew bootRun
    java -jar build/libs/kunde-1.0.jar --spring.profiles.active=dev `
         -Djavax.net.ssl.trustStore=./src/main/resources/truststore.p12 `
         -Djavax.net.ssl.trustStorePassword=zimmermann
```

Die [Dokumentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#executable-jar)
enthält weitere Details zu einer ausführbaren JAR-Datei bei Spring Boot

### Herunterfahren in einer eigenen Powershell

```CMD
    .\kunde.ps1 stop
```

### Tests

Folgende Server müssen gestartet sein:

* MongoDB
* Mailserver

```CMD
    .\gradlew test --fail-fast [--rerun-tasks]
    .\gradlew jacocoTestReport
```

### Codeanalyse durch ktlint und detekt

```CMD
    .\gradlew ktlint detekt
```

### API-Dokumentation

```CMD
    .\gradlew dokka
```

### Zertifikat ggf. in Chrome importieren

Chrome starten und die URI `chrome://settings` eingeben. Danach `Zertifikate verwalten`
im Suchfeld eingeben und auf den gefundenen Link klicken. Jetzt den Karteireiter
_Vertrauenswürdige Stammzertifizierungsstellen_ anklicken und über den Button _Importieren_
die Datei `src\test\resources\certificate.cer` auswählen.

## curl als REST-Client

Beispiel:

```CMD
   C:\Zimmermann\Git\mingw64\bin\curl --include --basic --user admin:p --tlsv1.3 --insecure https://localhost:8444/00000000-0000-0000-0000-000000000001
```
