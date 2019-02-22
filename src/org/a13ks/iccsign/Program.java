package org.a13ks.iccsign;

import com.fazecast.jSerialComm.SerialPort;

public class Program {

    public static void main(String[] args) {

        PosICReader icReader = new PosICReader();
        icReader.open("/dev/tty.usbserial-AH01SKWE");

        APDU apdu = new APDU();
        byte[] selectAPPResp = icReader.processAPDU(apdu.selectApplication("NEWPOS-CARD"));
        apdu.setSW(selectAPPResp);
        int status = apdu.statusSW();
    }
}
