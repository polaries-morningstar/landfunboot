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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { messageApi, type MessageListView, type BasePageQuery, type MessageTargetType } from '@/api/message'

const loading = ref(false)
const data = ref<MessageListView[]>([])
const total = ref(0)
const detailOpen = ref(false)
const detailItem = ref<MessageListView | null>(null)

const query = reactive<BasePageQuery>({
  page: 1,
  size: 10,
  sort: 'createdTime,desc'
})

const targetTypeLabel: Record<MessageTargetType, string> = {
  USER: '指定用户消息',
  DEPT: '部门消息',
  DEPT_WITH_CHILDREN: '部门及子部门消息',
  ALL: '系统消息',
  ROLE: '角色消息',
  USER_IDS: '指定用户列表'
}

function getTargetTypeLabel(t?: MessageTargetType | null): string {
  if (!t) return '-'
  return targetTypeLabel[t] ?? t
}

function getReceiversDisplay(item: MessageListView): string {
  const list = item.receivers
  if (!list?.length) return '-'
  return list.map((r) => r.user?.username ?? '').filter(Boolean).join('、') || '-'
}

function openDetail(item: MessageListView) {
  detailItem.value = item
  detailOpen.value = true
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await messageApi.list(query)
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

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="flex items-center justify-between space-y-2">
    <h2 class="text-3xl font-bold tracking-tight">全部消息</h2>
  </div>

  <div class="rounded-md border bg-card text-card-foreground shadow-sm">
    <p class="px-4 py-2 text-sm text-muted-foreground border-b">点击行或「查看内容」打开详情</p>
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>发送人</TableHead>
          <TableHead>标题</TableHead>
          <TableHead>消息类型</TableHead>
          <TableHead>收件人</TableHead>
          <TableHead>发送时间</TableHead>
          <TableHead class="w-[100px]">操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-if="loading">
          <TableCell colspan="6" class="h-24 text-center">加载中...</TableCell>
        </TableRow>
        <template v-else>
          <TableRow
            v-for="item in data"
            :key="item.id"
            class="cursor-pointer hover:bg-muted/50"
            @click="openDetail(item)"
          >
            <TableCell class="align-middle">{{ item.sender?.username ?? '-' }}</TableCell>
            <TableCell class="align-middle">
              <span class="font-medium">{{ item.title ?? '-' }}</span>
              <span class="ml-2 text-primary text-sm underline">查看</span>
            </TableCell>
            <TableCell class="align-middle">{{ getTargetTypeLabel(item.targetType) }}</TableCell>
            <TableCell class="align-middle max-w-[200px] truncate" :title="getReceiversDisplay(item)">{{ getReceiversDisplay(item) }}</TableCell>
            <TableCell class="align-middle">{{ item.createdTime ? new Date(item.createdTime).toLocaleString() : '-' }}</TableCell>
            <TableCell class="align-middle" @click.stop>
              <Button variant="outline" size="sm" @click="openDetail(item)">查看内容</Button>
            </TableCell>
          </TableRow>
          <TableRow v-if="!loading && data.length === 0">
            <TableCell colspan="6" class="h-24 text-center">暂无消息</TableCell>
          </TableRow>
        </template>
      </TableBody>
    </Table>

    <Dialog v-model:open="detailOpen">
      <DialogContent class="max-w-lg max-h-[85vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>{{ detailItem?.title ?? '消息内容' }}</DialogTitle>
        </DialogHeader>
        <div v-if="detailItem" class="space-y-3 overflow-y-auto flex-1 text-sm">
          <p><span class="text-muted-foreground">发送人：</span>{{ detailItem.sender?.username ?? '-' }}</p>
          <p><span class="text-muted-foreground">消息类型：</span>{{ getTargetTypeLabel(detailItem.targetType) }}</p>
          <p><span class="text-muted-foreground">收件人：</span>{{ getReceiversDisplay(detailItem) }}</p>
          <p><span class="text-muted-foreground">发送时间：</span>{{ detailItem.createdTime ? new Date(detailItem.createdTime).toLocaleString() : '-' }}</p>
          <div class="pt-2 border-t">
            <p class="text-muted-foreground mb-1">内容</p>
            <div class="whitespace-pre-wrap break-words rounded bg-muted/50 p-3">{{ detailItem.content || '（无正文）' }}</div>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <div class="flex items-center justify-end space-x-2 py-4 px-4">
      <div class="flex-1 text-sm text-muted-foreground">
        共 {{ total }} 条，第 {{ query.page }} / {{ Math.max(1, Math.ceil(total / (query.size || 10))) }} 页
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
</template>
