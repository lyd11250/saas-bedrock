package com.github.lyd11250.bedrock.biz.party.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.github.lyd11250.bedrock.biz.party.dto.AccountDTO;
import com.github.lyd11250.bedrock.biz.party.service.AccountService;
import com.github.lyd11250.bedrock.biz.party.vo.AccountVO;
import com.github.lyd11250.bedrock.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 相关方账户接口（本租户）。账户以相关方为上下文的子资源，路径用 {@code partyId} 定位；
 * 人员页与单位页复用同一组接口，相关方是组织还是人由父表决定。
 */
@Tag(name = "相关方账户")
@RestController
@RequestMapping("/api/v1/party")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{partyId}/accounts")
    @SaCheckPermission("party:account:list")
    public Result<List<AccountVO>> list(@PathVariable Long partyId) {
        return Result.ok(accountService.listByParty(partyId));
    }

    /** 已入库的开户银行（去重），供前端输入补全。 */
    @GetMapping("/accounts/bank-names")
    @SaCheckPermission("party:account:list")
    public Result<List<String>> bankNames() {
        return Result.ok(accountService.distinctBankNames());
    }

    @PostMapping("/{partyId}/accounts")
    @SaCheckPermission("party:account:create")
    public Result<Long> create(@PathVariable Long partyId, @Valid @RequestBody AccountDTO dto) {
        return Result.ok(accountService.create(partyId, dto));
    }

    @PutMapping("/accounts/{id}")
    @SaCheckPermission("party:account:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AccountDTO dto) {
        accountService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/accounts/{id}")
    @SaCheckPermission("party:account:delete")
    public Result<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return Result.ok();
    }
}
