package com.github.lyd11250.bedrock.biz.party.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.lyd11250.bedrock.biz.party.dto.PersonDTO;
import com.github.lyd11250.bedrock.biz.party.entity.Party;
import com.github.lyd11250.bedrock.biz.party.entity.PartyPerson;
import com.github.lyd11250.bedrock.biz.party.mapper.PartyMapper;
import com.github.lyd11250.bedrock.biz.party.mapper.PartyPersonMapper;
import com.github.lyd11250.bedrock.biz.party.vo.PersonVO;
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
 * 人员管理（party + party_person 共享主键；租户隔离由多租户插件自动完成）。
 *
 * <p>列表查询以主表 party（固定 {@code party_type=PERSON}）分页，再批量补子表详情，
 * 避免 N+1 且无需自定义 SQL。
 */
@Service
@RequiredArgsConstructor
public class PersonService {

    /** 相关方类型：人员。 */
    public static final String TYPE = "PERSON";

    private final PartyMapper partyMapper;
    private final PartyPersonMapper personMapper;
    private final AccountService accountService;
    private final QualificationService qualificationService;

    public IPage<PersonVO> page(long current, long size, String keyword) {
        Page<Party> page = partyMapper.selectPage(Page.of(current, size),
                Wrappers.<Party>lambdaQuery()
                        .eq(Party::getPartyType, TYPE)
                        .like(StringUtils.hasText(keyword), Party::getName, keyword)
                        .orderByDesc(Party::getCreatedAt));
        Map<Long, PartyPerson> details = loadDetails(page.getRecords().stream().map(Party::getId).toList());
        return page.convert(party -> toVO(party, details.get(party.getId())));
    }

    @Transactional
    public Long create(PersonDTO dto) {
        assertIdCardUnique(dto.getIdCard(), null);
        Party party = new Party();
        party.setPartyType(TYPE);
        party.setName(dto.getName());
        party.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        party.setRemark(dto.getRemark());
        partyMapper.insert(party);

        PartyPerson person = new PartyPerson();
        person.setId(party.getId());
        person.setGender(dto.getGender() != null ? dto.getGender() : 0);
        person.setIdCard(dto.getIdCard());
        person.setContact(dto.getContact());
        personMapper.insert(person);
        return party.getId();
    }

    @Transactional
    public void update(Long id, PersonDTO dto) {
        Party party = requireParty(id);
        assertIdCardUnique(dto.getIdCard(), id);
        party.setName(dto.getName());
        if (dto.getStatus() != null) {
            party.setStatus(dto.getStatus());
        }
        party.setRemark(dto.getRemark());
        partyMapper.updateById(party);

        PartyPerson person = personMapper.selectById(id);
        if (person == null) {
            person = new PartyPerson();
            person.setId(id);
            person.setGender(dto.getGender() != null ? dto.getGender() : 0);
            person.setIdCard(dto.getIdCard());
            person.setContact(dto.getContact());
            personMapper.insert(person);
        } else {
            person.setGender(dto.getGender() != null ? dto.getGender() : 0);
            person.setIdCard(dto.getIdCard());
            person.setContact(dto.getContact());
            personMapper.updateById(person);
        }
    }

    @Transactional
    public void delete(Long id) {
        requireParty(id);
        accountService.deleteByParty(id);
        qualificationService.deleteByParty(id);
        personMapper.deleteById(id);
        partyMapper.deleteById(id);
    }

    // ---- 内部 ----

    private Party requireParty(Long id) {
        Party party = partyMapper.selectById(id);
        if (party == null || !TYPE.equals(party.getPartyType())) {
            throw new BusinessException("人员不存在");
        }
        return party;
    }

    private void assertIdCardUnique(String idCard, Long excludeId) {
        if (!StringUtils.hasText(idCard)) {
            return;
        }
        Long count = personMapper.selectCount(Wrappers.<PartyPerson>lambdaQuery()
                .eq(PartyPerson::getIdCard, idCard)
                .ne(excludeId != null, PartyPerson::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("身份证号已存在");
        }
    }

    private Map<Long, PartyPerson> loadDetails(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return personMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(PartyPerson::getId, Function.identity()));
    }

    private PersonVO toVO(Party party, PartyPerson person) {
        PersonVO vo = new PersonVO();
        vo.setId(party.getId());
        vo.setName(party.getName());
        vo.setStatus(party.getStatus());
        vo.setRemark(party.getRemark());
        vo.setCreatedAt(party.getCreatedAt());
        if (person != null) {
            vo.setGender(person.getGender());
            vo.setIdCard(person.getIdCard());
            vo.setContact(person.getContact());
        }
        return vo;
    }
}
