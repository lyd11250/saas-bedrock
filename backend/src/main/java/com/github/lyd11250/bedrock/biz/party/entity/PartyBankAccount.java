package com.github.lyd11250.bedrock.biz.party.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 相关方账户（party 从属，挂 {@code party_id}；企业开户信息 / 人员账户信息合一表）。
 *
 * <p>同一相关方可有多个账户（1:N）；账户归属的相关方是组织还是人由父表 {@link Party} 的
 * {@code party_type} 区分，本表不重复存类型。账号在租户内同一相关方下唯一。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("party_bank_account")
public class PartyBankAccount extends BaseEntity {

    /** 所属相关方 id（= party.id）。 */
    private Long partyId;

    /** 户名（开户名称）。 */
    private String accountName;

    /** 银行账号。 */
    private String accountNo;

    /** 开户银行。 */
    private String bankName;

    /** 开户支行/网点。 */
    private String bankBranch;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    private String remark;
}
