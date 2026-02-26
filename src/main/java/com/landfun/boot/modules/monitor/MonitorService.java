package com.landfun.boot.modules.monitor;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class MonitorService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ServerInfo serverInfo() {
        return ServerInfo.builder()
                .jvm(buildJvmInfo())
                .cpu(buildCpuInfo())
                .mem(buildMemInfo())
                .disks(buildDiskInfo())
                .runtime(buildRuntimeInfo())
                .build();
    }

    // ---- JVM ----

    private ServerInfo.JvmInfo buildJvmInfo() {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long max = rt.maxMemory();
        long used = total - free;
        return ServerInfo.JvmInfo.builder()
                .version(System.getProperty("java.version"))
                .totalMemory(total)
                .usedMemory(used)
                .freeMemory(free)
                .maxMemory(max)
                .usedMemoryMb(used / 1024 / 1024)
                .maxMemoryMb(max / 1024 / 1024)
                .build();
    }

    // ---- CPU ----

    private ServerInfo.CpuInfo buildCpuInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        int cores = os.getAvailableProcessors();
        double load = os.getSystemLoadAverage();

        // On Windows, getSystemLoadAverage() returns -1; try com.sun API if available
        if (load < 0 && os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            load = sunOs.getCpuLoad() * 100;
        }
        return ServerInfo.CpuInfo.builder()
                .cores(cores)
                .loadAverage(Math.round(load * 100.0) / 100.0)
                .build();
    }

    // ---- System Memory ----

    private ServerInfo.MemInfo buildMemInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        long total = -1, free = -1;
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            total = sunOs.getTotalMemorySize();
            free = sunOs.getFreeMemorySize();
        }
        long used = (total >= 0 && free >= 0) ? total - free : -1;
        return ServerInfo.MemInfo.builder()
                .total(total)
                .free(free)
                .used(used)
                .totalMb(total > 0 ? total / 1024 / 1024 : -1)
                .usedMb(used > 0 ? used / 1024 / 1024 : -1)
                .build();
    }

    // ---- Disk ----

    private List<ServerInfo.DiskInfo> buildDiskInfo() {
        List<ServerInfo.DiskInfo> list = new ArrayList<>();
        for (File root : File.listRoots()) {
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            long used = total - free;
            double usedPercent = total > 0 ? Math.round(used * 10000.0 / total) / 100.0 : 0;
            list.add(ServerInfo.DiskInfo.builder()
                    .path(root.getAbsolutePath())
                    .total(total)
                    .free(free)
                    .used(used)
                    .totalGb(total / 1024 / 1024 / 1024)
                    .usedGb(used / 1024 / 1024 / 1024)
                    .usedPercent(usedPercent)
                    .build());
        }
        return list;
    }

    // ---- Runtime ----

    private ServerInfo.RuntimeInfo buildRuntimeInfo() {
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        long startMs = rt.getStartTime();
        long uptimeMs = rt.getUptime();

        String startFormatted = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(startMs), ZoneId.systemDefault())
                .format(FORMATTER);

        long totalSeconds = uptimeMs / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String uptimeFormatted = String.format("%d天 %02d:%02d:%02d", days, hours, minutes, seconds);

        return ServerInfo.RuntimeInfo.builder()
                .startTime(startMs)
                .startTimeFormatted(startFormatted)
                .uptime(uptimeMs)
                .uptimeFormatted(uptimeFormatted)
                .build();
    }
}
