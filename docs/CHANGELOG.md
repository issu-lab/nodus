# Changelog

## [0.1.21 — impostazioni in italiano e inglese, build locale] - 2026-10-30

- Estratti i testi delle impostazioni in risorse Android: inglese predefinito e
  italiano in values-it, selezionati dalla lingua Android senza preferenze nuove.
- Tradotti sottopagine, dialoghi, permessi, errori font, anteprime, destinazioni,
  riordino accessibile e Informazioni/Benvenuto. Localizzati anche i controlli
  musica e le descrizioni meteo nei componenti condivisi con le anteprime.
- Gestita la quantità dei preferiti con plurali Android; nomi app, calendari,
  font importati e chiavi delle preferenze restano invariati.
- Verificati copertura dei cataloghi, parametri e riferimenti dalle sorgenti;
  controllate risorse italiane/inglesi nell’APK con aapt dump --values resources.
- Superati 77 test Kotlin, build debug e Lint (0 errori, 4 avvisi preesistenti).
  APK 0.1.21 locale con SHA-256 e stessa firma delle versioni precedenti.
- Cambio lingua e resa sul telefono ancora da provare. Altri comandi home/hub
  restano italiani; nessun push o release. Conservati gli artefatti preesistenti.

## [0.1.20 — disposizione super compatta, build locale] - 2026-10-30

- Sostituita la precedente disposizione compatta: ora sopra; data, meteo,
  segnale/Wi-Fi e batteria sotto, su una sola riga allineata a sinistra.
- Il meteo super compatto mostra icona e temperatura; disponibilità e stato
  obsoleto sono descritti anche per l’accessibilità. Il layout attuale è invariato.
- La riga si adatta proporzionalmente allo spazio senza andare a capo; restano
  dimensioni indipendenti, anteprima, visibilità dei singoli elementi e ripristino.
- Verificati 77 test Kotlin, build debug e Lint (0 errori, 4 avvisi preesistenti).
  APK locale 0.1.20 con SHA-256 e stessa firma delle versioni di prova precedenti.
  Leggibilità e interazione sul telefono restano da collaudare nella roadmap.
- Nessun push o release; conservati APK e artefatti preesistenti.

## [0.1.19 — dimensioni separate e area superiore compatta, build locale] - 2026-10-30

- Aggiunti controlli persistenti separati per ora, data, meteo, batteria e icone
  segnale/Wi-Fi in Impostazioni → Home, con anteprima immediata a dati di esempio.
- Aggiunto preset compatto (ora/indicatori sopra, data/meteo sotto) e ripristino
  dedicato delle dimensioni/disposizione, senza modificare le altre impostazioni.
  Layout e dimensioni precedenti rimangono predefiniti per le installazioni esistenti.
- Estratto il componente superiore condiviso tra home e anteprima; adattamento
  delle righe allo spazio disponibile. Nessun accesso rete/posizione dall’anteprima.
- Verificati 77 test Kotlin, build debug e Lint (0 errori, 4 avvisi preesistenti).
  APK locale 0.1.19 con SHA-256; verifica visiva/tattile sul telefono aperta.
- Conservati APK precedenti e artefatti preesistenti; nessuna pubblicazione remota
  della 0.1.19 durante questa implementazione.

## [Screenshot reali nel README] - 2026-10-29

- Aggiunte tre schermate originali fornite dall’utente: home, cassetto app e
  informazioni, con miniature affiancate e link alle immagini complete.
- Indicata la versione effettiva delle catture (0.1.16), distinta dalla beta
  corrente. Esclusa la schermata hub con contenuti delle notifiche.
- Nessuna modifica all’app o al pacchetto beta.

## [Prima prerelease GitHub: v0.1.18-beta.1] - 2026-10-29

- Pubblicato il repository pubblico https://github.com/issu-lab/nodus con storia
  autonoma e soli sorgenti selezionati, senza cache/build, chiavi o storia del workspace.
- Pubblicata la prerelease v0.1.18-beta.1 con ZIP (APK 0.1.18, istruzioni e
  licenze) e SHA256SUMS.txt. README e guida beta includono i link reali.
- Riscaricati gli asset da GitHub: ZIP identico al locale, checksum dello ZIP e
  dell’APK estratto validi; verificati anche README e badge pubblicati.
- Confermati repository pubblico, branch main e prerelease sperimentale.
  App e firma non modificate per la pubblicazione; collaudo sul telefono aperto.

