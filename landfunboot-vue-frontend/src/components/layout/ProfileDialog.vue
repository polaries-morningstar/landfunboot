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
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { userApi } from '@/api/user'
import { useToast } from '@/components/ui/toast/use-toast'
import { useAuth } from '@/composables/useAuth'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const { toast } = useToast()
const { user } = useAuth()
const loading = ref(false)

const formSchema = toTypedSchema(
  z.object({
    username: z.string().min(2, '用户名至少2个字符'),
  })
)

const form = useForm({
  validationSchema: formSchema,
  initialValues: {
    username: ''
  }
})

watch(() => props.open, (newVal) => {
  if (newVal && user.value) {
    form.resetForm({
      values: { username: user.value.username || '' }
    })
  }
})

const onFormSubmit = form.handleSubmit(async (values) => {
  loading.value = true
  try {
    await userApi.updateSelf({ username: values.username })
    toast({
      title: '修改成功',
      description: '个人信息已更新',
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
        <DialogTitle>个人设置</DialogTitle>
        <DialogDescription>
          修改您的个人信息。
        </DialogDescription>
      </DialogHeader>

      <div class="py-4">
        <form @submit="onFormSubmit" class="space-y-4">
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-muted-foreground">邮箱</label>
            <Input :model-value="user?.email" disabled class="bg-muted/50" />
          </div>

          <FormField v-slot="{ componentField }" name="username">
            <FormItem>
              <FormLabel>用户名 <span class="text-destructive">*</span></FormLabel>
              <FormControl>
                <Input placeholder="请输入用户名" v-bind="componentField" />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

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
