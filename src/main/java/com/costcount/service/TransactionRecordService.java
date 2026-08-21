package com.costcount.service;

import com.costcount.dto.TransactionPageQuery;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.TransactionRecord;
import com.costcount.vo.PageResult;
import com.costcount.vo.TransactionVO;
import com.github.yulichang.base.MPJBaseService;

public interface TransactionRecordService extends MPJBaseService<TransactionRecord> {
    PageResult<TransactionVO> pageList(TransactionPageQuery query);
    String create(TransactionSaveDTO dto);
    String delete(Long id);
}
