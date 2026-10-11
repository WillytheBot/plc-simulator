package com.hmi;

// Importiamo le librerie native di Java per disegnare l'interfaccia (Swing e AWT)
import javax.swing.*;
import java.awt.*;
// Importiamo le librerie per leggere/scrivere dati e gestire la rete (Socket)
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Estendiamo "JFrame": significa che la nostra classe "App" è a tutti gli effetti una "Finestra" del computer
public class App extends JFrame {

    // Questo numero di serie serve per evitare i warning in VS Code quando si usano oggetti grafici
    private static final long serialVersionUID = 1L;

    // --- VARIABILI GRAFICHE GLOBALI ---
    // Devono essere dichiarate qui fuori (e non dentro al costruttore) così tutte le funzioni 
    // (compreso il Timer) possono vederle e aggiornarne il testo.
    private JTextArea logArea;       // Il display nero centrale per lo storico dei comandi
    private JLabel lblTemp;          // L'etichetta testuale azzurra in alto (Temperatura)
    private JLabel lblAxis;          // L'etichetta testuale arancione in alto (Asse)
    private JTextField txtAxisInput; // La casella di testo bianca dove l'utente digita i numeri

    // --- COSTRUTTORE ---
    // Viene eseguito automaticamente non appena si avvia il programma ("new App()")
    public App() {
        // 1. Configuriamo la Finestra Principale
        setTitle("PLC HMI Monitor"); 
        setSize(600, 450);           
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Chiude il programma quando premi la 'X' rossa
        setLayout(new BorderLayout()); // Usa il layout "a bussola": NORD, SUD, EST, OVEST, CENTRO

        // 2. Creiamo il Pannello Superiore (Il monitoraggio live in background)
        // Usiamo un GridLayout per avere 1 riga e 2 colonne perfette
        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        statusPanel.setBackground(Color.DARK_GRAY);
        
        lblTemp = new JLabel(" Temperatura: In attesa...", SwingConstants.CENTER);
        lblTemp.setForeground(Color.CYAN);
        lblTemp.setFont(new Font("Arial", Font.BOLD, 18));
        
        lblAxis = new JLabel(" Asse: In attesa...", SwingConstants.CENTER);
        lblAxis.setForeground(Color.ORANGE);
        lblAxis.setFont(new Font("Arial", Font.BOLD, 18));
        
        // Aggiungiamo le due etichette al pannello...
        statusPanel.add(lblTemp);
        statusPanel.add(lblAxis);
        // ...e attacchiamo il pannello in alto (NORD) della finestra
        add(statusPanel, BorderLayout.NORTH);

        // 3. Creiamo l'area di testo Centrale (Il Log nero)
        logArea = new JTextArea();
        logArea.setEditable(false); // Impedisce all'operatore di scriverci dentro direttamente
        logArea.setFont(new Font("Monospaced", Font.BOLD, 14));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        // "JScrollPane" aggiunge automaticamente la barra di scorrimento laterale se il testo è troppo lungo
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // 4. Creiamo il Pannello Inferiore (I pulsanti di comando manuali)
        // GridLayout(3, 2) significa: 3 righe e 2 colonne, con 5 pixel di spazio (margine) tra un bottone e l'altro
        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        // Costruiamo fisicamente i bottoni
        JButton btnTemp = new JButton("GET_TEMP (Manuale)");
        JButton btnAxis = new JButton("GET_AXIS (Manuale)");
        JButton btnAlarmOn = new JButton("ALARM_ON");
        JButton btnAlarmOff = new JButton("ALARM_OFF");
        JButton btnSetAxis = new JButton("SET_AXIS (Invia Quota)");

        // Costruiamo la casella di testo
        txtAxisInput = new JTextField();
        txtAxisInput.setToolTipText("Digita qui la quota dell'asse (es. 150)");

        // Aggiungiamo i componenti al pannello. L'ordine in cui li inseriamo riempirà la griglia da in alto a sinistra.
        buttonPanel.add(btnTemp);       // Riga 1, Colonna 1
        buttonPanel.add(btnAxis);       // Riga 1, Colonna 2
        buttonPanel.add(btnAlarmOn);    // Riga 2, Colonna 1
        buttonPanel.add(btnAlarmOff);   // Riga 2, Colonna 2
        buttonPanel.add(txtAxisInput);  // Riga 3, Colonna 1 (La casella di input)
        buttonPanel.add(btnSetAxis);    // Riga 3, Colonna 2 (Il bottone per inviare l'input)
        
        // Attacchiamo il pannello dei bottoni in basso (SUD)
        add(buttonPanel, BorderLayout.SOUTH);

        // 5. Assegniamo le "Azioni" ai bottoni
        // Quando un bottone viene premuto (e -> ...), invoca la funzione 'sendManualCommand' passando la stringa testuale
        btnTemp.addActionListener(e -> sendManualCommand("GET_TEMP"));
        btnAxis.addActionListener(e -> sendManualCommand("GET_AXIS"));
        btnAlarmOn.addActionListener(e -> sendManualCommand("ALARM_ON"));
        btnAlarmOff.addActionListener(e -> sendManualCommand("ALARM_OFF"));

        // L'azione del pulsante parametrico è più complessa:
        btnSetAxis.addActionListener(e -> {
            // A. Legge il testo attualmente scritto nella casella e usa trim() per rimuovere eventuali spazi vuoti prima e dopo
            String quota = txtAxisInput.getText().trim();
            // B. Se la casella NON è vuota...
            if (!quota.isEmpty()) {
                // C. Concatena la stringa fissa con il numero (es. "SET_AXIS " + "200") e lo invia al server C++
                sendManualCommand("SET_AXIS " + quota);
                // D. Svuota la casella di testo (rimettendola a "") per un nuovo inserimento
                txtAxisInput.setText(""); 
            }
        });

        // 6. Il Polling Automatico (Il Cuore dell'HMI)
        // Creiamo un timer che ogni 1000 millisecondi (1 secondo) esegue il codice tra le parentesi graffe
        Timer pollTimer = new Timer(1000, e -> {
            // Chiama la funzione "silenziosa" per chiedere la temperatura
            String tempResp = sendSilentCommand("GET_TEMP");
            // Chiama la funzione "silenziosa" per chiedere l'asse
            String axisResp = sendSilentCommand("GET_AXIS");
            
            // Se il server C++ ha risposto (non è null), aggiorna il testo delle etichette in alto.
            // .replace("\n", "") serve a togliere l'"A-Capo" che il server C++ invia alla fine di ogni risposta, 
            // altrimenti il layout grafico si deformerebbe.
            if(tempResp != null) lblTemp.setText(" " + tempResp.replace("\n", ""));
            if(axisResp != null) lblAxis.setText(" " + axisResp.replace("\n", ""));
        });
        // Avviamo il timer
        pollTimer.start();
    }

