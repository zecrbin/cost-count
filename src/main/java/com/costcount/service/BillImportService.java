package com.costcount.service;

import com.costcount.dto.TransactionSaveDTO;
import com.costcount.vo.BillImportPreviewVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BillImportService {
    BillImportPreviewVO previewExcel(MultipartFile file);
    BillImportPreviewVO recognizeImages(MultipartFile[] files);
    List<String> confirm(List<TransactionSaveDTO> rows);
}
