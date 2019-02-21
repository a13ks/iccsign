package org.a13ks.iccsign;

import java.util.LinkedList;
import com.fazecast.jSerialComm.SerialPort;

public class ProtoManager {
    private SerialPort serialPort;
    private final int SEQ_START = 4097;
    private int timeout = 2;
    private int pktSequence = 4097;
    private int recvNextSeq = 4097;
    private int ackedPktSeq = 0;
    private boolean restartSent = false;
    private LinkedList<Packet> rQueue;
    byte[] recvCache;
    int recvCacheIdx = 0;

    public void setTimeout(int timeout) {
        if (timeout != 0)
            this.timeout = timeout;
    }

    public static byte[] arrayStrip(byte[] src, int offset, int length) {
        int len = src.length - offset < length ? src.length - offset : length;
        if (len == 0) {
            return null;
        }
        byte[] tmp = new byte[len];
        System.arraycopy(src, offset, tmp, 0, len);
        return tmp;
    }

    public int getNewSequence() {
        return this.pktSequence++;
    }

    public ProtoManager(SerialPort serialPort) {
        this.serialPort = serialPort;
        this.recvCache = new byte[255];
        this.rQueue = new LinkedList();
    }

    private Packet lowWrite(Packet pkt) {
        int retry = 3;
        Packet ret = null;
        boolean acked = false;
        
        synchronized (this) {
            int seq = getNewSequence();
            pkt.setSequence(seq);
            pkt.calcCheckSum();
            this.serialPort.writeBytes(translated(pkt.getBytes()), pkt.getBytes().length);

            while (retry-- > 0) {
                int bytes = serialPort.bytesAvailable();
                if (bytes > 0) {
                    byte[] buffer = new byte[bytes];
                    callBack(buffer);
                }
                        
                try {
                    wait(this.timeout * 3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                if (this.rQueue.isEmpty()) {
                    if (!acked) {
                        this.serialPort.writeBytes(translated(pkt.getBytes()), pkt.getBytes().length);
                    }
                } else {
                    Packet rPkt = (Packet)this.rQueue.pop();
                    if (rPkt.getSequence() == seq)
                        if (rPkt.isAckType())  {
                            acked = true;
                            this.ackedPktSeq = rPkt.getSequence();
                        } else {
                            ret = rPkt;
                            break;
                        }
                }
            }
        }
        return ret;
    }

    public boolean connect() {
        return sendCommand(1);
    }

    public boolean sendCommand(int command) {
        Packet pkt = new Packet();
        
        if (!this.restartSent) {
            pkt.setRestartFlag();
            this.restartSent = true;
        }
        pkt.setCommand((short)command);
        
        Packet reply = lowWrite(pkt);
        if (reply == null)
            return false;
        byte[] result = reply.getICDataBytes();
        int status = 0;
        
        if (result != null) {
            status = (result[0] & 0xFF) + ((result[1] & 0xFF) << 8) + ((result[2] & 0xFF) << 16) + (
                (result[3] & 0xFF) << 24);
        }
        if ((reply != null) && (reply.getCommand() == command) && (status == 0)) {
            return true;
        }
        return false;
    }

    public byte[] write(int apduCommand, byte[] icData) {
        Packet pkt = new Packet();
        
        pkt.setCommand((short)apduCommand);
        pkt.setICDataLen((short)icData.length);
        pkt.setICData(icData);
        
        return lowWrite(pkt).getICDataBytes();
    }

    private byte[] translated(byte[] data) {
        int idx = 0;
        byte[] tmp = new byte[data.length * 2];
        
        tmp[(idx++)] = 126;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 126) {
                tmp[(idx++)] = -37;
                tmp[(idx++)] = -36;
            }
            else if (data[i] == -37) {
                tmp[(idx++)] = -37;
                tmp[(idx++)] = -35;
            } else {
                tmp[(idx++)] = data[i];
            }
        }
        tmp[(idx++)] = 126;
        
        byte[] ret = new byte[idx];
        System.arraycopy(tmp, 0, ret, 0, idx);
        return ret;
    }

    private byte[] restore(byte[] buf) {
        byte[] tmp = new byte[buf.length];
        int idx = 0;
        
        for (int i = 0; i < buf.length; i++) {
            byte c = buf[i];
            switch (c) {
            case 126: 
                break;
            case -37: 
                i++; 
                if (i >= buf.length) {
                    return null;
                }
                if (buf[i] == -36) {
                    c = 126;
                }
                else if (buf[i] == -35) {
                    c = -37;
                } else {
                    return null;
                }
                break;
            }
            tmp[(idx++)] = c;
        }
        return arrayStrip(tmp, 0, idx);
    }

    private void sendAck(int seq) {
        Packet pkt = new Packet();

        pkt.setAckType();
        pkt.setSequence(seq);
        pkt.calcCheckSum();
        this.serialPort.writeBytes(translated(pkt.getBytes()), pkt.getBytes().length);
    }

    private void inputPacket(Packet pkt)
    {
        if (pkt.verifyCheckSum() != 0) { 
            return;
        }

        if ((pkt.getRestartFlag() != 0) && ((this.ackedPktSeq >= 4097) || (this.recvNextSeq > 4097))) {
            this.pktSequence = 4097;
            this.recvNextSeq = 4097;
        }

        if (!pkt.isAckType()) {
            if (pkt.getSequence() > this.recvNextSeq)
            { 
                return;
            }
            sendAck(pkt.getSequence());
            if (pkt.getSequence() == this.recvNextSeq) {
                this.recvNextSeq += 1;
            } else {
                return;
            }
        }

        this.rQueue.add(pkt);

        synchronized (this) {
            notify();
        }
    }

    public void callBack(byte[] buf) {
        for (int i = 0; i < buf.length; i++) {
            if ((buf[i] == 126) && (this.recvCacheIdx > 0)) {
                byte[] pktBuf = restore(arrayStrip(this.recvCache, 0, this.recvCacheIdx));
                if (pktBuf != null) {
                    Packet pkt = new Packet(pktBuf);
                    inputPacket(pkt);
                }
                this.recvCacheIdx = 0;
            } else {
                this.recvCache[(this.recvCacheIdx++)] = buf[i];
            }
        }
    }
}