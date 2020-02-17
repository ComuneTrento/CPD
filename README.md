# Collaborative Procedure Designer (CPD)

Il Collaborative Procedure Designer (CPD) è uno strumento per creare e poi esporre una visulizzazione grafica e semplice dell'iter di un procedimento amministrativo sotto forma di diagrammi.
Nato nell'ambito del progetto europeo [SIMPATICO](https://www.simpatico-project.eu/), è diventato uno dei componenti a riuso del [Progetto PON-GOV "SPRINT"](https://github.com/ComuneTrento/SPRINT-informatizzazione-e-semplificazione-dei-procedimenti-amministrativi)

# Descrizione

Il CPD è uno strumento che consente di creare rappresentazioni grafiche di procedure pubbliche sotto
forma di diagrammi. Questi diagrammi possono rappresentare sia servizi elettronici che servizi non
digitali che i cittadini devono utilizzare per raggiungere un obiettivo specifico.

 ![CPD interfaccia](.README/cpd.1.png)

In particolare il CPD permette di:

 * creare e modificare un diagramma del flusso di lavoro, utilizzando simboli simili a UML.
 * Social/collaborative: consente di pubblicare commenti sul diagramma.

## Qualche dettaglio in più

Il **Collaborative Procedure Design** (CPD) consente al **cittadino** di consultare una rappresentazione grafica (diagramma) di una procedura amministrativa e al **funzionario** pubblico di collaborare alla definizione del diagramma di una procedura amministrativa.

Il cittadino può accedere al CPD per visualizzare la rappresentazione grafica di una procedura amministrativa e per ottenere informazioni su:

  1. quali comunicazioni sono coinvolte;
  2. la sequenza temporale delle comunicazioni;
  3. i canali di comunicazione utilizzati per scambiare documenti/informazioni con la pubblica amministrazione.

Inoltre, il cittadino ha la facoltà di inoltrare all'amministratore, su qualsiasi elemento grafico del diagramma, domande, commenti e suggerimenti.

Il funzionario accede a un ricco insieme di strumenti grafici che gli consente di disegnare i blocchi costituenti di una procedura amministrativa intesa come flusso di comunicazioni Ente-cittadino, ciascuna delle quali coinvolge il cittadino e la pubblica amministrazione.

I casi d'uso tipici del CPD sono, per tipologia d'utente, i seguenti:

  - **Cittadino**:

    * Visualizzare i diagrammi della procedura amministrativa;
    * Visualizzare tutte le informazioni associate a ciascun elemento del diagramma;
    * Inoltrare domande/commenti/suggerimenti su qualsiasi elemento del diagramma;

      > vai al [Manuale del cittadino](https://github.com/ComuneTrento/CPD-Collaborative-Procedure-Design/wiki/Manuale-del-cittadino#index)

  - **Funzionario**:

    * Creare un nuovo diagramma della procedura amministrativa;
    * Modificare un diagramma di procedura amministrativa esistente;
    * Visualizzare le domande/commenti/suggerimenti dei cittadini allegati a qualsiasi elemento del diagramma.

      > vai al [Manuale del funzionario](https://github.com/ComuneTrento/CPD-Collaborative-Procedure-Design/wiki/Manuale-del-funzionario#index)

![SIMPATICO](modeler-microservice/src/main/deploy-bundle/web/assets/img/left_simpatico_small.png)

![screen-capture](https://github.com/ComuneTrento/CPD-Collaborative-Procedure-Design/wiki/images/CPD-screen-capture.gif)

## Altri riferimenti

Per conoscere come il CPD viene utilizzato come componente di un sistema completo di digitalizzazione e semplificazione dei servizi puoi accedere alla soluzione SPRINT

Puoi inoltre consultare il documento di progetto in cui vine spiegata al meglio la modalità di integrazione del CPD a servizi esterni:

 * documento [di progetto](doc/BP-OR-AP-06_v1.0_Trento.pdf)

## Product status

Il prodotto è stabile e production ready e usato in produzione dal Comune di Trento. Lo sviluppo
avviene sia su richiesta degli Enti utilizzatori, sia su iniziativa autonoma del maintainer.

## <a name="struttura-repository"></a> Struttura del repository

Il repository è organizzato con una struttura di directory tipica dei progetti Java Apache Maven.

 * nel repository principale sono presenti due moduli:
    * `microservice-common`: è il modulo comune utilizzato per la gestione del database e degli
       schemi dell'applicazione. Contiene inoltre alcune classi di utility.
       Nella sottodirectory `src` sono presenti i sorgenti del modulo.
    * `modeler-microservice`: è il modulo principale dell'applicazione e contiene diversi script 
      bash per semplificare la gestione del progetto, oltre ai file di configurazione e ai dump di
      alcuni database di procedure di esempio. 
      Nella sottodirectory `src` sono presenti i sorgenti del modulo.
 * Nella directory `doc` è presente la documentazione del progetto SPRINT.

## Copyright

  > License: _[MIT](LICENSE)_\
  > Copyright Owner: _BEng Business Enginering_, _Comune di Trento_\
  > Repository Owner: _Comune di Trento_

## Soggetti incaricati del mantenimento

  > name: _Vincenzo Cartelli_\
  > email: _<v.cartelli@business-engineering.it>_\
  > affiliation: _BEng Business Engineering Srl_

## Segnalazioni di sicurezza
Le segnalazioni di sicurezza vanno inviate all'indirizzo v.cartelli@business-engineering.it

## Prerequisiti e dipendenze

 * AAC nel caso si intenda abilitare l'autenticazione utente per l'accesso alle funzionalità
   supportate
 * Citizenpedia-QAE nel caso si intenda utilizzarne le funzioni
 * TAE nel caso si intenda utilizzarne le funzioni

# Documentazione tecnica

Il progetto è organizato secondo Java Maven. Il `pom.xml` principale serve a raccogliere i due
moduli di cui si compone (vedi [Struttura del repository](#struttura-repository) sopra).

## Prerequisiti per la compilazione

Per la compilazionie del CPD sono necessari i seguenti pacchetti:

* Java Development Kit (JDK) 8+
* Maven 3+
* npm 3.5+

Eseguendo `mvn clean install` sarà compilata e installata localmente una copia delle librerie
necessarie al CPD.

Da questo momento si portà lavorare esclusivamente sul modulo
[`modeler-microservice`](modeler-microservice) al cui README rimandiamo per tutti gli ulteriori
dettagli tecnici.

## Prerequisiti per l'esecuzione

Per l'esecuzione del CPD sono necessari i seguenti pacchetti:

* Java Runtime Environment (JRE) 8+
* Mongo DB 3.4

Per ulteriori dettagli tecnici si rimanda al README del modulo
[`modeler-microservice`](modeler-microservice).
