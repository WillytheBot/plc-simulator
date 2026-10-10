#include <iostream>
#include <thread>
#include <chrono>
#include <sys/socket.h> // Contiene la funzione base socket()
#include <netinet/in.h> // Contiene le strutture per gli indirizzi IP e Porte
#include <unistd.h>     // Contiene la funzione close() per chiudere la connessione
#include <string>
class PlcState {
    private:
        float temperature;
        bool allarm;
        int axis_position;

    public:
        PlcState(): temperature(20.0), allarm(false), axis_position(0) {}
        
        float getTemperature() const { return temperature; }
        bool isAllarmActive() const { return allarm; }
        int getAxisPosition() const { return axis_position; }

        void setTemperature(float temp) { temperature = temp; }
        void allarmOn() { allarm = true; }
        void allarmOff() { allarm = false; }
        // Questo sarà il lavoro dell'operaio macchina
        void runSimulation() {
            bool isRunning = true;
            while (isRunning) {
                float tempActual = getTemperature();
                setTemperature(tempActual + 0.5);
                std::cout << "Temperatura attuale: " << tempActual << " gradi" << std::endl;
                std::this_thread::sleep_for(std::chrono::seconds(1));
            }
        }
};

class TcpServer {
    private:
        int porta;
        int server_fd;
        PlcState& plc; // IL PONTE: Riferimento al nostro PLC fisico

    public:
        // Costruttore: ora riceve anche il riferimento al PLC
        TcpServer(int porta_desiderata, PlcState& plc_ref) : porta(porta_desiderata), server_fd(-1), plc(plc_ref) {}

        void start() {
            std::cout << "Inizializzazione server sulla porta " << porta << std::endl;
            server_fd = socket(AF_INET, SOCK_STREAM, 0);
            
            if (server_fd == -1) {
                std::cerr << "ERRORE: Impossibile creare il socket di rete!" << std::endl;
                return;
            }
            
            struct sockaddr_in address;
            address.sin_family = AF_INET;
            address.sin_addr.s_addr = INADDR_ANY; 
            address.sin_port = htons(porta);      

            if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
                std::cerr << "ERRORE: Impossibile fare il bind" << std::endl;
                return;
            }

            if (listen(server_fd, 3) < 0) { 
                std::cerr << "ERRORE: Listen fallito!" << std::endl;
                return;
            }
            std::cout << "Server in ascolto. In attesa di connessioni..." << std::endl;

            while (true) {
                struct sockaddr_in client_address;
                socklen_t client_len = sizeof(client_address);
                
                int client_socket = accept(server_fd, (struct sockaddr *)&client_address, &client_len);
                
                if (client_socket < 0) {
                    continue; 
                }
                
                char buffer[1024] = {0}; 
                int valread = read(client_socket, buffer, 1024);
                
                if (valread > 0) {
                    // Trasformiamo i dati grezzi in una comoda stringa C++
                    std::string comando(buffer);
                    std::cout << "\n[RETE] Comando ricevuto: " << comando;

                    // Il nostro protocollo di comunicazione
                    // Il nostro protocollo di comunicazione
                    if (comando.find("GET_TEMP") != std::string::npos) {
                        std::string risposta = "Valore Temperatura: " + std::to_string(plc.getTemperature()) + "\n";
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    } 
                    else if (comando.find("ALARM_ON") != std::string::npos) {
                        plc.allarmOn();
                        std::string risposta = "OK: Allarme PLC Attivato!\n";
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    }
                    // --- I NUOVI COMANDI ---
                    else if (comando.find("ALARM_OFF") != std::string::npos) {
                        plc.allarmOff();
                        std::string risposta = "OK: Allarme PLC Disattivato!\n";
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    }
                    else if (comando.find("GET_AXIS") != std::string::npos) {
                        std::string risposta = "Posizione Asse: " + std::to_string(plc.getAxisPosition()) + "\n";
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    }
                    // -----------------------
                    else {
                        std::string risposta = "ERR: Comando Sconosciuto\n";
                        send(client_socket, risposta.c_str(), risposta.length(), 0);
                    }
                }

                close(client_socket);
            }
        }
};



int main() {
    std::cout << "Avvio del PLC Simulator..." << std::endl;
    
    // Costruiamo il PLC
    PlcState plcState;
    
    // Avviamo l'Operaio Macchina (Thread 1) 
    // Passiamo la funzione runSimulation() e l'oggetto plcState su cui deve lavorare
    std::thread plcThread(&PlcState::runSimulation, &plcState);
    
    // L'Operaio Centrale (Main Thread) si occuperà della rete
    TcpServer tcpServer(5020, plcState); // Passiamo il riferimento al PLC al server TCP
    tcpServer.start(); // Il programma si bloccherà qui sull'accept(), ma il plcThread continuerà a girare!
    
    // Aspettiamo che il Thread del PLC finisca (in questo caso non finirà mai per via del ciclo while)
    plcThread.join();

    return 0;
}
