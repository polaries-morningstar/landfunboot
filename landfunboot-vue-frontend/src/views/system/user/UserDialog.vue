<script setup lang="ts">
import { ref, watch } from 'vue'
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
//  Form,
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
import { Switch } from '@/components/ui/switch'
import { userApi, type User } from '@/api/user'
import { roleApi, type Role } from '@/api/role'
import { deptApi, type Dept } from '@/api/dept'
import { useToast } from '@/components/ui/toast/use-toast'

const props = defineProps<{
  open: boolean
  user?: User | null
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const { toast } = useToast()
const roles = ref<Role[]>([])
const depts = ref<Dept[]>([])
const loading = ref(false)
const active = ref(true)

const formSchema = toTypedSchema(
  z.object({
    id: z.number().optional(),
    username: z.string().min(2, '用户名至少2个字符'),
    email: z.string().email('请输入有效的邮箱地址'),
    password: z.string().min(6, '密码至少6个字符').optional().or(z.literal('')),
    // active 独立用 ref 管理
    deptId: z.number().optional(),
    roleId: z.number().optional(),
  })
)

const form = useForm({
  validationSchema: formSchema,
  initialValues: {
    roleId: undefined,
  }
})

const fetchOptions = async () => {
    try {
        const [rolesRes, deptsRes] = await Promise.all([
            roleApi.all(),
            deptApi.tree()
        ])
        roles.value = rolesRes
        // Flatten depts for simple select
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

watch(() => props.open, async (newVal) => {
  if (newVal) {
    fetchOptions()
    if (props.user) {
      // Re-fetch fresh user data from server to avoid showing stale roles
      try {
        const freshUser = await userApi.get(props.user.id)
        form.setValues({
            id: freshUser.id,
            username: freshUser.username,
            email: freshUser.email,
            deptId: freshUser.dept?.id,
            roleId: freshUser.role?.id
        })
        active.value = freshUser.active
      } catch (e) {
        // Fallback to prop data if fetch fails
        form.setValues({
            id: props.user.id,
            username: props.user.username,
            email: props.user.email,
            deptId: props.user.dept?.id,
            roleId: props.user.role?.id
        })
        active.value = props.user.active
      }
    } else {
      form.resetForm({
          values: {
              username: '',
              email: '',
              password: '',
              roleId: undefined,
              deptId: undefined
          }
      })
      active.value = true
    }
  }
})

const onFormSubmit = form.handleSubmit(async (values) => {
  if (!values.id && !values.password) {
    form.setFieldError('password', '新增用户时密码为必填项')
    return
  }
  loading.value = true
  try {
    const payload = {
      id: values.id,
      username: values.username,
      email: values.email,
      active: active.value,
      deptId: values.deptId,
      roleId: values.roleId ?? null,
      password: values.password || undefined,
    }

    await userApi.save(payload as any)
    toast({
      title: props.user ? '修改成功' : '添加成功',
      description: `用户 ${values.username} 已${props.user ? '更新' : '添加'}`,
    })
    emit('success')
    emit('update:open', false)
  } catch (error: any) {
    // Error handled by request interceptor usually
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <Dialog :open="open" @update:open="emit('update:open', $event)">
    <DialogContent class="sm:max-w-[600px]">
      <DialogHeader>
        <DialogTitle>{{ user ? '编辑用户' : '添加用户' }}</DialogTitle>
        <DialogDescription>
          填写用户信息及权限配置
        </DialogDescription>
      </DialogHeader>
      
      <div class="max-h-[85vh] overflow-y-auto px-1 py-4">
        <form @submit="onFormSubmit" class="space-y-4 pr-1">
          <FormField v-slot="{ componentField }" name="username">
            <FormItem>
              <FormLabel>用户名 <span class="text-destructive">*</span></FormLabel>
              <FormControl>
                <Input placeholder="请输入用户名" v-bind="componentField" />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="email">
            <FormItem>
              <FormLabel>邮箱 <span class="text-destructive">*</span></FormLabel>
              <FormControl>
                <Input placeholder="admin@example.com" v-bind="componentField" />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-if="!user" v-slot="{ componentField }" name="password">
            <FormItem>
              <FormLabel>密码 <span class="text-destructive">*</span></FormLabel>
              <FormControl>
                <Input type="password" placeholder="******" v-bind="componentField" />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ value, handleChange }" name="deptId">
            <FormItem>
              <FormLabel>部门</FormLabel>
              <Select :model-value="value?.toString()" @update:model-value="v => handleChange(Number(v))">
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="选择部门" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem v-for="dept in depts" :key="dept.id" :value="dept.id.toString()">
                    <pre class="m-0 font-sans">{{ dept.name }}</pre>
                  </SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ value, handleChange }" name="roleId">
            <FormItem>
              <FormLabel>角色</FormLabel>
              <Select
                :model-value="value != null ? value.toString() : undefined"
                @update:model-value="v => handleChange(v ? Number(v) : undefined)"
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="选择角色" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem
                    v-for="role in roles"
                    :key="role.id"
                    :value="role.id.toString()"
                  >
                    {{ role.name }}
                  </SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          </FormField>

          <div class="flex flex-row items-center justify-between rounded-lg border p-3 shadow-sm">
            <div class="space-y-0.5">
              <span class="text-sm font-medium leading-none">系统访问权限</span>
              <div class="text-[0.8rem]" :class="active ? 'text-green-600' : 'text-red-500'">
                {{ active ? '当前状态：允许登录 (开启)' : '当前状态：禁止登录 (关闭)' }}
              </div>
            </div>
            <Switch v-model="active" />
          </div>

          <DialogFooter class="mt-6 pt-2 border-t">
            <Button type="button" variant="outline" @click="emit('update:open', false)">
              取消
            </Button>
            <Button type="submit" :disabled="loading">
              {{ loading ? '保存中...' : '保存' }}
            </Button>
          </DialogFooter>
        </form>
      </div>
    </DialogContent>
  </Dialog>
</template>