    // --- FUNZIONI DI SUPPORTO PER LA RETE ---

    // Questa funzione serve ai BOTTONI. Invia il comando e poi STAMPA la risposta nel log nero.
    private void sendManualCommand(String command) {
        String response = sendSilentCommand(command); // Riutilizza la funzione principale sotto
        if (response != null) {
            logArea.append("> " + command + " | " + response + "\n");
        } else {
            logArea.append(">> ERRORE CONNESSIONE AL SERVER C++\n");
        }
    }

    // Questa è la funzione BASE. Apre fisicamente il socket, invia il testo e restituisce la stringa di risposta.
    // Viene usata sia dal Timer (che non stampa nulla nel log) sia dalla funzione manuale (che poi stampa).
    private String sendSilentCommand(String command) {
        // Il costrutto try(...) apre la connessione. Alla fine del blocco, chiude automaticamente il Socket per non esaurire la memoria.
        try (Socket socket = new Socket("localhost", 5020);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // "Strumento" per inviare dati
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) { // "Strumento" per leggere i dati ricevuti
            
            // Invia il comando verso il server C++
            out.println(command);
            // Restituisce la risposta del server
            return in.readLine();
            
        } catch (Exception ex) {
            // Se fallisce (es: il server C++ è spento), non va in crash ma ritorna "null" (niente)
            return null;
        }
    }

    // --- PUNTO DI INGRESSO (Main) ---
    public static void main(String[] args) {
        // SwingUtilities.invokeLater() è una regola standard di Java: assicura che l'interfaccia 
        // grafica venga disegnata in modo sicuro e senza "sfarfallii" dal sistema operativo.
        SwingUtilities.invokeLater(() -> {
            App hmi = new App(); // Crea un'istanza della finestra
            hmi.setVisible(true); // La rende visibile sullo schermo
        });
    }
}