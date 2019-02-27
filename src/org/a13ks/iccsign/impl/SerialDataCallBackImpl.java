// 
// Decompiled by Procyon v0.5.30
// 

package org.a13ks.iccsign.impl;

import java.io.IOException;
import org.a13ks.iccsign.DataChangeEvent;
import java.io.InputStream;
import java.io.OutputStream;
import org.a13ks.iccsign.PosConnectDao;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class SerialDataCallBackImpl implements PosConnectDao, SerialPortDataListener
{
    private SerialPort sPort;
    private OutputStream os;
    private InputStream is;
    private static DataChangeEvent.DataResponse response;
    private boolean open;
    
    public SerialDataCallBackImpl() {
        this.open = false;
    }
    
    public void openSerial(final String portName) {
        try {
//            this.portId = CommPortIdentifier.getPortIdentifier(portName);
        	this.sPort = SerialPort.getCommPort(portName);
        	this.sPort.setComPortParameters(115200, 8, 1, 0);
            this.sPort.addDataListener(this);
            this.open = this.sPort.openPort();

            this.sPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
            this.sPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
            
            this.os = this.sPort.getOutputStream();
            this.is = this.sPort.getInputStream();
        }
        catch (Exception e) {
            e.printStackTrace();
            this.open = false;
        }
    }
    
    public void closeSerial() {
        if (!this.open) {
            return;
        }
        if (this.sPort != null) {
            try {
                this.os.close();
                this.is.close();
            }
            catch (IOException e) {
                System.err.println(e);
            }
            this.sPort.closePort();
//            this.portId.removePortOwnershipListener(this);
        }
        this.open = false;
    }
    
    public boolean isOpen() {
        return this.open;
    }

    @Override
    public void write(final byte[] data, final DataChangeEvent.DataResponse dataResponse) {
        try {
            if (this.os != null) {
                this.os.write(data);
                this.os.flush();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (dataResponse != null) {
            SerialDataCallBackImpl.response = dataResponse;
        }
    }

	@Override
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
	}

    @Override
    public void serialEvent(final SerialPortEvent event) {
        switch (event.getEventType()) {
            case SerialPort.LISTENING_EVENT_DATA_AVAILABLE: {
                final byte[] readBuffer = new byte[2048];
                try {
                    final int length = this.is.available();
                    int numBytes;
                    for (int offset = 0; offset < length; offset += numBytes) {
                        numBytes = this.is.read(readBuffer, offset, length - offset);
                    }
                    final byte[] avaliableBytes = new byte[length];
                    System.arraycopy(readBuffer, 0, avaliableBytes, 0, length);
                    SerialDataCallBackImpl.response.callBack(avaliableBytes);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
