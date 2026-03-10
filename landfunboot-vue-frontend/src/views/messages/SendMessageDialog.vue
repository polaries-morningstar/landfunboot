<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import Textarea from '@/components/ui/textarea/Textarea.vue'
import { userApi, type User } from '@/api/user'
import { roleApi, type Role } from '@/api/role'
import { deptApi, type Dept } from '@/api/dept'
import { messageApi, type SendMessageReq, type MessageTargetType } from '@/api/message'
import { useToast } from '@/components/ui/toast/use-toast'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const TARGET_OPTIONS: { value: MessageTargetType; label: string }[] = [
  { value: 'USER', label: '指定用户' },
  { value: 'DEPT', label: '部门' },
  { value: 'DEPT_WITH_CHILDREN', label: '部门及子部门' },
  { value: 'ALL', label: '全系统' },
  { value: 'ROLE', label: '角色' },
  { value: 'USER_IDS', label: '自定义用户列表' },
]

const { toast } = useToast()
const loading = ref(false)
const users = ref<User[]>([])
const roles = ref<Role[]>([])
const depts = ref<Dept[]>([])
const selectedUserIds = ref<number[]>([])
const userSelectValue = ref<string>('')

const formSchema = toTypedSchema(
  z.object({
    title: z.string().min(1, '请输入标题'),
    content: z.string().optional(),
    targetType: z.enum(['USER', 'DEPT', 'DEPT_WITH_CHILDREN', 'ALL', 'ROLE', 'USER_IDS']),
    targetId: z.number().optional(),
  })
)

const form = useForm({
  validationSchema: formSchema,
  initialValues: {
    title: '',
    content: '',
    targetType: 'USER' as MessageTargetType,
    targetId: undefined as number | undefined,
  }
})

const needTargetId = computed(() => {
  const t = form.values.targetType
  return t === 'USER' || t === 'DEPT' || t === 'DEPT_WITH_CHILDREN' || t === 'ROLE'
})

const needTargetIds = computed(() => form.values.targetType === 'USER_IDS')

const fetchOptions = async () => {
  try {
    const [userRes, rolesRes, deptsRes] = await Promise.all([
      userApi.page({ page: 1, size: 500, sort: 'username,asc' }),
      roleApi.all(),
      deptApi.tree()
    ])
    users.value = userRes.rows
    roles.value = rolesRes
    const flattenDepts: Dept[] = []
    const traverse = (list: Dept[], level = 0) => {
      list.forEach(item => {
        flattenDepts.push({ ...item, name: '  '.repeat(level) + item.name })
        if (item.children) traverse(item.children, level + 1)
      })
    }
    traverse(deptsRes)
    depts.value = flattenDepts
  } catch (e) {
    console.error('Failed to fetch options', e)
  }
}

const addUser = () => {
  const id = userSelectValue.value ? Number(userSelectValue.value) : 0
  if (id && !selectedUserIds.value.includes(id)) {
    selectedUserIds.value = [...selectedUserIds.value, id]
    userSelectValue.value = ''
  }
}

const removeUser = (id: number) => {
  selectedUserIds.value = selectedUserIds.value.filter(x => x !== id)
}

const selectedUserNames = computed(() => {
  return selectedUserIds.value.map(id => users.value.find(u => u.id === id)?.username ?? id)
})

watch(() => props.open, async (newVal) => {
  if (newVal) {
    await fetchOptions()
    form.resetForm({
      values: {
        title: '',
        content: '',
        targetType: 'USER',
        targetId: undefined,
      }
    })
    selectedUserIds.value = []
    userSelectValue.value = ''
  }
})

