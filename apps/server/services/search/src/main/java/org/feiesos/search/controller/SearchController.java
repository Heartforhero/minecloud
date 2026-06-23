package org.feiesos.search.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.common.result.R;
import org.feiesos.search.dto.SearchResultDTO;
import org.feiesos.search.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public R<Map<String, Object>> search(@RequestParam("q") String keyword,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(defaultValue = "date") String sort,
                                          @RequestParam(defaultValue = "desc") String order,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "50") int pageSize,
                                          HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            List<SearchResultDTO> items = searchService.search(
                    userId, keyword, type, sort, order, page, pageSize);
            int total = searchService.count(userId, keyword, type);
            return R.ok(Map.of(
                    "items", items,
                    "total", total,
                    "page", page,
                    "pageSize", pageSize
            ));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return R.fail("搜索失败: " + e.getMessage());
        }
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new BusinessException(401, "未认证");
        }
        return (Long) userId;
    }
}