## [0.1.18 — chiusura del cassetto con trascinamento, build locale] - 2026-10-29

- Il trascinamento verso il basso oltre l’inizio della lista passa al pannello,
  che segue il dito e si richiude; un gesto breve o invertito torna aperto.
  Lo scorrimento inerziale della lista non avvia la chiusura.
- Verificati 77 test Kotlin, assembleDebug e Lint (0 errori, 11 avvisi: quattro
  già rilevati e sette segnalazioni di versioni delle dipendenze disponibili).
- Generato APK locale 0.1.18 con SHA-256 e firma debug identica alla 0.1.17;
  APK precedente e artefatti preesistenti conservati. Collaudo tattile sul
  telefono ancora necessario e mantenuto nella roadmap.
- Pubblicazione GitHub mantenuta soltanto in locale su richiesta dell’utente.

## [Preparazione repository e pacchetto beta GitHub] - 2026-10-29

- Preparata esportazione pulita dei sorgenti, senza storia del workspace,
  cache/build, configurazione SDK locale, copie non versionate o note storiche.
- Preparato un pacchetto ZIP beta con l’APK 0.1.17 invariato, istruzioni di
  installazione, MIT e copie OFL; aggiunti manifest dei sorgenti e SHA-256.
- Aggiunti guida per i beta tester, modulo GitHub per feedback riproducibili e
  bozza d’invito in italiano. Nessun invito inviato automaticamente.
- Verificati integrità ZIP, identità dell’APK, licenze, YAML, link, esclusioni
  dei sorgenti e controllo di percorsi privati/segreti; output esistente protetto
  da sovrascrittura. Sorgenti app, APK, impostazioni di build e chiavi invariati.
- Pubblicazione remota e verifica degli asset scaricati restano distinte dalla
  preparazione locale e richiedono la conferma finale prevista dal metodo.

## [0.1.17 — preparazione app e pausa con notifica attiva, build locale] - 2026-10-29

- Mantenuta pronta la prima pagina del pannello app dietro la superficie home,
  opaca e proprietaria dei tocchi. Rimossi reset della lista dal primo movimento
  del dito; reset e arresto dello scorrimento avvengono alla chiusura.
- Ammorbidito il completamento dell’animazione. Pannello chiuso dietro la home,
  escluso dall’accessibilità; pannello visibile sopra la home con protezione dai
  tocchi sugli elementi sottostanti. La fluidità reale resta da verificare.
- Confermata con l’utente la regola musicale: riproduzione sempre idonea;
  pausa mostrata solo con una notifica musicale attiva. Escluse le sessioni
  residue senza notifica, anche dopo rimozione della notifica e indipendentemente
  dai filtri di destinazione home/hub.
- Il listener pubblica evidenza media non filtrata, solo in memoria. I widget
  reagiscono alla sua variazione e ignorano aggiornamenti quando non osservati.
  Rimozione dai recenti e arresto forzato non sono equiparati: Android può
  conservare servizio e notifica dopo la prima operazione.
- Verificati 76 test Kotlin (4 nuovi sulla selezione delle sessioni musicali),
  assembleDebug e lintDebug in copia isolata: zero errori e 4 avvisi. Preservati
  i file preesistenti; codice compilato corrispondente ai sorgenti correnti.
- Preparato `dist/nodus-0.1.17-debug.apk`, SHA-256 e firma uguale alla 0.1.16,
  esclusi dal commit. Le prove sul telefono restano in roadmap; nessun push
  o release.

## [0.1.16 — recupero batteria e widget reali nell’hub, build locale] - 2026-10-29

- Rafforzata la lettura batteria: snapshot iniziale, recupero tramite capacità
  diretta, aggiornamento al rientro e ogni minuto, rifiuto di dati incompleti o
  fuori intervallo senza cancellare l’ultimo valore valido. Ignorati callback
  di ricevitori già sostituiti. «—%» rimane se non è mai arrivato un dato valido.
- Collegati musica e calendario all’hub con le destinazioni «Solo hub» e
  «Home e hub»: widget reali da sessioni media e calendari selezionati, non solo
  le rispettive notifiche Android. Aggiunti controlli, apertura eventi e chiusura
  temporanea per swipe nella schermata corrente.
- Aggiunta richiesta esplicita del permesso calendario anche nell’hub e accesso
  alla selezione calendari per questa destinazione. Gli osservatori seguono la
  visibilità della schermata; il meteo resta attivo solo sulla home.
