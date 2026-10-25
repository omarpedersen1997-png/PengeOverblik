# PengeOverblik — Android MVP

Dansksproget budgetapp skrevet i Kotlin og Jetpack Compose. Appen har manuelt indtastede indtægter/udgifter, kategorier, månedligt råderum, faste poster og lokal lagring på enheden.

## Projektstatus

Dette er et **første MVP-/kildekodeprojekt**, ikke en verificeret færdig Google Play-udgivelse. Koden er ikke blevet bygget eller kørt i Android-emulator i dette miljø, fordi Android SDK/Gradle-buildmiljø ikke er tilgængeligt her. Før udgivelse skal projektet åbnes og bygges i en aktuel Android Studio-installation, og funktionerne skal testes på fysiske enheder/emulator.

## Byg i Android Studio

1. Installer en aktuel stabil Android Studio-version.
2. Installer Android SDK Platform 36 og JDK 17 fra SDK Manager.
3. Åbn mappen `PengeOverblik` som et eksisterende Gradle-projekt.
4. Vent på Gradle Sync og ret eventuelle afhængigheds-/SDK-advarsler efter den installerede Android Studio-version.
5. Kør `app` på en emulator eller Android-telefon.
6. Test alle punkter i `RELEASE_CHECKLIST.md`.
7. Vælg **Build > Generate Signed Bundle / APK > Android App Bundle** for at oprette en signeret `.aab`.

Projektet kræver netadgang første gang, så Gradle kan hente plugins og biblioteker. Der medfølger ikke Gradle Wrapper, fordi Gradle ikke var installeret i dette byggemiljø.

## Funktioner i denne version

- Dashboard med samlede indtægter, udgifter og forskel.
- Tilføj indtægter og udgifter med navn, beløb, kategori og markering af fast månedlig post.
- Slet poster.
- Lokal lagring i appens private SharedPreferences.
- Ingen bankintegration, konto, reklamer, analytics eller netværkspermission i manifestet.

## Begrænsninger, der skal løses før offentlig lancering

- Redigering af eksisterende poster mangler.
- Opsparings-/gældsmål er endnu kun en informationsskærm.
- Eksport/import af backup mangler.
- Gentagne poster er kun markeret som faste; appen opretter ikke automatisk månedlige transaktioner.
- Der er ingen månedshistorik, kalender, søgning eller fælles synkronisering mellem to telefoner.
- UI- og tilgængelighedstest samt kontrol af Android 16/API 36-adfærd mangler.
- Butiksnavn og application ID skal kontrolleres for ledighed og ejerskab før udgivelse.

Se `store-assets/` og `RELEASE_CHECKLIST.md` for udgivelsesmateriale.

## Designretning
Den opdaterede brugerflade bruger et enkelt, moderne økonomi-dashboard med god luft, afrundede kort, afdæmpede grønne/mintfarver, varm neutral baggrund og en tydelig visning af rådighedsbeløb. Designet er inspireret af moderne budgetapps, men bruger PengeOverbliks eget navn, farvesystem og layout.

## Teststatus
Designopdateringen er ændret i kildekoden, men er ikke kompileret eller kørt på en Android-enhed i dette miljø. Byg og test projektet i Android Studio, før en udgivelse.
