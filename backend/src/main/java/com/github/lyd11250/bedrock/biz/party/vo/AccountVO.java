package com.github.lyd11250.bedrock.biz.party.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相关方账户出参。
 */
@Data
public class AccountVO {

    private Long id;

    private Long partyId;

    private String accountName;

    private String accountNo;

    private String bankName;

    private String bankBranch;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;
}
