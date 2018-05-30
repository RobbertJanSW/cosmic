package com.cloud.model.enumeration;

public enum DiskCacheMode {
    DEFAULT("default"),
    NONE("none"),
    WRITETHROUGH("writethrough"),
    WRITEBACK("writeback"),
    DIRECTSYNC("directsync"),
    UNSAFE("unsafe");

    private final String _diskCacheMode;

    DiskCacheMode(final String cacheMode) {
        _diskCacheMode = cacheMode;
    }

    @Override
    public String toString() {
        return _diskCacheMode;
    }
}
