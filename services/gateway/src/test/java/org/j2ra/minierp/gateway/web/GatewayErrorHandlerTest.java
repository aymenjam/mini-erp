package org.j2ra.minierp.gateway.web;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-21: the status comes from the type of the error (and of its causes), never from its text. */
class GatewayErrorHandlerTest {

    @Test
    @DisplayName("AC-21: connection refused (also wrapped) gives 503, and an unknown host too")
    void unreachable_gives_503() {
        assertThat(GatewayErrorHandler.statusFor(new ConnectException("Connection refused"))).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(GatewayErrorHandler.statusFor(new IllegalStateException("wrapper", new ConnectException("refused"))))
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(GatewayErrorHandler.statusFor(new UnknownHostException("customers"))).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("AC-21: a connect timeout gives 504, not 503 (Netty's ConnectTimeoutException is also a ConnectException)")
    void connect_timeout_gives_504() {
        assertThat(GatewayErrorHandler.statusFor(new ConnectTimeoutException("connection timed out"))).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    @DisplayName("AC-21: a response timeout gives 504 (as the gateway reports it, and as raw exceptions)")
    void response_timeout_gives_504() {
        assertThat(GatewayErrorHandler.statusFor(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "took too long", new TimeoutException())))
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(GatewayErrorHandler.statusFor(new TimeoutException("slow"))).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(GatewayErrorHandler.statusFor(ReadTimeoutException.INSTANCE)).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    @DisplayName("AC-21: a client error of Spring (404, 405) keeps its status")
    void client_errors_keep_their_status() {
        assertThat(GatewayErrorHandler.statusFor(new ResponseStatusException(HttpStatus.NOT_FOUND))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(GatewayErrorHandler.statusFor(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED))).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("AC-21: everything else gives 500, also a 5xx status that we do not know")
    void everything_else_gives_500() {
        assertThat(GatewayErrorHandler.statusFor(new RuntimeException("boom"))).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(GatewayErrorHandler.statusFor(new ResponseStatusException(HttpStatus.BAD_GATEWAY))).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("AC-21: a cause chain that points to itself does not loop for ever")
    void cause_loop_ends() {
        class Loop extends RuntimeException {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        }
        assertThat(GatewayErrorHandler.statusFor(new Loop())).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
