package org.feiesos.search.service;

import org.feiesos.search.dto.SearchResultDTO;

import java.util.List;

public interface SearchService {

    List<SearchResultDTO> search(Long userId, String keyword, String type,
                                 String sortField, String sortOrder,
                                 int page, int pageSize);

    int count(Long userId, String keyword, String type);
}
