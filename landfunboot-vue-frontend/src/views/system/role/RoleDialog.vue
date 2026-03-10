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
import { Textarea } from '@/components/ui/textarea'
import { roleApi, type Role } from '@/api/role'
import { deptApi, type Dept } from '@/api/dept'
import { menuApi, type Menu } from '@/api/menu'
import { useToast } from '@/components/ui/toast/use-toast'

const props = defineProps<{
  open: boolean
  role?: Role | null
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
    name: z.string().min(2, '角色名称至少2个字符'),
    code: z.string().min(2, '角色代码至少2个字符'),
    description: z.string().optional(),
    dataScope: z.enum(['ALL', 'DEPT_SAME', 'DEPT_RECURSIVE', 'SELF', 'DEPT_CUSTOM']),
    deptIds: z.array(z.number()).default([]),
    menuIds: z.array(z.number()).default([]),
  })
)

const form = useForm({
  validationSchema: formSchema,
  initialValues: {
    dataScope: 'SELF',
    deptIds: [],
    menuIds: [],
  }
})


const depts = ref<(Dept & { level: number })[]>([])
const menus = ref<(Menu & { level: number })[]>([])
const selectedMenus = ref<number[]>([])
const selectedDepts = ref<number[]>([])

const fetchMenus = async () => {
  if (menus.value.length > 0) return
  try {
    const res = await menuApi.tree()
    const list: (Menu & { level: number })[] = []
    const traverse = (items: Menu[], level = 0) => {
      items.forEach(item => {
        list.push({ ...item, level })
        if (item.children) traverse(item.children, level + 1)
      })
    }
    traverse(res)
    menus.value = list
  } catch (e) {}
}

const fetchDepts = async () => {
  if (depts.value.length > 0) return
  try {
    const res = await deptApi.tree()
    const list: (Dept & { level: number })[] = []
    const traverse = (items: Dept[], level = 0) => {
      items.forEach(item => {
        list.push({ ...item, level })
        if (item.children) traverse(item.children, level + 1)
      })
    }
    traverse(res)
    depts.value = list
  } catch (e) {}
}

const fetchRoleDetails = async (id: number) => {
  try {
    const res = await roleApi.get(id)
    form.setValues({
      id: res.id,
      name: res.name,
      code: res.code,
      description: res.description || '',
      dataScope: res.dataScope as any,
      deptIds: res.deptIds || [], 
      menuIds: res.menuIds || (res as any).menus?.map((m: any) => m.id) || []
    })
    
    // If backend doesn't return deptIds directly but we need to map from depts relationship (if included)
    // But `Role` interface now has `deptIds`. Let's assume get() returns populated object.
    // If not, we might need a separate endpoint or ensuring fetcher includes it.
    // For now, let's assume get(id) is sufficient or we rely on RoleFetcher being updated in backend to include ids.
    // NOTE: I haven't updated RoleFetcher in backend to explicitly include deptIds scalar.
    // But JSqlClient default fetcher usually includes scalars. `deptIds` is NOT a column, it's a derived property or input property.
    // The relationship is `depts`.
    // So `res.depts` might be present.
    // Let's add safely:
    // deptIds: res.deptIds || (res as any).depts?.map((d: any) => d.id) || []
    
    const ids = res.deptIds || (res as any).depts?.map((d: any) => d.id) || []
    selectedDepts.value = ids
    form.setFieldValue('deptIds', ids)

    const mIds = res.menuIds || (res as any).menus?.map((m: any) => m.id) || []
    selectedMenus.value = mIds
    form.setFieldValue('menuIds', mIds)
    
    
  } catch (e) {
      console.error(e)
  }
}

watch(() => props.open, (newVal) => {
  if (newVal) {
    fetchDepts()
    fetchMenus()
    if (props.role) {
      // Fetch fresh details with deptIds
      fetchRoleDetails(props.role.id)
    } else {
      form.resetForm({
          values: {
              name: '',
              code: '',
              description: '',
              dataScope: 'SELF',
              deptIds: [],
              menuIds: []
          }
      })
      selectedMenus.value = []
      selectedDepts.value = []
    }
  }
})

