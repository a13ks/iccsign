package org.a13ks.iccsign;

import com.fazecast.jSerialComm.SerialPort;

public class Program {

    public static void main(String[] args) {
        SerialPort[] ports = SerialPort.getCommPorts();
        SerialPort port = ports[2];
        port.setComPortParameters(115200, 8, 1, 0);
        port.openPort();

        ProtoManager manager = new ProtoManager(port);
        manager.connect();
        System.out.println("Connect done");

        manager.sendCommand(2); // power on card
        System.out.println("Power ON done");
    }
}