- Widget e notifiche condividono l’elenco scorrevole ma conservano ruoli
  distinti: conteggio e «Cancella tutte» operano soltanto sulle notifiche.
- Verificati 72 test Kotlin, inclusi 7 nuovi casi della batteria e 4 delle
  destinazioni home/hub, assembleDebug e lintDebug in copia isolata: zero errori
  e 4 avvisi Lint. Preservati artefatti e duplicati locali preesistenti.
- Preparato `dist/nodus-0.1.16-debug.apk`, con SHA-256 e firma uguale alla 0.1.15,
  escluso dal commit. L’assenza del difetto intermittente sul telefono e le nuove
  integrazioni reali restano da verificare nella roadmap. Nessun push o release.

## [0.1.15 — apertura app interattiva e animata, build locale] - 2026-10-29

- Sostituito il cambio secco della home con un pannello delle app che sale dal
  basso seguendo il dito; la home resta montata e ferma sotto. Al rilascio,
  distanza e velocità determinano apertura o rientro con animazione smorzata.
- Aggiunti gestione dell’inversione, annullamento/multitocco e chiusura verso il
  basso tramite HOME/Indietro anche durante una transizione. I bordi restano
  riservati ad Android e lo scorrimento dell’elenco aperto non viene intercettato.
- Separato l’elenco completo in una LazyColumn con intestazione fissa: vengono
  composte le righe necessarie, senza caricare tutte le icone durante l’apertura.
- Rimossi i tocchi e i nodi accessibili della home coperta; il pannello chiuso
  non è presente nell’albero interattivo. L’animazione aggiorna la traslazione
  grafica senza ricomporre l’elenco a ogni fotogramma.
- Verificati 61 test Kotlin (8 nuovi su progresso, distanza/velocità, inversione,
  rumore e densità), assembleDebug e lintDebug in copia isolata: zero errori e
  4 avvisi Lint. Sorgenti compilati corrispondenti al workspace e file preesistenti
  preservati. Questi controlli non misurano la fluidità su un dispositivo reale.
- Preparato `dist/nodus-0.1.15-debug.apk` con SHA-256 e firma uguale alla 0.1.14,
  escluso dal commit. Le prove sul telefono restano nella roadmap; nessun push
  o release.

## [0.1.14 — allineamento inclusivo di puntini e icone, build locale] - 2026-10-29

- Corretto il riferimento di allineamento dopo il chiarimento: il bordo comune
  a 24 dp comprende puntini/icone delle app, orologio, widget e notifiche dell’hub.
  I nomi delle app seguono nella corsia interna; rimossi i rientri delle altre sezioni.
- Allineati i puntini a sinistra della corsia invece di centrarli nel riquadro
  dell’icona. L’adattamento della home mantiene fisso il bordo sinistro del blocco.
- Verificati assembleDebug e lintDebug in copia isolata: zero errori e 4 avvisi
  Lint. Nessuna modifica alle politiche coperte dai 53 test della 0.1.13.
- Preparato `dist/nodus-0.1.14-debug.apk` con SHA-256 e firma uguale alla versione
  precedente. Preservati artefatti e duplicati locali preesistenti. Resa su telefono
  ancora da verificare; nessun push o release.

## [0.1.13 — home fissa e widget richiudibili, build locale] - 2026-10-29

- Aumentata la percentuale batteria da 12 a 16 sp e introdotta una griglia comune
  per home e hub: testi a 48 dp, colonna laterale per icone/frecce, margine destro
  24 dp. Rimossi i rientri aggiuntivi delle notifiche e dei pulsanti di intestazione.
- Rimossa la home dal contenitore scorrevole. Il blocco viene misurato una volta,
  riposizionato verso l’alto quando cresce e adattato proporzionalmente solo se
  supera tutta l’altezza disponibile. Lo swipe apre direttamente tutte le app;
  elenco completo e hub mantengono il loro scorrimento.
- Aggiunta spaziatura righe/widget 0–32 dp, predefinita a 20, persistente nelle
  impostazioni Home con anteprima dal vivo. Riduce i distacchi senza modificare
  il font salvato; i comandi musicali non vengono compattati dal cursore.
- Aggiunto swipe orizzontale sui widget musica e calendario, con azioni
  accessibili equivalenti: nasconde solo la card dalla home, senza fermare il
  lettore, eliminare eventi o cancellare notifiche Android.
