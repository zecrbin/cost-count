package com.costcount.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "账单导入预览")
public class BillImportPreviewVO {
    @Schema(description = "导入来源，EXCEL 或 OCR", example = "EXCEL")
    private String source;
    @Schema(description = "文件数量", example = "1")
    private Integer fileCount;
    @Schema(description = "OCR 原始文本，仅截图识别返回")
    private String rawText;
    @Schema(description = "提示和警告")
    private List<String> warnings = new ArrayList<>();
    @Schema(description = "可人工修正的预览行")
    private List<BillImportRowVO> rows = new ArrayList<>();
}
