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
import { menuApi, type Menu } from '@/api/menu'
import { useToast } from '@/components/ui/toast/use-toast'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'

const props = defineProps<{
  open: boolean
  menu?: Menu | null
  allMenus: Menu[]
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const { toast } = useToast()
const loading = ref(false)

const formSchema = toTypedSchema(
  z.object({
    id: z.number().optional(),
    name: z.string().min(2, '菜单名称至少2个字符'),
    type: z.enum(['DIR', 'MENU', 'BUTTON']),
    parentId: z.number().nullable().default(null),
    permission: z.string().optional(),
    path: z.string().optional(),
    icon: z.string().optional(),
  })
)

const form = useForm({
  validationSchema: formSchema,
  initialValues: {
    type: 'MENU',
    parentId: null
  }
})

// Flatten menus for select
const flattenOptions = computed(() => {
    const list: { id: number, name: string }[] = []
    const excludeIds = new Set<number>()
    
    if (props.menu?.id) {
        const collectIds = (items: Menu[]) => {
            items.forEach(item => {
                excludeIds.add(item.id)
                if (item.children) collectIds(item.children)
            })
        }
        const findAndExclude = (items: Menu[]) => {
            for (const item of items) {
                if (item.id === props.menu!.id) {
                    excludeIds.add(item.id)
                    if (item.children) collectIds(item.children)
                    return true
                }
                if (item.children && findAndExclude(item.children)) return true
            }
            return false
        }
        findAndExclude(props.allMenus)
    }

    const traverse = (items: Menu[], level = 0) => {
        items.forEach(item => {
            if (!excludeIds.has(item.id) && (item.type === 'MENU' || item.type === 'DIR')) {
                const label = '  '.repeat(level) + (item.type === 'DIR' ? `目录 - ${item.name}` : item.name)
                list.push({ id: item.id, name: label })
                if (item.children) traverse(item.children, level + 1)
            }
        })
    }
    traverse(props.allMenus)
    return list
})

watch(() => props.open, (newVal) => {
  if (newVal) {
    if (props.menu) {
      form.setValues({
          id: props.menu.id,
          name: props.menu.name,
          type: props.menu.type as any,
          parentId: props.menu.parent?.id || null,
          permission: props.menu.permission || '',
          path: props.menu.path || '',
          icon: props.menu.icon || ''
      })
    } else {
      form.resetForm({
          values: {
              name: '',
              type: 'MENU',
              parentId: null,
              permission: '',
              path: '',
              icon: ''
          }
      })
    }
  }
})

const onFormSubmit = form.handleSubmit(async (values) => {
  loading.value = true
  try {
    const payload: Record<string, unknown> = { ...values }
    if (payload.parentId === null || payload.parentId === undefined) {
      payload.parentId = 0
    }
    if (payload.id === undefined || payload.id === null) {
      delete payload.id
    }
    await menuApi.save(payload as any)
    toast({
      title: props.menu ? '修改成功' : '添加成功',
      description: `菜单 ${values.name} 已${props.menu ? '更新' : '添加'}`,
    })
    emit('success')
    emit('update:open', false)
  } catch (error: any) {
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <Dialog :open="open" @update:open="emit('update:open', $event)">
    <DialogContent class="sm:max-w-[500px]">
      <DialogHeader>
        <DialogTitle>{{ menu ? '编辑菜单' : '添加菜单' }}</DialogTitle>
        <DialogDescription>
          配置菜单项的基本信息、路径及按钮权限
        </DialogDescription>
      </DialogHeader>
      
      <form @submit="onFormSubmit" class="grid gap-4 py-4">
        <div class="grid grid-cols-2 gap-4">
            <FormField v-slot="{ value, handleChange }" name="type">
              <FormItem class="col-span-2">
                <FormLabel>类型</FormLabel>
                <FormControl>
                  <RadioGroup :model-value="value" @update:model-value="handleChange" class="flex space-x-4">
                    <div class="flex items-center space-x-2">
                      <RadioGroupItem value="DIR" id="r0" />
                      <label for="r0">目录</label>
                    </div>
                    <div class="flex items-center space-x-2">
                      <RadioGroupItem value="MENU" id="r1" />
                      <label for="r1">菜单</label>
                    </div>
                    <div class="flex items-center space-x-2">
                      <RadioGroupItem value="BUTTON" id="r2" />
                      <label for="r2">按钮</label>
                    </div>
                  </RadioGroup>
                </FormControl>
                <FormMessage />
              </FormItem>
            </FormField>

            <FormField v-slot="{ componentField }" name="name">
              <FormItem class="col-span-2">
                <FormLabel>名称</FormLabel>
                <FormControl>
                  <Input placeholder="请输入名称" v-bind="componentField" />
                </FormControl>
                <FormMessage />
              </FormItem>
            </FormField>

            <FormField v-slot="{ value, handleChange }" name="parentId">
              <FormItem class="col-span-2">
                <FormLabel>上级菜单</FormLabel>
                <Select :model-value="value?.toString() || '0'" @update:model-value="v => handleChange(v === '0' ? null : Number(v))">
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="选择上级菜单" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="0">无（作为顶级）</SelectItem>
                    <SelectItem v-for="opt in flattenOptions" :key="opt.id" :value="opt.id.toString()">
                      <pre class="m-0 font-sans">{{ opt.name }}</pre>
                    </SelectItem>
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            </FormField>

            <template v-if="form.values.type !== 'BUTTON'">
                <FormField v-slot="{ componentField }" name="path">
                  <FormItem>
                    <FormLabel>路由路径</FormLabel>
                    <FormControl>
                      <Input placeholder="/system/user" v-bind="componentField" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                </FormField>
                <FormField v-slot="{ componentField }" name="icon">
                  <FormItem>
                    <FormLabel>图标 (Lucide)</FormLabel>
                    <FormControl>
                      <Input placeholder="Search, Users, etc." v-bind="componentField" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                </FormField>
            </template>



            <FormField v-slot="{ componentField }" name="permission">
              <FormItem :class="form.values.type !== 'DIR' ? '' : 'col-span-2'">
                <FormLabel>权限标识</FormLabel>
                <FormControl>
                  <Input placeholder="sys:user:list" v-bind="componentField" />
                </FormControl>
                <FormMessage />
              </FormItem>
            </FormField>


        </div>

        <DialogFooter>
          <Button type="button" variant="outline" @click="emit('update:open', false)">
            取消
          </Button>
          <Button type="submit" :disabled="loading">
            {{ loading ? '保存中...' : '保存' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
</template>
