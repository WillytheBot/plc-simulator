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
    private JLabel lblTemp;
    private JLabel lblAxis;

    public App() {
        setTitle("PLC HMI Monitor");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- PANNELLO SUPERIORE (Monitoraggio Live) ---
        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        statusPanel.setBackground(Color.DARK_GRAY);
        
        lblTemp = new JLabel(" Temperatura: In attesa...", SwingConstants.CENTER);
        lblTemp.setForeground(Color.CYAN);
        lblTemp.setFont(new Font("Arial", Font.BOLD, 18));
        
        lblAxis = new JLabel(" Asse: In attesa...", SwingConstants.CENTER);
        lblAxis.setForeground(Color.ORANGE);
        lblAxis.setFont(new Font("Arial", Font.BOLD, 18));
        
        statusPanel.add(lblTemp);
        statusPanel.add(lblAxis);
        add(statusPanel, BorderLayout.NORTH);

        // --- PANNELLO CENTRALE (Log) ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.BOLD, 14));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // --- PANNELLO INFERIORE (Bottoni Manuali) ---
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        
        JButton btnTemp = new JButton("GET_TEMP (Manuale)");
        JButton btnAxis = new JButton("GET_AXIS (Manuale)");
        JButton btnAlarmOn = new JButton("ALARM_ON");
        JButton btnAlarmOff = new JButton("ALARM_OFF");

        buttonPanel.add(btnTemp);
        buttonPanel.add(btnAxis);
        buttonPanel.add(btnAlarmOn);
        buttonPanel.add(btnAlarmOff);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- EVENTI DEI BOTTONI ---
        btnTemp.addActionListener(e -> sendManualCommand("GET_TEMP"));
        btnAxis.addActionListener(e -> sendManualCommand("GET_AXIS"));
        btnAlarmOn.addActionListener(e -> sendManualCommand("ALARM_ON"));
        btnAlarmOff.addActionListener(e -> sendManualCommand("ALARM_OFF"));

        // --- IL POLLING AUTOMATICO (Ogni 1000 millisecondi) ---
        Timer pollTimer = new Timer(1000, e -> {
            // Chiede i dati in background e aggiorna le etichette in alto
            String tempResp = sendSilentCommand("GET_TEMP");
            String axisResp = sendSilentCommand("GET_AXIS");
            
            // Pulisce la risposta per mostrare solo i valori
            if(tempResp != null) lblTemp.setText(" " + tempResp.replace("\n", ""));
            if(axisResp != null) lblAxis.setText(" " + axisResp.replace("\n", ""));
        });
        pollTimer.start();
    }

    // Funzione per i comandi manuali (scrive nel log nero)
    private void sendManualCommand(String command) {
        String response = sendSilentCommand(command);
        if (response != null) {
            logArea.append("> " + command + " | " + response + "\n");
        } else {
            logArea.append(">> ERRORE CONNESSIONE\n");
        }
    }

    // Funzione di base che apre e chiude il socket, restituendo la stringa letta
    private String sendSilentCommand(String command) {
        try (Socket socket = new Socket("localhost", 5020);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println(command);
            return in.readLine();
            
        } catch (Exception ex) {
            return null; // Ritorna null se il server C++ è spento
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            App hmi = new App();
            hmi.setVisible(true);
        });
    }
}