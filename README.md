# PLC Simulator & Monitor

Un progetto sviluppato per simulare un ambiente industriale client-server. 
Il sistema è composto da un PLC virtuale che gestisce stati interni (es. temperatura, posizione, allarmi) e un client di monitoraggio.

## Architettura
- **Server (Simulatore PLC):** Scritto in C++ standard. Utilizza le API di rete Linux (Socket POSIX) tramite WSL per la comunicazione TCP, e `std::thread` per la gestione simultanea di client multipli e del ciclo macchina.
- **Client (Monitor/HMI):** Scritto in Java. Utilizza un'interfaccia grafica Swing e Socket di rete per interrogare e comandare il PLC simulato.

## Stato del Progetto
- [x] Inizializzazione CMake e boilerplate Server C++
- [x] Struttura Dati PLC e Thread di simulazione
- [ ] Implementazione Server TCP (Listen/Accept)
- [ ] Setup Client Java (Maven) e GUI Swing
- [ ] Comunicazione tra Client e Server

## Compilazione ed Esecuzione (Linux / WSL)
Per compilare il simulatore, aprire il terminale nella radice del progetto ed eseguire:
```bash
mkdir build
cd build
cmake ..
make