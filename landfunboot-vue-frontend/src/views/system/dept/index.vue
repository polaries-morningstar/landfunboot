<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { deptApi, type Dept } from '@/api/dept'
import { Pencil, Trash2, Plus, ChevronDown, ChevronRight, Building2 } from 'lucide-vue-next'
import DeptDialog from './DeptDialog.vue'
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
const rawData = ref<Dept[]>([])
const displayData = ref<(Dept & { level: number, expanded?: boolean, hasChildren?: boolean })[]>([])
const expandedKeys = ref<Set<number>>(new Set())

// Dialog states
const dialogOpen = ref(false)
const editingDept = ref<Dept | null>(null)
const deleteConfirmOpen = ref(false)
const deletingDeptId = ref<number | null>(null)

const fetchData = async () => {
    loading.value = true
    try {
        const res = await deptApi.tree()
        rawData.value = res
        updateDisplayData()
    } catch (e) {
    } finally {
        loading.value = false
    }
}

const updateDisplayData = () => {
    const list: (Dept & { level: number, expanded?: boolean, hasChildren?: boolean })[] = []
    const traverse = (items: Dept[], level = 0) => {
        items.forEach(item => {
            const hasChildren = item.children && item.children.length > 0
            const expanded = expandedKeys.value.has(item.id)
            list.push({ ...item, level, expanded, hasChildren })
            if (hasChildren && expanded) {
                traverse(item.children!, level + 1)
            }
        })
    }
    traverse(rawData.value)
    displayData.value = list
}

const toggleExpand = (id: number) => {
    if (expandedKeys.value.has(id)) {
        expandedKeys.value.delete(id)
    } else {
        expandedKeys.value.add(id)
    }
    updateDisplayData()
}

const handleAdd = () => {
    editingDept.value = null
    dialogOpen.value = true
}

const handleEdit = (dept: Dept) => {
    editingDept.value = dept
    dialogOpen.value = true
}

const handleDeleteClick = (id: number) => {
    deletingDeptId.value = id
    deleteConfirmOpen.value = true
}

const confirmDelete = async () => {
    if (!deletingDeptId.value) return
    try {
        await deptApi.delete(deletingDeptId.value)
        toast({
            title: '删除成功',
            description: '部门及其子部门已被删除',
        })
        fetchData()
    } catch (e) {}
}

onMounted(() => {
    fetchData()
})
</script>

<template>
  <div class="flex items-center justify-between space-y-2 mb-4">
    <h2 class="text-3xl font-bold tracking-tight">部门管理</h2>
    <div class="flex items-center space-x-2">
      <Button v-if="hasPermission('sys:dept:add')" @click="handleAdd">
          <Plus class="h-4 w-4 mr-2" />
          添加部门
      </Button>
    </div>
  </div>
  
  <div class="rounded-md border bg-card text-card-foreground shadow-sm">
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>部门名称</TableHead>
          <TableHead class="w-[100px]">ID</TableHead>
          <TableHead class="text-right">操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-if="loading">
             <TableCell colspan="3" class="h-24 text-center">加载中...</TableCell>
        </TableRow>
        <template v-else>
            <TableRow v-for="item in displayData" :key="item.id">
              <TableCell>
                  <div class="flex items-center" :style="{ paddingLeft: `${item.level * 24}px` }">
                      <Button
                        v-if="item.hasChildren"
                        variant="ghost"
                        size="icon"
                        @click="toggleExpand(item.id)"
                        class="h-6 w-6 mr-1"
                      >
                        <ChevronDown v-if="item.expanded" class="h-4 w-4" />
                        <ChevronRight v-else class="h-4 w-4" />
                      </Button>
                      <span v-else class="w-7"></span>
                      <Building2 class="h-4 w-4 mr-2 text-muted-foreground" />
                      {{ item.name }}
                  </div>
              </TableCell>
              <TableCell>{{ item.id }}</TableCell>
              <TableCell class="text-right">
                 <div class="flex justify-end gap-2">
                    <Button v-if="hasPermission('sys:dept:update')" variant="ghost" size="icon" @click="handleEdit(item)" class="h-8 w-8 text-blue-500 hover:text-blue-600 hover:bg-blue-50" title="编辑">
                        <Pencil class="h-4 w-4" />
                        <span class="sr-only">编辑</span>
                    </Button>
                    <Button v-if="hasPermission('sys:dept:delete')" variant="ghost" size="icon" @click="handleDeleteClick(item.id)" class="h-8 w-8 text-red-500 hover:text-red-600 hover:bg-red-50" title="删除">
                        <Trash2 class="h-4 w-4" />
                        <span class="sr-only">删除</span>
                    </Button>
                 </div>
              </TableCell>
            </TableRow>
            <TableRow v-if="!loading && displayData.length === 0">
                 <TableCell colspan="3" class="h-24 text-center">暂无数据</TableCell>
            </TableRow>
        </template>
      </TableBody>
    </Table>
  </div>

  <DeptDialog 
    v-model:open="dialogOpen" 
    :dept="editingDept" 
    :all-depts="rawData"
    @success="fetchData" 
  />

  <AlertDialog v-model:open="deleteConfirmOpen">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>确定要删除该部门吗？</AlertDialogTitle>
        <AlertDialogDescription>
          此操作将物理删除该部门及其下属的所有子部门。
        </AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel>取消</AlertDialogCancel>
        <AlertDialogAction @click="confirmDelete" class="bg-red-500 hover:bg-red-600">确定删除</AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
