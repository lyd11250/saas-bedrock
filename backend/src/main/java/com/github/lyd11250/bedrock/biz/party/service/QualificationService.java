package com.github.lyd11250.bedrock.biz.party.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.biz.party.dto.QualificationDTO;
import com.github.lyd11250.bedrock.biz.party.entity.Party;
import com.github.lyd11250.bedrock.biz.party.entity.PartyQualification;
import com.github.lyd11250.bedrock.biz.party.mapper.PartyMapper;
import com.github.lyd11250.bedrock.biz.party.mapper.PartyQualificationMapper;
import com.github.lyd11250.bedrock.biz.party.vo.QualificationVO;
import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.system.service.FileService;
import com.github.lyd11250.bedrock.system.vo.FileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 相关方资质管理（party 从属，挂 party_id；租户隔离由多租户插件自动完成）。
 *
 * <p>资质「一行一项」，用「类型/名称/等级」三维刻画。证书扫描件复用基座 {@link FileService}：
 * 上传/下载走资质自身权限（见 Controller），本服务经 FileService 落库/取流/软删，
 * <b>不直连 SysFileMapper</b>（遵接入规范模块边界）。bizType 按相关方类型选择。
 */
@Service
@RequiredArgsConstructor
public class QualificationService {

    /** 资质附件业务类型：组织 / 人员（与 PartyFileBizTypeProvider 登记一致）。 */
    private static final String BIZ_TYPE_ORG = "party:organization:qualification";
    private static final String BIZ_TYPE_PERSON = "party:person:qualification";

    private final PartyMapper partyMapper;
    private final PartyQualificationMapper qualificationMapper;
    private final FileService fileService;

