package com.costcount.service.impl;

import com.costcount.exception.BizException;
import com.costcount.service.AccountIconStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** 以提供方图标为底图、叠加账户尾号，生成并管理账户专属图标。 */
@Service
public class AccountIconStorageServiceImpl implements AccountIconStorageService {

    /** 账户图标边长，与提供方图标尺寸一致。 */
    private static final int ICON_SIZE = 512;
    /** 提供方图标在 classpath 中的根目录，对应访问路径 /icons/default/。 */
    private static final String PROVIDER_ICON_ROOT = "static/icons/default/";
    /** 账户图标相对路径前缀，对应访问路径 /icons/accounts/。 */
    private static final String ACCOUNT_ICON_PREFIX = "accounts/";

    /** 系统生成的图标在本机文件系统中的存放目录。 */
    @Value("${app.storage.account-icon-dir:data/account-icons}")
    private String accountIconDir;

    @Override
    public String storeAccountIcon(Long accountId, String providerIcon, String providerName, String accTailNum) {
        try {
            Path directory = Path.of(accountIconDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                BufferedImage base = loadProviderIcon(providerIcon);
                if (base != null) {
                    graphics.drawImage(base, 0, 0, ICON_SIZE, ICON_SIZE, null);
                } else {
                    drawFallbackBase(graphics, providerName);
                }
                drawTailNumber(graphics, accTailNum);
            } finally {
                graphics.dispose();
            }
            String filename = accountId + "-" + UUID.randomUUID() + ".png";
            ImageIO.write(image, "png", directory.resolve(filename).toFile());
            return ACCOUNT_ICON_PREFIX + filename;
        } catch (IOException exception) {
            throw new BizException(500, "账户图标生成失败");
        }
    }

    @Override
    public void deleteAccountIcon(String iconPath) {
        if (!StringUtils.hasText(iconPath) || !iconPath.startsWith(ACCOUNT_ICON_PREFIX)) {
            return;
        }
        try {
            Path directory = Path.of(accountIconDir).toAbsolutePath().normalize();
            Path file = directory.resolve(iconPath.substring(ACCOUNT_ICON_PREFIX.length())).normalize();
            // 规范化后再次校验父目录，避免异常路径越界删除其他文件。
            if (file.startsWith(directory)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException exception) {
            throw new BizException(500, "账户图标删除失败");
        }
    }

    /** 读取提供方图标；路径为空、文件不存在或不是图片时返回 null，由调用方退回默认底图。 */
    private BufferedImage loadProviderIcon(String providerIcon) throws IOException {
        if (!StringUtils.hasText(providerIcon)) {
            return null;
        }
        ClassPathResource resource = new ClassPathResource(PROVIDER_ICON_ROOT + providerIcon);
        if (!resource.exists()) {
            return null;
        }
        try (InputStream input = resource.getInputStream()) {
            return ImageIO.read(input);
        }
    }

    /** 没有提供方图标时，按提供方名称稳定取色，绘制带名称首字的圆角方形底图。 */
    private void drawFallbackBase(Graphics2D graphics, String providerName) {
        String name = providerName == null ? "" : providerName.trim();
        Color background = Color.getHSBColor(Math.floorMod(name.hashCode(), 360) / 360F, 0.58F, 0.68F);
        graphics.setColor(background);
        graphics.fill(new RoundRectangle2D.Double(0, 0, ICON_SIZE, ICON_SIZE, 112, 112));
        if (name.isEmpty()) {
            return;
        }
        String initial = name.substring(0, name.offsetByCodePoints(0, 1));
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Microsoft YaHei", Font.BOLD, 220));
        drawCentered(graphics, initial, 0, 40, ICON_SIZE, 300);
    }

    /** 在底部居中叠加深色胶囊，显示账户尾号。 */
    private void drawTailNumber(Graphics2D graphics, String accTailNum) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 120));
        Rectangle2D textBounds = visualBounds(graphics, accTailNum);
        int pillHeight = 156;
        int pillWidth = (int) Math.min(ICON_SIZE - 32, textBounds.getWidth() + 96);
        int pillX = (ICON_SIZE - pillWidth) / 2;
        int pillY = ICON_SIZE - pillHeight - 28;

        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fill(new RoundRectangle2D.Double(pillX, pillY, pillWidth, pillHeight, pillHeight, pillHeight));
        graphics.setColor(Color.WHITE);
        drawCentered(graphics, accTailNum, pillX, pillY, pillWidth, pillHeight);
    }

    /** 按字形的实际可见边界居中绘制文字，数字没有下伸部分，按字体行高居中会偏上。 */
    private void drawCentered(Graphics2D graphics, String text, int x, int y, int width, int height) {
        Rectangle2D bounds = visualBounds(graphics, text);
        float drawX = (float) (x + (width - bounds.getWidth()) / 2 - bounds.getX());
        float drawY = (float) (y + (height - bounds.getHeight()) / 2 - bounds.getY());
        graphics.drawString(text, drawX, drawY);
    }

    private Rectangle2D visualBounds(Graphics2D graphics, String text) {
        return graphics.getFont().createGlyphVector(graphics.getFontRenderContext(), text).getVisualBounds();
    }
}
