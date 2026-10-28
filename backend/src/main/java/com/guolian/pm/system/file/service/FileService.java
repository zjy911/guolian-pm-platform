package com.guolian.pm.system.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.config.AppProperties;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.system.file.entity.SysFile;
import com.guolian.pm.system.file.mapper.SysFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 附件管理：统一附件表，业务模块通过 fileId 引用。
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final SysFileMapper fileMapper;
    private final AppProperties properties;

    public SysFile upload(MultipartFile file, String bizType, Long bizId) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要上传的文件");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String stored = UUID.randomUUID().toString().replace("-", "") + ext;
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dir = Paths.get(properties.getUpload().getDir(), datePath);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(stored);
            file.transferTo(target.toAbsolutePath());
            SysFile entity = new SysFile();
            entity.setFileName(stored);
            entity.setOriginalName(original);
            entity.setFilePath(datePath + "/" + stored);
            entity.setFileSize(file.getSize());
            entity.setContentType(file.getContentType());
            entity.setBizType(bizType);
            entity.setBizId(bizId);
            fileMapper.insert(entity);
            return entity;
        } catch (IOException e) {
            throw BizException.biz("文件保存失败");
        }
    }

    public PageResult<SysFile> page(PageQuery query, String keyword, String bizType) {
        LambdaQueryWrapper<SysFile> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysFile::getOriginalName, keyword);
        }
        if (StringUtils.hasText(bizType)) {
            wrapper.eq(SysFile::getBizType, bizType);
        }
        wrapper.orderByDesc(SysFile::getCreatedTime);
        Page<SysFile> page = fileMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), query.safePageNum(), query.safePageSize());
    }

    public SysFile get(Long id) {
        SysFile file = fileMapper.selectById(id);
        if (file == null) {
            throw BizException.notFound("附件不存在");
        }
        return file;
    }

    public Path resolve(SysFile file) {
        return Paths.get(properties.getUpload().getDir(), file.getFilePath());
    }

    public void delete(Long id) {
        SysFile file = get(id);
        try {
            Files.deleteIfExists(resolve(file));
        } catch (IOException ignored) {
            // 物理文件删除失败不阻断业务
        }
        fileMapper.deleteById(id);
    }
}
