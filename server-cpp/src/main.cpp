// --- LIBRERIE (La cassetta degli attrezzi) ---
#include <iostream>      // Permette di stampare scritte nel terminale (std::cout)
#include <thread>        // Permette di creare "operai" paralleli (Multithreading)
#include <chrono>        // Gestisce il tempo (es. per mettere in pausa un thread per 1 secondo)
#include <sys/socket.h>  // Libreria base di Linux per creare connessioni di rete
#include <netinet/in.h>  // Contiene le strutture dati per gestire Indirizzi IP e Porte
#include <unistd.h>      // Contiene funzioni di base di Linux, tra cui close() per chiudere i file/socket
#include <string>        // Gestione moderna del testo in C++
#include <stdio.h>       // Libreria classica del C, ci serve per la funzione sscanf() (lettura di numeri da testo)

// --- LA MACCHINA FISICA (Simulatore del PLC) ---
class PlcState {
    private:
        // Le variabili di stato sono "private" per sicurezza: nessuno dall'esterno può modificarle direttamente.
        float temperature;
        bool allarm;
        int axis_position;

    public:
        // Costruttore: Questa riga scatta appena accendi il programma. Imposta i valori di fabbrica.
        PlcState(): temperature(20.0), allarm(false), axis_position(0) {}
        
        // "Getters": Funzioni per LEGGERE i dati
        float getTemperature() const { return temperature; }
        bool isAllarmActive() const { return allarm; }
        int getAxisPosition() const { return axis_position; }

        // "Setters": Funzioni per SCRIVERE i dati
        void setTemperature(float temp) { temperature = temp; }
        void allarmOn() { allarm = true; }
        void allarmOff() { allarm = false; }
        void setAxisPosition(int pos) { axis_position = pos; }

        // IL CICLO MACCHINA: Questa è la funzione che gira in loop continuo in background.
        void runSimulation() {
            bool isRunning = true;
            while (isRunning) {
                // Legge la temperatura, aggiunge 0.5 e la salva di nuovo.
                float tempActual = getTemperature();
                setTemperature(tempActual + 0.5);
                
                // std::cout nascosto: se lo attivassimo stamperebbe ogni secondo intasando il terminale
                // std::cout << "Temperatura attuale: " << tempActual << " gradi" << std::endl;
                
                // Mette l'operaio in pausa per 1 secondo, poi ricomincia il giro.
                std::this_thread::sleep_for(std::chrono::seconds(1));
            }
        }
};

// --- IL CENTRALINO DI RETE (Server TCP) ---
class TcpServer {
    private:
        int porta;
        int server_fd; // L'"identificativo" del socket (File Descriptor) dato da Linux
        
        // CRITICO: La "E-commerciale" (&) significa "Riferimento". 
        // Il server non crea un PLC finto, ma usa i cavi per collegarsi alla memoria del PLC REALE.
        PlcState& plc;

    public:
        // Costruttore: Riceve il numero di porta e il collegamento fisico al PLC
        TcpServer(int porta_desiderata, PlcState& plc_ref) : porta(porta_desiderata), server_fd(-1), plc(plc_ref) {}

        // La procedura di avvio del Server
        void start() {
            std::cout << "Inizializzazione server sulla porta " << porta << std::endl;
            
            // 1. Creazione del Socket (L'apparecchio telefonico)
            server_fd = socket(AF_INET, SOCK_STREAM, 0); // IPv4, TCP, Protocollo Standard
            if (server_fd == -1) return; // Se Linux rifiuta, si ferma.
            
            // 2. Configurazione dell'indirizzo
            struct sockaddr_in address;
            address.sin_family = AF_INET;
            address.sin_addr.s_addr = INADDR_ANY; // Accetta connessioni da qualsiasi IP
            address.sin_port = htons(porta);      // Converte il numero di porta nel formato di rete

            // 3. Bind: Collega fisicamente l'apparecchio alla porta 5020 del computer
            if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) return;
            
            // 4. Listen: Accende la "suoneria" e si prepara a ricevere chiamate (max 3 in coda)
            if (listen(server_fd, 3) < 0) return;
            
            std::cout << "Server in ascolto. In attesa di connessioni..." << std::endl;

