package com.github.lyd11250.bedrock.biz.party.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 相关方资质（party 从属，挂 {@code party_id}；企业资质 / 人员资质合一表，一行一项）。
 *
 * <p>用「类型 / 名称 / 等级」三维刻画资质项：一份实体证书若授予多项（如特种作业证含高压/低压
 * 电工作业、建筑资质含多个承包专业），拆成多行登记。证书扫描件复用 {@code sys_file}，
 * {@code fileId} 引用其 id（应用层维护，不加外键）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("party_qualification")
public class PartyQualification extends BaseEntity {

    /** 所属相关方 id（= party.id）。 */
    private Long partyId;

    /** 资质类型（证照大类，如 特种作业操作证/驾驶证/建筑业企业资质证书）。 */
    private String qualType;

    /** 资质名称/操作领域（具体资质项，如 高压电工作业/输变电工程专业承包）。 */
    private String qualName;

    /** 资质等级（如 C1、壹级/贰级；可空，特种作业操作项目无等级）。 */
    private String qualLevel;

    /** 证书/证照编号。 */
    private String qualNo;

    /** 发证机关。 */
    private String issuingAuthority;

    /** 发证日期。 */
    private LocalDate issueDate;

    /** 有效期至（可空 = 长期有效）。 */
    private LocalDate expiryDate;

    /** 证书扫描件 id（引用 sys_file；空 = 未上传）。 */
    private Long fileId;

    /** 状态：1 有效，0 失效。 */
    private Integer status;

    private String remark;
}
