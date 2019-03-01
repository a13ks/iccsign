// 
// Decompiled by Procyon v0.5.30
// 

package org.a13ks.iccsign.impl;

import java.util.LinkedList;
import org.a13ks.iccsign.PosConnectDao;
import org.a13ks.iccsign.DataChangeEvent;

public class ProtoManager implements DataChangeEvent.DataResponse
{
	private final int SEQ_START = 4097;
    private int timeout;
    private PosConnectDao posConnectDao;
    DataChangeEvent.DataResponse protoCallback;
    private int pktSequence;
    private int recvNextSeq;
    private int ackedPktSeq;
    private boolean restartSent;
    private LinkedList<Packet> rQueue;
    byte[] recvCache;
    int recvCacheIdx;
    
    public void setTimeout(final int timeout) {
        if (timeout != 0) {
            this.timeout = timeout;
        }
    }
    
    public static byte[] arrayStrip(final byte[] src, final int offset, final int length) {
        final int len = (src.length - offset < length) ? (src.length - offset) : length;
        if (len == 0) {
            return null;
        }
        final byte[] tmp = new byte[len];
        System.arraycopy(src, offset, tmp, 0, len);
        return tmp;
    }
    
    public int getNewSequence() {
        return this.pktSequence++;
    }
    
    public ProtoManager(final PosConnectDao connectDao) {
        this.timeout = 2;
        this.pktSequence = SEQ_START;
        this.recvNextSeq = SEQ_START;
        this.ackedPktSeq = 0;
        this.restartSent = false;
        this.recvCacheIdx = 0;
        this.posConnectDao = connectDao;
        this.recvCache = new byte[2048];
        this.rQueue = new LinkedList<Packet>();
    }
    
    private Packet lowWrite(final Packet pkt) {
        int retry = 3;
        Packet ret = null;
        boolean acked = false;
        synchronized (this) {
            final int seq = this.getNewSequence();
            pkt.setSequence(seq);
            pkt.calcCheckSum();
            this.posConnectDao.write(this.translated(pkt.getBytes()), this);
            while (retry-- > 0) {
                try {
                    this.wait(this.timeout * 1000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (this.rQueue.isEmpty()) {
                    if (acked) {
                        continue;
                    }
                    this.posConnectDao.write(this.translated(pkt.getBytes()), this);
                } else {
                    final Packet rPkt = this.rQueue.pop();
                    if (rPkt.getSequence() != seq) {
                        continue;
                    }
                    if (!rPkt.isAckType()) {
                        ret = rPkt;
                        break;
                    }
                    acked = true;
                    this.ackedPktSeq = rPkt.getSequence();
                }
            }
        }
        return ret;
    }
    
    public boolean connect() {
        return this.sendCommand(1);
    }
    
    public boolean sendCommand(final int command) {
        final Packet pkt = new Packet();
        if (!this.restartSent) {
            pkt.setRestartFlag();
            this.restartSent = true;
        }
        pkt.setCommand((short)command);
        final Packet reply = this.lowWrite(pkt);
        if (reply == null) {
            return false;
        }
        final byte[] result = reply.getICDataBytes();
        int status = 0;
        if (result != null) {
            status = (result[0] & 0xFF) + ((result[1] & 0xFF) << 8) + ((result[2] & 0xFF) << 16) + ((result[3] & 0xFF) << 24);
        }
        return reply != null && reply.getCommand() == command && status == 0;
    }
    
    public byte[] write(final int apduCommand, final byte[] icData) {
        final Packet pkt = new Packet();
        pkt.setCommand((short)apduCommand);
        pkt.setICDataLen((short)icData.length);
        pkt.setICData(icData);
        return this.lowWrite(pkt).getICDataBytes();
    }
    
    private byte[] translated(final byte[] data) {
        int idx = 0;
        final byte[] tmp = new byte[data.length * 2];
        tmp[idx++] = 126;
        for (int i = 0; i < data.length; ++i) {
            if (data[i] == 126) {
                tmp[idx++] = -37;
                tmp[idx++] = -36;
            }
            else if (data[i] == -37) {
                tmp[idx++] = -37;
                tmp[idx++] = -35;
            }
            else {
                tmp[idx++] = data[i];
            }
        }
        tmp[idx++] = 126;
        final byte[] ret = new byte[idx];
        System.arraycopy(tmp, 0, ret, 0, idx);
        return ret;
    }
    
    private byte[] restore(final byte[] buf) {
        final byte[] tmp = new byte[buf.length];
        int idx = 0;
        for (int i = 0; i < buf.length; ++i) {
            byte c = buf[i];
            switch (c) {
                case 126: {
                    continue;
                }
                case -37: {
                    if (++i >= buf.length) {
                        return null;
                    }
                    if (buf[i] == -36) {
                        c = 126;
                        break;
                    }
                    if (buf[i] == -35) {
                        c = -37;
                        break;
                    }
                    return null;
                }
            }
            tmp[idx++] = c;
        }
        return arrayStrip(tmp, 0, idx);
    }
    
    private void sendAck(final int seq) {
        final Packet pkt = new Packet();
        pkt.setAckType();
        pkt.setSequence(seq);
        pkt.calcCheckSum();
        this.posConnectDao.write(this.translated(pkt.getBytes()), null);
    }
    
    private void inputPacket(final Packet pkt) {
        if (pkt.verifyCheckSum() != 0) {
            return;
        }
        if (pkt.getRestartFlag() != 0 && (this.ackedPktSeq >= SEQ_START || this.recvNextSeq > SEQ_START)) {
            this.pktSequence = SEQ_START;
            this.recvNextSeq = SEQ_START;
        }
        if (!pkt.isAckType()) {
            if (pkt.getSequence() > this.recvNextSeq) {
                return;
            }
            this.sendAck(pkt.getSequence());
            if (pkt.getSequence() != this.recvNextSeq) {
                return;
            }
            ++this.recvNextSeq;
        }
        this.rQueue.add(pkt);
        synchronized (this) {
            this.notify();
        }
    }
    
    @Override
    public void callBack(final byte[] buf) {
        for (int i = 0; i < buf.length; ++i) {
            if (buf[i] == 126 && this.recvCacheIdx > 0) {
                final byte[] pktBuf = this.restore(arrayStrip(this.recvCache, 0, this.recvCacheIdx));
                if (pktBuf != null) {
                    final Packet pkt = new Packet(pktBuf);
                    this.inputPacket(pkt);
                }
                this.recvCacheIdx = 0;
            }
            else {
                this.recvCache[this.recvCacheIdx++] = buf[i];
            }
        }
    }
}
