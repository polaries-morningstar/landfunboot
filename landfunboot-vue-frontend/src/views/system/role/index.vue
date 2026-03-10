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
import { roleApi, type Role } from '@/api/role'
import type { BasePageQuery } from '@/api/user'
import { Pencil, Trash2, Plus } from 'lucide-vue-next'
import RoleDialog from './RoleDialog.vue'
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
import { Badge } from '@/components/ui/badge'
import { useAuth } from '@/composables/useAuth'

const { toast } = useToast()
const { hasPermission } = useAuth()
const loading = ref(false)
const data = ref<Role[]>([])
const total = ref(0)

// Dialog states
const dialogOpen = ref(false)
const editingRole = ref<Role | null>(null)
const deleteConfirmOpen = ref(false)
const deletingRoleId = ref<number | null>(null)

const query = reactive<BasePageQuery>({
    page: 1,
    size: 10,
    sort: 'id,desc'
})

const fetchData = async () => {
    loading.value = true
    try {
        const res = await roleApi.page(query)
        data.value = res.rows
        total.value = res.total
    } catch (e) {
    } finally {
        loading.value = false
    }
}

const handlePageChange = (newPage: number) => {
    query.page = newPage
    fetchData()
}

const handleAdd = () => {
    editingRole.value = null
    dialogOpen.value = true
}

const handleEdit = (role: Role) => {
    editingRole.value = role
    dialogOpen.value = true
}

const handleDeleteClick = (id: number) => {
    deletingRoleId.value = id
    deleteConfirmOpen.value = true
}

const confirmDelete = async () => {
    if (!deletingRoleId.value) return
    try {
        await roleApi.delete(deletingRoleId.value)
        toast({
            title: '删除成功',
            description: '角色已被删除',
        })
        fetchData()
    } catch (e) {}
}

const getDataScopeLabel = (scope: string) => {
    const map: Record<string, string> = {
        'ALL': '所有数据',
        'DEPT_SAME': '所在部门',
        'DEPT_RECURSIVE': '所在部门及子部门',
        'DEPT_CUSTOM': '自定义部门',
        'SELF': '个人数据'
    }
    return map[scope] || scope
}

onMounted(() => {
    fetchData()
})
</script>

<template>
  <div class="flex items-center justify-between space-y-2 mb-4">
    <h2 class="text-3xl font-bold tracking-tight">角色管理</h2>
    <div class="flex items-center space-x-2">
      <Button v-if="hasPermission('sys:role:add')" @click="handleAdd">
          <Plus class="h-4 w-4 mr-2" />
          添加角色
      </Button>
    </div>
  </div>
  
  <div class="rounded-md border bg-card text-card-foreground shadow-sm">
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead class="w-[80px]">ID</TableHead>
          <TableHead>角色名称</TableHead>
          <TableHead>角色代码</TableHead>
          <TableHead>数据范围</TableHead>
          <TableHead>描述</TableHead>
          <TableHead class="text-right">操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-if="loading">
             <TableCell colspan="6" class="h-24 text-center">加载中...</TableCell>
        </TableRow>
        <template v-else>
            <TableRow v-for="item in data" :key="item.id">
              <TableCell class="font-medium">{{ item.id }}</TableCell>
              <TableCell>{{ item.name }}</TableCell>
              <TableCell>
                  <Badge variant="outline">{{ item.code }}</Badge>
              </TableCell>
              <TableCell>
                  <Badge>{{ getDataScopeLabel(item.dataScope) }}</Badge>
              </TableCell>
              <TableCell>{{ item.description || '-' }}</TableCell>
              <TableCell class="text-right">
                 <div class="flex justify-end gap-2">
                    <Button v-if="hasPermission('sys:role:update')" variant="ghost" size="icon" @click="handleEdit(item)" class="h-8 w-8 text-blue-500 hover:text-blue-600 hover:bg-blue-50" title="编辑">
                        <Pencil class="h-4 w-4" />
                        <span class="sr-only">编辑</span>
                    </Button>
                    <Button v-if="hasPermission('sys:role:delete')" variant="ghost" size="icon" @click="handleDeleteClick(item.id)" class="h-8 w-8 text-red-500 hover:text-red-600 hover:bg-red-50" title="删除">
                        <Trash2 class="h-4 w-4" />
                        <span class="sr-only">删除</span>
                    </Button>
                 </div>
              </TableCell>
            </TableRow>
            <TableRow v-if="!loading && data.length === 0">
                 <TableCell colspan="6" class="h-24 text-center">暂无数据</TableCell>
            </TableRow>
        </template>
      </TableBody>
    </Table>
    
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

  <RoleDialog 
    v-model:open="dialogOpen" 
    :role="editingRole" 
    @success="fetchData" 
  />

  <AlertDialog v-model:open="deleteConfirmOpen">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>确定要删除该角色吗？</AlertDialogTitle>
        <AlertDialogDescription>
          此操作将物理删除该角色。删除后关联该角色的用户将失去相应权限。
        </AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel>取消</AlertDialogCancel>
        <AlertDialogAction @click="confirmDelete" class="bg-red-500 hover:bg-red-600">确定删除</AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
