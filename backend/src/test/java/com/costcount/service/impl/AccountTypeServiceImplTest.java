package com.costcount.service.impl;

import com.costcount.dto.account.type.AccountTypeSaveDTO;
import com.costcount.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTypeServiceImplTest {

    private final AccountTypeServiceImpl service = new AccountTypeServiceImpl();

    @Test
    void addShouldRejectTypeCodeOtherThanDebitOrCredit() {
        AccountTypeSaveDTO dto = new AccountTypeSaveDTO();
        dto.setProviderId(10001L);
        dto.setTypeCode("credit_card");
        dto.setTypeName("信用卡");

        BizException exception = assertThrows(BizException.class, () -> service.addAccountType(dto));

        assertEquals(400, exception.getCode());
        assertEquals("账户类型编码只能是 DEBIT 或 CREDIT", exception.getMessage());
    }
}
