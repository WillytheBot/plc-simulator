package com.hmi;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class App extends JFrame {

    // Componenti grafici
    private JTextArea logArea;

    public App() {
        // 1. Configurazione della finestra principale
        setTitle("PLC HMI Monitor");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 2. Creiamo il pannello superiore con i 4 bottoni
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 2, 10, 10)); // Griglia 2x2 con spazio

        JButton btnTemp = new JButton("GET_TEMP");
        JButton btnAxis = new JButton("GET_AXIS");
        JButton btnAlarmOn = new JButton("ALARM_ON");
        JButton btnAlarmOff = new JButton("ALARM_OFF");

        buttonPanel.add(btnTemp);
        buttonPanel.add(btnAxis);
        buttonPanel.add(btnAlarmOn);
        buttonPanel.add(btnAlarmOff);

        add(buttonPanel, BorderLayout.NORTH);

        // 3. Creiamo l'area di testo centrale (lo "schermo" dell'HMI)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.BOLD, 14));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // 4. Colleghiamo i bottoni alle azioni di rete
        // Quando clicchi, chiama la funzione sendCommand() passando il testo corretto
        btnTemp.addActionListener(e -> sendCommand("GET_TEMP"));
        btnAxis.addActionListener(e -> sendCommand("GET_AXIS"));
        btnAlarmOn.addActionListener(e -> sendCommand("ALARM_ON"));
        btnAlarmOff.addActionListener(e -> sendCommand("ALARM_OFF"));
    }

    // Questa funzione è l'equivalente del nostro "Netcat"
    private void sendCommand(String command) {
        try {
            // A. Creiamo il socket Client verso il server C++
            Socket socket = new Socket("localhost", 5020);
            
            // B. Prepariamo gli strumenti per SCRIVERE e LEGGERE
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // C. Inviamo il comando sulla rete
            out.println(command);

            // D. Leggiamo la risposta e la scriviamo a schermo
            String response = in.readLine();
            logArea.append("> Comando: " + command + " | Risposta: " + response + "\n");

            // E. Chiudiamo la comunicazione
            socket.close();

        } catch (Exception ex) {
            logArea.append(">> ERRORE DI CONNESSIONE: Il PLC Simulator è acceso?\n");
        }
    }

    public static void main(String[] args) {
        // Avviamo l'interfaccia grafica in modo sicuro
        SwingUtilities.invokeLater(() -> {
            App hmi = new App();
            hmi.setVisible(true);
        });
    }
}