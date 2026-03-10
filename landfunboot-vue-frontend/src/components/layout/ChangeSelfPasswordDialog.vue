<script setup lang="ts">
import { ref } from 'vue'
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

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const { toast } = useToast()
const loading = ref(false)

const formSchema = toTypedSchema(
  z.object({
    password: z.string().min(6, '密码至少6个字符'),
    confirmPassword: z.string().min(6, '密码至少6个字符')
  }).refine((data) => data.password === data.confirmPassword, {
    message: "两次输入的密码不一致",
    path: ["confirmPassword"]
  })
)

const form = useForm({
  validationSchema: formSchema,
  initialValues: {
    password: '',
    confirmPassword: ''
  }
})

import { watch } from 'vue'
watch(() => props.open, (newVal) => {
  if (newVal) {
    form.resetForm()
  }
})

const onFormSubmit = form.handleSubmit(async (values) => {
  loading.value = true
  try {
    await userApi.changeSelfPassword({ 
      password: values.password 
    })
    toast({
      title: '修改成功',
      description: `密码已成功修改。下次请使用新密码登录。`,
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
    <DialogContent class="sm:max-w-[425px]">
      <DialogHeader>
        <DialogTitle>修改密码</DialogTitle>
        <DialogDescription>
          修改您自己的登录密码。
        </DialogDescription>
      </DialogHeader>
      
      <div class="py-4">
        <form @submit="onFormSubmit" class="space-y-4">
          <FormField v-slot="{ componentField }" name="password">
            <FormItem>
              <FormLabel>新密码</FormLabel>
              <FormControl>
                <Input type="password" placeholder="******" v-bind="componentField" />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="confirmPassword">
            <FormItem>
              <FormLabel>确认新密码</FormLabel>
              <FormControl>
                <Input type="password" placeholder="******" v-bind="componentField" />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <DialogFooter class="mt-6 pt-2 border-t">
            <Button type="button" variant="outline" @click="emit('update:open', false)">
              取消
            </Button>
            <Button type="submit" :disabled="loading">
              {{ loading ? '提交中...' : '确认修改' }}
            </Button>
          </DialogFooter>
        </form>
      </div>
    </DialogContent>
  </Dialog>
</template>