    /** 某相关方的资质列表（按创建时间倒序）；批量回填附件名/类型，无 N+1。 */
    public List<QualificationVO> listByParty(Long partyId) {
        List<PartyQualification> rows = qualificationMapper.selectList(
                Wrappers.<PartyQualification>lambdaQuery()
                        .eq(PartyQualification::getPartyId, partyId)
                        .orderByDesc(PartyQualification::getCreatedAt));
        List<Long> fileIds = rows.stream()
                .map(PartyQualification::getFileId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, FileVO> fileMetas = fileService.metaByIds(fileIds);
        return rows.stream().map(row -> toVO(row, fileMetas.get(row.getFileId()))).toList();
    }

    /** 本租户已入库的资质类型（去重、非空），供前端输入补全。 */
    public List<String> distinctQualTypes() {
        return qualificationMapper.selectList(Wrappers.<PartyQualification>lambdaQuery()
                        .select(PartyQualification::getQualType)
                        .isNotNull(PartyQualification::getQualType)
                        .ne(PartyQualification::getQualType, "")
                        .groupBy(PartyQualification::getQualType))
                .stream().map(PartyQualification::getQualType).toList();
    }

    /** 本租户已入库的资质等级（去重、非空），供前端输入补全。 */
    public List<String> distinctQualLevels() {
        return qualificationMapper.selectList(Wrappers.<PartyQualification>lambdaQuery()
                        .select(PartyQualification::getQualLevel)
                        .isNotNull(PartyQualification::getQualLevel)
                        .ne(PartyQualification::getQualLevel, "")
                        .groupBy(PartyQualification::getQualLevel))
                .stream().map(PartyQualification::getQualLevel).toList();
    }

    @Transactional
    public Long create(Long partyId, QualificationDTO dto) {
        requireParty(partyId);
        PartyQualification qual = new PartyQualification();
        qual.setPartyId(partyId);
        copyDetail(dto, qual);
        qual.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        qualificationMapper.insert(qual);
        return qual.getId();
    }

    @Transactional
    public void update(Long id, QualificationDTO dto) {
        PartyQualification qual = requireQualification(id);
        copyDetail(dto, qual);
        if (dto.getStatus() != null) {
            qual.setStatus(dto.getStatus());
        }
        qualificationMapper.updateById(qual);
    }

    @Transactional
    public void delete(Long id) {
        PartyQualification qual = requireQualification(id);
        if (qual.getFileId() != null) {
            fileService.softDelete(qual.getFileId());
        }
        qualificationMapper.deleteById(id);
    }

    /**
     * 上传/替换资质附件。bizType 按相关方类型选择；旧附件软删（同头像换图逻辑）。
     *
     * @return 新附件 file id
     */
    @Transactional
    public Long uploadFile(Long id, MultipartFile file) {
        PartyQualification qual = requireQualification(id);
        Party party = partyMapper.selectById(qual.getPartyId());
        String bizType = party != null && "PERSON".equals(party.getPartyType())
                ? BIZ_TYPE_PERSON : BIZ_TYPE_ORG;
        Long oldFileId = qual.getFileId();
        Long newFileId = fileService.storeFile(file, bizType).getId();
        qual.setFileId(newFileId);
        qualificationMapper.updateById(qual);
        if (oldFileId != null) {
            fileService.softDelete(oldFileId);
        }
        return newFileId;
    }

    /** 下载/预览资质附件流（内联展示，流由调用方关闭）。 */
    public FileService.DownloadFile downloadFile(Long id) {
        PartyQualification qual = requireQualification(id);
        if (qual.getFileId() == null) {
            throw new BusinessException("该资质未上传附件");
        }
        return fileService.loadInline(qual.getFileId());
    }

    /** 级联：软删某相关方全部资质，并逐行软删其附件（相关方删除时由 party 主表 service 编排调用）。 */
    @Transactional
    public void deleteByParty(Long partyId) {
        List<PartyQualification> rows = qualificationMapper.selectList(
                Wrappers.<PartyQualification>lambdaQuery()
                        .eq(PartyQualification::getPartyId, partyId));
        for (PartyQualification row : rows) {
            if (row.getFileId() != null) {
                fileService.softDelete(row.getFileId());
            }
        }
        if (!rows.isEmpty()) {
            qualificationMapper.delete(Wrappers.<PartyQualification>lambdaQuery()
                    .eq(PartyQualification::getPartyId, partyId));
        }
    }

    // ---- 内部 ----

    private void requireParty(Long partyId) {
        if (partyId == null || partyMapper.selectById(partyId) == null) {
            throw new BusinessException("相关方不存在");
        }
    }

    private PartyQualification requireQualification(Long id) {
        PartyQualification qual = qualificationMapper.selectById(id);
        if (qual == null) {
            throw new BusinessException("资质不存在");
        }
        return qual;
    }

    private void copyDetail(QualificationDTO dto, PartyQualification qual) {
        qual.setQualType(dto.getQualType());
        qual.setQualName(dto.getQualName());
        qual.setQualLevel(dto.getQualLevel());
        qual.setQualNo(dto.getQualNo());
        qual.setIssuingAuthority(dto.getIssuingAuthority());
        qual.setIssueDate(dto.getIssueDate());
        qual.setExpiryDate(dto.getExpiryDate());
        qual.setRemark(dto.getRemark());
    }

    private QualificationVO toVO(PartyQualification qual, FileVO fileMeta) {
        QualificationVO vo = new QualificationVO();
        vo.setId(qual.getId());
        vo.setPartyId(qual.getPartyId());
        vo.setQualType(qual.getQualType());
        vo.setQualName(qual.getQualName());
        vo.setQualLevel(qual.getQualLevel());
        vo.setQualNo(qual.getQualNo());
        vo.setIssuingAuthority(qual.getIssuingAuthority());
        vo.setIssueDate(qual.getIssueDate());
        vo.setExpiryDate(qual.getExpiryDate());
        vo.setStatus(qual.getStatus());
        vo.setRemark(qual.getRemark());
        vo.setCreatedAt(qual.getCreatedAt());
        vo.setFileId(qual.getFileId());
        if (fileMeta != null) {
            vo.setFileName(fileMeta.getOriginalName());
            vo.setFileContentType(fileMeta.getContentType());
        }
        return vo;
    }
}
