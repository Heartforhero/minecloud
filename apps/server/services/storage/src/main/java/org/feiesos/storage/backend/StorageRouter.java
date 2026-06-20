package org.feiesos.storage.backend;

import org.feiesos.storage.config.StorageProperties;
import org.feiesos.storage.entity.FileNode;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StorageRouter {

    private final Map<StorageType, StorageBackend> backends;
    private final StorageType defaultType;

    public StorageRouter(List<StorageBackend> backendList, StorageProperties properties) {
        this.backends = backendList.stream()
                .collect(Collectors.toMap(StorageBackend::type, Function.identity(), (a, b) -> a));
        this.defaultType = properties.getDefaultType();
        if (!backends.containsKey(defaultType)) {
            throw new IllegalStateException("默认存储后端未注册: " + defaultType);
        }
    }

    public StorageBackend route(FileNode node) {
        if (node != null && node.getStorageType() != null) {
            try {
                StorageType type = StorageType.valueOf(node.getStorageType());
                StorageBackend backend = backends.get(type);
                if (backend != null) {
                    return backend;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return backends.get(defaultType);
    }

    public StorageBackend defaultBackend() {
        return backends.get(defaultType);
    }
}
