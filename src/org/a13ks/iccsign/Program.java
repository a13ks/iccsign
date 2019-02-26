package org.a13ks.iccsign;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

public class Program {

    public static volatile String sigVersion = "V02";
    public static final String[] SIG = { "V01", "V02" };

    private static Map<Integer, byte[]> userCertMap = new HashMap();
    private static int userSeq = 0;
    private static byte[] tmpData = null;

    public static CardInfo getCardInfo(PosICReader icReader)
    {
        CardInfo cardInfo = null;
        APDU selectFileApdu = new APDU();
        byte[] selectFileReq = selectFileApdu.selectFile((short)1);
        byte[] receiveData = icReader.processAPDU(selectFileReq);

        if (receiveData.length == 2) {
            byte[] sw = receiveData;
            selectFileApdu.setSW(sw);
            if (selectFileApdu.statusSW() == 0) {
                byte[] readFileReq = selectFileApdu.readFile((short)0);
                byte[] readFileBackData = icReader.processAPDU(readFileReq);

                if (readFileBackData.length > 2) {
                    byte[] readFileBackSW = new byte[2];
                    System.arraycopy(readFileBackData, readFileBackData.length - 2, readFileBackSW, 0, 2);
                    selectFileApdu.setSW(readFileBackSW);
                    if (selectFileApdu.statusSW() == 0) {
                        byte[] data = new byte[readFileBackData.length - 2];
                        System.arraycopy(readFileBackData, 0, data, 0, data.length);

                        if (data.length == 15) {
                            cardInfo = new CardInfo();
                            byte[] version = new byte[3];
                            byte[] SN = new byte[4];
                            byte[] type = new byte[1];
                            byte[] makeTime = new byte[7];

                            System.arraycopy(data, 0, version, 0, 3);
                            System.arraycopy(data, 3, SN, 0, 4);
                            System.arraycopy(data, 7, type, 0, 1);
                            System.arraycopy(data, 8, makeTime, 0, 7);

                            cardInfo.setVersion(version);
                            cardInfo.setSN(SN);
                            cardInfo.setType(type);
                            cardInfo.setMakeTime(bytesToHexString(makeTime));
                        }
                    }
                }
            }
        }
        return cardInfo;
    }
    
    public static CustomerInfo getCustomerInfo(PosICReader icReader)
    {
        CustomerInfo customerInfo = null;
        APDU customFileApdu = new APDU();
        byte[] selectFileReq = customFileApdu.selectFile((short)2);
        byte[] receiveData = icReader.processAPDU(selectFileReq);
    
        if (receiveData.length == 2) {
            byte[] sw = receiveData;
            customFileApdu.setSW(sw);
            if (customFileApdu.statusSW() == 0) {
                byte[] readFileReq = customFileApdu.readFile((short)0);
                byte[] customerBackData = icReader.processAPDU(readFileReq);
    
                if (customerBackData.length > 2) {
                    byte[] customerFileBackSW = new byte[2];
                    System.arraycopy(customerBackData, customerBackData.length - 2, 
                        customerFileBackSW, 0, 2);
                    customFileApdu.setSW(customerFileBackSW);
                    if (customFileApdu.statusSW() == 0) {
                        byte[] data = new byte[customerBackData.length - 2];
                        System.arraycopy(customerBackData, 0, data, 0, data.length);
    
                        customerInfo = new CustomerInfo();
                        byte[] cid = new byte[2];
                        byte[] name = new byte[64];
                        byte[] address = new byte[100];
                        byte[] phone = new byte[30];
                        byte[] reserve = new byte[18];
    
                        System.arraycopy(data, 0, cid, 0, 2);
                        System.arraycopy(data, 2, name, 0, 64);
                        System.arraycopy(data, 66, address, 0, 100);
                        System.arraycopy(data, 166, phone, 0, 30);
                        System.arraycopy(data, 196, reserve, 0, 18);
    
                        customerInfo.setCid(cid);
                        customerInfo.setName(name);
                        customerInfo.setAddress(bytesToHexString(address));
                        customerInfo.setPhone(bytesToHexString(phone));
                        customerInfo.setReserve(reserve);
                    }
                }
            }
        }
        return customerInfo;
    }

