package com.github.lyd11250.bedrock.biz.party.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 相关方资质入参。归属相关方由路径 {@code partyId} 指定；附件 {@code fileId} 经独立的
 * 附件上传接口写入，不在本 DTO 内。
 */
@Data
public class QualificationDTO {

    @NotBlank(message = "资质类型不能为空")
    private String qualType;

    @NotBlank(message = "资质名称不能为空")
    private String qualName;

    /** 资质等级（可空）。 */
    private String qualLevel;

    /** 证书/证照编号。 */
    private String qualNo;

    /** 发证机关。 */
    private String issuingAuthority;

    /** 发证日期。 */
    private LocalDate issueDate;

    /** 有效期至（可空 = 长期有效）。 */
    private LocalDate expiryDate;

    /** 状态：1 有效，0 失效（缺省有效）。 */
    private Integer status;

    private String remark;
}
