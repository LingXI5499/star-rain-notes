package com.starrainnotes.account.vo;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVO {

    private String id;
    private String code;
    private String resource;
    private String action;
    private String name;
    private String status;
}