    public static int signFile(String fileName, PosICReader icReader) throws Exception {

        FileELF elf = new FileELF();

        int user = Integer.parseInt(elf.getUser().replace("User", ""));
        userSeq = user;
        int flleTag = user + 32;

        if (SIG[1].equals(sigVersion)) {
            if ((userCertMap != null) && (userCertMap.get(Integer.valueOf(user)) != null))
            {
                int statusCode = 0;
                int ver = 0;
                byte[] verBytes = icReader.processAPDU(APDU.buildCardAppVerINS());
                if (verBytes.length >= 3) {
                    try {
                        String verStr = bytesToHexString(verBytes);
                        ver = Integer.parseInt(verStr.substring(0, 6));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.err.println("ver:" + ver);
                if (ver >= 30000) {
                    statusCode = startSignFileForAuth(elf, (byte[])userCertMap.get(Integer.valueOf(user)));
                } else {
                    statusCode = startSignFile(elf, (byte[])userCertMap.get(Integer.valueOf(user)));
                }
                if (statusCode == 0)
                {
//                    setProgress(this.seq);
//                    this.seq += 1;
//                    this.status = 0;
                    elf.setSignedTag(SIG[1]);
//                    NEWPOSUtil.saveLog(NewposCardMain.this.customerInfo, elf);
                } else {
//                    this.status = -1;
//                    return null;
                }
            } else {
                APDU apdu = new APDU();
                int pukLength = 0;
                byte[] pukCertData = null;
                byte[] selectFileReq = apdu.selectFile((short)flleTag);
                byte[] receiveData = icReader.processAPDU(selectFileReq);

                if (receiveData.length == 2) {
                    byte[] sw = receiveData;
                    apdu.setSW(sw);
                    
                    if (apdu.statusSW() == 0) {
                        for (;;) {
                            byte[] readCertReq = apdu.readFile((short)0);
                            receiveData = icReader.processAPDU(readCertReq);

//                            if (receiveData.length <= 2) break label854;
                            sw = new byte[2];
                            System.arraycopy(receiveData, 
                                receiveData.length - 2, sw, 0, 2);
                            apdu.setSW(sw);
                            
                            int dataLength = receiveData.length - 2;
//                            if (apdu.statusSW() != 0) break label847;
                            if (dataLength != 255)
                                break;
                            byte[] data = new byte[receiveData.length - 2];
                            System.arraycopy(receiveData, 0, data, 0, 
                                data.length);
                            
                            int len = pukLength;
                            pukLength += data.length;
                            pukCertData = new byte[pukLength];
                            if (tmpData != null)
                                System.arraycopy(tmpData, 0, 
                                    pukCertData, 0, tmpData.length);
                            System.arraycopy(data, 0, pukCertData, len, 
                                data.length);
                            tmpData = pukCertData;
                        }

                        byte[] data = new byte[receiveData.length - 2];
                        System.arraycopy(receiveData, 0, data, 0, 
                            data.length);

                        pukLength += data.length;
                        pukCertData = new byte[pukLength];
                        System.arraycopy(tmpData, 0, pukCertData, 0, tmpData.length);
                        System.arraycopy(data, 0, pukCertData, tmpData.length, data.length);
                        tmpData = null;
                        userCertMap.put(Integer.valueOf(user), pukCertData);

                        int statusCode = 0;
                        int ver = 0;
                        byte[] verBytes = icReader.processAPDU(APDU.buildCardAppVerINS());
                        if (verBytes.length >= 3) {
                            try {
                                String verStr = bytesToHexString(verBytes);
                                ver = Integer.parseInt(verStr.substring(0, 6));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        System.err.println("ver:" + ver);
                        if (ver >= 30000) {
                            statusCode = startSignFileForAuth(elf, pukCertData);
                        } else {
                            statusCode = startSignFile(elf, pukCertData);
                        }
                        if (statusCode == 0) {
//                            setProgress(this.seq);
//                            this.seq += 1;
//                            this.status = 0;
                            elf.setSignedTag(SIG[1]);
//                            NEWPOSUtil.saveLog(NewposCardMain.this.customerInfo, elf);
                        } else {

//                            if (receiveData.length == 2) {
//                                apdu.setSW(receiveData);
//                                if (apdu.statusSW() == 253) {
//                                    this.status = 253;
//                                    return null;
//                                }
//                                if (apdu.statusSW() == 254) {
//                                    this.status = 254;
//                                    return null;
//                                }
//                                this.status = -1;
//                                return null;
//                            }
//                            
//                            this.status = -1;
//                            return null;
                        }
                        
                    } else {
//                        this.status = -2;
//                        return null;
                    }
                } else {
//                    this.status = -1;
//                    return null;
                }
            }
        } else if (SIG[0].equals(sigVersion)) {
            int statusCode = startSignFile(elf, null);
            if (statusCode == 0) {
//                setProgress(this.seq);
//                this.seq += 1;
//                this.status = 0;
                elf.setSignedTag(SIG[0]);
//                NEWPOSUtil.saveLog(NewposCardMain.this.customerInfo, elf);
            } else {
//                this.status = statusCode;
//                return null;
            }
        }
		return flleTag;
    }
    
    public static int startSignFile(FileELF elf, byte[] pukCert) throws Exception {
//        byte[] signedData = null;
//        byte[] signedHash = null;
//        byte[] pukCertData = null;
//        int fileLen = 0;
//        byte[] fileContent = TableUtil.getNeededSignedFile(elf.getFilePath());
//        byte[] signedTail = TableUtil.getSignedTailContent(this.customerInfo, elf);
//        
//        if (SIG[1].equals(sigVersion)) {
//            pukCertData = pukCert;
//            
//            signedHash = TableUtil.calcSignSRCHash(fileContent, pukCertData, signedTail);
//        } else if (SIG[0].equals(sigVersion)) {
//            signedHash = TableUtil.calcSignSRCHashV01(fileContent, signedTail);
//        }
//
//        byte[] signedDate = NEWPOSUtil.getCurrentSecond();
//        NEWPOSUtil.reverseArray(signedDate);
//
//        byte[] machineId = new byte[8];
//        System.arraycopy(this.cardInfo.getSN(), 0, machineId, 0, 4);
//
//        byte[] result = new byte[12 + signedHash.length];
//        System.arraycopy(signedDate, 0, result, 0, 4);
//        System.arraycopy(machineId, 0, result, 4, 8);
//        System.arraycopy(signedHash, 0, result, 12, signedHash.length);
//        
//        APDU apdu = new APDU();
//        byte[] reqData = apdu.buildSignAPDU(elf, result);
//        byte[] receiveData = this.posICReader.processAPDU(reqData);
//
//        if (receiveData.length > 2) {
//            byte[] sw = new byte[2];
//            System.arraycopy(receiveData, receiveData.length - 2, sw, 0, 2);
//            apdu.setSW(sw);
//            
//            int dataLength = receiveData.length - 2;
//            
//            if (apdu.statusSW() == 0) {
//                int i = 1;
//                
//                while (dataLength == 255) {
//                    byte[] data = new byte[receiveData.length - 2];
//                    System.arraycopy(receiveData, 0, data, 0, data.length);
//
//                    byte[] reqMore = apdu.buildGetMoreSignAPDU(i);
//                    receiveData = this.posICReader.processAPDU(reqMore);
//                    dataLength = receiveData.length - 2;
//                    
//                    int len = fileLen;
//                    fileLen += data.length;
//                    signedData = new byte[fileLen];
//                    if (this.tmpData != null)
//                        System.arraycopy(this.tmpData, 0, signedData, 0, this.tmpData.length);
//                    System.arraycopy(data, 0, signedData, len, data.length);
//                    this.tmpData = signedData;
//                    i++;
//                }
//                
//                byte[] data = new byte[receiveData.length - 2];
//                System.arraycopy(receiveData, 0, data, 0, data.length);
//                
//                fileLen += data.length;
//                signedData = new byte[fileLen];
//                System.arraycopy(this.tmpData, 0, signedData, 0, this.tmpData.length);
//                System.arraycopy(data, 0, signedData, this.tmpData.length, data.length);
//                this.tmpData = null;
//                
//                byte[] totalContent = null;
//                if (SIG[1].equals(sigVersion)) {
//                    totalContent = new byte[fileContent.length + pukCertData.length + 
//                        signedData.length + signedTail.length];
//                    
//                    System.arraycopy(fileContent, 0, totalContent, 0, fileContent.length);
//                    System.arraycopy(pukCertData, 0, totalContent, fileContent.length, 
//                        pukCertData.length);
//                    System.arraycopy(signedData, 0, totalContent, fileContent.length + 
//                        pukCertData.length, signedData.length);
//                    System.arraycopy(signedTail, 0, totalContent, fileContent.length + 
//                        pukCertData.length + signedData.length, signedTail.length);
//                } else if (SIG[0].equals(sigVersion)) {
//                    totalContent = new byte[fileContent.length + signedData.length + 
//                        signedTail.length];
//                    
//                    System.arraycopy(fileContent, 0, totalContent, 0, fileContent.length);
//                    System.arraycopy(signedData, 0, totalContent, fileContent.length, 
//                        signedData.length);
//                    System.arraycopy(signedTail, 0, totalContent, fileContent.length + 
//                        signedData.length, signedTail.length);
//                }
//                String filePath = this.outputSignedDirField.getText() + "/" + elf.getFileName();
//                
//                NEWPOSUtil.writeOutBinaryFile(filePath, totalContent);
//                return 0;
//            return -1;
//        }
//        
//        if (receiveData.length == 2) {
//            apdu.setSW(receiveData);
//            if (apdu.statusSW() == 253) {
//                return 253;
//            }
//            return -1;
//        }
//        
        return -1;
    }

    public static int startSignFileForAuth(FileELF elf, byte[] pukCert) throws Exception {
//        byte[] signedData = null;
//        byte[] signedHash = null;
//        byte[] pukCertData = null;
//        int fileLen = 0;
//
//        byte[] fileContent = TableUtil.getNeededSignedFile(elf.getFilePath());
//        byte[] signedTail = TableUtil.getSignedTailContent(this.customerInfo, elf);
//        
//        if (SIG[1].equals(sigVersion)) {
//            pukCertData = pukCert;
//            signedHash = TableUtil.calcSignSRCHash(fileContent, pukCertData, signedTail);
//        } else if (SIG[0].equals(sigVersion)) {
//            signedHash = TableUtil.calcSignSRCHashV01(fileContent, signedTail);
//        }
//
//        byte[] signedDate = NEWPOSUtil.getCurrentSecond();
//        NEWPOSUtil.reverseArray(signedDate);
//
//        byte[] machineId = new byte[8];
//        System.arraycopy(this.cardInfo.getSN(), 0, machineId, 0, 4);
//
//        int randomSize = 253 - (12 + signedHash.length + 32);
//        
//        byte[] fillRandom = new byte[randomSize];
//        this.random.nextBytes(fillRandom);
//        
//        int length = 12 + signedHash.length + randomSize + 32;
//        
//        byte[] toHashSrc = new byte[13 + signedHash.length + randomSize];
//        toHashSrc[0] = 1;
//
//        byte[] result = new byte[length];
//        int idx = 0;
//        System.arraycopy(signedDate, 0, result, idx, 4);
//        idx += 4;
//        System.arraycopy(machineId, 0, result, idx, 8);
//        idx += 8;
//        System.arraycopy(signedHash, 0, result, idx, signedHash.length);
//        idx += signedHash.length;
//        System.arraycopy(fillRandom, 0, result, idx, randomSize);
//        idx += randomSize;
//        
//        System.arraycopy(result, 0, toHashSrc, 1, idx);
//        
//        byte[] fillRandomHash = NEWPOSUtil.calcSha256Sum(toHashSrc);
//        
//        System.arraycopy(fillRandomHash, 0, result, idx, fillRandomHash.length);
//        idx += fillRandomHash.length;
//        
//        APDU apdu = new APDU();
//        byte[] reqData = apdu.buildSignAPDU(elf, result);
//        byte[] receiveData = this.posICReader.processAPDU(reqData);
//
//        if (receiveData.length > 2) {
//            byte[] sw = new byte[2];
//            System.arraycopy(receiveData, receiveData.length - 2, sw, 0, 2);
//            apdu.setSW(sw);
//            
//            int dataLength = receiveData.length - 2;
//            
//            if (apdu.statusSW() == 0) {
//                int i = 1;
//                
//                while (dataLength == 255) {
//                    byte[] data = new byte[receiveData.length - 2];
//                    System.arraycopy(receiveData, 0, data, 0, data.length);
//
//                    byte[] reqMore = apdu.buildGetMoreSignAPDU(i);
//                    receiveData = this.posICReader.processAPDU(reqMore);
//                    dataLength = receiveData.length - 2;
//                    
//                    int len = fileLen;
//                    fileLen += data.length;
//                    signedData = new byte[fileLen];
//                    if (this.tmpData != null)
//                        System.arraycopy(this.tmpData, 0, signedData, 0, this.tmpData.length);
//                    System.arraycopy(data, 0, signedData, len, data.length);
//                    this.tmpData = signedData;
//                    i++;
//                }
//                
//                byte[] data = new byte[receiveData.length - 2];
//                System.arraycopy(receiveData, 0, data, 0, data.length);
//                
//                fileLen += data.length;
//                signedData = new byte[fileLen];
//                System.arraycopy(this.tmpData, 0, signedData, 0, this.tmpData.length);
//                System.arraycopy(data, 0, signedData, this.tmpData.length, data.length);
//                this.tmpData = null;
//                
//                byte[] totalContent = null;
//                if (SIG[1].equals(sigVersion)) {
//                    totalContent = new byte[fileContent.length + pukCertData.length + 
//                        signedData.length + signedTail.length];
//                    
//                    System.arraycopy(fileContent, 0, totalContent, 0, fileContent.length);
//                    System.arraycopy(pukCertData, 0, totalContent, fileContent.length, 
//                        pukCertData.length);
//                    System.arraycopy(signedData, 0, totalContent, fileContent.length + 
//                        pukCertData.length, signedData.length);
//                    System.arraycopy(signedTail, 0, totalContent, fileContent.length + 
//                        pukCertData.length + signedData.length, signedTail.length);
//                } else if (SIG[0].equals(sigVersion)) {
//                    totalContent = new byte[fileContent.length + signedData.length + 
//                        signedTail.length];
//                    
//                    System.arraycopy(fileContent, 0, totalContent, 0, fileContent.length);
//                    System.arraycopy(signedData, 0, totalContent, fileContent.length, 
//                        signedData.length);
//                    System.arraycopy(signedTail, 0, totalContent, fileContent.length + 
//                        signedData.length, signedTail.length);
//                }
//                
//                String filePath = this.outputSignedDirField.getText() + "/" + elf.getFileName();
//                
//                NEWPOSUtil.writeOutBinaryFile(filePath, totalContent);
//
//                return 0;
//            }
//
//            return -1;
//        }
//        
//        if (receiveData.length == 2) {
//            apdu.setSW(receiveData);
//            if (apdu.statusSW() == 253) {
//                return 253;
//            }
//            return -1;
//        }
//        
        return -1;
    }

    public static void main(String[] args) {
        PosICReader icReader = new PosICReader();
        icReader.open("/dev/tty.usbserial-AH01SKWE");

        APDU apdu = new APDU();
        byte[] selectAPPResp = icReader.processAPDU(apdu.selectApplication("NEWPOS-CARD"));
        apdu.setSW(selectAPPResp);
        int status = apdu.statusSW();

        if (status == 0) {
            CardInfo cardInfo = getCardInfo(icReader);
            CustomerInfo customerInfo = getCustomerInfo(icReader);

        }
    }

    public static String bytesToHexString(byte[] src)
    {
        StringBuilder stringBuilder = new StringBuilder("");
        if ((src == null) || (src.length <= 0)) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
    
    public static byte[] getNeededSignedFile(String filePath)
    {
		return tmpData;
    }

    public static byte[] getSignedTailContent(CustomerInfo customerInfo, FileELF elf) throws Exception {
        byte[] tail = new byte[255];
        byte[] type = new byte[4];
        byte[] cid = customerInfo.getCid();
        byte[] approvier = customerInfo.getName();
        byte userID = Byte.parseByte(elf.getUser().replace("User", ""));

        if ("APP".equals(elf.getFileType())) {
            type = new byte[] { 6, userID, cid[1], cid[0] };
        } else if ("LIB".equals(elf.getFileType())) {
            type = new byte[] { 4, userID, cid[1], cid[0] };
        }

        byte[] effectiveTime = getSpecDateSecond(elf.getEffectiveDate().replace("/", "").replace(" ", ""));
        byte[] expirationTime = getSpecDateSecond(elf.getExpireDate().replace("/", "").replace(" ", ""));
        byte[] appName = new byte[64];
        byte[] currentName = elf.getFileName().getBytes();
        if (currentName.length <= 64) {
            System.arraycopy(currentName, 0, appName, 0, currentName.length);
        } else {
            throw new Exception("APP name should not more than 64 characters");
        }

        byte[] appVersion = new byte[4];
        String[] version = elf.getVersion().replace(".", "-").split("-");
        appVersion = new byte[] { 0, Byte.parseByte(version[0]), Byte.parseByte(version[1]), 
            Byte.parseByte(version[2]) };

        byte[] reserve = new byte[44];
        byte[] note = elf.getNote().getBytes("GBK");
        if (note.length <= 44) {
            System.arraycopy(note, 0, reserve, 0, note.length);
        } else {
            throw new Exception("Note should not more than 44 characters");
        }

        byte[] signatureLength = { 1 };
        byte[] signedTailLength = { 0, -56 };
        byte[] signedFlag = null;
        if (SIG[1].equals(sigVersion)) {
            signedFlag = "SIG:0002".getBytes();
        } else if (SIG[0].equals(sigVersion)) {
            signedFlag = "SIG:0001".getBytes();
        }

        reverseArray(effectiveTime);
        reverseArray(expirationTime);
        reverseArray(signatureLength);
        reverseArray(signedTailLength);

        System.arraycopy(type, 0, tail, 0, 4);
        System.arraycopy(effectiveTime, 0, tail, 4, 4);
        System.arraycopy(expirationTime, 0, tail, 8, 4);
        System.arraycopy(appName, 0, tail, 12, 64);
        System.arraycopy(approvier, 0, tail, 76, 64);
        System.arraycopy(appVersion, 0, tail, 140, 4);
        System.arraycopy(reserve, 0, tail, 144, 44);
        System.arraycopy(signatureLength, 0, tail, 188, 2);
        System.arraycopy(signedTailLength, 0, tail, 190, 2);
        System.arraycopy(signedFlag, 0, tail, 192, 8);
        
        return tail;
    }

    public static byte[] getSpecDateSecond(String yymmdd) throws java.text.ParseException
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = null;

        int hh = 0;
        int mm = 0;
        int ss = 0;

        StringBuffer sb = new StringBuffer();
        sb.append(yymmdd);
        sb.append(hh < 10 ? "0" + String.valueOf(hh) : String.valueOf(hh));
        sb.append(mm < 10 ? "0" + String.valueOf(mm) : String.valueOf(mm));
        sb.append(ss < 10 ? "0" + String.valueOf(ss) : String.valueOf(ss));
        try {
            date = df.parse(sb.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        long second = date.getTime() / 1000L;
        if (second > 2147356800L) {
            second = 2147356800L;
        }

        return hexStringToBytes(Long.toHexString(second));
    }
    
    public static void reverseArray(byte[] byteArray)
    {
        int i = 0;int n = byteArray.length - 1;
        while (n > 2 * i) {
            byte x = byteArray[i];
            byteArray[i] = byteArray[(n - i)];
            byteArray[(n - i)] = x;
            i++;
        }
    }

    public static byte[] hexStringToBytes(String hexString)
    {
        if ((hexString == null) || (hexString.equals(""))) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = ((byte)(charToByte(hexChars[pos]) << 4 | charToByte(hexChars[(pos + 1)])));
        }
        return d;
    }

    private static byte charToByte(char c) {
        byte b = (byte)"0123456789ABCDEF".indexOf(c);
        return b;
    }

    public static byte[] calcSignSRCHash(byte[] fileContent, byte[] pukCertData, byte[] signedTail) throws Exception {
        byte[] hash = null;
        byte[] signSRC = new byte[fileContent.length + pukCertData.length + signedTail.length];
        
        System.arraycopy(fileContent, 0, signSRC, 0, fileContent.length);
        System.arraycopy(pukCertData, 0, signSRC, fileContent.length, pukCertData.length);
        System.arraycopy(signedTail, 0, signSRC, fileContent.length + pukCertData.length, signedTail.length);
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(signSRC);
        hash = digest.digest();
        return hash;
    }

    public static byte[] calcSignSRCHashV01(byte[] fileContent, byte[] signedTail) throws Exception {
        byte[] hash = null;
        byte[] signSRC = new byte[fileContent.length + signedTail.length];
        
        System.arraycopy(fileContent, 0, signSRC, 0, fileContent.length);
        System.arraycopy(signedTail, 0, signSRC, fileContent.length, signedTail.length);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(signSRC);
        hash = digest.digest();
        return hash;
    }
}