- Mantenuta la chiusura musicale sugli aggiornamenti ordinari e sulla pausa;
  nuovo brano/sessione o ripresa rilevata permettono il ritorno. Il calendario
  resta chiuso quando terminano eventi già presenti e torna per nuove occorrenze
  o al cambio di giornata. Stato temporaneo conservato nella home ricreata.
- Verificati 53 test Kotlin, assembleDebug e lintDebug in copia isolata:
  zero errori Lint e 4 avvisi. Preservati 293 artefatti modificati preesistenti
  e quattro copie locali dei sorgenti non versionate.
- Preparato `dist/nodus-0.1.13-debug.apk`, SHA-256 e firma uguale alla 0.1.12,
  esclusi dal commit. Le verifiche di layout, tocchi e gesti reali sul telefono
  restano aperte nella roadmap. Nessun push o release.

## [0.1.12 — rifiniture home, build locale] - 2026-10-29

- Aggiunta icona Wi-Fi accanto al segnale cellulare quando connesso, con
  osservazione della rete limitata alla home visibile, senza nuovi permessi runtime.
- Sistemato il widget musica: righe distinte e limitate per titolo e artista,
  pulizia del titolo ripetuto nei metadati, stato e controlli su una riga dedicata,
  icone vettoriali indipendenti dal font e comandi non supportati disabilitati.
- Aggiunto «Nascondi barra di stato» nelle impostazioni Home, persistente e
  applicato a tutte le schermate, anche al ritorno da altre app. Restano la
  navigazione Android e la rivelazione temporanea dal bordo superiore.
- Sostituito «Tutte le app» con swipe verso l’alto nell’interno della home,
  riservando i bordi al sistema. Home lunghe e lista completa mantengono lo
  scorrimento; aggiunti accesso per tecnologie assistive e rientro tramite Home.
- Ricevuti screenshot della prima prova su telefono della 0.1.11: riscontro
  positivo sull’aspetto con font pixel e richieste di rifinitura. Non attestano
  il collaudo completo dei comportamenti né della nuova versione.
- Verificati 38 test Kotlin, assembleDebug e lintDebug in copia isolata:
  zero errori Lint e 4 avvisi. Esclusi dalla sola copia di build quattro duplicati
  locali non versionati; conservati originali e 293 artefatti modificati preesistenti.
- Preparato `dist/nodus-0.1.12-debug.apk` con SHA-256 e firma uguale alla 0.1.11,
  aggiornabile senza disinstallazione; APK escluso dal commit. Restano aperte
  le prove su telefono, specialmente convivenza delle gesture e barra di stato.
  Nessun push o release.

## [Presentazione GitHub preparata localmente] - 2026-10-29

- Preparato README in inglese con stato sperimentale, funzionalità, installazione,
  permessi, limiti e indice tecnico; mantenute le specifiche approvate in
  `BEHAVIOR.md`, senza perdere il riferimento italiano.
- Preparati banner RGB 5120×1280 e social preview 1280×640, SVG autosufficienti
  con testi tracciati e generatori riproducibili. Conservati geometria comune
  iSSU, N approvata senza tagli, magenta #F800FF e badge condiviso originale.
- Aggiunte licenza MIT, copie OFL dei font e attribuzioni separate; documentata
  la verifica dei notice nel futuro pacchetto Android prima della distribuzione.
- Aggiunte esclusioni Git per cache, build, APK locali e dipendenze degli strumenti
  grafici. Gli artefatti già tracciati non sono stati eliminati né modificati.
- Verificati link locali, dimensioni e geometria degli asset, identità SHA-256
  del badge e rigenerazione byte per byte di SVG e PNG. Controllata l'anteprima
  locale del README a larghezza desktop e mobile; non è una verifica su GitHub.
- Nessuna modifica ai sorgenti Android, nuova build, pubblicazione, push o release.
  Il collaudo sul telefono e le verifiche finali di pubblicazione restano aperti.

## [0.1.11 — prototipo locale, non rilasciato] - 2026-10-29

- Implementate home e impostazioni secondo le anteprime approvate: intestazione
  selezionabile, spostamento verticale dell'intero blocco limitato allo spazio,
  accesso alle impostazioni dal vuoto e dall'ora, elenco app aggiornato.
