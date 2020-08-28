# Turing: disTribUted collaboRative edItiNG.
Il progetto e' stato sviluppato per il corso di Reti di Calcolatori. 

Recentemente l’uso di strumenti on line di produzione collaborativa di documenti e' enormemente cresciuto, in parallelo con la diffusione del web.
La scrittura collaborativa consiste nella creazione di testi da parte di un gruppo di persone, dove ognuno fornisce un contributo individuale alla produzione del documento finale. I testi prodotti possono essere di varia natura: da documenti scientifici e didattici a codice sorgente, ed anche testi letterari. Esempi di strumenti di questo tipo sono Google Documents, OverLeaf, BoomWriter.

Il progetto consiste nell’implementazione di uno strumento per l’editing collaborativo di documenti che offre un insieme di servizi minimale.

Operazioni implementate nel progetto:
- Registrazione al servizio: ogni utente deve per prima cosa effettuare una registrazione, fornendo username e password e, solo successivamente, puo' inziare ad usufruire del servizio;
- Creazione di un documento: dopo essersi registrato, l’utente puo' creare un nuovo documento ed invitare altri utenti registrati al servizio a collaborare all’editing del documento creato;
- Gestione Inviti: quando un utente U1 decide di invitare un altro utente U2, TURING aggiunge U2 alla lista degli utenti autorizzati a lavorare sul documento. L’invito viene notificato immediatamente ad un utente, se online, oppure al momento del successivo login;
- Lista dei documenti: il server offre un servizio per richiedere la lista di documenti che un certo utente, precedentemente autenticato, e' autorizzato ad accedere e modificare;
- Editing di un documento: ogni documento e' strutturato in un insieme di sezioni, dove ogni sezione contiene una sequenza di linee di testo (non e' previsto l’inserimento di foto o di immagini nel documento).
- Chat: tutti gli utenti che collaborano, in un certo istante, all’editing di un documento (su sezioni diverse) possono interagire sfruttando un servizio di chat. TURING crea la chat non appena un utente sta editando il documento e la chiude quando nessun utente sta lavorando sul documento;
- Memorizzazione dei documenti: ogni sezione di un documento e' memorizzata su un file ed un documento puo' essere quindi considerato un insieme di file. Non appena un utente ha terminato l’editing di una sezione, invia la sezione modificata a TURING che la sostituisce nel file corispondente.



Utilizzo della Commad Line Interface - CLI:
$ turing --help
usage: turing COMMAND [ARGS...]

commands:
- register <username > <password>  
- login <username > <password>
- logout
  
- create <doc> <numsezioni> 
- share <doc> <username>
- show <doc> <sec>
- show <doc>
- list (mostra la lista dei documenti)
  
- edit <doc>
- end-edit <doc> <sec> 
- send <msg> (invia un msg sulla chat)
- receive (visualizza i msg ricevuti sulla chat)
