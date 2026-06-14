package com.github.lyd11250.bedrock.biz.party.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 相关方账户入参。归属相关方由路径 {@code partyId} 指定，不在 DTO 内。
 */
@Data
public class AccountDTO {

    @NotBlank(message = "户名不能为空")
    private String accountName;

    @NotBlank(message = "银行账号不能为空")
    private String accountNo;

    /** 开户银行（前端按历史值补全）。 */
    private String bankName;

    /** 开户支行/网点。 */
    private String bankBranch;

    /** 状态：1 启用，0 停用（缺省启用）。 */
    private Integer status;

    private String remark;
}
