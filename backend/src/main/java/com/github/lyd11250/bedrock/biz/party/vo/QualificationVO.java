package com.github.lyd11250.bedrock.biz.party.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 相关方资质出参。附件名/类型由 {@code fileId} 经 {@code FileService} 回填，供前端判断是否
 * 图片（决定缩略图）与展示文件名。
 */
@Data
public class QualificationVO {

    private Long id;

    private Long partyId;

    private String qualType;

    private String qualName;

    private String qualLevel;

    private String qualNo;

    private String issuingAuthority;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;

    /** 证书扫描件 id（空 = 未上传）。 */
    private Long fileId;

    /** 附件原始文件名（由 FileService 回填，空 = 未上传或已删）。 */
    private String fileName;

    /** 附件 MIME 类型（前端据此判断是否图片）。 */
    private String fileContentType;
}
