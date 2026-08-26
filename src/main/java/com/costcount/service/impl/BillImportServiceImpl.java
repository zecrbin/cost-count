package com.costcount.service.impl;

import com.costcount.dto.TransactionSaveDTO;
import com.costcount.exception.BizException;
import com.costcount.service.BillImportService;
import com.costcount.service.TransactionRecordService;
import com.costcount.service.support.BillImportRowMapper;
import com.costcount.service.support.ExcelBillReader;
import com.costcount.service.support.WindowsOcrClient;
import com.costcount.vo.BillImportPreviewVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BillImportServiceImpl implements BillImportService {
    @Resource
    private ExcelBillReader excelBillReader;
    @Resource
    private WindowsOcrClient windowsOcrClient;
    @Resource
    private BillImportRowMapper rowMapper;
    @Resource
    private TransactionRecordService transactionRecordService;

    @Override
    public BillImportPreviewVO previewExcel(MultipartFile file) {
        BillImportPreviewVO preview = new BillImportPreviewVO();
        preview.setSource("EXCEL");
        preview.setFileCount(1);
        preview.setRows(rowMapper.mapExcel(excelBillReader.read(file)));
        rowMapper.addWarnings(preview);
        return preview;
    }

    @Override
    public BillImportPreviewVO recognizeImages(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BizException(400, "请选择账单截图");
        }
        List<BillImportRowMapper.OcrText> recognizedTexts = new ArrayList<>(files.length);
        StringBuilder rawText = new StringBuilder();
        for (int index = 0; index < files.length; index++) {
            MultipartFile file = files[index];
            String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("账单截图");
            String text = windowsOcrClient.recognize(file);
            recognizedTexts.add(new BillImportRowMapper.OcrText(index + 1, filename, text));
            rawText.append("【").append(filename).append("】\n").append(text).append('\n');
        }
        return buildOcrPreview(files.length, recognizedTexts, rawText);
    }

    @Override
    public List<String> confirm(List<TransactionSaveDTO> rows) {
        return transactionRecordService.batchCreate(rows, "IMPORT");
    }

    private BillImportPreviewVO buildOcrPreview(int fileCount, List<BillImportRowMapper.OcrText> recognizedTexts,
                                                StringBuilder rawText) {
        BillImportPreviewVO preview = new BillImportPreviewVO();
        preview.setSource("OCR");
        preview.setFileCount(fileCount);
        preview.setRawText(rawText.toString().trim());
        preview.setRows(rowMapper.mapOcr(recognizedTexts));
        rowMapper.addWarnings(preview);
        return preview;
    }
}
