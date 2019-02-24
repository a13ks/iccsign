package org.a13ks.iccsign;

import java.util.Arrays;
import java.util.LinkedList;

public class APDU
{
    public static final int SW_ERROR = -1;
    public static final int SW_SUCCESS = 0;
    public static final int SW_COMMAND_NOT_ALLOWED = -2;
    public static final int SW_WRONG_P1P2 = -3;
    public static final short SW_USER_KEY_NOT_IMPORTED = 253;
    public static final short SW_CERT_NOT_IMPORTED = 254;
    public static final int USER_PUB_KEY_NULL = 255;
    public static final int SW_FILE_NOT_FOUND = -2;
    public static final byte DOWNLOAD_RSA_KEY_INS = -43;
    public static final byte DOWNLOAD_CERT_INS = -9;
    public static final byte CARD_COPY_INS = -13;
    public static final byte READ_CARD_VER_INS = -54;
    public static final byte CLA_00 = 0;
    public static final byte CLA_80 = -128;
    public static final byte[] SW_9000 = { -112 };

    public static final byte[] SW_6801 = new byte[2];

    public static final byte[] SW_6a82 = { 106, -126 };
    
    public static final byte[] SW_6986 = { 105, -122 };
    
    public static final byte[] SW_6b00 = { 107 };
    
    public static final byte[] SW_6983 = { 105, -125 };
    
    public static final byte[] SW_6a80 = { 106, Byte.MIN_VALUE };
    
    private byte[] SW;
    
    public void setSW(byte[] sW)
    {
        this.SW = sW;
    }

    public int statusSW()
    {
        int status = -1;
        if (this.SW == null)
            return status;
        if (this.SW.length != 2) {
            status = -1;
        } else if (Arrays.equals(this.SW, SW_9000)) {
            status = 0;
        } else if (Arrays.equals(this.SW, SW_6a82)) {
            status = -2;
        } else if (Arrays.equals(this.SW, SW_6986)) {
            status = -2;
        } else if (Arrays.equals(this.SW, SW_6b00)) {
            status = -3;
        } else if (Arrays.equals(this.SW, SW_6983)) {
            status = 253;
        } else if (Arrays.equals(this.SW, SW_6a80)) {
            status = 254;
        } else {
            status = -1;
        }
        return status;
    }

    public byte[] selectApplication(String appName)
    {
        byte appNameLen = (byte)appName.length();
        byte[] selectApdu = new byte[5 + appNameLen];
        byte[] head = { 0, -92, 4 };
        byte[] lc = { appNameLen };
        byte[] data = appName.getBytes();
        System.arraycopy(head, 0, selectApdu, 0, 3);
        System.arraycopy(lc, 0, selectApdu, 3, 1);
        System.arraycopy(data, 0, selectApdu, 4, appNameLen);
        return selectApdu;
    }

    public static byte[] buildCardAppVerINS()
    {
        return new byte[] { 0, -54 };
    }

    public LinkedList<byte[]> buildDownloadAPDU(byte cla, byte ins, int fileTag, byte[] data)
    {
        LinkedList<byte[]> apdus = new LinkedList();
        int times = data.length / 255;
        int remainder = data.length % 255;
        int seq = 1;
        
        byte[] CLA = { cla };
        byte[] INS = { ins };
        byte[] p1 = { (byte)fileTag };
        
        for (int i = 0; i < times; i++)
        {
            byte[] range255 = new byte['?']; // ÿ
            
            System.arraycopy(data, i * 255, range255, 0, 255);
            
            if ((remainder == 0) && (i == times - 1)) {
                seq |= 0x80;
            }
            byte[] p2 = { (byte)seq };
            byte[] lc = { -1 };
            
            byte[] packet = new byte[range255.length + 5];
            
            System.arraycopy(CLA, 0, packet, 0, 1);
            System.arraycopy(INS, 0, packet, 1, 1);
            System.arraycopy(p1, 0, packet, 2, 1);
            System.arraycopy(p2, 0, packet, 3, 1);
            System.arraycopy(lc, 0, packet, 4, 1);
            System.arraycopy(range255, 0, packet, 5, 255);
            
            apdus.add(packet);
            
            seq++;
        }
        
        if (remainder != 0) {
            seq |= 0x80;
            byte[] p2 = { (byte)seq };
            byte[] lc = { (byte)remainder };
            byte[] remainderByte = new byte[remainder];
            System.arraycopy(data, times * 255, remainderByte, 0, remainder);
            
            byte[] packet = new byte[remainder + 5];
            
            System.arraycopy(CLA, 0, packet, 0, 1);
            System.arraycopy(INS, 0, packet, 1, 1);
            System.arraycopy(p1, 0, packet, 2, 1);
            System.arraycopy(p2, 0, packet, 3, 1);
            System.arraycopy(lc, 0, packet, 4, 1);
            System.arraycopy(remainderByte, 0, packet, 5, remainder);
            
            apdus.add(packet);
        }
        
        return apdus;
    }

    public byte[] buildReqPukAPDU(int fileTag)
    {
        byte[] reqData = null;
        
        byte[] head = { 0, -10, (byte)fileTag };
        reqData = head;
        return reqData;
    }

    public byte[] selectFile(short fileTag)
    {
        byte[] reqData = null;
        byte[] head = { 0, -92, 0, 0, 2 };
        byte[] data = { (byte)((fileTag & 0xFF00) >> 8), (byte)(fileTag & 0xFF) };
        reqData = new byte[head.length + data.length];
        
        System.arraycopy(head, 0, reqData, 0, head.length);
        System.arraycopy(data, 0, reqData, head.length, data.length);
        
        return reqData;
    }

    public byte[] readFile(short offset)
    {
        byte[] reqData = null;
        byte[] head = { 0, -80, (byte)((offset & 0xFF00) >> 8), (byte)(offset & 0xFF) };
        reqData = head;
        return reqData;
    }

    public byte[] buildSignAPDU(FileELF elf, byte[] result)
    {
        byte[] reqData = null;
        byte[] head = { Byte.MIN_VALUE, -15, Byte.parseByte(elf.getUser().replace("User", "")), 
            0, (byte)result.length };
        
        reqData = new byte[head.length + result.length];
        
        System.arraycopy(head, 0, reqData, 0, head.length);
        System.arraycopy(result, 0, reqData, head.length, result.length);
        
        return reqData;
    }

    public byte[] buildGetMoreSignAPDU(int seq)
    {
        return new byte[] { Byte.MIN_VALUE, -15, 0, (byte)seq };
    }

    public byte[] buildSign8110APDU(byte[] result)
    {
        byte[] reqData = null;
        byte[] head = { Byte.MIN_VALUE, -8, Byte.parseByte("User0".replace("User", "")), 
            0, (byte)result.length };
        
        reqData = new byte[head.length + result.length];
        
        System.arraycopy(head, 0, reqData, 0, head.length);
        System.arraycopy(result, 0, reqData, head.length, result.length);
        
        return reqData;
    }

    public byte[] buildGetMoreSign8110APDU(int seq)
    {
        return new byte[] { Byte.MIN_VALUE, -8, 0, (byte)seq };
    }

    public byte[] reqContactCardData(byte length)
    {
        return new byte[] { 0, -64, 0, 0, length };
    }

    public byte[] reqCardBackupData(int fileTag, int pktSeq)
    {
        return new byte[] { Byte.MIN_VALUE, -14, (byte)(fileTag + 16), (byte)pktSeq };
    }
}
