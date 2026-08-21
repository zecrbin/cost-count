package com.costcount.service;

import com.costcount.dto.TransactionPageQuery;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.TransactionRecord;
import com.costcount.vo.PageResult;
import com.costcount.vo.TransactionVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

public interface TransactionRecordService extends MPJBaseService<TransactionRecord> {
    PageResult<TransactionVO> pageList(TransactionPageQuery query);
    String create(TransactionSaveDTO dto);
    List<String> batchCreate(List<TransactionSaveDTO> rows, String source);
    String delete(Long id);
}
