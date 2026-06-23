package org.feiesos.search.service.impl;

import org.feiesos.search.dto.NodeInfo;
import org.feiesos.search.dto.SearchResultDTO;
import org.feiesos.search.mapper.FileNodeMapper;
import org.feiesos.search.service.AuthzService;
import org.feiesos.search.service.SearchService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final FileNodeMapper fileNodeMapper;
    private final AuthzService authzService;

    public SearchServiceImpl(FileNodeMapper fileNodeMapper, AuthzService authzService) {
        this.fileNodeMapper = fileNodeMapper;
        this.authzService = authzService;
    }

    @Override
    public List<SearchResultDTO> search(Long userId, String keyword, String type,
                                        String sortField, String sortOrder,
                                        int page, int pageSize) {
        authzService.checkPermission(userId, "file:read");

        String safeKeyword = keyword != null ? keyword.trim() : "";
        if (safeKeyword.isEmpty()) {
            return List.of();
        }

        Boolean isDir = null;
        if ("folder".equals(type)) {
            isDir = true;
        } else if ("file".equals(type)) {
            isDir = false;
        }

        String safeSort = "date";
        if ("name".equals(sortField) || "size".equals(sortField)) {
            safeSort = sortField;
        }

        String safeOrder = "desc";
        if ("asc".equalsIgnoreCase(sortOrder)) {
            safeOrder = "asc";
        }

        int limit = Math.min(Math.max(pageSize, 1), 100);
        int offset = Math.max(page - 1, 0) * limit;

        List<SearchResultDTO> results = fileNodeMapper.search(
                userId, safeKeyword, isDir, safeSort, safeOrder, limit, offset);

        if (!results.isEmpty()) {
            enrichPaths(results);
        }

        return results;
    }

    private void enrichPaths(List<SearchResultDTO> results) {
        Set<Long> parentIds = results.stream()
                .map(SearchResultDTO::getParentId)
                .filter(Objects::nonNull)
                .filter(id -> id != 0)
                .collect(Collectors.toSet());

        Map<Long, NodeInfo> nodeMap = new HashMap<>();

        while (!parentIds.isEmpty()) {
            List<NodeInfo> nodes = fileNodeMapper.selectNodesByIds(new ArrayList<>(parentIds));
            parentIds.clear();
            for (NodeInfo node : nodes) {
                nodeMap.put(node.getId(), node);
                if (node.getParentId() != null && node.getParentId() != 0
                        && !nodeMap.containsKey(node.getParentId())) {
                    parentIds.add(node.getParentId());
                }
            }
        }

        for (SearchResultDTO r : results) {
            r.setPath(buildPath(r.getParentId(), nodeMap));
        }
    }

    private static String buildPath(Long parentId, Map<Long, NodeInfo> nodeMap) {
        if (parentId == null || parentId == 0) {
            return "/";
        }
        Deque<String> segments = new ArrayDeque<>();
        Long current = parentId;
        while (current != null && current != 0) {
            NodeInfo node = nodeMap.get(current);
            if (node == null) break;
            segments.addFirst(node.getName());
            current = node.getParentId();
        }
        return "/" + String.join("/", segments);
    }

    @Override
    public int count(Long userId, String keyword, String type) {
        String safeKeyword = keyword != null ? keyword.trim() : "";
        if (safeKeyword.isEmpty()) {
            return 0;
        }

        Boolean isDir = null;
        if ("folder".equals(type)) {
            isDir = true;
        } else if ("file".equals(type)) {
            isDir = false;
        }

        return fileNodeMapper.countSearch(userId, safeKeyword, isDir);
    }
}
