package org.feiesos.search.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchResultDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    Long id;
    String name;
    @JsonSerialize(using = ToStringSerializer.class)
    Long parentId;
    Boolean isDir;
    Long size;
    LocalDateTime createTime;
    String path;
}
