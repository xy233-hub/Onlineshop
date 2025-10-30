// java
package com.example.onlineshop.dto.response;

public class TempMediaResponse {
    private String tempKey;
    private String mediaUrl;
    private long fileSize;
    private String mimeType;
    private String purpose;

    public TempMediaResponse() {}

    public TempMediaResponse(String tempKey, String mediaUrl, long fileSize, String mimeType, String purpose) {
        this.tempKey = tempKey;
        this.mediaUrl = mediaUrl;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.purpose = purpose;
    }

    public String getTempKey() { return tempKey; }
    public void setTempKey(String tempKey) { this.tempKey = tempKey; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
