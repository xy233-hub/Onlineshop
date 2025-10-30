package com.example.onlineshop.dto.response;



public class TempMediaResponse {
    private String tempKey;
    private String mediaUrl;
    private long fileSize;
    private String mimeType;

    public TempMediaResponse() {}

    public TempMediaResponse(String tempKey, String mediaUrl, long fileSize, String mimeType) {
        this.tempKey = tempKey;
        this.mediaUrl = mediaUrl;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public String getTempKey() { return tempKey; }
    public void setTempKey(String tempKey) { this.tempKey = tempKey; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}