const onFormSubmit = form.handleSubmit(async (values) => {
  loading.value = true
  try {
    const payload: SendMessageReq = {
      title: values.title,
      content: values.content ?? '',
      targetType: values.targetType as MessageTargetType,
    }
    if (values.targetType === 'USER_IDS') {
      if (selectedUserIds.value.length === 0) {
        toast({ title: '请至少选择一个用户', variant: 'destructive' })
        loading.value = false
        return
      }
      payload.targetIds = selectedUserIds.value
    } else if (needTargetId.value && values.targetId != null) {
      payload.targetId = values.targetId
    } else if (needTargetId.value) {
      toast({ title: '请选择接收对象', variant: 'destructive' })
      loading.value = false
      return
    }
    await messageApi.send(payload)
    toast({ title: '发送成功' })
    emit('success')
    emit('update:open', false)
  } catch (e) {
    // Error handled by request interceptor
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <Dialog :open="open" @update:open="emit('update:open', $event)">
    <DialogContent class="sm:max-w-[560px]">
      <DialogHeader>
        <DialogTitle>发送消息</DialogTitle>
        <DialogDescription>
          选择接收范围并填写标题与内容
        </DialogDescription>
      </DialogHeader>

      <form @submit="onFormSubmit" class="space-y-4">
        <FormField v-slot="{ componentField }" name="title">
          <FormItem>
            <FormLabel>标题</FormLabel>
            <FormControl>
              <Input placeholder="消息标题" v-bind="componentField" />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <FormField v-slot="{ componentField }" name="content">
          <FormItem>
            <FormLabel>内容</FormLabel>
            <FormControl>
              <Textarea placeholder="消息内容（可选）" class="min-h-[100px]" v-bind="componentField" />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <FormField v-slot="{ value, handleChange }" name="targetType">
          <FormItem>
            <FormLabel>接收范围</FormLabel>
            <Select :model-value="value" @update:model-value="v => handleChange(v)">
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="选择范围" />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                <SelectItem
                  v-for="opt in TARGET_OPTIONS"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </SelectItem>
              </SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        </FormField>

        <FormField v-if="needTargetId" v-slot="{ value, handleChange }" name="targetId">
          <FormItem>
            <FormLabel>{{ form.values.targetType === 'USER' ? '选择用户' : form.values.targetType === 'ROLE' ? '选择角色' : '选择部门' }}</FormLabel>
            <Select
              :model-value="value != null ? value.toString() : undefined"
              @update:model-value="v => handleChange(v ? Number(v) : undefined)"
            >
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="请选择" />
                </SelectTrigger>
              </FormControl>
              <SelectContent v-if="form.values.targetType === 'USER'">
                <SelectItem v-for="u in users" :key="u.id" :value="u.id.toString()">
                  {{ u.username }} ({{ u.email }})
                </SelectItem>
              </SelectContent>
              <SelectContent v-else-if="form.values.targetType === 'ROLE'">
                <SelectItem v-for="r in roles" :key="r.id" :value="r.id.toString()">
                  {{ r.name }}
                </SelectItem>
              </SelectContent>
              <SelectContent v-else>
                <SelectItem v-for="d in depts" :key="d.id" :value="d.id.toString()">
                  <pre class="m-0 font-sans">{{ d.name }}</pre>
                </SelectItem>
              </SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        </FormField>

        <template v-if="needTargetIds">
          <FormItem>
            <FormLabel>选择用户</FormLabel>
            <div class="flex gap-2">
              <Select v-model="userSelectValue">
                <SelectTrigger class="flex-1">
                  <SelectValue placeholder="添加用户" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem
                    v-for="u in users"
                    :key="u.id"
                    :value="u.id.toString()"
                  >
                    {{ u.username }} ({{ u.email }})
                  </SelectItem>
                </SelectContent>
              </Select>
              <Button type="button" variant="outline" @click="addUser">添加</Button>
            </div>
            <div v-if="selectedUserIds.length > 0" class="mt-2 flex flex-wrap gap-2">
              <span
                v-for="(id, i) in selectedUserIds"
                :key="id"
                class="inline-flex items-center rounded-md border px-2.5 py-0.5 text-sm"
              >
                {{ selectedUserNames[i] }}
                <button type="button" class="ml-1 text-muted-foreground hover:text-foreground" @click="removeUser(id)">×</button>
              </span>
            </div>
          </FormItem>
        </template>

        <DialogFooter class="mt-6 pt-2 border-t">
          <Button type="button" variant="outline" @click="emit('update:open', false)">
            取消
          </Button>
          <Button type="submit" :disabled="loading">
            {{ loading ? '发送中...' : '发送' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
</template>
