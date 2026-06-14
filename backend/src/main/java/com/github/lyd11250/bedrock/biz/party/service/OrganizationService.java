package com.github.lyd11250.bedrock.biz.party.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.lyd11250.bedrock.biz.party.dto.OrganizationDTO;
import com.github.lyd11250.bedrock.biz.party.entity.Party;
import com.github.lyd11250.bedrock.biz.party.entity.PartyOrganization;
import com.github.lyd11250.bedrock.biz.party.mapper.PartyMapper;
import com.github.lyd11250.bedrock.biz.party.mapper.PartyOrganizationMapper;
import com.github.lyd11250.bedrock.biz.party.vo.OrganizationVO;
import com.github.lyd11250.bedrock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组织/单位管理（party + party_organization 共享主键；租户隔离由多租户插件自动完成）。
 *
 * <p>涵盖企业、政府机构、事业单位、社会组织等，由 {@code org_type} 区分。
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    /** 相关方类型：组织。 */
    public static final String TYPE = "ORGANIZATION";

    private final PartyMapper partyMapper;
    private final PartyOrganizationMapper organizationMapper;
    private final AccountService accountService;
    private final QualificationService qualificationService;

    public IPage<OrganizationVO> page(long current, long size, String keyword) {
        Page<Party> page = partyMapper.selectPage(Page.of(current, size),
                Wrappers.<Party>lambdaQuery()
                        .eq(Party::getPartyType, TYPE)
                        .like(StringUtils.hasText(keyword), Party::getName, keyword)
                        .orderByDesc(Party::getCreatedAt));
        Map<Long, PartyOrganization> details = loadDetails(page.getRecords().stream().map(Party::getId).toList());
        return page.convert(party -> toVO(party, details.get(party.getId())));
    }

    /** 本租户已入库的组织类型（去重、非空），供前端输入补全。 */
    public List<String> distinctTypes() {
        return organizationMapper.selectList(Wrappers.<PartyOrganization>lambdaQuery()
                        .select(PartyOrganization::getOrgType)
                        .isNotNull(PartyOrganization::getOrgType)
                        .ne(PartyOrganization::getOrgType, "")
                        .groupBy(PartyOrganization::getOrgType))
                .stream().map(PartyOrganization::getOrgType).toList();
    }

    @Transactional
    public Long create(OrganizationDTO dto) {
        assertTaxNoUnique(dto.getTaxNo(), null);
        Party party = new Party();
        party.setPartyType(TYPE);
        party.setName(dto.getName());
        party.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        party.setRemark(dto.getRemark());
        partyMapper.insert(party);

        PartyOrganization org = new PartyOrganization();
        org.setId(party.getId());
        copyDetail(dto, org);
        organizationMapper.insert(org);
        return party.getId();
    }

    @Transactional
    public void update(Long id, OrganizationDTO dto) {
        Party party = requireParty(id);
        assertTaxNoUnique(dto.getTaxNo(), id);
        party.setName(dto.getName());
        if (dto.getStatus() != null) {
            party.setStatus(dto.getStatus());
        }
        party.setRemark(dto.getRemark());
        partyMapper.updateById(party);

        PartyOrganization org = organizationMapper.selectById(id);
        if (org == null) {
            org = new PartyOrganization();
            org.setId(id);
            copyDetail(dto, org);
            organizationMapper.insert(org);
        } else {
            copyDetail(dto, org);
            organizationMapper.updateById(org);
        }
    }

    @Transactional
    public void delete(Long id) {
        requireParty(id);
        accountService.deleteByParty(id);
        qualificationService.deleteByParty(id);
        organizationMapper.deleteById(id);
        partyMapper.deleteById(id);
    }

    // ---- 内部 ----

    private Party requireParty(Long id) {
        Party party = partyMapper.selectById(id);
        if (party == null || !TYPE.equals(party.getPartyType())) {
            throw new BusinessException("单位不存在");
        }
        return party;
    }

    private void assertTaxNoUnique(String taxNo, Long excludeId) {
        if (!StringUtils.hasText(taxNo)) {
            return;
        }
        Long count = organizationMapper.selectCount(Wrappers.<PartyOrganization>lambdaQuery()
                .eq(PartyOrganization::getTaxNo, taxNo)
                .ne(excludeId != null, PartyOrganization::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("统一社会信用代码已存在");
        }
    }

    private void copyDetail(OrganizationDTO dto, PartyOrganization org) {
        org.setOrgType(dto.getOrgType());
        org.setTaxNo(dto.getTaxNo());
        org.setRegisteredCapital(dto.getRegisteredCapital());
        org.setEstablishedDate(dto.getEstablishedDate());
        org.setLegalPerson(dto.getLegalPerson());
        org.setRegAddress(dto.getRegAddress());
        org.setBusinessScope(dto.getBusinessScope());
    }

    private Map<Long, PartyOrganization> loadDetails(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return organizationMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(PartyOrganization::getId, Function.identity()));
    }

    private OrganizationVO toVO(Party party, PartyOrganization org) {
        OrganizationVO vo = new OrganizationVO();
        vo.setId(party.getId());
        vo.setName(party.getName());
        vo.setStatus(party.getStatus());
        vo.setRemark(party.getRemark());
        vo.setCreatedAt(party.getCreatedAt());
        if (org != null) {
            vo.setOrgType(org.getOrgType());
            vo.setTaxNo(org.getTaxNo());
            vo.setRegisteredCapital(org.getRegisteredCapital());
            vo.setEstablishedDate(org.getEstablishedDate());
            vo.setLegalPerson(org.getLegalPerson());
            vo.setRegAddress(org.getRegAddress());
            vo.setBusinessScope(org.getBusinessScope());
        }
        return vo;
    }
}
