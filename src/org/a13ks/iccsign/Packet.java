package org.a13ks.iccsign;

class Packet
{
    private byte[] data;
    private int dataLen;

    public Packet(byte[] recvData) {
        this.data = new byte[recvData.length];
        System.arraycopy(recvData, 0, this.data, 0, recvData.length);
        this.dataLen = recvData.length;
    }

    public Packet() {
        this.dataLen = 16;
        this.data = new byte[0xFF];
        this.data[0] = 32;
    }

    public void setRestartFlag() {
        int tmp5_4 = 0; byte[] tmp5_1 = this.data;tmp5_1[tmp5_4] = ((byte)(tmp5_1[tmp5_4] | 0x80));
    }

    public int getRestartFlag() {
        return this.data[0] & 0x80;
    }

    public void setAckType() {
        int tmp5_4 = 1; byte[] tmp5_1 = this.data;tmp5_1[tmp5_4] = ((byte)(tmp5_1[tmp5_4] | 0x1));
    }

    public boolean isAckType() {
        if ((this.data[1] & 0x1) != 0) {
            return true;
        }
        return false;
    }

    public void setICDataLen(short len) {
        len = (short)(len + 4);
        this.data[3] = ((byte)(len >>> 8));
        this.data[2] = ((byte)(len & 0xFF));
    }

    public void setSequence(int seq) {
        this.data[7] = ((byte)((seq & 0xFF000000) >> 24));
        this.data[6] = ((byte)((seq & 0xFF0000) >> 16));
        this.data[5] = ((byte)((seq & 0xFF00) >> 8));
        this.data[4] = ((byte)(seq & 0xFF));
    }

    public int getSequence() {
        return ((this.data[7] & 0xFF) << 24) + ((this.data[6] & 0xFF) << 16) + ((this.data[5] & 0xFF) << 8) + (this.data[4] & 0xFF);
    }

    private void setChecksumData(short checksum) {
        this.data[13] = ((byte)(checksum >>> 8));
        this.data[12] = ((byte)(checksum & 0xFF));
    }

    private void setChecksumHead(short checksum) {
        this.data[15] = ((byte)(checksum >>> 8));
        this.data[14] = ((byte)(checksum & 0xFF));
    }

    public void setCommand(short command) {
        this.data[17] = ((byte)(command >>> 8));
        this.data[16] = ((byte)(command & 0xFF));
        
        this.data[2] = 4;
        
        this.dataLen += 4;
    }
    
    public short getCommand() {
        return (short)(((this.data[17] & 0xFF) << 8) + (this.data[16] & 0xFF));
    }
    
    public void setICData(byte[] icData) {
        if (icData.length > 400)
        {

            return;
        }
        System.arraycopy(icData, 0, this.data, 20, icData.length);
        this.dataLen += icData.length;
    }

    private static short from32to16(long x) {
        x = (x & 0xFFFF) + (x >>> 16);
        
        x = (x & 0xFFFF) + (x >>> 16);
        
        return (short)(int)x;
    }

    public void calcCheckSum() {
        setChecksumData(csum(this.data, 16, this.dataLen - 16));
        setChecksumHead(csum(this.data, 0, 16));
    }

    public int verifyCheckSum() {
        if (csum(this.data, 0, 16) != 0)
        {

            return 1;
        }
        if (!isAckType()) {
            short dataChecksum = (short)(((this.data[13] & 0xFF) << 8) + (this.data[12] & 0xFF));
            if (csum(this.data, 16, this.dataLen - 16) != dataChecksum)
            {

                return 1;
            }
        }
        return 0;
    }

    public byte[] getBytes() {
        byte[] ret = new byte[this.dataLen];
        System.arraycopy(this.data, 0, ret, 0, this.dataLen);
        
        return ret;
    }

    public byte[] getICDataBytes() {
        return ProtoManager.arrayStrip(this.data, 20, this.dataLen - 20);
    }

    private static short csum(byte[] addr, int offset, int length) {
        int sum = 0;
        try {
            int count = length;
            int i = offset;
            
            while (count > 1) {
                sum += (addr[i] & 0xFF) + ((addr[(i + 1)] & 0xFF) << 8);
                i += 2;
                count -= 2;
            }
            if (count > 0) {
                sum += (addr[i] & 0xFF);
            }
            while (sum >> 16 != 0) {
                sum = (sum & 0xFFFF) + (sum >>> 16);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (short)(sum ^ 0xFFFFFFFF);
    }
}