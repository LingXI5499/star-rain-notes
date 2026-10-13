package com.starrainnotes.boot;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.catalina.valves.ValveBase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Exercise the actual Tomcat valve, never a homegrown first-header IP parser. */
class TrustedProxyTest {
    private static final String TRUSTED = "127\\.0\\.0\\.1|0:0:0:0:0:0:0:1|::1";

    /** Distinct client addresses survive the trusted local proxy. */
    @Test void isolatesTwoClients() throws Exception {
        assertThat(client("127.0.0.1", "198.51.100.1")).isEqualTo("198.51.100.1");
        assertThat(client("127.0.0.1", "198.51.100.2")).isEqualTo("198.51.100.2");
    }

    /** A public or private untrusted peer cannot forge its source. */
    @Test void ignoresForgedHeadersFromUntrustedPeers() throws Exception {
        assertThat(client("198.51.100.1", "203.0.113.99")).isEqualTo("198.51.100.1");
        assertThat(client("10.0.0.1", "203.0.113.99")).isEqualTo("10.0.0.1");
    }

    /** With an appended client source, the attacker-controlled leftmost value is ignored. */
    @Test void walksForwardedChainFromTrustedEnd() throws Exception {
        assertThat(client("127.0.0.1", "203.0.113.99, 198.51.100.1")).isEqualTo("198.51.100.1");
        assertThat(client("127.0.0.1", null)).isEqualTo("127.0.0.1");
    }

    /** Invoke the same valve used by Spring Boot's NATIVE strategy. */
    private String client(String peer, String forwarded) throws Exception {
        RemoteIpValve valve = new RemoteIpValve();
        valve.setInternalProxies(TRUSTED);
        valve.setRemoteIpHeader("x-forwarded-for");
        AtomicReference<String> observed = new AtomicReference<>();
        valve.setNext(new ValveBase() {
            @Override public void invoke(Request request, Response response) {
                observed.set(request.getRemoteAddr());
            }
        });
        Request request = new Request(new Connector());
        org.apache.coyote.Request coyote = new org.apache.coyote.Request();
        request.setCoyoteRequest(coyote);
        request.setRemoteAddr(peer);
        request.setRemoteHost(peer);
        if (forwarded != null) coyote.getMimeHeaders().addValue("x-forwarded-for").setString(forwarded);
        Response response = new Response();
        response.setCoyoteResponse(new org.apache.coyote.Response());
        valve.invoke(request, response);
        return observed.get();
    }
}
