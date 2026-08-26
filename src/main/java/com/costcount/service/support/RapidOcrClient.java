package com.costcount.service.support;

import com.costcount.exception.BizException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RapidOcrClient {
    private static final int MAX_IMAGE_COUNT = 50;
    private static final Set<String> IMAGE_SUFFIXES = Set.of(".png", ".jpg", ".jpeg", ".webp");
    private static final TypeReference<List<WorkerResult>> RESULT_TYPE = new TypeReference<>() {
    };

    @Resource
    private ObjectMapper objectMapper;
    @Value("${app.ocr.python:python}")
    private String ocrPython;
    @Value("${app.ocr.script:scripts/rapidocr.py}")
    private String ocrScript;
    @Value("${app.ocr.use-cuda:true}")
    private boolean ocrUseCuda;
    @Value("${app.ocr.timeout-seconds:120}")
    private int ocrTimeoutSeconds;

    public List<OcrText> recognize(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BizException(400, "请选择账单截图");
        }
        if (files.length > MAX_IMAGE_COUNT) {
            throw new BizException(400, "单次最多识别 " + MAX_IMAGE_COUNT + " 张账单截图");
        }
        Path tempDirectory = null;
        List<Path> temporaryFiles = new ArrayList<>();
        try {
            tempDirectory = Files.createTempDirectory("cost-count-ocr-");
            List<Path> imagePaths = saveImages(files, tempDirectory, temporaryFiles);
            Path outputFile = tempDirectory.resolve("result.json");
            Path errorFile = tempDirectory.resolve("worker.log");
            temporaryFiles.add(outputFile);
            temporaryFiles.add(errorFile);
            Process process = startWorker(imagePaths, outputFile, errorFile);
            if (!process.waitFor(ocrTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    log.error("OCR Worker 强制退出超时");
                }
                throw new BizException(408, "OCR 批量识别超时");
            }
            String output = Files.exists(outputFile) ? Files.readString(outputFile).trim() : "";
            String error = Files.exists(errorFile) ? Files.readString(errorFile).trim() : "";
            if (process.exitValue() != 0 || !StringUtils.hasText(output)) {
                log.error("OCR Worker 执行失败，退出码：{}，错误：{}", process.exitValue(), error);
                throw new BizException(500, "本地 OCR 模型未能完成批量识别");
            }
            return parseResults(files, output);
        } catch (BizException exception) {
            throw exception;
        } catch (IOException exception) {
            log.error("调用本地 OCR Worker 失败", exception);
            throw new BizException(500, "无法启动本地 OCR 模型");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("等待本地 OCR Worker 时被中断", exception);
            throw new BizException(500, "OCR 识别被中断");
        } finally {
            deleteTemporaryFiles(temporaryFiles, tempDirectory);
        }
    }

    private List<Path> saveImages(MultipartFile[] files, Path directory, List<Path> temporaryFiles)
        throws IOException {
        List<Path> imagePaths = new ArrayList<>(files.length);
        for (int index = 0; index < files.length; index++) {
            MultipartFile file = files[index];
            if (file == null || file.isEmpty()) {
                throw new BizException(400, "第 " + (index + 1) + " 张账单截图为空");
            }
            Path imagePath = Files.createTempFile(directory, "image-" + (index + 1) + "-",
                imageSuffix(file.getOriginalFilename()));
            file.transferTo(imagePath);
            imagePaths.add(imagePath);
            temporaryFiles.add(imagePath);
        }
        return imagePaths;
    }

    private Process startWorker(List<Path> imagePaths, Path outputFile, Path errorFile) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(ocrPython);
        command.add(resolveOcrScript().toString());
        command.add("--use-cuda");
        command.add(Boolean.toString(ocrUseCuda));
        imagePaths.forEach(imagePath -> {
            command.add("--image");
            command.add(imagePath.toString());
        });
        return new ProcessBuilder(command).redirectOutput(outputFile.toFile()).redirectError(errorFile.toFile()).start();
    }

    private List<OcrText> parseResults(MultipartFile[] files, String output) throws IOException {
        List<WorkerResult> results = objectMapper.readValue(output, RESULT_TYPE);
        if (results.size() != files.length) {
            throw new BizException(500, "OCR 返回结果数量与图片数量不一致");
        }
        List<OcrText> texts = new ArrayList<>(files.length);
        for (int index = 0; index < files.length; index++) {
            WorkerResult result = results.get(index);
            String filename = Optional.ofNullable(files[index].getOriginalFilename()).orElse("账单截图");
            texts.add(new OcrText(filename, result.text(), result.error()));
        }
        return texts;
    }

    private Path resolveOcrScript() {
        return List.of(Path.of(ocrScript), Path.of("backend", ocrScript)).stream()
            .map(Path::toAbsolutePath).filter(Files::exists).findFirst()
            .orElseThrow(() -> new BizException(500, "找不到本地 OCR 模型脚本"));
    }

    private String imageSuffix(String filename) {
        String suffix = Optional.ofNullable(filename).filter(name -> name.contains("."))
            .map(name -> name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT)).orElse(".png");
        if (!IMAGE_SUFFIXES.contains(suffix)) {
            throw new BizException(400, "仅支持 PNG、JPG、JPEG 或 WEBP 图片");
        }
        return suffix;
    }

    private void deleteTemporaryFiles(List<Path> temporaryFiles, Path tempDirectory) {
        temporaryFiles.forEach(path -> deleteTemporaryFile(path, "OCR 临时文件"));
        if (tempDirectory != null) {
            deleteTemporaryFile(tempDirectory, "OCR 临时目录");
        }
    }

    private void deleteTemporaryFile(Path path, String description) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.error("删除{}失败：{}", description, path, exception);
        }
    }

    public record OcrText(String filename, String text, String error) {
    }

    private record WorkerResult(String text, String error) {
    }
}
