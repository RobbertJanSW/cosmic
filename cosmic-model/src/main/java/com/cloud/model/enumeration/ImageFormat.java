package com.cloud.model.enumeration;

public enum ImageFormat {
    QCOW2("qcow2"),
    RAW("raw"),
    VHD("vhd"),
    ISO("iso"),
    VDI("vdi");

    private final String fileExtension;

    ImageFormat(final String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        if (fileExtension == null) {
            return toString().toLowerCase();
        }

        return fileExtension;
    }
}
