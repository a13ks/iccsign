package org.a13ks.iccsign;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileELF
{
    public static final String ET_APP = "APP";
    public static final String ET_LIB = "LIB";
    private String fileName;
    private String fileType;
    private String filePath;
    private String effectiveDate;
    private String expireDate;
    private String user;
    private String appProvider;
    private String version;
    private String note;
    private String signedTag;
    private byte[] fileConent;

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    
    public String getFileName()
    {
        return this.fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileType() {
        return this.fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getFilePath() {
        return this.filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public byte[] getFileConent() {
        return this.fileConent;
    }
    
    public void setFileConent(byte[] fileConent) {
        this.fileConent = fileConent;
    }
    
    public String getEffectiveDate() {
        return this.effectiveDate;
    }
    
    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public void setEffectiveDate(Date effectiveDate) {

        this.effectiveDate = dateFormat.format(effectiveDate);
    }
    
    public String getExpireDate() {
        return this.expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = dateFormat.format(expireDate);
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }
    
    public String getUser() {
        return this.user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public String getVersion() {
        return this.version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getNote() {
        return this.note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public String getAppProvider() {
        return this.appProvider;
    }
    
    public void setAppProvider(String appProvider) {
        this.appProvider = appProvider;
    }
    
    public String getSignedTag() {
        return this.signedTag;
    }
    
    public void setSignedTag(String signedTag) {
        this.signedTag = signedTag;
    }
}
