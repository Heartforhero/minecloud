package org.feiesos.storage.dto;

import lombok.Builder;
import lombok.Data;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

@Data
@Builder
public class StorageObject implements Closeable {
    private String filename;
    private long size;
    private String mimeType;
    private InputStream inputStream;

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
