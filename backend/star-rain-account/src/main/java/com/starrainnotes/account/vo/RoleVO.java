package com.starrainnotes.account.vo;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {

    private String id;
    private String code;
    private String name;
    private String status;
    private Set<String> permissionIds;
}
