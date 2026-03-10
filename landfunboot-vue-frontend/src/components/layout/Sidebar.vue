<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
const route = useRoute()
const { menus } = storeToRefs(useAuthStore())

/** Normalize backend tree to groups with items (DIR nodes as groups, their MENU children as items) */
const visibleMenus = computed(() => {
    const list = menus.value || []
    return list
        .filter((n) => n.type === 'DIR' && n.children?.length)
        .map((group) => ({
            title: group.name,
            items: (group.children || [])
                .filter((c) => c.type === 'MENU' && c.path)
                .map((c) => {
                    const path = c.path ?? ''
                    const href = path.startsWith('/') ? path : `/${path}`
                    return {
                        title: c.name,
                        href,
                        icon: (c.icon || 'default').toLowerCase(),
                        permission: c.permission ?? undefined
                    }
                })
        }))
        .filter((g) => g.items.length > 0)
})
</script>

<template>
  <div class="hidden md:flex flex-col min-h-screen w-[220px] lg:w-[240px] flex-shrink-0"
       style="background: hsl(var(--sidebar-bg)); border-right: 1px solid hsl(var(--sidebar-border));">

    <!-- Logo -->
    <div class="flex h-[60px] items-center px-5 flex-shrink-0"
         style="border-bottom: 1px solid hsl(var(--sidebar-border));">
      <a href="/" class="flex items-center gap-2.5">
        <div class="w-8 h-8 rounded-lg flex items-center justify-center font-bold text-base text-white flex-shrink-0"
             style="background: hsl(var(--primary));">L</div>
        <span class="font-semibold text-[15px] tracking-wide"
              style="color: hsl(var(--sidebar-text));">Landfun Boot</span>
      </a>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto py-3">
      <!-- Welcome Home Link (always visible) -->
      <div class="px-2 mb-1">
        <router-link
          to="/welcome"
          class="flex items-center gap-2.5 px-3 py-[9px] rounded-md text-[13.5px] font-[450] transition-all duration-150"
          :style="route.path === '/welcome'
            ? 'background: hsl(var(--primary)); color: white;'
            : 'color: hsl(var(--sidebar-text));'"
          @mouseover="(e: MouseEvent) => { if (route.path !== '/welcome') (e.currentTarget as HTMLElement).style.background = 'hsl(var(--sidebar-hover))' }"
          @mouseleave="(e: MouseEvent) => { if (route.path !== '/welcome') (e.currentTarget as HTMLElement).style.background = '' }"
        >
          <svg class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          <span>工作台</span>
        </router-link>
      </div>
      <template v-for="group in visibleMenus" :key="group.title">
       <div class="mb-2">
        <!-- Group Label -->
        <div class="px-5 py-2 text-[11px] font-semibold uppercase tracking-[0.7px]"
             style="color: hsl(var(--sidebar-text-muted));">
          {{ group.title }}
        </div>
        <!-- Items -->
        <div class="px-2 grid gap-0.5">
          <template v-for="item in group.items" :key="String(item.href)">
          <router-link
            :to="item.href as string"
            class="flex items-center gap-2.5 px-3 py-[9px] rounded-md text-[13.5px] font-[450] transition-all duration-150"
            :style="route.path === item.href
              ? 'background: hsl(var(--primary)); color: white;'
              : 'color: hsl(var(--sidebar-text));'"
            @mouseover="(e: MouseEvent) => { if (route.path !== item.href) (e.currentTarget as HTMLElement).style.background = 'hsl(var(--sidebar-hover))' }"
            @mouseleave="(e: MouseEvent) => { if (route.path !== item.href) (e.currentTarget as HTMLElement).style.background = '' }"
          >
            <!-- User icon -->
            <svg v-if="item.icon === 'user'" class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <!-- Role icon -->
            <svg v-else-if="item.icon === 'role'" class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <!-- Dept icon -->
            <svg v-else-if="item.icon === 'dept'" class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            <!-- Menu icon -->
            <svg v-else-if="item.icon === 'menu'" class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
            <!-- Monitor icon -->
            <svg v-else-if="item.icon === 'monitor'" class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
            <!-- Online users icon -->
            <svg v-else-if="item.icon === 'online'" class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><circle cx="19" cy="11" r="2"/><path d="M23 21v-1a4 4 0 0 0-3-3.85"/></svg>
            <!-- Message icon -->
            <svg v-else-if="item.icon === 'message'" class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            <!-- List icon (for 全部消息) -->
            <svg v-else-if="item.icon === 'list'" class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
            <!-- Settings icon -->
            <svg v-else-if="item.icon === 'settings'" class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
            <!-- Default icon -->
            <svg v-else class="w-4 h-4 flex-shrink-0 opacity-80" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/></svg>

            <span>{{ item.title }}</span>
          </router-link>
          </template>
        </div>
       </div>
      </template>
    </nav>

    <!-- Footer -->
    <div class="px-2 py-3 flex-shrink-0" style="border-top: 1px solid hsl(var(--sidebar-border));">
      <div class="flex items-center gap-2.5 px-3 py-[9px] rounded-md text-[13px] cursor-pointer transition-all duration-150"
           style="color: hsl(var(--sidebar-text-muted));"
           @mouseover="(e: MouseEvent) => (e.currentTarget as HTMLElement).style.color = 'hsl(var(--sidebar-text))'"
           @mouseleave="(e: MouseEvent) => (e.currentTarget as HTMLElement).style.color = 'hsl(var(--sidebar-text-muted))'">
        <svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.08"/></svg>
        系统设置
      </div>
    </div>
  </div>
</template>
