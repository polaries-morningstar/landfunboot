package com.landfun.boot.modules.monitor;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.R;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Monitor", description = "System Monitor APIs")
@RestController
@RequestMapping("/sys/monitor")
@RequiredArgsConstructor
public class MonitorController {

    /** Push interval */
    private static final Duration PUSH_INTERVAL = Duration.ofSeconds(1);

    /** SSE connection timeout: 10 minutes */
    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

    private final MonitorService monitorService;

    // ---- One-shot GET ----

    @Operation(summary = "Get Server Info (CPU / Memory / Disk / JVM)")
    @GetMapping("/server")
    @HasPermission("sys:monitor:info")
    public R<ServerInfo> serverInfo() {
        return R.ok(monitorService.serverInfo());
    }

    // ---- SSE stream (Java 21 virtual thread) ----

    @Operation(summary = "Server-Sent Events: push ServerInfo every second", description = "EventSource does not support custom headers. "
            + "Pass the JWT token via the ?token= query parameter.")
    @GetMapping(value = "/stream", produces = "text/event-stream")
    @HasPermission("sys:monitor:info")
    public SseEmitter stream(
            @Parameter(description = "JWT token (required for EventSource clients)") @RequestParam(required = false) String token) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // AtomicReference avoids the array-trick race condition:
        // the thread ref is set before any callback fires.
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        Thread vThread = Thread.ofVirtual().name("sse-monitor").start(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ServerInfo info = monitorService.serverInfo();
                    // Spring auto-serializes via its configured Jackson MessageConverter
                    emitter.send(SseEmitter.event()
                            .name("server-info")
                            .data(info, MediaType.APPLICATION_JSON));
                    Thread.sleep(PUSH_INTERVAL);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore interrupt flag
                log.debug("SSE virtual thread interrupted, closing emitter");
                emitter.complete();
            } catch (IOException e) {
                // Client disconnected mid-send
                log.debug("SSE client disconnected: {}", e.getMessage());
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.warn("SSE push error: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        threadRef.set(vThread);

        // Interrupt the virtual thread on any terminal event
        Runnable cancel = () -> {
            Thread t = threadRef.get();
            if (t != null)
                t.interrupt();
        };
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(e -> cancel.run());

        return emitter;
    }
}
