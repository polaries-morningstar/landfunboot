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
import { deptApi, type Dept } from '@/api/dept'
import { useToast } from '@/components/ui/toast/use-toast'

const props = defineProps<{
  open: boolean
  dept?: Dept | null
  allDepts: Dept[]
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
    name: z.string().min(2, '部门名称至少2个字符'),
    parentId: z.number().nullable().default(null)
  })
)

const form = useForm({
  validationSchema: formSchema,
  initialValues: {
    parentId: null,
  }
})

// Flatten depts for select, exclude current dept and its descendants to prevent cycles
const flattenOptions = computed(() => {
    const list: { id: number, name: string }[] = []
    const excludeIds = new Set<number>()
    
    if (props.dept?.id) {
        const collectIds = (items: Dept[]) => {
            items.forEach(item => {
                excludeIds.add(item.id)
                if (item.children) collectIds(item.children)
            })
        }
        // Find current dept in tree to collect descendants
        const findAndExclude = (items: Dept[]) => {
            for (const item of items) {
                if (item.id === props.dept!.id) {
                    excludeIds.add(item.id)
                    if (item.children) collectIds(item.children)
                    return true
                }
                if (item.children && findAndExclude(item.children)) return true
            }
            return false
        }
        findAndExclude(props.allDepts)
    }

    const traverse = (items: Dept[], level = 0) => {
        items.forEach(item => {
            if (!excludeIds.has(item.id)) {
                list.push({ id: item.id, name: '  '.repeat(level) + item.name })
            }
            if (item.children) traverse(item.children, level + 1)
        })
    }
    traverse(props.allDepts)
    return list
})

watch(() => props.open, (newVal) => {
  if (newVal) {
    if (props.dept) {
      form.setValues({
          id: props.dept.id,
          name: props.dept.name,
          parentId: props.dept.parent?.id || null
      })
    } else {
      form.resetForm({
          values: {
              name: '',
              parentId: null
          }
      })
    }
  }
})

const onFormSubmit = form.handleSubmit(async (values) => {
  loading.value = true
  try {
    const payload = { ...values }
    // Ensure parentId is sent correctly (if null, backend usually expects 0 or null depending on implementation)
    // DeptInput in backend handles null parentId
    await deptApi.save(payload as any)
    toast({
      title: props.dept ? '修改成功' : '添加成功',
      description: `部门 ${values.name} 已${props.dept ? '更新' : '添加'}`,
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
    <DialogContent class="sm:max-w-[425px]">
      <DialogHeader>
        <DialogTitle>{{ dept ? '编辑部门' : '添加部门' }}</DialogTitle>
        <DialogDescription>
          填写部门基本信息
        </DialogDescription>
      </DialogHeader>
      
      <form @submit="onFormSubmit" class="space-y-4 py-4">
        <FormField v-slot="{ componentField }" name="name">
          <FormItem>
            <FormLabel>部门名称</FormLabel>
            <FormControl>
              <Input placeholder="请输入部门名称" v-bind="componentField" />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <FormField v-slot="{ value, handleChange }" name="parentId">
          <FormItem>
            <FormLabel>上级部门</FormLabel>
            <Select :model-value="value?.toString() || '0'" @update:model-value="v => handleChange(v === '0' ? null : Number(v))">
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="选择上级部门" />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                <SelectItem value="0">无（作为根部门）</SelectItem>
                <SelectItem v-for="opt in flattenOptions" :key="opt.id" :value="opt.id.toString()">
                  <pre class="m-0 font-sans">{{ opt.name }}</pre>
                </SelectItem>
              </SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        </FormField>

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
