package com.github.lyd11250.bedrock.biz.party.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.github.lyd11250.bedrock.biz.party.dto.QualificationDTO;
import com.github.lyd11250.bedrock.biz.party.service.QualificationService;
import com.github.lyd11250.bedrock.biz.party.vo.QualificationVO;
import com.github.lyd11250.bedrock.common.Result;
import com.github.lyd11250.bedrock.system.service.FileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 相关方资质接口（本租户）。资质以相关方为上下文的子资源，路径用 {@code partyId} 定位；
 * 人员页与单位页复用同一组接口，相关方是组织还是人由父表决定。
 */
@Tag(name = "相关方资质")
@RestController
@RequestMapping("/api/v1/party")
@RequiredArgsConstructor
public class QualificationController {

    private final QualificationService qualificationService;

    @GetMapping("/{partyId}/qualifications")
    @SaCheckPermission("party:qualification:list")
    public Result<List<QualificationVO>> list(@PathVariable Long partyId) {
        return Result.ok(qualificationService.listByParty(partyId));
    }

    /** 已入库的资质类型（去重），供前端输入补全。 */
    @GetMapping("/qualifications/qual-types")
    @SaCheckPermission("party:qualification:list")
    public Result<List<String>> qualTypes() {
        return Result.ok(qualificationService.distinctQualTypes());
    }

    /** 已入库的资质等级（去重），供前端输入补全。 */
    @GetMapping("/qualifications/qual-levels")
    @SaCheckPermission("party:qualification:list")
    public Result<List<String>> qualLevels() {
        return Result.ok(qualificationService.distinctQualLevels());
    }

    @PostMapping("/{partyId}/qualifications")
    @SaCheckPermission("party:qualification:create")
    public Result<Long> create(@PathVariable Long partyId, @Valid @RequestBody QualificationDTO dto) {
        return Result.ok(qualificationService.create(partyId, dto));
    }

    @PutMapping("/qualifications/{id}")
    @SaCheckPermission("party:qualification:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody QualificationDTO dto) {
        qualificationService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/qualifications/{id}")
    @SaCheckPermission("party:qualification:delete")
    public Result<Void> delete(@PathVariable Long id) {
        qualificationService.delete(id);
        return Result.ok();
    }

    /** 上传/替换资质附件。 */
    @PostMapping("/qualifications/{id}/file")
    @SaCheckPermission("party:qualification:update")
    public Result<Long> uploadFile(@PathVariable Long id, @RequestParam MultipartFile file) {
        return Result.ok(qualificationService.uploadFile(id, file));
    }

    /** 下载/预览资质附件（内联展示）。 */
    @GetMapping("/qualifications/{id}/file")
    @SaCheckPermission("party:qualification:list")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable Long id) {
        FileService.DownloadFile f = qualificationService.downloadFile(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                f.contentType() != null ? f.contentType() : "application/octet-stream"));
        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(f.content()));
    }
}
