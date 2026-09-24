package com.costcount.service.impl;

import com.costcount.exception.BizException;
import com.costcount.service.AccountIconStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** 使用 Java2D 生成并管理账户专属银行卡图标。 */
@Service
public class AccountIconStorageServiceImpl implements AccountIconStorageService {

    /** 银行卡图标画布宽度。 */
    private static final int ICON_WIDTH = 512;
    /** 银行卡图标画布高度。 */
    private static final int ICON_HEIGHT = 320;
    /** 系统生成图标的相对路径前缀，对应 {@code /icons/accounts/**} 静态资源映射。 */
    private static final String GENERATED_ICON_PREFIX = "accounts/";
    /** 提供方名称字体候选，按顺序选取本机已安装且能显示中文的字体。 */
    private static final List<String> NAME_FONT_CANDIDATES = List.of(
            "Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", "Source Han Sans SC",
            "WenQuanYi Micro Hei", "SimHei");

    /** 系统生成的图标在本机文件系统中的存放目录。 */
    @Value("${app.storage.account-icon-dir:data/account-icons}")
    private String accountIconDir;

    @Override
    public String storeBankCardIcon(Long accountId, String providerName, String accTailNum) {
        try {
            Path directory = Path.of(accountIconDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Path file = directory.resolve(accountId + ".png");
            BufferedImage image = new BufferedImage(ICON_WIDTH, ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                drawBankCard(graphics, providerName, accTailNum);
            } finally {
                graphics.dispose();
            }
            ImageIO.write(image, "png", file.toFile());
            return GENERATED_ICON_PREFIX + accountId + ".png";
        } catch (IOException exception) {
            throw new BizException(500, "账户图标生成失败");
        }
    }

    @Override
    public boolean isGeneratedIcon(String iconPath) {
        return StringUtils.hasText(iconPath) && iconPath.startsWith(GENERATED_ICON_PREFIX);
    }

    @Override
    public void deleteBankCardIcon(String iconPath) {
        if (!isGeneratedIcon(iconPath)) {
            return;
        }
        try {
            Path directory = Path.of(accountIconDir).toAbsolutePath().normalize();
            Path file = directory.resolve(iconPath.substring(GENERATED_ICON_PREFIX.length())).normalize();
            // 规范化后再次校验父目录，避免异常路径越界删除其他文件。
            if (file.startsWith(directory)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException exception) {
            throw new BizException(500, "账户图标删除失败");
        }
    }

    /** 绘制由提供方名称确定配色、并展示账户尾号的银行卡卡面。 */
    private void drawBankCard(Graphics2D graphics, String providerName, String accTailNum) {
        String name = StringUtils.hasText(providerName) ? providerName.trim() : "";
        // 使用稳定哈希生成配色，使同一提供方在多次生成时保持一致视觉风格。
        // 先取模再取绝对值，避免 Integer.MIN_VALUE 取绝对值后仍为负数。
        int seed = Math.abs(name.hashCode() % 360);
        Color background = Color.getHSBColor(seed / 360F, 0.58F, 0.68F);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(background);
        graphics.fill(new RoundRectangle2D.Double(8, 8, ICON_WIDTH - 16, ICON_HEIGHT - 16, 36, 36));
        graphics.setColor(new Color(255, 255, 255, 56));
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(new RoundRectangle2D.Double(8, 8, ICON_WIDTH - 16, ICON_HEIGHT - 16, 36, 36));
        graphics.setColor(Color.WHITE);
        if (!name.isEmpty()) {
            graphics.setFont(resolveNameFont(name));
            graphics.drawString(name, 42, 88);
        }
        if (StringUtils.hasText(accTailNum)) {
            graphics.setFont(new Font(Font.DIALOG, Font.BOLD, 48));
            graphics.drawString("**** " + accTailNum.trim(), 42, 264);
        }
    }

    /** 选取能完整显示名称的字体；都不满足时退回逻辑字体，由 JDK 按系统字体配置兜底。 */
    private Font resolveNameFont(String name) {
        Set<String> installed = Set.of(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String family : NAME_FONT_CANDIDATES) {
            Font font = new Font(family, Font.BOLD, 42);
            if (installed.contains(family) && font.canDisplayUpTo(name) == -1) {
                return font;
            }
        }
        return new Font(Font.SANS_SERIF, Font.BOLD, 42);
    }
}
