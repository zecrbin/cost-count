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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 使用 Java2D 生成并管理账户专属银行卡图标。 */
@Service
public class AccountIconStorageServiceImpl implements AccountIconStorageService {

    /** 银行卡图标画布宽度。 */
    private static final int ICON_WIDTH = 512;
    /** 银行卡图标画布高度。 */
    private static final int ICON_HEIGHT = 320;

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
            return "accounts/" + accountId + ".png";
        } catch (IOException exception) {
            throw new BizException(500, "账户图标生成失败");
        }
    }

    @Override
    public void deleteBankCardIcon(String iconPath) {
        if (!StringUtils.hasText(iconPath) || !iconPath.startsWith("accounts/")) {
            return;
        }
        try {
            Path directory = Path.of(accountIconDir).toAbsolutePath().normalize();
            Path file = directory.resolve(iconPath.substring("accounts/".length())).normalize();
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
        // 使用稳定哈希生成配色，使同一提供方在多次生成时保持一致视觉风格。
        int seed = Math.abs((providerName == null ? "" : providerName).hashCode());
        Color background = Color.getHSBColor((seed % 360) / 360F, 0.58F, 0.68F);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(background);
        graphics.fill(new RoundRectangle2D.Double(8, 8, ICON_WIDTH - 16, ICON_HEIGHT - 16, 36, 36));
        graphics.setColor(new Color(255, 255, 255, 56));
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(new RoundRectangle2D.Double(8, 8, ICON_WIDTH - 16, ICON_HEIGHT - 16, 36, 36));
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Microsoft YaHei", Font.BOLD, 42));
        graphics.drawString(providerName, 42, 88);
        graphics.setFont(new Font("Dialog", Font.BOLD, 48));
        graphics.drawString("**** " + accTailNum, 42, 264);
    }
}
