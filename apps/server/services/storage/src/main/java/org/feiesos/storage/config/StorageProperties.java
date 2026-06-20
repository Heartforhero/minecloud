package org.feiesos.storage.config;

import org.feiesos.storage.backend.StorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minecloud.storage")
public class StorageProperties {

    private String uploadPath = "D:/minecloud/data/";
    private StorageType defaultType = StorageType.LOCAL;
    private Chunk chunk = new Chunk();

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public StorageType getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(StorageType defaultType) {
        this.defaultType = defaultType;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public static class Chunk {
        private String tempDir = "temp";

        public String getTempDir() {
            return tempDir;
        }

        public void setTempDir(String tempDir) {
            this.tempDir = tempDir;
        }
    }
}