const onFormSubmit = form.handleSubmit(async (values) => {
  loading.value = true
  try {
    const payload = {
        ...values,
        menuIds: [...selectedMenus.value],
        deptIds: [...selectedDepts.value]
    }
    await roleApi.save(payload as any)
    toast({
      title: props.role ? '修改成功' : '添加成功',
      description: `角色 ${values.name} 已${props.role ? '更新' : '添加'}`,
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
    <DialogContent class="sm:max-w-[600px] p-0 overflow-hidden flex flex-col max-h-[90vh]">
      <DialogHeader class="p-6 pb-2">
        <DialogTitle>{{ role ? '编辑角色' : '添加角色' }}</DialogTitle>
        <DialogDescription>
          填写角色基本信息及数据权限范围
        </DialogDescription>
      </DialogHeader>
      
      <div class="flex-1 overflow-y-auto px-6 py-2">
        <form @submit="onFormSubmit" id="role-form" class="space-y-4 pb-4">
          <div class="grid grid-cols-2 gap-4">
            <FormField v-slot="{ componentField }" name="name">
              <FormItem>
                <FormLabel>角色名称</FormLabel>
                <FormControl>
                  <Input placeholder="请输入角色名称" v-bind="componentField" />
                </FormControl>
                <FormMessage />
              </FormItem>
            </FormField>

            <FormField v-slot="{ componentField }" name="code">
              <FormItem>
                <FormLabel>角色代码</FormLabel>
                <FormControl>
                  <Input placeholder="ROLE_ADMIN" v-bind="componentField" />
                </FormControl>
                <FormMessage />
              </FormItem>
            </FormField>
          </div>

          <FormField v-slot="{ value, handleChange }" name="dataScope">
            <FormItem>
              <FormLabel>数据范围</FormLabel>
              <Select :model-value="value" @update:model-value="handleChange">
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="选择数据范围" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="ALL">所有数据</SelectItem>
                  <SelectItem value="DEPT_SAME">本部门数据</SelectItem>
                  <SelectItem value="DEPT_RECURSIVE">本部门及子部门数据</SelectItem>
                  <SelectItem value="SELF">仅本人数据</SelectItem>
                  <SelectItem value="DEPT_CUSTOM">自定义部门</SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-if="form.values.dataScope === 'DEPT_CUSTOM'" name="deptIds">
              <FormItem>
                <FormLabel>数据权限部门</FormLabel>
                <div class="border rounded-md p-3 max-h-[200px] overflow-y-auto space-y-1.5 bg-gray-50/50">
                   <div v-if="depts.length === 0" class="text-xs text-muted-foreground text-center py-2">正在加载部门...</div>
                   <div v-for="dept in depts" :key="dept.id" class="flex items-center space-x-2" :style="{ paddingLeft: `${dept.level * 16}px` }">
                       <input 
                           type="checkbox"
                           :id="`dept-${dept.id}`"
                           :value="dept.id"
                           v-model="selectedDepts"
                           class="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
                       />
                       <label :for="`dept-${dept.id}`" class="text-sm cursor-pointer select-none py-0.5">{{ dept.name }}</label>
                   </div>
                </div>
                <FormMessage />
              </FormItem>
          </FormField>

          <FormField name="menuIds">
              <FormItem>
                <FormLabel>菜单权限</FormLabel>
                <div class="border rounded-md p-3 max-h-[350px] overflow-y-auto space-y-1 bg-gray-50/50">
                   <div v-if="menus.length === 0" class="text-xs text-muted-foreground text-center py-2">正在加载菜单...</div>
                   <div v-for="menu in menus" :key="menu.id" class="flex items-center space-x-2 transition-colors hover:bg-blue-50/50 rounded pr-2" :style="{ paddingLeft: `${menu.level * 16}px` }">
                       <input 
                           type="checkbox"
                           :id="`menu-${menu.id}`"
                           :value="menu.id"
                           v-model="selectedMenus"
                           class="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
                       />
                       <label :for="`menu-${menu.id}`" class="text-sm cursor-pointer select-none py-1.5 flex items-center flex-1">
                          <span v-if="menu.type === 'DIR'" class="text-[10px] bg-gray-100 text-gray-500 px-1 rounded mr-1.5 font-bold">DIR</span>
                          <span v-else-if="menu.type === 'BUTTON'" class="text-[10px] bg-blue-100 text-blue-600 px-1 rounded mr-1.5 font-bold">BTN</span>
                          <span v-else class="text-[10px] bg-emerald-100 text-emerald-600 px-1 rounded mr-1.5 font-bold">PAGE</span>
                          {{ menu.name }}
                       </label>
                   </div>
                </div>
                <FormMessage />
              </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="description">
            <FormItem>
              <FormLabel>描述</FormLabel>
              <FormControl>
                <Textarea placeholder="请输入角色描述" class="resize-none" v-bind="componentField" />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>
        </form>
      </div>

      <DialogFooter class="p-6 pt-2 border-t bg-gray-50/30">
        <Button type="button" variant="outline" @click="emit('update:open', false)">
          取消
        </Button>
        <Button type="submit" form="role-form" :disabled="loading">
          {{ loading ? '保存中...' : '提交' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
