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
import { messageApi, type MyMessageRow, type BasePageQuery } from '@/api/message'
import { Plus } from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'
import SendMessageDialog from './SendMessageDialog.vue'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

const { hasPermission } = useAuth()
const loading = ref(false)
const data = ref<MyMessageRow[]>([])
const total = ref(0)
const sendDialogOpen = ref(false)
const detailOpen = ref(false)
const detailMessageId = ref<number | null>(null)
const detailContent = ref<{ title: string; content?: string; sender?: string; createdTime: string } | null>(null)

const query = reactive<BasePageQuery>({
  page: 1,
  size: 10,
  sort: 'createdTime,desc'
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await messageApi.myList(query)
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

const handleSend = () => {
  sendDialogOpen.value = true
}

const handleRowClick = async (row: MyMessageRow) => {
  detailMessageId.value = row.message.id
  detailContent.value = {
    title: row.message.title,
    content: row.message.content,
    sender: row.message.sender?.username,
    createdTime: row.message.createdTime
  }
  detailOpen.value = true
  if (!row.readAt) {
    try {
      await messageApi.markRead(row.message.id)
      row.readAt = new Date().toISOString()
    } catch (e) {}
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="flex items-center justify-between space-y-2">
    <h2 class="text-3xl font-bold tracking-tight">我的消息</h2>
    <div class="flex items-center space-x-2">
      <Button v-if="hasPermission('msg:send')" @click="handleSend">
        <Plus class="h-4 w-4 mr-2" />
        发送消息
      </Button>
    </div>
  </div>

  <div class="rounded-md border bg-card text-card-foreground shadow-sm">
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>发送人</TableHead>
          <TableHead>标题</TableHead>
          <TableHead>时间</TableHead>
          <TableHead class="w-[80px]">已读</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-if="loading">
          <TableCell colspan="4" class="h-24 text-center">加载中...</TableCell>
        </TableRow>
        <template v-else>
          <TableRow
            v-for="item in data"
            :key="item.id"
            class="cursor-pointer hover:bg-muted/50"
            @click="handleRowClick(item)"
          >
            <TableCell>{{ item.message?.sender?.username ?? '-' }}</TableCell>
            <TableCell>{{ item.message?.title ?? '-' }}</TableCell>
            <TableCell>{{ item.message?.createdTime ? new Date(item.message.createdTime).toLocaleString() : '-' }}</TableCell>
            <TableCell>
              <span
                v-if="item.readAt"
                class="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold border-transparent bg-secondary text-secondary-foreground"
              >
                已读
              </span>
              <span v-else class="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold border-transparent bg-primary text-primary-foreground">
                未读
              </span>
            </TableCell>
          </TableRow>
          <TableRow v-if="!loading && data.length === 0">
            <TableCell colspan="4" class="h-24 text-center">暂无消息</TableCell>
          </TableRow>
        </template>
      </TableBody>
    </Table>

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

  <SendMessageDialog v-model:open="sendDialogOpen" @success="fetchData" />

  <Dialog v-model:open="detailOpen">
    <DialogContent class="max-w-lg">
      <DialogHeader>
        <DialogTitle>{{ detailContent?.title }}</DialogTitle>
      </DialogHeader>
      <div v-if="detailContent" class="space-y-2 text-sm">
        <p class="text-muted-foreground">发送人：{{ detailContent.sender }}</p>
        <p class="text-muted-foreground">时间：{{ new Date(detailContent.createdTime).toLocaleString() }}</p>
        <div class="rounded-md border p-3 whitespace-pre-wrap">{{ detailContent.content || '（无内容）' }}</div>
      </div>
    </DialogContent>
  </Dialog>
</template>
