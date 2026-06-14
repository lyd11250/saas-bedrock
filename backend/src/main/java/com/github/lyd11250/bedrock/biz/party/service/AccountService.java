package com.github.lyd11250.bedrock.biz.party.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.biz.party.dto.AccountDTO;
import com.github.lyd11250.bedrock.biz.party.entity.PartyBankAccount;
import com.github.lyd11250.bedrock.biz.party.mapper.PartyBankAccountMapper;
import com.github.lyd11250.bedrock.biz.party.mapper.PartyMapper;
import com.github.lyd11250.bedrock.biz.party.vo.AccountVO;
import com.github.lyd11250.bedrock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 相关方账户管理（party 从属，挂 party_id；租户隔离由多租户插件自动完成）。
 *
 * <p>账户归属相关方是组织还是人由父表 party 决定，本服务不区分；同一相关方账号唯一。
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final PartyMapper partyMapper;
    private final PartyBankAccountMapper accountMapper;

    /** 某相关方的账户列表（按创建时间倒序）。 */
    public List<AccountVO> listByParty(Long partyId) {
        return accountMapper.selectList(Wrappers.<PartyBankAccount>lambdaQuery()
                        .eq(PartyBankAccount::getPartyId, partyId)
                        .orderByDesc(PartyBankAccount::getCreatedAt))
                .stream().map(this::toVO).toList();
    }

    /** 本租户已入库的开户银行（去重、非空），供前端输入补全。 */
    public List<String> distinctBankNames() {
        return accountMapper.selectList(Wrappers.<PartyBankAccount>lambdaQuery()
                        .select(PartyBankAccount::getBankName)
                        .isNotNull(PartyBankAccount::getBankName)
                        .ne(PartyBankAccount::getBankName, "")
                        .groupBy(PartyBankAccount::getBankName))
                .stream().map(PartyBankAccount::getBankName).toList();
    }

    @Transactional
    public Long create(Long partyId, AccountDTO dto) {
        requireParty(partyId);
        assertAccountNoUnique(partyId, dto.getAccountNo(), null);
        PartyBankAccount account = new PartyBankAccount();
        account.setPartyId(partyId);
        copyDetail(dto, account);
        account.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        accountMapper.insert(account);
        return account.getId();
    }

    @Transactional
    public void update(Long id, AccountDTO dto) {
        PartyBankAccount account = requireAccount(id);
        assertAccountNoUnique(account.getPartyId(), dto.getAccountNo(), id);
        copyDetail(dto, account);
        if (dto.getStatus() != null) {
            account.setStatus(dto.getStatus());
        }
        accountMapper.updateById(account);
    }

    @Transactional
    public void delete(Long id) {
        requireAccount(id);
        accountMapper.deleteById(id);
    }

    /** 级联：软删某相关方全部账户（相关方删除时由 party 主表 service 编排调用）。 */
    @Transactional
    public void deleteByParty(Long partyId) {
        accountMapper.delete(Wrappers.<PartyBankAccount>lambdaQuery()
                .eq(PartyBankAccount::getPartyId, partyId));
    }

    // ---- 内部 ----

    private void requireParty(Long partyId) {
        if (partyId == null || partyMapper.selectById(partyId) == null) {
            throw new BusinessException("相关方不存在");
        }
    }

    private PartyBankAccount requireAccount(Long id) {
        PartyBankAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        return account;
    }

    private void assertAccountNoUnique(Long partyId, String accountNo, Long excludeId) {
        if (!StringUtils.hasText(accountNo)) {
            return;
        }
        Long count = accountMapper.selectCount(Wrappers.<PartyBankAccount>lambdaQuery()
                .eq(PartyBankAccount::getPartyId, partyId)
                .eq(PartyBankAccount::getAccountNo, accountNo)
                .ne(excludeId != null, PartyBankAccount::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("该相关方下银行账号已存在");
        }
    }

    private void copyDetail(AccountDTO dto, PartyBankAccount account) {
        account.setAccountName(dto.getAccountName());
        account.setAccountNo(dto.getAccountNo());
        account.setBankName(dto.getBankName());
        account.setBankBranch(dto.getBankBranch());
        account.setRemark(dto.getRemark());
    }

    private AccountVO toVO(PartyBankAccount account) {
        AccountVO vo = new AccountVO();
        vo.setId(account.getId());
        vo.setPartyId(account.getPartyId());
        vo.setAccountName(account.getAccountName());
        vo.setAccountNo(account.getAccountNo());
        vo.setBankName(account.getBankName());
        vo.setBankBranch(account.getBankBranch());
        vo.setStatus(account.getStatus());
        vo.setRemark(account.getRemark());
        vo.setCreatedAt(account.getCreatedAt());
        return vo;
    }
}
