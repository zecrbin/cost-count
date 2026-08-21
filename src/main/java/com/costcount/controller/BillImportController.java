package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.service.BillImportService;
import com.costcount.vo.BillImportPreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "账单导入")
@RestController
@RequestMapping("/api/bill-import")
public class BillImportController {
    @Resource
    private BillImportService billImportService;

    @Operation(summary = "解析 Excel 账单并返回预览")
    @PostMapping(value = "/excel/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<BillImportPreviewVO> previewExcel(@RequestPart("file") MultipartFile file) {
        return R.ok(billImportService.previewExcel(file));
    }

    @Operation(summary = "使用本机 Windows OCR 识别账单截图")
    @PostMapping(value = "/image/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<BillImportPreviewVO> recognizeImages(@RequestPart("files") MultipartFile[] files) {
        return R.ok(billImportService.recognizeImages(files));
    }

    @Operation(summary = "确认并批量导入已人工修正的账目")
    @PostMapping("/confirm")
    public R<List<String>> confirm(@RequestBody List<@Valid TransactionSaveDTO> rows) {
        return R.ok(billImportService.confirm(rows));
    }
}
