package org.j2ra.minierp.gateway.web;

import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * The only way the gateway writes a log line about a request.
 *
 * <p>MDC is thread-local, and a reactive chain changes threads. So we do not keep the id in the MDC.
 * We put it in only around one log call, on the same thread, and remove it at once. Then the log pattern
 * of the services ({@code %X{correlationId}}) works for our lines. Lines of libraries (Spring, Netty) have no id (ADR-010).
 */
final class CorrelationLog {

    static final String MDC_KEY = "correlationId";

    private CorrelationLog() {
    }

    static void info(Logger log, String correlationId, String message, Object... args) {
        MDC.put(MDC_KEY, correlationId);
        try {
            log.info(message, args);
        } finally {
            // The thread serves other requests later. A left-over id would be in the wrong log lines.
            MDC.remove(MDC_KEY);
        }
    }

    /** The last argument may be an exception: then SLF4J writes its stack trace to the log (never to the response). */
    static void error(Logger log, String correlationId, String message, Object... args) {
        MDC.put(MDC_KEY, correlationId);
        try {
            log.error(message, args);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
