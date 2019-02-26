package org.a13ks.iccsign;

public class CardInfo
{
    private byte[] version;
    private byte[] SN;
    private byte[] type;
    private String makeTime;

    public byte[] getVersion()
    {
        return this.version;
    }
    
    public void setVersion(byte[] version) {
        this.version = version;
    }
    
    public byte[] getSN() {
        return this.SN;
    }
    
    public void setSN(byte[] sN) {
        this.SN = sN;
    }
    
    public byte[] getType() {
        return this.type;
    }
    
    public void setType(byte[] type) {
        this.type = type;
    }
    
    public String getMakeTime() {
        return this.makeTime;
    }
    
    public void setMakeTime(String makeTime) {
        this.makeTime = makeTime;
    }
}