# PLC Simulator & Java HMI Monitor 🏭☕

Un'architettura client-server industriale costruita da zero per simulare la comunicazione tra un PLC (Programmable Logic Controller) e un'interfaccia HMI (Human-Machine Interface).

Questo progetto è un "Proof of Concept" che dimostra l'integrazione tra logiche hardware a basso livello e interfacce grafiche ad alto livello, utilizzando socket TCP nativi.

## 🏗️ Architettura del Sistema

Il progetto è diviso in due macro-componenti indipendenti:

1. **Server C++ (Il PLC):**
   * Backend multithread scritto in C++ moderno.
   * Gestisce il ciclo macchina fisico in background (simulazione di temperature, allarmi e posizionamento assi).
   * Espone un server TCP/IP su porta 5020 per ricevere comandi in tempo reale usando socket POSIX.

2. **Client Java (L'HMI):**
   * Interfaccia grafica sviluppata in Java Swing.
   * Implementa un Thread di Polling automatico (1Hz) per monitorare lo stato della macchina in background.
   * Permette l'invio di comandi manuali e parametrici (es. `SET_AXIS <quota>`) parsati direttamente dal server C++.

## 🚀 Come avviare il progetto

### Prerequisiti
* Sistema Linux (o WSL)
* Compilatore `g++` e `cmake`
* Java JDK 17+ e `maven`

### 1. Compilazione e Avvio del Server C++

git clone [https://github.com/WillytheBot/plc-simulator.git](https://github.com/WillytheBot/plc-simulator.git)
cd plc-simulator/server-cpp
mkdir build && cd build
cmake ..
make
./plc-simulator