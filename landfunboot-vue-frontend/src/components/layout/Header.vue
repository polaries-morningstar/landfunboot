<script setup lang="ts">
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useRouter } from 'vue-router'
import { ref, computed, onMounted, onUnmounted } from 'vue'
import ChangeSelfPasswordDialog from './ChangeSelfPasswordDialog.vue'
import ProfileDialog from './ProfileDialog.vue'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { user, clearAuth, fetchUserInfo } = useAuth()
const changePasswordOpen = ref(false)
const profileOpen = ref(false)
const isFullscreen = ref(false)

const initials = computed(() => {
  const u = user.value
  if (!u?.username) return ''
  return u.username.slice(0, 2).toUpperCase()
})

const toggleFullscreen = () => {
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    document.documentElement.requestFullscreen()
  }
}

const onFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => document.addEventListener('fullscreenchange', onFullscreenChange))
onUnmounted(() => document.removeEventListener('fullscreenchange', onFullscreenChange))

const handleProfileSuccess = () => {
  fetchUserInfo()
}

const logout = () => {
    clearAuth()
    router.push('/login')
}
</script>

<template>
  <header class="sticky top-0 z-30 flex h-[60px] items-center gap-4 px-5 sm:px-6"
          style="background: white; border-bottom: 1px solid hsl(var(--border));">

    <!-- Breadcrumb / Mobile brand -->
    <div class="flex items-center gap-2 md:hidden">
      <div class="w-7 h-7 rounded-md flex items-center justify-center font-bold text-sm text-white flex-shrink-0"
           style="background: hsl(var(--primary));">L</div>
      <span class="font-semibold text-sm">Landfun Boot</span>
    </div>

    <!-- Desktop breadcrumb -->
    <nav class="hidden md:flex items-center gap-1.5 text-[13.5px]" style="color: hsl(var(--muted-foreground));">
      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
      <span>工作台</span>
      <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      <span class="font-medium" style="color: hsl(var(--foreground));">控制台</span>
    </nav>

    <!-- Right actions -->
    <div class="ml-auto flex items-center gap-1">
      <!-- Notification button -->
      <button class="w-8 h-8 rounded-md flex items-center justify-center transition-colors duration-150 border-0 bg-transparent cursor-pointer"
              style="color: hsl(var(--muted-foreground));"
              title="通知"
              @mouseover="(e: MouseEvent) => { (e.currentTarget as HTMLElement).style.background = 'hsl(var(--muted))'; (e.currentTarget as HTMLElement).style.color = 'hsl(var(--foreground))' }"
              @mouseleave="(e: MouseEvent) => { (e.currentTarget as HTMLElement).style.background = 'transparent'; (e.currentTarget as HTMLElement).style.color = 'hsl(var(--muted-foreground))' }">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
      </button>

      <!-- Fullscreen button -->
      <button class="w-8 h-8 rounded-md hidden md:flex items-center justify-center transition-colors duration-150 border-0 bg-transparent cursor-pointer"
              style="color: hsl(var(--muted-foreground));"
              :title="isFullscreen ? '退出全屏' : '全屏'"
              @click="toggleFullscreen"
              @mouseover="(e: MouseEvent) => { (e.currentTarget as HTMLElement).style.background = 'hsl(var(--muted))'; (e.currentTarget as HTMLElement).style.color = 'hsl(var(--foreground))' }"
              @mouseleave="(e: MouseEvent) => { (e.currentTarget as HTMLElement).style.background = 'transparent'; (e.currentTarget as HTMLElement).style.color = 'hsl(var(--muted-foreground))' }">
        <svg v-if="!isFullscreen" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3"/></svg>
      </button>

      <!-- Divider -->
      <div class="w-px h-5 mx-1" style="background: hsl(var(--border));"></div>

      <!-- User dropdown -->
      <DropdownMenu>
        <DropdownMenuTrigger as-child>
          <div class="flex items-center gap-2 cursor-pointer px-1 py-1 rounded-md transition-colors"
               @mouseover="(e: MouseEvent) => (e.currentTarget as HTMLElement).style.background = 'hsl(var(--muted))'"
               @mouseleave="(e: MouseEvent) => (e.currentTarget as HTMLElement).style.background = 'transparent'">
            <span class="text-[13.5px] font-medium hidden md:block" style="color: hsl(var(--foreground));">{{ user?.username }}</span>
            <Avatar class="h-8 w-8">
              <AvatarImage src="" />
              <AvatarFallback class="text-xs font-semibold text-white" style="background: hsl(var(--primary));">{{ initials }}</AvatarFallback>
            </Avatar>
          </div>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" class="w-48">
          <DropdownMenuLabel class="font-normal">
            <div class="flex flex-col gap-0.5">
              <span class="text-sm font-semibold">{{ user?.username }}</span>
              <span class="text-xs" style="color: hsl(var(--muted-foreground));">{{ user?.email }}</span>
            </div>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem @click="profileOpen = true">个人设置</DropdownMenuItem>
          <DropdownMenuItem @click="changePasswordOpen = true">修改密码</DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem @click="logout" class="text-destructive focus:text-destructive">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" class="mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            退出登录
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  </header>

  <ChangeSelfPasswordDialog v-model:open="changePasswordOpen" />
  <ProfileDialog v-model:open="profileOpen" @success="handleProfileSuccess" />
</template>
