<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { request } from '@/lib/request'

interface JvmInfo {
  version: string
  totalMemory: number
  usedMemory: number
  freeMemory: number
  maxMemory: number
  usedMemoryMb: number
  maxMemoryMb: number
}

interface CpuInfo {
  cores: number
  loadAverage: number
}

interface MemInfo {
  total: number
  free: number
  used: number
  totalMb: number
  usedMb: number
}

interface DiskInfo {
  path: string
  total: number
  free: number
  used: number
  totalGb: number
  usedGb: number
  usedPercent: number
}

interface RuntimeInfo {
  startTime: number
  startTimeFormatted: string
  uptime: number
  uptimeFormatted: string
}

interface ServerInfo {
  jvm: JvmInfo
  cpu: CpuInfo
  mem: MemInfo
  disks: DiskInfo[]
  runtime: RuntimeInfo
}

const info = ref<ServerInfo | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
let eventSource: EventSource | null = null

const formatBytes = (bytes: number) => {
  if (bytes < 0) return 'N/A'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

const usagePercent = (used: number, total: number) => {
  if (!total || total <= 0) return 0
  return Math.round((used / total) * 100)
}

const startSseStream = () => {
  const token = localStorage.getItem('token')
  const url = `/api/sys/monitor/stream${token ? `?token=${encodeURIComponent(token)}` : ''}`
  eventSource = new EventSource(url)
  eventSource.addEventListener('server-info', (e) => {
    try {
      info.value = JSON.parse(e.data)
      loading.value = false
      error.value = null
    } catch (err) {
      console.error('Parse error', err)
    }
  })
  eventSource.onerror = () => {
    // Keep internal error tracking subtle for SSE retries
    console.warn('SSE connection disrupted, retrying...')
  }
}

const handleRetry = () => {
  window.location.reload()
}

onMounted(async () => {
  try {
    const data = await request<ServerInfo>('/api/sys/monitor/server')
    info.value = data
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
  startSseStream()
})

onUnmounted(() => {
  eventSource?.close()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold" style="color: hsl(var(--foreground));">服务监控</h2>
        <p class="text-sm mt-0.5" style="color: hsl(var(--muted-foreground));">实时监控服务器 CPU、内存、磁盘及 JVM 状态</p>
      </div>
      <div v-if="!loading && !error" class="flex items-center gap-2 text-xs px-3 py-1.5 rounded-full"
           style="background: hsl(142 71% 95%); color: hsl(142 71% 28%);">
        <span class="w-1.5 h-1.5 rounded-full inline-block animate-pulse" style="background: hsl(142 71% 45%);"></span>
        实时更新中
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center h-48 rounded-lg bg-white border border-gray-100 shadow-sm">
      <div class="text-center">
        <div class="w-8 h-8 border-2 border-t-transparent rounded-full animate-spin mx-auto mb-2" style="border-color: hsl(var(--primary));"></div>
        <p class="text-sm" style="color: hsl(var(--muted-foreground));">获取系统资源信息...</p>
      </div>
    </div>

    <template v-else-if="info">
      <!-- Stats Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- CPU Card -->
        <div class="bg-white p-5 rounded-lg border border-gray-100 shadow-sm relative overflow-hidden group hover:shadow-md transition-shadow">
          <div class="absolute top-0 left-0 right-0 h-1 bg-blue-500"></div>
          <div class="flex justify-between items-start mb-4">
            <span class="text-xs font-bold uppercase text-gray-400 tracking-wider">CPU 使用率</span>
            <div class="p-1.5 bg-blue-50 rounded-md text-blue-600">
              <svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="4" width="16" height="16" rx="2" ry="2"/><rect x="9" y="9" width="6" height="6"/><line x1="9" y1="1" x2="9" y2="4"/><line x1="15" y1="1" x2="15" y2="4"/><line x1="9" y1="20" x2="9" y2="23"/><line x1="15" y1="20" x2="15" y2="23"/><line x1="20" y1="9" x2="23" y2="9"/><line x1="20" y1="15" x2="23" y2="15"/><line x1="1" y1="9" x2="4" y2="9"/><line x1="1" y1="15" x2="4" y2="15"/></svg>
            </div>
          </div>
          <div class="text-3xl font-bold text-gray-800 mb-2">{{ info.cpu.loadAverage >= 0 ? info.cpu.loadAverage.toFixed(1) + '%' : 'N/A' }}</div>
          <div class="w-full bg-gray-100 rounded-full h-1.5 mb-2">
            <div class="h-1.5 rounded-full bg-blue-500 transition-all duration-500" :style="{ width: (info.cpu.loadAverage >= 0 ? info.cpu.loadAverage : 0) + '%' }"></div>
          </div>
          <p class="text-[11px] text-gray-500">{{ info.cpu.cores }} 线程处理能力</p>
        </div>

        <!-- RAM Card -->
        <div class="bg-white p-5 rounded-lg border border-gray-100 shadow-sm relative overflow-hidden group hover:shadow-md transition-shadow">
          <div class="absolute top-0 left-0 right-0 h-1 bg-emerald-500"></div>
          <div class="flex justify-between items-start mb-4">
            <span class="text-xs font-bold uppercase text-gray-400 tracking-wider">系统内存</span>
            <div class="p-1.5 bg-emerald-50 rounded-md text-emerald-600">
              <svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 19v-3"/><path d="M10 19v-3"/><path d="M14 19v-3"/><path d="M18 19v-3"/><path d="M8 11V9"/><path d="M16 11V9"/><rect x="3" y="11" width="18" height="8" rx="2"/><path d="M3 15h18"/></svg>
            </div>
          </div>
          <div class="text-3xl font-bold text-gray-800 mb-2">{{ usagePercent(info.mem.used, info.mem.total) }}%</div>
          <div class="w-full bg-gray-100 rounded-full h-1.5 mb-2">
            <div class="h-1.5 rounded-full bg-emerald-500 transition-all duration-500" :style="{ width: usagePercent(info.mem.used, info.mem.total) + '%' }"></div>
          </div>
          <p class="text-[11px] text-gray-500">{{ formatBytes(info.mem.used) }} / {{ formatBytes(info.mem.total) }}</p>
        </div>

        <!-- JVM Card -->
        <div class="bg-white p-5 rounded-lg border border-gray-100 shadow-sm relative overflow-hidden group hover:shadow-md transition-shadow">
          <div class="absolute top-0 left-0 right-0 h-1 bg-purple-500"></div>
          <div class="flex justify-between items-start mb-4">
            <span class="text-xs font-bold uppercase text-gray-400 tracking-wider">JVM 堆内存</span>
            <div class="p-1.5 bg-purple-50 rounded-md text-purple-600">
              <svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v10"/><path d="M18.4 4.6a9 9 0 1 1-12.8 0"/></svg>
            </div>
          </div>
          <div class="text-3xl font-bold text-gray-800 mb-2">{{ usagePercent(info.jvm.usedMemory, info.jvm.totalMemory) }}%</div>
          <div class="w-full bg-gray-100 rounded-full h-1.5 mb-2">
            <div class="h-1.5 rounded-full bg-purple-500 transition-all duration-500" :style="{ width: usagePercent(info.jvm.usedMemory, info.jvm.totalMemory) + '%' }"></div>
          </div>
          <p class="text-[11px] text-gray-500">{{ formatBytes(info.jvm.usedMemory) }} / {{ formatBytes(info.jvm.totalMemory) }}</p>
        </div>

        <!-- Uptime Card -->
        <div class="bg-white p-5 rounded-lg border border-gray-100 shadow-sm relative overflow-hidden group hover:shadow-md transition-shadow">
          <div class="absolute top-0 left-0 right-0 h-1 bg-orange-500"></div>
          <div class="flex justify-between items-start mb-4">
            <span class="text-xs font-bold uppercase text-gray-400 tracking-wider">运行时长</span>
            <div class="p-1.5 bg-orange-50 rounded-md text-orange-600">
              <svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            </div>
          </div>
          <div class="text-xl font-bold text-gray-800 leading-tight mb-2">{{ info.runtime.uptimeFormatted }}</div>
          <p class="text-[11px] text-gray-500 mt-auto">自 {{ info.runtime.startTimeFormatted }}</p>
        </div>
      </div>

      <!-- Detail Sections -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Java Info -->
        <div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
          <div class="px-5 py-4 bg-gray-50/50 border-b border-gray-100 flex items-center justify-between">
            <h3 class="text-sm font-bold text-gray-700 flex items-center gap-2">
              <svg class="w-4 h-4 text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
              JVM 信息
            </h3>
          </div>
          <div class="p-5 space-y-4">
            <div class="flex items-center justify-between group">
              <span class="text-sm text-gray-500">Java 版本</span>
              <span class="px-2 py-0.5 bg-blue-50 text-blue-700 rounded text-xs font-mono font-medium">{{ info.jvm.version }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">最大可用内存</span>
              <span class="text-sm font-semibold text-gray-700">{{ formatBytes(info.jvm.maxMemory) }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">总分配内存</span>
              <span class="text-sm font-semibold text-gray-700">{{ formatBytes(info.jvm.totalMemory) }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">已使用内存</span>
              <span class="text-sm font-semibold text-gray-700 text-purple-600">{{ formatBytes(info.jvm.usedMemory) }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-500">剩余可用内存</span>
              <span class="text-sm font-semibold text-emerald-600">{{ formatBytes(info.jvm.freeMemory) }}</span>
            </div>
          </div>
        </div>

        <!-- Disk Info -->
        <div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden text-sm">
          <div class="px-5 py-4 bg-gray-50/50 border-b border-gray-100 flex items-center justify-between">
            <h3 class="text-sm font-bold text-gray-700 flex items-center gap-2">
              <svg class="w-4 h-4 text-orange-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
              磁盘状态
            </h3>
          </div>
          <div class="max-h-[220px] overflow-y-auto">
            <table class="w-full text-left">
              <thead class="text-[10px] uppercase text-gray-400 bg-gray-50/30 sticky top-0">
                <tr>
                  <th class="px-5 py-3 font-bold">挂载路径</th>
                  <th class="px-3 py-3 font-bold">总大小</th>
                  <th class="px-3 py-3 font-bold">已用</th>
                  <th class="px-5 py-3 font-bold text-right">使用率</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="disk in info.disks" :key="disk.path" class="hover:bg-gray-50 transition-colors">
                  <td class="px-5 py-4 font-mono text-xs text-gray-600 truncate max-w-[140px]" :title="disk.path">{{ disk.path }}</td>
                  <td class="px-3 py-4 text-gray-600">{{ disk.totalGb }} GB</td>
                  <td class="px-3 py-4 text-gray-600">{{ disk.usedGb }} GB</td>
                  <td class="px-5 py-4">
                    <div class="flex items-center justify-end gap-2">
                      <span class="text-xs font-medium" :class="disk.usedPercent > 90 ? 'text-red-500' : 'text-gray-500'">{{ disk.usedPercent }}%</span>
                      <div class="w-12 bg-gray-100 rounded-full h-1.5 overflow-hidden">
                        <div class="h-1.5 rounded-full transition-all" :class="disk.usedPercent > 90 ? 'bg-red-500' : 'bg-orange-400'" :style="{ width: disk.usedPercent + '%' }"></div>
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </template>

    <!-- Error -->
    <div v-else-if="error" class="rounded-xl p-8 text-center bg-red-50 border border-red-100">
      <div class="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4 text-red-600">
        <svg class="w-6 h-6" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
      </div>
      <h3 class="text-lg font-bold text-red-800 mb-1">监控数据获取失败</h3>
      <p class="text-sm text-red-600 mb-6">{{ error }}</p>
      <button @click="handleRetry" class="px-5 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 transition-colors shadow-sm">
        重试连接
      </button>
    </div>
  </div>
</template>
