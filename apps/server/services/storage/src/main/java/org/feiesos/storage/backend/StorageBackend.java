package org.feiesos.storage.backend;

import org.feiesos.storage.dto.StorageObject;
import java.io.InputStream;
import java.util.List;

public interface StorageBackend {

    StorageObject read(String storagePath);

    void write(String storagePath, InputStream data, long size);

    void delete(String storagePath);

    void deleteDirectory(String storagePath);

    List<String> list(String storagePath);

    boolean exists(String storagePath);

    StorageType type();
}
