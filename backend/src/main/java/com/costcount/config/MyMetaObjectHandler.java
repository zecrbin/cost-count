package com.costcount.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "createdBy", String.class, "system");
        strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "updatedBy", String.class, "system");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // strictUpdateFill 不覆盖已有值；服务层普遍先查询再更新实体，必须强制刷新更新时间。
        setFieldValByName("updatedTime", LocalDateTime.now(), metaObject);
        setFieldValByName("updatedBy", "system", metaObject);
    }
}
