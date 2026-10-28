package com.guolian.pm.system.file.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.system.file.entity.SysFile;
import com.guolian.pm.system.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestPart("file") MultipartFile file,
                                                   @RequestParam(required = false) String bizType,
                                                   @RequestParam(required = false) Long bizId) {
        SysFile entity = fileService.upload(file, bizType, bizId);
        return ApiResponse.ok(Map.of("id", entity.getId(), "name", entity.getOriginalName(),
                "size", entity.getFileSize()));
    }

    @GetMapping
    @Perm("system:file:list")
    public ApiResponse<PageResult<SysFile>> page(PageQuery query,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String bizType) {
        return ApiResponse.ok(fileService.page(query, keyword, bizType));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        SysFile file = fileService.get(id);
        Resource resource = new FileSystemResource(fileService.resolve(file));
        String encoded = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=UTF-8''" + encoded)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @Perm("system:file:list")
    @OpLog(module = "附件管理", action = "删除附件")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return ApiResponse.ok();
    }
}
