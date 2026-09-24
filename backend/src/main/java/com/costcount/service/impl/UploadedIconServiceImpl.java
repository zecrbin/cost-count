package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.costcount.common.AccountDictionaryWriteLock;
import com.costcount.common.AfterCommit;
import com.costcount.entity.Account;
import com.costcount.entity.AccountProvider;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.service.UploadedIconService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 保存用户上传的图标。
 *
 * <p>按文件内容（而非扩展名或 Content-Type）识别格式，只接受 PNG、JPEG、GIF、WEBP，不接受 SVG
 * 以免嵌入脚本。PNG / JPEG 会重新编码为 PNG 并把长边缩到 {@value #MAX_EDGE} 像素，同时去掉原图元数据。</p>
 */
@Slf4j
@Service
public class UploadedIconServiceImpl implements UploadedIconService {

    /** 相对路径前缀，对应 {@code /icons/uploads/**} 静态资源映射。 */
    static final String UPLOADED_ICON_PREFIX = "uploads/";
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final int MAX_EDGE = 256;
    /** 只清理本服务生成的文件名，目录里手动放入的其他文件不受影响。 */
    private static final Pattern GENERATED_FILE_NAME = Pattern.compile("[0-9a-f]{32}\\.(png|gif|webp)");

    @Resource
    private AccountProviderMapper accountProviderMapper;

    @Resource
    private AccountMapper accountMapper;

    @Value("${app.storage.uploaded-icon-dir:data/uploaded-icons}")
    private String uploadedIconDir;

    @Override
    public String storeUploadedIcon(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException(400, "图片不能超过 2MB");
        }
        try {
            byte[] bytes = file.getBytes();
            ImageFormat format = ImageFormat.detect(bytes);
            if (format == null) {
                throw new BizException(400, "只支持 PNG、JPG、GIF、WEBP 格式的图片");
            }
            byte[] content = format.reencodable ? toPng(bytes) : bytes;
            String extension = format.reencodable ? "png" : format.extension;
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path directory = directory();
            Files.createDirectories(directory);
            Files.write(directory.resolve(fileName), content);
            return UPLOADED_ICON_PREFIX + fileName;
        } catch (IOException exception) {
            throw new BizException(500, "图片保存失败");
        }
    }

    @Override
    public boolean isUploadedIcon(String iconPath) {
        return StringUtils.hasText(iconPath) && iconPath.startsWith(UPLOADED_ICON_PREFIX);
    }

    @Override
    public void deleteUploadedIcon(String iconPath) {
        if (!isUploadedIcon(iconPath)) {
            return;
        }
        Path directory = directory();
        Path file = directory.resolve(iconPath.substring(UPLOADED_ICON_PREFIX.length())).normalize();
        // 规范化后再次校验父目录，避免异常路径越界删除其他文件。
        if (!file.startsWith(directory)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new BizException(500, "图标删除失败");
        }
    }

    @Override
    public void deleteReplacedIconAfterCommit(String previousIcon, String currentIcon) {
        if (isUploadedIcon(previousIcon) && !Objects.equals(previousIcon, currentIcon)) {
            AfterCommit.run("删除上传图标 " + previousIcon, () -> deleteUploadedIcon(previousIcon));
        }
    }

    @Override
    public int cleanupOrphanedIcons(Duration retention) {
        Path directory = directory();
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        Instant cutoff = Instant.now().minus(retention);
        // 与机构、账户保存共用写锁，避免"刚保存引用"和"判定为孤儿"交错。
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            Set<String> referenced = referencedIconFileNames();
            int deleted = 0;
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.toList()) {
                    String name = file.getFileName().toString();
                    if (!GENERATED_FILE_NAME.matcher(name).matches() || referenced.contains(name)
                            || !Files.isRegularFile(file) || !Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                        continue;
                    }
                    Files.deleteIfExists(file);
                    deleted++;
                }
            }
            if (deleted > 0) {
                log.info("已清理 {} 个未被使用的上传图标", deleted);
            }
            return deleted;
        } catch (IOException exception) {
            throw new BizException(500, "清理上传图标失败");
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    /** 收集仍被机构、账户引用的上传图标文件名。 */
    private Set<String> referencedIconFileNames() {
        Set<String> names = new HashSet<>();
        List<AccountProvider> providers = accountProviderMapper.selectList(new LambdaQueryWrapper<AccountProvider>()
                .select(AccountProvider::getIcon)
                .likeRight(AccountProvider::getIcon, UPLOADED_ICON_PREFIX));
        providers.forEach(provider -> names.add(provider.getIcon().substring(UPLOADED_ICON_PREFIX.length())));
        List<Account> accounts = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                .select(Account::getIcon)
                .likeRight(Account::getIcon, UPLOADED_ICON_PREFIX));
        accounts.forEach(account -> names.add(account.getIcon().substring(UPLOADED_ICON_PREFIX.length())));
        return names;
    }

    private Path directory() {
        return Path.of(uploadedIconDir).toAbsolutePath().normalize();
    }

    /** 读取 PNG / JPEG，按需缩小后编码为 PNG，保留透明通道。 */
    private byte[] toPng(byte[] bytes) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null) {
            throw new BizException(400, "图片已损坏，无法读取");
        }
        double scale = Math.min(1D, (double) MAX_EDGE / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(target, "png", output);
        return output.toByteArray();
    }

    /** 按文件头识别的图片格式；reencodable 表示可由 JDK 解码并重新编码。 */
    private enum ImageFormat {
        PNG("png", true, new byte[]{(byte) 0x89, 'P', 'N', 'G'}),
        JPEG("jpg", true, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        GIF("gif", false, new byte[]{'G', 'I', 'F', '8'}),
        WEBP("webp", false, null);

        private final String extension;
        private final boolean reencodable;
        private final byte[] magic;

        ImageFormat(String extension, boolean reencodable, byte[] magic) {
            this.extension = extension;
            this.reencodable = reencodable;
            this.magic = magic;
        }

        static ImageFormat detect(byte[] bytes) {
            if (bytes.length >= 12 && startsWith(bytes, 0, "RIFF".getBytes()) && startsWith(bytes, 8, "WEBP".getBytes())) {
                return WEBP;
            }
            return Arrays.stream(values())
                    .filter(format -> format.magic != null && startsWith(bytes, 0, format.magic))
                    .findFirst()
                    .orElse(null);
        }

        private static boolean startsWith(byte[] bytes, int offset, byte[] prefix) {
            if (bytes.length < offset + prefix.length) {
                return false;
            }
            for (int i = 0; i < prefix.length; i++) {
                if (bytes[offset + i] != prefix[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