- Implementati ordine persistente dei preferiti, limite atomico con avviso,
  riempimento senza duplicati e suggerimenti da aperture degli ultimi 7 giorni
  con cache giornaliera e completamento alfabetico senza autorizzazione.
- Collegati batteria, segnale cellulare, sessioni musicali reali e controlli,
  calendari selezionabili con eventi di oggi non conclusi e relative destinazioni.
- Implementato meteo Open-Meteo da posizione approssimata, con consenso Android,
  icona monocromatica/testo, località, cache, timeout e indicazione dati vecchi.
- Implementati importazione TTF/OTF validata, font comune, dimensioni separate
  home/hub, puntini o icone originali, colore uniforme e ripristino aspetto.
- Integrati logo approvato, icona adattiva/monocromatica, nome noduS launcher,
  versione locale 0.1.11, informazioni e benvenuto con contenuti condivisi.
- Verificati 26 test Kotlin, compilazione debug e Android Lint senza errori;
  risposta API meteo verificata con coordinate pubbliche di esempio. Build
  eseguita in copia isolata; artefatti preesistenti preservati. APK debug locale
  generato in `dist/`, firma verificata e SHA-256 salvato, senza versionare il binario.
- Nessuna installazione sul telefono, verifica UI reale, push o release.
  Le prove sul dispositivo sono elencate nella roadmap.

## [Implementazione locale hub, non rilasciata] - 2026-10-29

- Implementato l'hub cronologico con apertura tramite contentIntent, risposta
  diretta e rimozioni attraverso l'istanza del listener collegata da Android.
- Introdotti controlli della revisione attiva per evitare azioni su notifiche
  sostituite e cancellazioni di nuovi messaggi dopo una risposta.
- Aggiunti espansione del testo, indicatore nuovi arrivi, swipe in pixel con
  soglie in dp, rientro dal vuoto e cancellazione completa con conferma.
- Aggiunti compositore con conservazione/copia del testo e stati espliciti per
  notifiche scomparse, aggiornate o non disponibili e servizio scollegato.
- Corretto il tema per limitare l'attributo display cutout alle versioni Android
  supportate; lettura dei messaggi tramite AndroidX compatibile col minimo SDK.
- Consolidate le ultime decisioni su area superiore, meteo, posizione verticale
  dell'intero blocco e accesso alle impostazioni dal vuoto della home.
- Verificati: 14 test Kotlin della logica comandi, assembleDebug e lintDebug
  senza errori in copia isolata con JDK 17, link e formattazione documentale.
  Rimangono 12 avvisi Lint preesistenti. Gli artefatti modificati prima del lavoro
  sono stati preservati. Nessuna installazione, release o prova sul telefono;
  le verifiche sul dispositivo restano aperte nella roadmap.

## [Documentazione e identità] - 2026-10-29

- Consolidate nel README le decisioni approvate su hub, home, widget,
  personalizzazione, informazioni e identità noduS.
- Allineata la roadmap alle sole attività approvate e ancora aperte.
- Conservati SVG e PNG del logo approvato: N morbida originale senza tagli,
  accento #F800FF, S ravvicinata e firma iSSU Open Homelab.
- Verifica limitata a documentazione, link locali, struttura SVG e render PNG.
  Nessuna implementazione, compilazione, installazione o prova su telefono.

La sezione 0.1.0 seguente è preesistente; non attesta un nuovo collaudo.

## [0.1.0] - 2026-10-28

### Added
- Progetto Nodus inizializzato in Homelab/nodus
- Configurazione Kotlin + Jetpack Compose
- Tema dark/true black
- Font IBM Plex Mono integrato
- Manifest configurato come launcher (HOME/DEFAULT)
- NotificationListenerService registrato
- Modello AppInfo e AppRepository
- HomeScreen con area superiore (ora + data)
- Lista unica: app preferite prima, poi resto in ordine alfabetico
- HubScreen stile BlackBerry
- Swipe a sinistra per aprire hub, swipe a destra per tornare
- Animazioni transizione
- DataStore per app preferite
- Pill notifiche visibile solo se presenti (con conteggio)
- Tap sulla pill o swipe a sinistra per aprire hub
- Long press per aggiungere/rimuovere dai preferiti
- ViewModel per gestione stato

### Changed
- Struttura progetto organizzata per feature

### Notes
- MVP strutturale funzionale
- Gestione widget personalizzabile da implementare
- Modalità "puntini colorati" da implementare
