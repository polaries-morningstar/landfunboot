package com.landfun.boot.modules.monitor;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Server info snapshot")
public class ServerInfo {

    private JvmInfo jvm;
    private CpuInfo cpu;
    private MemInfo mem;
    private List<DiskInfo> disks;
    private RuntimeInfo runtime;

    // ---- JVM ----

    @Data
    @Builder
    @Schema(description = "JVM information")
    public static class JvmInfo {
        @Schema(description = "Java version")
        private String version;
        @Schema(description = "JVM total memory (bytes)")
        private long totalMemory;
        @Schema(description = "JVM used memory (bytes)")
        private long usedMemory;
        @Schema(description = "JVM free memory (bytes)")
        private long freeMemory;
        @Schema(description = "JVM max memory (bytes)")
        private long maxMemory;
        @Schema(description = "JVM used memory (MB)")
        private long usedMemoryMb;
        @Schema(description = "JVM max memory (MB)")
        private long maxMemoryMb;
    }

    // ---- CPU ----

    @Data
    @Builder
    @Schema(description = "CPU information")
    public static class CpuInfo {
        @Schema(description = "Logical CPU cores")
        private int cores;
        @Schema(description = "System load average (-1 if unavailable)")
        private double loadAverage;
    }

    // ---- Memory ----

    @Data
    @Builder
    @Schema(description = "System memory information")
    public static class MemInfo {
        @Schema(description = "Total physical memory (bytes)")
        private long total;
        @Schema(description = "Free physical memory (bytes)")
        private long free;
        @Schema(description = "Used physical memory (bytes)")
        private long used;
        @Schema(description = "Total physical memory (MB)")
        private long totalMb;
        @Schema(description = "Used physical memory (MB)")
        private long usedMb;
    }

    // ---- Disk ----

    @Data
    @Builder
    @Schema(description = "Disk partition information")
    public static class DiskInfo {
        @Schema(description = "Partition path")
        private String path;
        @Schema(description = "Total space (bytes)")
        private long total;
        @Schema(description = "Free space (bytes)")
        private long free;
        @Schema(description = "Used space (bytes)")
        private long used;
        @Schema(description = "Total space (GB)")
        private long totalGb;
        @Schema(description = "Used space (GB)")
        private long usedGb;
        @Schema(description = "Usage percentage (%)")
        private double usedPercent;
    }

    // ---- Runtime ----

    @Data
    @Builder
    @Schema(description = "JVM runtime information")
    public static class RuntimeInfo {
        @Schema(description = "Start timestamp (ms)")
        private long startTime;
        @Schema(description = "Start time (formatted)")
        private String startTimeFormatted;
        @Schema(description = "Uptime (ms)")
        private long uptime;
        @Schema(description = "Uptime (formatted)")
        private String uptimeFormatted;
    }
}
