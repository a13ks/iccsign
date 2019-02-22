package org.a13ks.iccsign;

import com.fazecast.jSerialComm.SerialPort;

public class PosICReader
{
        public static final int OPEN_COMM_ERROR = -1;
        public static final int CARD_ON_FAILED = -2;
        public static final int CARD_ON_SUCCESS = 0;
        private final int CMD_POWER_ON_ICCARD = 2;
        private final int CMD_POWER_DOWN_ICCARD = 3;
        private final int CMD_EXCHANGE_APDU = 4;
        private SerialPort port;
        private ProtoManager pm;

        public boolean open(String portName)
        {
            SerialPort[] ports = SerialPort.getCommPorts();

            // todo: find by portName
            this.port = ports[2];

            if (this.port != null) {
                port.setComPortParameters(115200, 8, 1, 0);
                port.openPort();

                if (this.port.isOpen()) {
                    this.pm = new ProtoManager(this.port);
                    if (this.pm.connect()) {
                        return true;
                    }
                }
            }

            return false;
        }

        public boolean cardPowerOn()
        {
            return this.pm.sendCommand(2);
        }

        public boolean cardPowerOff()
        {
            boolean flag = false;
            if (this.pm != null) {
                flag = this.pm.sendCommand(3);
                if (flag) {
                    if (this.port.isOpen()) {
                        this.port.closePort();
                    }
                    return !this.port.isOpen();
                }
            }
            return flag;
        }

        public byte[] processAPDU(byte[] reqData)
        {
            byte[] recData = this.pm.write(4, reqData);
            byte[] data = null;
            if ((recData != null) && (recData.length > 4)) {
                byte[] code = new byte[4];
                data = new byte[recData.length - 4];
                System.arraycopy(recData, 0, code, 0, 4);
                int codeStatus = (code[0] & 0xFF) + ((code[1] & 0xFF) << 8) + ((code[2] & 0xFF) << 16) + (
                    (code[3] & 0xFF) << 24);
                if (codeStatus != 0) {
                    return null;
                }
                System.arraycopy(recData, 4, data, 0, data.length);
            }
            
            if ((data.length == 2) && 
                (data[0] == 97)) {
                APDU apdu = new APDU();
                reqData = apdu.reqContactCardData(data[1]);
                recData = this.pm.write(4, reqData);
                if ((recData != null) && (recData.length > 4)) {
                    byte[] code = new byte[4];
                    data = new byte[recData.length - 4];
                    System.arraycopy(recData, 0, code, 0, 4);
                    int codeStatus = (code[0] & 0xFF) + ((code[1] & 0xFF) << 8) + ((code[2] & 0xFF) << 16) + (
                        (code[3] & 0xFF) << 24);
                    if (codeStatus != 0) {
                        return null;
                    }
                    System.arraycopy(recData, 4, data, 0, data.length);
                }
            }

            return data;
        }
}
