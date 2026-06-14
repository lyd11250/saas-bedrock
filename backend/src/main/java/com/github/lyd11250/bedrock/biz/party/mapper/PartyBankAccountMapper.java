package com.github.lyd11250.bedrock.biz.party.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.lyd11250.bedrock.biz.party.entity.PartyBankAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 相关方账户 Mapper（CRUD 由 MyBatis-Plus 提供，多租户隔离自动追加）。
 */
@Mapper
public interface PartyBankAccountMapper extends BaseMapper<PartyBankAccount> {
}
