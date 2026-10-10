# PLC Simulator & HMI Monitor

Un progetto industriale client-server che simula un PLC (Programmable Logic Controller) e un'interfaccia HMI di monitoraggio.

## Architettura
Il progetto è strutturato su due moduli separati:
1. **Server (C++)**: Il "cervello" della macchina (PLC Simulator), sviluppato per ambiente Linux.
2. **Client HMI (Java)**: *(In sviluppo)* L'interfaccia grafica per il monitoraggio e il controllo da parte dell'operatore.

## Il Server C++ (Simulatore PLC)
Il backend è scritto in C++ moderno e implementa logiche industriali reali:
- **Multithreading**: Il ciclo macchina (aggiornamento continuo dei sensori) e il centralino di rete TCP girano su due thread paralleli e indipendenti. Questo garantisce che la macchina non si blocchi mai in attesa di una connessione di rete.
- **Socket POSIX**: Utilizza le API di rete native del kernel Linux per la comunicazione TCP affidabile (Socket, Bind, Listen, Accept continua in loop).
- **Sincronizzazione Memoria**: Il server TCP agisce direttamente sulla memoria della simulazione fisica passando l'indirizzo (Referenza `&`) dell'oggetto PLC, facendo da ponte tra la rete e l'hardware simulato.

### Protocollo di Comunicazione (Porta 5020)
Il server rimane costantemente in ascolto sulla porta 5020 e risponde istantaneamente ai seguenti comandi testuali:
- `GET_TEMP`: Legge dal sensore e restituisce la temperatura attuale.
- `ALARM_ON`: Attiva l'allarme generale del PLC.
- `ALARM_OFF`: Disattiva l'allarme generale del PLC.
- `GET_AXIS`: Legge e restituisce la quota attuale dell'asse motore.

### Come compilare ed eseguire il Server (Linux / WSL)
Dalla cartella `server-cpp` contenente i sorgenti C++:
```bash
mkdir build
cd build
cmake ..
make
./plc-simulator