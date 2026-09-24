package com.costcount.service.impl;

import com.costcount.entity.ImportRecord;
import com.costcount.mapper.ImportRecordMapper;
import com.costcount.service.ImportRecordService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ImportRecordServiceImpl
        extends MPJBaseServiceImpl<ImportRecordMapper, ImportRecord>
        implements ImportRecordService {
}
