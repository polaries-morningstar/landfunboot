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
import { menuApi, type Menu } from '@/api/menu'
import { Pencil, Trash2, Plus, ChevronDown, ChevronRight, LayoutList, MousePointer2 } from 'lucide-vue-next'
import MenuDialog from './MenuDialog.vue'
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
const rawData = ref<Menu[]>([])
const displayData = ref<(Menu & { level: number, expanded?: boolean, hasChildren?: boolean, parent?: { id: number, name: string } })[]>([])
const expandedKeys = ref<Set<number>>(new Set())

// Dialog states
const dialogOpen = ref(false)
const editingMenu = ref<Menu | null>(null)
const deleteConfirmOpen = ref(false)
const deletingMenuId = ref<number | null>(null)

const fetchData = async () => {
    loading.value = true
    try {
        const res = await menuApi.tree()
        rawData.value = res
        updateDisplayData()
    } catch (e) {
    } finally {
        loading.value = false
    }
}

const updateDisplayData = () => {
    const list: (Menu & { level: number, expanded?: boolean, hasChildren?: boolean, parent?: { id: number, name: string } })[] = []
    const traverse = (items: Menu[], level = 0, parent?: Menu) => {
        items.forEach(item => {
            const hasChildren = item.children && item.children.length > 0
            const expanded = expandedKeys.value.has(item.id)
            list.push({
                ...item,
                level,
                expanded,
                hasChildren,
                parent: parent ? { id: parent.id, name: parent.name } : undefined
            })
            if (hasChildren && expanded) {
                traverse(item.children!, level + 1, item)
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
    editingMenu.value = null
    dialogOpen.value = true
}

const handleEdit = (menu: Menu) => {
    editingMenu.value = menu
    dialogOpen.value = true
}

const handleDeleteClick = (id: number) => {
    deletingMenuId.value = id
    deleteConfirmOpen.value = true
}

const confirmDelete = async () => {
    if (!deletingMenuId.value) return
    try {
        await menuApi.delete(deletingMenuId.value)
        toast({
            title: '删除成功',
            description: '菜单已被删除',
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
    <h2 class="text-3xl font-bold tracking-tight">菜单管理</h2>
    <div class="flex items-center space-x-2">
      <Button v-if="hasPermission('sys:menu:add')" @click="handleAdd">
          <Plus class="h-4 w-4 mr-2" />
          添加菜单
      </Button>
    </div>
  </div>
  
  <div class="rounded-md border bg-card text-card-foreground shadow-sm overflow-hidden">
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>名称</TableHead>
          <TableHead>类型</TableHead>
          <TableHead>路径/权限</TableHead>
          <TableHead class="text-right">操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-if="loading">
             <TableCell colspan="4" class="h-24 text-center">加载中...</TableCell>
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
                      <LayoutList v-if="item.type === 'MENU'" class="h-4 w-4 mr-2 text-blue-500" />
                      <MousePointer2 v-else class="h-4 w-4 mr-2 text-green-500" />
                      {{ item.name }}
                  </div>
              </TableCell>
              <TableCell>
                  <Badge :variant="item.type === 'DIR' ? 'outline' : item.type === 'MENU' ? 'default' : 'secondary'">
                      {{ item.type === 'DIR' ? '目录' : item.type === 'MENU' ? '菜单' : '按钮' }}
                  </Badge>
              </TableCell>
              <TableCell>
                  <div class="flex flex-col gap-1">
                      <span v-if="item.path" class="text-xs text-muted-foreground font-mono">{{ item.path }}</span>
                      <span v-if="item.permission" class="text-xs font-mono bg-slate-100 px-1 rounded">{{ item.permission }}</span>
                  </div>
              </TableCell>
              <TableCell class="text-right">
                 <div class="flex justify-end gap-2">
                    <Button v-if="hasPermission('sys:menu:update')" variant="ghost" size="icon" @click="handleEdit(item)" class="h-8 w-8 text-blue-500 hover:text-blue-600 hover:bg-blue-50" title="编辑">
                        <Pencil class="h-4 w-4" />
                        <span class="sr-only">编辑</span>
                    </Button>
                    <Button v-if="hasPermission('sys:menu:delete')" variant="ghost" size="icon" @click="handleDeleteClick(item.id)" class="h-8 w-8 text-red-500 hover:text-red-600 hover:bg-red-50" title="删除">
                        <Trash2 class="h-4 w-4" />
                        <span class="sr-only">删除</span>
                    </Button>
                 </div>
              </TableCell>
            </TableRow>
            <TableRow v-if="!loading && displayData.length === 0">
                 <TableCell colspan="4" class="h-24 text-center">暂无数据</TableCell>
            </TableRow>
        </template>
      </TableBody>
    </Table>
  </div>

  <MenuDialog 
    v-model:open="dialogOpen" 
    :menu="editingMenu" 
    :all-menus="rawData"
    @success="fetchData" 
  />

  <AlertDialog v-model:open="deleteConfirmOpen">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>确定要删除该菜单项吗？</AlertDialogTitle>
        <AlertDialogDescription>
          此操作将物理删除该菜单及其所有关联的子菜单或按钮权限。
        </AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel>取消</AlertDialogCancel>
        <AlertDialogAction @click="confirmDelete" class="bg-red-500 hover:bg-red-600">确定删除</AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
