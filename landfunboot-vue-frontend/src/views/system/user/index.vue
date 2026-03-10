<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
// import { Input } from '@/components/ui/input' // Optional for search later
// import {
//   DropdownMenu,
//   DropdownMenuContent,
//   DropdownMenuItem,
//   DropdownMenuLabel,
//   DropdownMenuTrigger,
// } from '@/components/ui/dropdown-menu'
import { userApi, type User, type BasePageQuery } from '@/api/user'
import { Pencil, Trash2, Plus, KeyRound } from 'lucide-vue-next'
import UserDialog from './UserDialog.vue'
import ResetPasswordDialog from './ResetPasswordDialog.vue'
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
import { useToast } from '@/components/ui/toast/use-toast'
import { useAuth } from '@/composables/useAuth'

const { toast } = useToast()
const { hasPermission } = useAuth()
const loading = ref(false)
const data = ref<User[]>([])
const total = ref(0)

// Dialog states
const dialogOpen = ref(false)
const editingUser = ref<User | null>(null)
const deleteConfirmOpen = ref(false)
const deletingUserId = ref<number | null>(null)
const resetPasswordDialogOpen = ref(false)
const resetPasswordUserId = ref<number | null>(null)

const query = reactive<BasePageQuery>({
    page: 1,
    size: 10,
    sort: 'createdTime,desc'
})

const fetchData = async () => {
    loading.value = true
    try {
        const res = await userApi.page(query)
        data.value = res.rows
        total.value = res.total
    } catch (e) {
        // Error handled in request interceptor
    } finally {
        loading.value = false
    }
}

const handlePageChange = (newPage: number) => {
    query.page = newPage
    fetchData()
}

const handleAdd = () => {
    editingUser.value = null
    dialogOpen.value = true
}

const handleEdit = (user: User) => {
    editingUser.value = user
    dialogOpen.value = true
}

const handleResetPassword = (id: number) => {
    resetPasswordUserId.value = id
    resetPasswordDialogOpen.value = true
}

const handleDeleteClick = (id: number) => {
    deletingUserId.value = id
    deleteConfirmOpen.value = true
}

const confirmDelete = async () => {
    if (!deletingUserId.value) return
    try {
        await userApi.delete(deletingUserId.value)
        toast({
            title: '删除成功',
            description: '用户已被移入回收站',
        })
        fetchData()
    } catch (e) {}
}

onMounted(() => {
    fetchData()
})
</script>

<template>
  <div class="flex items-center justify-between space-y-2">
    <h2 class="text-3xl font-bold tracking-tight">用户管理</h2>
    <div class="flex items-center space-x-2">
      <Button v-if="hasPermission('sys:user:add')" @click="handleAdd">
          <Plus class="h-4 w-4 mr-2" />
          添加用户
      </Button>
    </div>
  </div>
  
  <div class="rounded-md border bg-card text-card-foreground shadow-sm">
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead class="w-[80px]">ID</TableHead>
          <TableHead>用户名</TableHead>
          <TableHead>邮箱</TableHead>
          <TableHead>角色</TableHead>
          <TableHead>部门</TableHead>
          <TableHead>状态</TableHead>
          <TableHead>创建时间</TableHead>
          <TableHead class="text-right">操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-if="loading">
             <TableCell colspan="8" class="h-24 text-center">加载中...</TableCell>
        </TableRow>
        <template v-else>
            <TableRow v-for="item in data" :key="item.id">
              <TableCell class="font-medium">{{ item.id }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-2">
                  <span>{{ item.username }}</span>
                  <span
                    v-if="item.superuser"
                    class="inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-semibold bg-amber-100 text-amber-700 border-amber-200"
                  >
                    超级管理员
                  </span>
                </div>
              </TableCell>
              <TableCell>{{ item.email }}</TableCell>
              <TableCell>
                <span
                  v-if="item.role"
                  class="inline-flex items-center rounded-md border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 border-transparent bg-secondary text-secondary-foreground"
                >
                  {{ item.role.name }}
                </span>
                <span v-else class="text-muted-foreground text-sm">-</span>
              </TableCell>
              <TableCell>{{ item.dept?.name || '-' }}</TableCell>
              <TableCell>
                  <span v-if="item.active" class="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 border-transparent bg-primary text-primary-foreground hover:bg-primary/80">
                      启用
                  </span>
                  <span v-else class="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80">
                      禁用
                  </span>
              </TableCell>
              <TableCell>{{ new Date(item.createdTime).toLocaleString() }}</TableCell>
              <TableCell class="text-right">
                 <div class="flex justify-end gap-2">
                    <Button v-if="hasPermission('sys:user:update')" variant="ghost" size="icon" @click="handleResetPassword(item.id)" class="h-8 w-8 text-orange-500 hover:text-orange-600 hover:bg-orange-50" title="重置密码">
                        <KeyRound class="h-4 w-4" />
                        <span class="sr-only">重置密码</span>
                    </Button>
                    <Button v-if="hasPermission('sys:user:update')" variant="ghost" size="icon" @click="handleEdit(item)" class="h-8 w-8 text-blue-500 hover:text-blue-600 hover:bg-blue-50" title="编辑">
                        <Pencil class="h-4 w-4" />
                        <span class="sr-only">编辑</span>
                    </Button>
                    <Button v-if="hasPermission('sys:user:delete') && !item.superuser" variant="ghost" size="icon" @click="handleDeleteClick(item.id)" class="h-8 w-8 text-red-500 hover:text-red-600 hover:bg-red-50">
                        <Trash2 class="h-4 w-4" />
                        <span class="sr-only">删除</span>
                    </Button>
                 </div>
              </TableCell>
            </TableRow>
            <TableRow v-if="!loading && data.length === 0">
                 <TableCell colspan="8" class="h-24 text-center">暂无数据</TableCell>
            </TableRow>
        </template>
      </TableBody>
    </Table>
    
    <!-- Simple Pagination -->
    <div class="flex items-center justify-end space-x-2 py-4 px-4">
        <div class="flex-1 text-sm text-muted-foreground">
          共 {{ total }} 条记录，第 {{ query.page }} / {{ Math.max(1, Math.ceil(total / (query.size || 10))) }} 页
        </div>
        <div class="space-x-2">
          <Button
            variant="outline"
            size="sm"
            :disabled="query.page === 1"
            @click="handlePageChange((query.page || 1) - 1)"
          >
            上一页
          </Button>
          <Button
            variant="outline"
            size="sm"
            :disabled="(query.page || 1) * (query.size || 10) >= total"
            @click="handlePageChange((query.page || 1) + 1)"
          >
            下一页
          </Button>
        </div>
      </div>
  </div>

  <!-- Dialogs -->
  <UserDialog 
    v-model:open="dialogOpen" 
    :user="editingUser" 
    @success="fetchData" 
  />

  <ResetPasswordDialog 
    v-model:open="resetPasswordDialogOpen" 
    :userId="resetPasswordUserId" 
    @success="fetchData" 
  />

  <AlertDialog v-model:open="deleteConfirmOpen">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>确定要删除该用户吗？</AlertDialogTitle>
        <AlertDialogDescription>
          此操作无法撤销。
        </AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel>取消</AlertDialogCancel>
        <AlertDialogAction @click="confirmDelete" class="bg-red-500 hover:bg-red-600">确定删除</AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
