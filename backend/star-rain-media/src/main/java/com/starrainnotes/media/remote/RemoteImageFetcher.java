package com.starrainnotes.media.remote;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.enumeration.MediaType;
import com.starrainnotes.media.properties.MediaProperties;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Fetches only public HTTP images; DNS is validated at the actual connection. */
@Component
@RequiredArgsConstructor
public class RemoteImageFetcher {
    private final MediaProperties properties;

    static URI validateUrl(String input) {
        try {
            URI uri = URI.create(input);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != ("https".equalsIgnoreCase(scheme) ? 443 : 80))) {
                throw failure();
            }
            return uri;
        } catch (IllegalArgumentException ex) { throw failure(); }
    }

    static boolean publicAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            return (bytes[0] & 0xe0) == 0x20
                    && !((bytes[0] & 255) == 0x20 && bytes[1] == 1
                        && (bytes[2] & 255) == 0x0d && (bytes[3] & 255) == 0xb8);
        }
        int a = bytes[0] & 255, b = bytes[1] & 255, c = bytes[2] & 255;
        return a != 0 && a != 10 && a != 127 && a < 224
                && !(a == 169 && b == 254) && !(a == 172 && b >= 16 && b <= 31)
                && !(a == 192 && b == 168) && !(a == 100 && b >= 64 && b <= 127)
                && !(a == 198 && (b == 18 || b == 19))
                && !(a == 192 && b == 0 && (c == 0 || c == 2))
                && !(a == 198 && b == 51 && c == 100) && !(a == 203 && b == 0 && c == 113);
    }

    public MultipartFile fetch(String input) {
        URI uri = validateUrl(input);
        int limit = (int) Math.min(properties.maxBytesFor(MediaType.IMAGE), 20L * 1024 * 1024);
        DnsResolver resolver = new DnsResolver() {
            @Override public InetAddress[] resolve(String host) throws UnknownHostException {
                InetAddress[] addresses = SystemDefaultDnsResolver.INSTANCE.resolve(host);
                for (InetAddress address : addresses) {
                    if (!publicAddress(address)) { throw new UnknownHostException("Public image host required"); }
                }
                return addresses;
            }
            @Override public String resolveCanonicalHostname(String host) throws UnknownHostException {
                resolve(host);
                return host;
            }
        };
        var connections = PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(resolver)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofSeconds(3)).setSocketTimeout(Timeout.ofSeconds(8)).build()).build();
        try (var client = HttpClients.custom().setConnectionManager(connections).disableRedirectHandling()
                .disableCookieManagement().disableAutomaticRetries()
                .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofSeconds(8)).build()).build()) {
            long deadline = System.nanoTime() + Duration.ofSeconds(25).toNanos();
            for (int redirect = 0; redirect <= 3; redirect++) {
                // Literal IPs may bypass the client's DNS resolver.
                String host = uri.getHost().replace("[", "").replace("]", "");
                if ((host.contains(":") || host.matches("[0-9.]+")) && !publicAddress(InetAddress.getByName(host))) {
                    throw failure();
                }
                try (var response = client.executeOpen(null, new HttpGet(uri), null)) {
                    int status = response.getCode();
                    if (status >= 300 && status < 400 && response.getFirstHeader("Location") != null) {
                        uri = validateUrl(uri.resolve(response.getFirstHeader("Location").getValue()).toString());
                        continue;
                    }
                    if (status != 200 || response.getEntity() == null || response.getEntity().getContentLength() > limit) {
                        throw failure();
                    }
                    String mime = response.getEntity().getContentType();
                    mime = mime == null ? "" : mime.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
                    String extension = switch (mime) {
                        case "image/png" -> "png"; case "image/jpeg" -> "jpg";
                        case "image/webp" -> "webp"; case "image/gif" -> "gif";
                        case "image/bmp" -> "bmp"; case "image/tiff" -> "tiff";
                        case "image/avif" -> "avif"; default -> throw failure();
                    };
                    try (InputStream stream = response.getEntity().getContent(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = stream.read(buffer)) != -1) {
                            if (bytes.size() + count > limit || System.nanoTime() > deadline) { throw failure(); }
                            bytes.write(buffer, 0, count);
                        }
                        return new ImportedImage(bytes.toByteArray(), extension, mime);
                    }
                }
            }
        } catch (IOException ex) { throw failure(); }
        throw failure();
    }

    private static ApiException failure() {
        return new ApiException("MEDIA_IMAGE_IMPORT_FAILED", "图片无法导入，请检查图片地址，或将图片保存后直接上传。", 400);
    }

    private record ImportedImage(byte[] bytes, String extension, String mime) implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return "imported-image." + extension; }
        @Override public String getContentType() { return mime; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes.clone(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), bytes); }
    }
}
