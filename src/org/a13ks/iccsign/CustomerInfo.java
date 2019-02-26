package org.a13ks.iccsign;

public class CustomerInfo
{
    private byte[] cid;
    private byte[] name;
    private String address;
    private String phone;
    private byte[] reserve;

    public byte[] getCid()
    {
        return this.cid;
    }

    public void setCid(byte[] cid) {
        this.cid = cid;
    }

    public byte[] getName() {
        return this.name;
    }

    public void setName(byte[] name) {
        this.name = name;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public byte[] getReserve() {
        return this.reserve;
    }

    public void setReserve(byte[] reserve) {
        this.reserve = reserve;
    }
}
