# Roadmap noduS

Sole attività approvate e ancora aperte. Le specifiche e il logo approvato sono
nel [riferimento di comportamento](BEHAVIOR.md); presentazione e stato sono
nel [README](../README.md). Le funzionalità approvate sono implementate localmente.
Il primo collaudo della 0.1.11 sul telefono ha fornito riscontri visivi e
richieste di rifinitura. Restano il collaudo completo e le nuove prove della 0.1.18.

## Preparazione della futura pubblicazione

- [ ] Raccogliere i primi beta tester tramite la guida e il modulo feedback,
  includendo dispositivi Android meno recenti. Nessun invito automatico.

- [ ] Completare il collaudo sul telefono e aggiungere screenshot reali.
- [ ] Includere e verificare le licenze dei font e le altre attribuzioni nel
  pacchetto Android prima di distribuire un APK.
- [ ] Verificare la resa README desktop/mobile su GitHub e impostare la social
  preview dalle impostazioni del repository con l’asset già pronto.

Repository pubblico: https://github.com/issu-lab/nodus . Le versioni sperimentali
sono distribuite come prerelease; nessuna pubblicazione sul Play Store.

## Priorità: collaudo dell'hub sul telefono

Il prototipo 0.1.18 è compilato localmente; 77 test Kotlin e Android Lint
supportano la verifica locale. Restano aperte le verifiche Android e UI seguenti:

- [ ] Provare accesso concesso/negato/revocato, disconnessione, riconnessione,
  riavvio del processo e conteggio senza snapshot obsoleti.
- [ ] Provare tap con contentIntent, notifica senza contenuto apribile e
  auto-cancellazione richiesta dall'app di origine.
- [ ] Provare risposta riuscita con rimozione, errore d'invio, errore di
  rimozione dopo invio, risposta non supportata e campi vuoti.
- [ ] Durante la scrittura: scomparsa, aggiornamento, revoca accesso e rotazione;
  verificare conservazione e copia del testo, blocco invio e assenza di duplicati.
- [ ] Provare swipe in entrambe le direzioni, annullamento del gesto e
  notifiche non eliminabili; verificare che nessuno swipe su una riga navighi.
- [ ] Provare swipe destro nel vuoto, pulsante Indietro dell'hub e Indietro
  Android su elenchi vuoti, corti e lunghi.
- [ ] Provare «Cancella tutte», annullamento della conferma, nuovi arrivi o
  aggiornamenti mentre la conferma è aperta e notifiche non eliminabili.
- [ ] Verificare intestazione fissa, ordinamento, espansione del testo, nuovi
  arrivi senza salti nella lettura e «Nuove notifiche», anche con tante righe.

## Collaudo home e impostazioni

- [ ] Verificare chiusura del cassetto trascinando verso il basso oltre le prime
  app: elenco lungo/corto, arrivo in cima durante lo stesso gesto, gesto breve
  annullato, inversione verso l’alto, lancio inerziale senza chiusura e Indietro.



- [ ] Verificare apertura a freddo e ripetuta dopo la preparazione anticipata
  della lista, completamento più morbido e reset fuori dal gesto. Le immagini
  della 0.1.16 confermano layout/versione, ma non misurano la fluidità.
- [ ] Verificare pausa con notifica musicale attiva, rimozione della notifica,
  chiusura dai recenti e arresto forzato: distinguere sessione residua e servizio
  ancora attivo. La scelta confermata è mantenere la pausa con notifica attiva.

- [ ] Verificare la batteria dopo ripetuti rientri nella home, schermo spento,
  ricarica e variazioni del listener: niente «—%» dopo una lettura valida,
  percentuale aggiornata e coerente con Android, anche vicino a 0/100%.

- [ ] Provare apertura animata: il pannello deve seguire il dito mentre la home
  resta ferma; gesto corto/lento annullato, trascinamento lungo e lancio rapido
  completati, inversione e multitocco annullati. Verificare bordi Android.
- [ ] Provare HOME/Indietro durante trascinamento e animazione, comandi ripetuti,
  rotazione e ritorno da un’app; nessun salto, pannello bloccato o tocco sulla home
  sottostante. Verificare anche animazioni disattivate nelle impostazioni Android.
- [ ] Verificare fluidità con molte app, scorrimento normale della lista aperta,
  icone/font personalizzati e accessibilità senza elementi duplicati della home.

- [ ] Verificare batteria a 16 sp e allineamento inclusivo in home/hub:
  il bordo dei puntini/icone deve coincidere con orologio, widget e testi
  delle notifiche, anche con dimensioni diverse e font pixel/di sistema.
- [ ] Verificare home fissa senza trascinamento o scorrimento, riposizionamento
  all’arrivo/rimozione dei widget e adattamento nei casi sovraffollati/rotazione.
- [ ] Provare spaziatura 0/20/32 dp, anteprima durante il trascinamento,
  persistenza, riposizionamento automatico e leggibilità con molte app/eventi.