            // 5. IL CICLO DI RISPOSTA: Il centralino non stacca mai dal turno di lavoro
            while (true) {
                struct sockaddr_in client_address;
                socklen_t client_len = sizeof(client_address);
                
                // ACCEPT: Il codice SI BLOCCA QUI finché non arriva una richiesta (es. dal client Java).
                // Quando arriva, Linux crea un nuovo socket temporaneo (client_socket) dedicato solo a questa "telefonata".
                int client_socket = accept(server_fd, (struct sockaddr *)&client_address, &client_len);
                if (client_socket < 0) continue; 
                
                // Prepariamo un contenitore vuoto (buffer) di 1024 caratteri e leggiamo i dati in arrivo
                char buffer[1024] = {0}; 
                int valread = read(client_socket, buffer, 1024);
                
                // Se abbiamo letto qualcosa di valido (valread > 0)
                if (valread > 0) {
                    // Trasformiamo i vecchi caratteri 'C' in una comoda stringa 'C++'
                    std::string comando(buffer);
                    
                    // Se il comando NON è quello del polling continuo (GET_TEMP o GET_AXIS), lo stampiamo a video.
                    // npos significa "Non Trovato". Quindi: se "GET_TEMP" non c'è, E "GET_AXIS" non c'è, allora stampa.
                    if (comando.find("GET_TEMP") == std::string::npos && comando.find("GET_AXIS") == std::string::npos) {
                        std::cout << "\n[RETE] Comando ricevuto: " << comando;
                    }

                    // --- INIZIO PROTOCOLLO DI COMUNICAZIONE ---
                    
                    if (comando.find("GET_TEMP") != std::string::npos) {
                        // std::to_string() trasforma il numero decimale (es. 25.5) in parola testuale ("25.5")
                        std::string risposta = "Valore Temperatura: " + std::to_string(plc.getTemperature()) + "\n";
                        // c_str() ritrasforma la stringa C++ nel vecchio formato C richiesto dalla funzione send() di Linux
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    } 
                    else if (comando.find("ALARM_ON") != std::string::npos) {
                        plc.allarmOn(); // Modifica la memoria del PLC
                        std::string risposta = "OK: Allarme PLC Attivato!\n";
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    }
                    else if (comando.find("ALARM_OFF") != std::string::npos) {
                        plc.allarmOff(); // Modifica la memoria del PLC
                        std::string risposta = "OK: Allarme PLC Disattivato!\n";
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    }
                    else if (comando.find("GET_AXIS") != std::string::npos) {
                        std::string risposta = "Posizione Asse: " + std::to_string(plc.getAxisPosition()) + "\n";
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    }
                    // COMANDO PARAMETRICO: L'utente invia ad esempio "SET_AXIS 150"
                    else if (comando.find("SET_AXIS") != std::string::npos) {
                        int target_pos = 0; // Preparo una variabile vuota per ospitare il numero
                        
                        // sscanf analizza la stringa ricevuta ("comando.c_str()"). 
                        // Cerca lo schema preciso "SET_AXIS " seguito da un numero intero ("%d").
                        // Se lo trova, prende il numero e lo "inietta" nella variabile target_pos tramite il puntatore (&).
                        // Se sscanf restituisce 1, significa che ha convertito correttamente 1 parametro.
                        if (sscanf(comando.c_str(), "SET_AXIS %d", &target_pos) == 1) {
                            plc.setAxisPosition(target_pos); // Comando fisico al motore simulato
                            std::string risposta = "OK: Asse in movimento verso quota " + std::to_string(target_pos) + "\n";
                            send(client_socket, risposta.c_str(), risposta.length(), 0);
                        } else {
                            // Se l'utente digita "SET_AXIS ciao" o "SET_AXIS", sscanf fallisce e andiamo in errore.
                            std::string risposta = "ERR: Formato comando errato (usa SET_AXIS <numero>)\n";
                            send(client_socket, risposta.c_str(), risposta.length(), 0);
                        }
                    }
                    else {
                        // Il server non riconosce la parola
                        std::string risposta = "ERR: Comando Sconosciuto\n";
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    }
                }
                
                // CRITICO: Finito di parlare, chiudiamo il socket temporaneo. 
                // Se dimentichi questa riga, dopo 1024 richieste il server crasha per esaurimento risorse di Linux.
                close(client_socket);
            }
        }
};

// --- PUNTO DI INGRESSO ---
int main() {
    std::cout << "Avvio del PLC Simulator..." << std::endl;
    
    // 1. Accendiamo l'hardware
    PlcState plcState;
    
    // 2. Assumiamo il Primo Operaio (Thread) per gestire il ciclo fisico della macchina
    // Gli passiamo la funzione da eseguire e il puntatore (&) all'oggetto fisico su cui lavorare
    std::thread plcThread(&PlcState::runSimulation, &plcState);
    
    // 3. Creiamo il Centralino di Rete, passandogli la porta 5020 e il riferimento (&) all'hardware
    TcpServer tcpServer(5020, plcState); 
    
    // 4. L'Operaio Principale (Main Thread) diventa il centralinista. 
    // Questa funzione blocca il programma qui, ascoltando all'infinito la rete.
    tcpServer.start(); 
    
    // 5. Istruzione di sicurezza: Aspetta che l'operaio 1 finisca prima di chiudere tutto.
    // (Nel nostro caso il ciclo while è infinito, quindi non arriverà mai qui)
    plcThread.join();
    
    return 0;
}