package org.feiesos.search.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.feiesos.search.dto.SearchResultDTO;
import org.feiesos.search.dto.NodeInfo;

import java.util.List;

@Mapper
public interface FileNodeMapper {

    List<SearchResultDTO> search(@Param("userId") Long userId,
                                 @Param("keyword") String keyword,
                                 @Param("isDir") Boolean isDir,
                                 @Param("sortField") String sortField,
                                 @Param("sortOrder") String sortOrder,
                                 @Param("limit") int limit,
                                 @Param("offset") int offset);

    int countSearch(@Param("userId") Long userId,
                    @Param("keyword") String keyword,
                    @Param("isDir") Boolean isDir);

    List<NodeInfo> selectNodesByIds(@Param("ids") List<Long> ids);
}
