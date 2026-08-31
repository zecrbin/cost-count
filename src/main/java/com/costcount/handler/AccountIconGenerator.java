package com.costcount.handler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AccountIconGenerator {

    /**
     * 生成圆形账户图标
     *
     * @param logoInputStream 提供方 logo 输入流，建议 png
     * @param tailNum         尾号，如 9527
     * @param size            图标尺寸，如 96 / 128
     */
    public byte[] generateCircleIcon(
            InputStream logoInputStream,
            String tailNum,
            int size
    ) throws Exception {

        BufferedImage canvas =
                new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = canvas.createGraphics();

        try {
            enableQuality(g);

            // 背景透明
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, size, size);
            g.setComposite(AlphaComposite.SrcOver);

            // 画白色圆底
            g.setColor(Color.WHITE);
            g.fillOval(0, 0, size, size);

            // 外边框
            g.setColor(new Color(220, 220, 220));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(1, 1, size - 2, size - 2);

            // 加载 logo
            BufferedImage logo = ImageIO.read(logoInputStream);
            if (logo == null) {
                throw new IllegalArgumentException("logo 读取失败，请使用 png/jpg 图片");
            }

            // logo 摆放区域
            // 上半部分稍微大一些，下半部分留给尾号
            int logoBoxSize = (int) (size * 0.44);
            int logoX = (size - logoBoxSize) / 2;
            int logoY = (int) (size * 0.18);

            drawContainImage(g, logo, logoX, logoY, logoBoxSize, logoBoxSize);

            // 画尾号
            if (tailNum != null && !tailNum.isBlank()) {
                drawTailNum(g, tailNum, size);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", out);
            return out.toByteArray();

        } finally {
            g.dispose();
        }
    }

    private void drawTailNum(Graphics2D g, String tailNum, int size) {
        // 只保留后4位
        String text = tailNum.length() > 4
                ? tailNum.substring(tailNum.length() - 4)
                : tailNum;

        int fontSize = Math.max(14, size / 7);
        Font font = new Font("SansSerif", Font.BOLD, fontSize);
        g.setFont(font);

        FontRenderContext frc = g.getFontRenderContext();
        int textWidth = (int) font.getStringBounds(text, frc).getWidth();

        int x = (size - textWidth) / 2;
        int y = (int) (size * 0.78);

        // 白底小胶囊，增强可读性
        int paddingX = 8;
        int paddingY = 4;
        int rectX = x - paddingX;
        int rectY = y - fontSize + 2;
        int rectW = textWidth + paddingX * 2;
        int rectH = fontSize + paddingY * 2;

        g.setColor(new Color(255, 255, 255, 235));
        g.fillRoundRect(rectX, rectY, rectW, rectH, 16, 16);

        g.setColor(new Color(220, 220, 220));
        g.drawRoundRect(rectX, rectY, rectW, rectH, 16, 16);

        g.setColor(new Color(51, 51, 51));
        g.drawString(text, x, y);
    }

    /**
     * 按 contain 方式绘制图片，不拉伸变形
     */
    private void drawContainImage(
            Graphics2D g,
            BufferedImage image,
            int x,
            int y,
            int boxW,
            int boxH
    ) {
        int imgW = image.getWidth();
        int imgH = image.getHeight();

        double scale = Math.min((double) boxW / imgW, (double) boxH / imgH);

        int drawW = (int) (imgW * scale);
        int drawH = (int) (imgH * scale);

        int drawX = x + (boxW - drawW) / 2;
        int drawY = y + (boxH - drawH) / 2;

        g.drawImage(image, drawX, drawY, drawW, drawH, null);
    }

    private void enableQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}