package nsu.security.demoapplication.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name="Files")
@RestController
public class ConvertController {

    private static final String UPLOAD_DIR = "/app/uploads";

    private static StreamResult readAll(InputStream in) {
        try (in) {
            return new StreamResult(in.readAllBytes(), null);
        } catch (IOException e) {
            return new StreamResult(new byte[0], e);
        }
    }

    @GetMapping(value = "/files/convert")
    public ResponseEntity<Resource> convert(@RequestParam String filename, @RequestParam(defaultValue = "png") String format) throws IOException, InterruptedException {

        String inputPath = UPLOAD_DIR + "/" + filename;
        String cmd = "convert " + inputPath + " " + format + ":-";

        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", cmd);
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            return textError(500, "Failed to start conversion");
        }

        CompletableFuture<StreamResult> outFuture = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
        CompletableFuture<StreamResult> errFuture = CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()));

        boolean finished;
        try {
            finished = process.waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return textError(500, "Conversion interrupted");
        }

        if (!finished) {
            process.destroyForcibly();
            return textError(504, "Conversion timed out");
        }

        StreamResult out = outFuture.join();
        StreamResult err = errFuture.join();

        if (!out.ok()) {
            return textError(500, "Failed to read conversion result");
        }

        int exit = process.exitValue();
        byte[] stdout = out.data();
        byte[] stderr = err.ok() ? err.data() : new byte[0];

        String stdoutStr = new String(stdout, StandardCharsets.UTF_8);
        String stderrStr = new String(stderr, StandardCharsets.UTF_8);

        // Отладочный режим: format=debug — показать CMD, код возврата и оба потока.
        if ("debug".equalsIgnoreCase(format)) {
            String body = "CMD: " + cmd + "\n\nEXIT: " + exit + "\n\nSTDOUT:\n" + stdoutStr + "\n\nSTDERR:\n" + stderrStr;

            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(new ByteArrayResource(body.getBytes(StandardCharsets.UTF_8)));
        }

        if (exit != 0) {
            String msg = "CMD: " + cmd + "\n\nSTDERR:\n" + stderrStr;
            return ResponseEntity.status(500).contentType(MediaType.TEXT_PLAIN).body(new ByteArrayResource(msg.getBytes(StandardCharsets.UTF_8)));
        }

        return ResponseEntity.ok().contentType(mediaTypeFor(format)).body(new ByteArrayResource(stdout));
    }

    @GetMapping(value = "/files/convert/safe")
    public ResponseEntity<Resource> convertSafe(@RequestParam String filename, @RequestParam(defaultValue = "png") String format) throws IOException, InterruptedException {

        Set<String> ALLOWED_FORMATS = Set.of("png", "jpg", "jpeg", "gif", "webp");

        String fmt = format.toLowerCase(Locale.ROOT);
        if (!ALLOWED_FORMATS.contains(fmt)) {
            return textError(400, "Unsupported format: " + format);
        }

        java.util.regex.Pattern SAFE_FILENAME = java.util.regex.Pattern.compile("^[A-Za-z0-9._-]+$");

        if (!SAFE_FILENAME.matcher(filename).matches()) {
            return textError(400, "Invalid filename");
        }

        Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
        Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();

        if (!filePath.startsWith(uploadDir)) {
            return textError(400, "Invalid path");
        }

        String outSpec = fmt + ":-";

        List<String> command = List.of("convert", filePath.toString(), outSpec);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            return textError(500, "Failed to start process: " + e.getMessage());
        }

        CompletableFuture<StreamResult> outFuture = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
        CompletableFuture<StreamResult> errFuture = CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()));

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return textError(504, "Conversion timed out");
        }

        StreamResult out = outFuture.join();
        StreamResult err = errFuture.join();

        if (!out.ok()) {
            return textError(500, "Failed to read process output: " + out.error().getMessage());
        }

        byte[] stdout = out.data();
        byte[] stderr = err.ok() ? err.data() : new byte[0];
        int exit = process.exitValue();

        if (exit != 0) {
            return textError(500, "Conversion failed: " + new String(stderr, StandardCharsets.UTF_8));
        }

        return ResponseEntity.ok().contentType(mediaTypeFor(fmt)).body(new ByteArrayResource(stdout));
    }

    private ResponseEntity<Resource> textError(int status, String msg) {
        byte[] body = msg.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(new ByteArrayResource(body));
    }

    private MediaType mediaTypeFor(String format) {
        return switch (format.toLowerCase()) {
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private record StreamResult(byte[] data, IOException error) {
        boolean ok() {
            return error == null;
        }
    }
}