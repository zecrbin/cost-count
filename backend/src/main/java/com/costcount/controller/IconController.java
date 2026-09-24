package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.service.UploadedIconService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "图标上传")
@RestController
@RequestMapping("/api/icons")
public class IconController {

    @Resource
    private UploadedIconService uploadedIconService;

    @Operation(summary = "上传图标", description = "支持 PNG、JPG、GIF、WEBP，不超过 2MB；返回的相对路径可直接保存到机构或账户的图标字段")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(uploadedIconService.storeUploadedIcon(file));
    }
}
