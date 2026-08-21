package com.costcount.dto;

import com.costcount.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "收支记录分页请求")
public class TransactionPageQuery extends PageQuery<TransactionQueryDTO> {
}
