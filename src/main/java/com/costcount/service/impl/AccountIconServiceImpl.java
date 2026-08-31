package com.costcount.service.impl;

import com.costcount.config.properties.IconStorageProperties;
import com.costcount.entity.Account;
import com.costcount.service.AccountIconService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class AccountIconServiceImpl
        implements AccountIconService {

    private static final int ICON_SIZE = 128;

    @Resource
    private IconStorageProperties iconStorageProperties;

    @Override
    public String generateBankCardIcon(
            String accountTypeName,
            String providerIcon,
            Account account) {

        if (!StringUtils.hasText(providerIcon)) {
            throw new IllegalArgumentException("账户提供方图标不能为空");
        }

        String normalizedTailNum =
                normalizeTailNum(account.getAccTailNum());

        try {

            /*
             * 动态图标输出目录
             */
            Path outputDir = Path.of(
                            iconStorageProperties
                                    .getGenerated()
                                    .getPath()
                    )
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(outputDir);

            /*
             * 加载默认provider Logo
             */
            BufferedImage providerLogo =
                    loadDefaultIcon(providerIcon);

            /*
             * 生成账户图标
             */
            BufferedImage accountIcon =
                    buildAccountIcon(
                            providerLogo,
                            normalizedTailNum
                    );

            /*
             * 固定使用账户ID作为文件名。
             *
             * 修改尾号时直接覆盖即可。
             */
            String fileName =
                    "account_" + account.getAccName() + ".png";

            Path outputFile =
                    outputDir.resolve(fileName);

            ImageIO.write(
                    accountIcon,
                    "png",
                    outputFile.toFile()
            );

            return buildGeneratedUrl(fileName);

        } catch (Exception e) {

            log.error(
                    "生成账户图标失败，accountName={}, providerIcon={}",
                    account.getAccName(),
                    providerIcon,
                    e
            );

            throw new RuntimeException(
                    "生成账户图标失败",
                    e
            );
        }
    }

    @Override
    public void deleteAccountIcon(Long accountId) {

        if (accountId == null) {
            return;
        }

        try {

            Path file = Path.of(
                            iconStorageProperties
                                    .getGenerated()
                                    .getPath()
                    )
                    .toAbsolutePath()
                    .normalize()
                    .resolve(
                            "account_" + accountId + ".png"
                    );

            Files.deleteIfExists(file);

        } catch (IOException e) {

            log.warn(
                    "删除账户图标失败，accountId={}",
                    accountId,
                    e
            );
        }
    }

    /**
     * 加载系统默认图标。
     * <p>
     * providerIcon:
     * <p>
     * providers/cmb.png
     * <p>
     * 实际classpath：
     * <p>
     * static/icons/default/providers/cmb.png
     */
    private BufferedImage loadDefaultIcon(
            String providerIcon)
            throws IOException {

        String defaultPrefix =
                iconStorageProperties.getDefaultPrefix();

        /*
         * /icons/default/
         * ->
         * icons/default/
         */
        defaultPrefix =
                trimSlash(defaultPrefix);

        /*
         * resources/static 是Spring Boot默认静态目录，
         * classpath读取需要加 static/
         */
        String classpath =
                "static/"
                        + defaultPrefix
                        + "/"
                        + trimSlash(providerIcon);

        ClassPathResource resource =
                new ClassPathResource(classpath);

        if (!resource.exists()) {
            throw new IOException(
                    "默认图标不存在：" + classpath
            );
        }

        try (InputStream inputStream =
                     resource.getInputStream()) {

            BufferedImage image =
                    ImageIO.read(inputStream);

            if (image == null) {
                throw new IOException(
                        "无法解析默认图标：" + classpath
                );
            }

            return image;
        }
    }

    private BufferedImage buildAccountIcon(
            BufferedImage providerLogo,
            String tailNum) {

        BufferedImage image =
                new BufferedImage(
                        ICON_SIZE,
                        ICON_SIZE,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D g =
                image.createGraphics();

        try {

            enableQuality(g);

            /*
             * 透明背景
             */
            g.setComposite(
                    AlphaComposite.Clear
            );

            g.fillRect(
                    0,
                    0,
                    ICON_SIZE,
                    ICON_SIZE
            );

            g.setComposite(
                    AlphaComposite.SrcOver
            );

            /*
             * 圆形白底
             */
            g.setColor(Color.WHITE);

            g.fillOval(
                    2,
                    2,
                    ICON_SIZE - 4,
                    ICON_SIZE - 4
            );

            /*
             * 边框
             */
            g.setColor(
                    new Color(225, 225, 225)
            );

            g.setStroke(
                    new BasicStroke(2F)
            );

            g.drawOval(
                    2,
                    2,
                    ICON_SIZE - 4,
                    ICON_SIZE - 4
            );

            /*
             * Provider Logo
             */
            int logoSize = 62;

            drawContainImage(
                    g,
                    providerLogo,
                    (ICON_SIZE - logoSize) / 2,
                    17,
                    logoSize,
                    logoSize
            );

            /*
             * 卡尾号
             */
            drawTailNum(
                    g,
                    tailNum
            );

            return image;

        } finally {
            g.dispose();
        }
    }

    private void drawTailNum(
            Graphics2D g,
            String tailNum) {

        Font font =
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        19
                );

        g.setFont(font);

        FontRenderContext context =
                g.getFontRenderContext();

        int textWidth =
                (int) font
                        .getStringBounds(
                                tailNum,
                                context
                        )
                        .getWidth();

        int x =
                (ICON_SIZE - textWidth) / 2;

        int y = 106;

        g.setColor(
                new Color(60, 60, 60)
        );

        g.drawString(
                tailNum,
                x,
                y
        );
    }

    private void drawContainImage(
            Graphics2D g,
            BufferedImage source,
            int x,
            int y,
            int boxWidth,
            int boxHeight) {

        int sourceWidth =
                source.getWidth();

        int sourceHeight =
                source.getHeight();

        double scale =
                Math.min(
                        (double) boxWidth / sourceWidth,
                        (double) boxHeight / sourceHeight
                );

        int width =
                (int) (sourceWidth * scale);

        int height =
                (int) (sourceHeight * scale);

        int drawX =
                x + (boxWidth - width) / 2;

        int drawY =
                y + (boxHeight - height) / 2;

        g.drawImage(
                source,
                drawX,
                drawY,
                width,
                height,
                null
        );
    }

    private String normalizeTailNum(
            String tailNum) {

        if (!StringUtils.hasText(tailNum)) {
            throw new IllegalArgumentException(
                    "银行卡尾号不能为空"
            );
        }

        String value =
                tailNum.trim();

        if (!value.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "银行卡尾号必须为4位数字"
            );
        }

        return value;
    }

    /**
     * 动态图标URL：
     * <p>
     * /static/account-icons/account_10001.png
     */
    private String buildGeneratedUrl(
            String fileName) {

        String prefix =
                iconStorageProperties
                        .getGenerated()
                        .getUrlPrefix();

        if (!prefix.endsWith("/")) {
            prefix += "/";
        }

        return prefix + fileName;
    }

    private String trimSlash(String value) {

        if (value == null) {
            return "";
        }

        value = value.trim();

        while (value.startsWith("/")) {
            value = value.substring(1);
        }

        while (value.endsWith("/")) {
            value = value.substring(
                    0,
                    value.length() - 1
            );
        }

        return value;
    }

    private void enableQuality(
            Graphics2D g) {

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );
    }
}