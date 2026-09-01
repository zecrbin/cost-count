package com.costcount.service.impl;

import com.costcount.config.properties.IconStorageProperties;
import com.costcount.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AccountIconServiceImplTest {

    @TempDir
    Path outputDirectory;

    private AccountIconServiceImpl accountIconService;

    @BeforeEach
    void setUp() {
        IconStorageProperties properties = new IconStorageProperties();
        properties.setDefaultPrefix("/icons/default/");

        IconStorageProperties.Generated generated = new IconStorageProperties.Generated();
        generated.setPath(outputDirectory.toString());
        generated.setUrlPrefix("/static/account-icons/");
        properties.setGenerated(generated);

        accountIconService = new AccountIconServiceImpl();
        ReflectionTestUtils.setField(accountIconService, "iconStorageProperties", properties);
    }

    @Test
    void shouldGenerateAndDeleteBankCardIcon() throws Exception {
        Account account = new Account();
        account.setAccName("中国银行储蓄卡");
        account.setAccTailNum("1234");

        String iconUrl = accountIconService.generateBankCardIcon(
                "储蓄卡", "providers/boc.png", account);

        Path outputFile = outputDirectory.resolve("account_中国银行储蓄卡.png");
        assertThat(iconUrl).isEqualTo("/static/account-icons/account_中国银行储蓄卡.png");
        assertThat(outputFile).isRegularFile();

        BufferedImage image = ImageIO.read(outputFile.toFile());
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(128);
        assertThat(image.getHeight()).isEqualTo(128);

        accountIconService.deleteAccountIcon(iconUrl);

        assertThat(outputFile).doesNotExist();
    }

    @Test
    void shouldRejectInvalidCardTailNumber() {
        Account account = new Account();
        account.setAccName("中国银行储蓄卡");
        account.setAccTailNum("123");

        assertThatIllegalArgumentException().isThrownBy(() ->
                accountIconService.generateBankCardIcon("储蓄卡", "providers/boc.png", account))
                .withMessage("银行卡尾号必须为4位数字");
    }
}
