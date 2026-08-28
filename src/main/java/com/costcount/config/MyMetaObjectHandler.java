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
        strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updatedBy", String.class, "system");
    }
}
