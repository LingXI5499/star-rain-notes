package com.starrainnotes.media.remote;

import java.net.InetAddress;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class RemoteImageFetcherTest {
    @ParameterizedTest
    @ValueSource(strings={"127.0.0.1", "0.0.0.0", "10.0.0.1", "172.16.0.1", "192.168.1.1", "169.254.169.254", "100.64.0.1", "198.18.0.1", "224.0.0.1", "::1", "fd00::1", "fe80::1", "2001:db8::1", "::ffff:127.0.0.1"})
    void rejectsNonPublicAddresses(String value) throws Exception {
        assertFalse(RemoteImageFetcher.publicAddress(InetAddress.getByName(value)));
    }
    @ParameterizedTest @ValueSource(strings={"8.8.8.8", "1.1.1.1", "2606:4700:4700::1111"})
    void acceptsPublicAddresses(String value) throws Exception {
        assertTrue(RemoteImageFetcher.publicAddress(InetAddress.getByName(value)));
    }
    @ParameterizedTest @ValueSource(strings={"file:///tmp/a.png", "ftp://example.org/a.png", "http://u:p@example.org/a.png", "https://example.org:8080/a.png", "http://example.org/a#secret", "data:image/png;base64,AA"})
    void rejectsUnsupportedUrls(String value) {
        assertThrows(RuntimeException.class, () -> RemoteImageFetcher.validateUrl(value));
    }
}
