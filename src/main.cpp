#include <iostream>

#include <thread>

#include <chrono>

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
};

int main() {



    std::cout << "Avvio del PLC Simulator..." << std::endl;

    bool isRunning = true;

   PlcState plcState;

   

    while (isRunning) {

        std::cout << "Simulazione in corso..." << std::endl;
        float tempActual = plcState.getTemperature();
        plcState.setTemperature(tempActual + 0.5);
        std::cout << "Temperatura attuale: " << tempActual << " gradi" << std::endl;

        std::this_thread::sleep_for(std::chrono::seconds(1));

    }

    return 0;

} 
