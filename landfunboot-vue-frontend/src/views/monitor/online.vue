<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { userApi } from '@/api/user'
import { useToast } from '@/components/ui/toast/use-toast'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

interface OnlineUser {
  userId: number
  username: string
  email: string
  isActive: boolean
}

import { useAuth } from '@/composables/useAuth'

const { toast } = useToast()
const { hasPermission } = useAuth()
const users = ref<OnlineUser[]>([])
const loading = ref(false)
const kickoutConfirmOpen = ref(false)
const kickingUserId = ref<number | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    users.value = await userApi.onlineUsers()
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const handleKickoutClick = (userId: number) => {
  kickingUserId.value = userId
  kickoutConfirmOpen.value = true
}

const confirmKickout = async () => {
  if (!kickingUserId.value) return
  try {
    await userApi.kickout(kickingUserId.value)
    toast({ title: '下线成功', description: '用户已被强制下线' })
    fetchData()
  } catch (e) {}
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-5">
      <div>
        <h2 class="text-xl font-semibold" style="color: hsl(var(--foreground));">在线用户</h2>
        <p class="text-sm mt-0.5" style="color: hsl(var(--muted-foreground));">当前登录系统的用户列表</p>
      </div>
      <button
        class="flex items-center gap-2 text-sm px-3 py-1.5 rounded-md transition-colors"
        style="background: hsl(var(--secondary)); color: hsl(var(--secondary-foreground)); border: 1px solid hsl(var(--border));"
        @click="fetchData"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-.08-8.76"/></svg>
        刷新
      </button>
    </div>

    <div class="rounded-lg overflow-hidden" style="background: white; border: 1px solid hsl(var(--border));">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center h-32 text-sm" style="color: hsl(var(--muted-foreground));">
        <div class="w-5 h-5 border-2 border-t-transparent rounded-full animate-spin mr-2" style="border-color: hsl(var(--primary)); border-top-color: transparent;"></div>
        加载中...
      </div>

      <table v-else class="w-full" style="border-collapse: collapse; font-size: 13.5px;">
        <thead style="background: hsl(var(--muted));">
          <tr>
            <th class="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide" style="color: hsl(var(--muted-foreground)); border-bottom: 1px solid hsl(var(--border));">用户名</th>
            <th class="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide" style="color: hsl(var(--muted-foreground)); border-bottom: 1px solid hsl(var(--border));">邮箱</th>
            <th class="text-left px-4 py-3 font-semibold text-xs uppercase tracking-wide" style="color: hsl(var(--muted-foreground)); border-bottom: 1px solid hsl(var(--border));">账号状态</th>
            <th class="text-right px-4 py-3 font-semibold text-xs uppercase tracking-wide" style="color: hsl(var(--muted-foreground)); border-bottom: 1px solid hsl(var(--border));">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="users.length === 0">
            <td colspan="4" class="text-center py-10 text-sm" style="color: hsl(var(--muted-foreground));">暂无在线用户</td>
          </tr>
          <tr v-for="u in users" :key="u.userId" style="border-bottom: 1px solid hsl(210 25% 96%);" class="hover-row">
            <td class="px-4 py-3 font-medium" style="color: hsl(var(--foreground));">
              <div class="flex items-center gap-2">
                <div class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold text-white" style="background: hsl(var(--primary));">
                  {{ u.username?.charAt(0).toUpperCase() }}
                </div>
                {{ u.username }}
              </div>
            </td>
            <td class="px-4 py-3" style="color: hsl(var(--muted-foreground));">{{ u.email }}</td>
            <td class="px-4 py-3">
              <span v-if="u.isActive" class="px-2 py-0.5 rounded-full text-[11px] font-medium bg-emerald-100 text-emerald-700">活跃中</span>
              <span v-else class="px-2 py-0.5 rounded-full text-[11px] font-medium bg-gray-100 text-gray-500">已禁用</span>
            </td>
            <td class="px-4 py-3 text-right">
              <button
                v-if="hasPermission('sys:user:kickout')"
                class="text-xs px-3 py-1 rounded-md transition-all font-medium"
                style="background: hsl(0 84% 96%); color: hsl(0 60% 35%); border: 1px solid hsl(0 84% 88%);"
                @click="handleKickoutClick(u.userId)"
              >
                强制下线
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <AlertDialog v-model:open="kickoutConfirmOpen">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>确定要强制该用户下线吗？</AlertDialogTitle>
          <AlertDialogDescription>
            该用户的登录会话将立即失效，用户需重新登录才能继续使用系统。
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>取消</AlertDialogCancel>
          <AlertDialogAction @click="confirmKickout" class="bg-red-500 hover:bg-red-600">确认下线</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

<style scoped>
.hover-row:hover td {
  background: hsl(210 30% 98%);
}
.hover-row:last-child td {
  border-bottom: none;
}
</style>