- [ ] Provare swipe sui widget in entrambe le direzioni, gesti corti/annullati,
  azioni accessibili, indipendenza da swipe hub/app drawer e tocchi sui controlli.
- [ ] Verificare che la musica chiusa resti nascosta sugli aggiornamenti ordinari
  e ricompaia su nuovo brano/sessione o ripresa rilevata; provare ritorno da altre app.
- [ ] Verificare calendario chiuso con eventi che terminano, nuove occorrenze,
  cambio giorno, rotazione e rientro nella home, senza eliminare eventi/notifiche.

- [ ] Provare Wi-Fi connesso/disconnesso, passaggio ai dati mobili, VPN e rientro
  nella home: l’icona Wi-Fi deve seguire la connessione insieme al segnale cellulare.
- [ ] Provare lo switch «Nascondi barra di stato» su tutte le schermate, ritorno
  da un’altra app, riavvio e rivelazione temporanea dal bordo superiore.
- [ ] Provare swipe verso l’alto dall’interno della home, da righe e vuoto,
  gesti corti/diagonali/annullati, multitocco, home fissa, elenco completo e
  Indietro e ritorno alla home tramite Android. Verificare gesti Android dai bordi con navigazione a gesti e pulsanti.
- [ ] Provare musica con font pixel/grandi, titoli e artisti lunghi, metadati
  incompleti o ripetuti e controlli non supportati, senza aperture involontarie.

- [ ] Verificare orologio e cambio data/fuso, batteria e segnale su dispositivo,
  incluse assenza SIM, modalità aereo e dati non disponibili.
- [ ] Verificare suggerimenti su 7 giorni, cambio giorno e aggiornamento della
  cache, concessione/revoca accesso, parità, pochi dati e completamento alfabetico.
- [ ] Verificare migrazione preferiti esistenti, avviso per limite già superato,
  aggiunta/rimozione, rifiuto di un limite insufficiente e ordine persistente.
- [ ] Provare trascinamento e comandi su/giù, ricaricamento dopo installazione o
  rimozione di app e avvio dell'attività corretta senza duplicati.
- [ ] Provare spostamento verticale con tutti i widget, molti eventi, font
  grandi e rotazione: nessun elemento deve diventare irraggiungibile.
- [ ] Provare pressione prolungata nel vuoto e sull'ora, anche con tutti gli
  elementi superiori nascosti; verificare distinzione da click/long press sulle app.

## Collaudo musica, calendario e meteo

- [ ] Verificare i widget reali nell’hub con «Solo hub»/«Home e hub», controlli
  musicali, apertura eventi, aggiornamento al rientro e assenza di contenuti vuoti.
- [ ] Provare autorizzazione calendario dall’hub e dalle impostazioni, rifiuto,
  revoca e scelta calendari. Il meteo non deve attivarsi entrando nell’hub.
- [ ] Provare chiusura per swipe dei widget nell’hub, indipendenza dalla home,
  lista lunga con notifiche, conteggio notifiche e «Cancella tutte» separati dai widget.

- [ ] Provare ciascuna destinazione (home, hub, entrambi, nessuno), inclusa
  variazione del conteggio hub, senza eliminare notifiche nascoste da Android.
- [ ] Provare sessioni musicali reali, pausa/ripresa, fine sessione, più lettori,
  controlli non supportati e revoca accesso alle notifiche.
- [ ] Provare calendari multipli, selezione vuota, eventi conclusi, in corso,
  ricorrenti e dell'intera giornata; verificare mezzanotte, cambio fuso, inviti
  rifiutati, evento cancellato e apertura della ricorrenza corretta.
- [ ] Provare autorizzazioni calendario/posizione concesse, negate e revocate;
  nessuna richiesta di posizione deve restare attiva fuori dalla home.
- [ ] Provare meteo reale con posizione approssimata, località, rete assente,
  posizione disattivata, timeout, cache vecchia e passaggio icona/testo.

## Collaudo aspetto e identità

- [ ] Provare puntini automatici/uniformi, colore esadecimale, dimensioni e
  icone originali delle app, inclusi nomi lunghi e icone adattive.
- [ ] Importare TTF e OTF reali; provare file invalido, oltre 8 MB, importazione
  annullata, font molto larghi e ripristino senza perdere le altre impostazioni.
- [ ] Provare dimensioni home/hub, font di sistema, ricreazione della schermata
  e riavvio processo; verificare la conservazione della bozza dell'hub.
- [ ] Verificare icona adattiva e monocromatica su launcher Android, logo e
  spaziatura approvata, nome noduS launcher, versione 0.1.18 e benvenuto riapribile.
- [ ] Verificare navigazione Indietro tra sottopagine delle impostazioni e
  ritorno alla home, scroll e leggibilità sulle dimensioni reali del telefono.
