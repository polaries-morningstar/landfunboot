<script setup lang="ts">
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { Button } from '@/components/ui/button'
import {
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { useToast } from '@/components/ui/toast/use-toast'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '@/lib/request'

const { toast } = useToast()
const router = useRouter()

const formSchema = toTypedSchema(
  z.object({
    email: z.string().email({ message: '请输入有效的邮箱地址' }).min(2, { message: '邮箱长度至少2位' }),
    password: z.string().min(1, { message: '请输入密码' }),
  })
)

const form = useForm({
  validationSchema: formSchema,
})

const isLoading = ref(false)

const onSubmit = form.handleSubmit(async (values) => {
  isLoading.value = true
  try {
    const data = await request<{ token: string }>('/api/auth/login', {
      method: 'POST',
      body: values,
    })
    const token = data?.token
    if (token) {
      localStorage.setItem('token', token)
      toast({
        title: '登录成功',
        description: '即将进入系统...',
      })
      setTimeout(() => {
        router.push('/')
      }, 500)
    } else {
      throw new Error('未获取到令牌')
    }
  } catch (_error: any) {
    // Error already shown by request()
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <!-- 整页背景：极浅蓝灰 -->
  <div class="flex min-h-screen items-center justify-center" style="background: hsl(210, 20%, 96%);">

    <!-- 登录卡片 -->
    <div class="w-full max-w-[400px] mx-4 rounded-lg overflow-hidden"
         style="background: white; border: 1px solid hsl(var(--border)); box-shadow: 0 4px 24px hsl(214 50% 20% / 0.08);">

      <!-- 卡片顶部蓝色条 -->
      <div class="h-1 w-full" style="background: hsl(var(--primary));"></div>

      <!-- 头部 -->
      <div class="px-8 pt-8 pb-6 text-center">
        <!-- Logo -->
        <div class="w-12 h-12 rounded-xl mx-auto mb-4 flex items-center justify-center font-bold text-xl text-white"
             style="background: hsl(var(--primary));">L</div>
        <h1 class="text-xl font-bold" style="color: hsl(var(--foreground));">Landfun Boot</h1>
        <p class="text-[13px] mt-1" style="color: hsl(var(--muted-foreground));">企业管理系统 · 请登录您的账户</p>
      </div>

      <!-- 表单 -->
      <div class="px-8 pb-8">
        <form @submit="onSubmit" class="grid gap-4">

          <FormField v-slot="{ componentField }" name="email">
            <FormItem>
              <FormLabel class="text-[13px] font-medium" style="color: hsl(215, 25%, 22%);">邮箱地址</FormLabel>
              <FormControl>
                <Input
                  type="text"
                  placeholder="admin@landfun.com"
                  v-bind="componentField"
                  class="h-9 text-[13.5px]"
                  style="border-color: hsl(var(--border));"
                />
              </FormControl>
              <FormMessage class="text-[12px]" />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="password">
            <FormItem>
              <div class="flex items-center justify-between">
                <FormLabel class="text-[13px] font-medium" style="color: hsl(215, 25%, 22%);">密码</FormLabel>
                <a href="#" class="text-[12.5px] hover:underline" style="color: hsl(var(--primary));">忘记密码？</a>
              </div>
              <FormControl>
                <Input
                  type="password"
                  placeholder="请输入密码"
                  v-bind="componentField"
                  class="h-9 text-[13.5px]"
                  style="border-color: hsl(var(--border));"
                />
              </FormControl>
              <FormMessage class="text-[12px]" />
            </FormItem>
          </FormField>

          <Button
            type="submit"
            class="w-full h-9 text-[14px] font-medium mt-1"
            :disabled="isLoading"
            style="background: hsl(var(--primary)); color: white;"
          >
            <svg v-if="isLoading" class="mr-2 h-4 w-4 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            {{ isLoading ? '登录中...' : '登 录' }}
          </Button>

        </form>

        <!-- Footer info -->
        <p class="mt-6 text-center text-[12px]" style="color: hsl(var(--muted-foreground));">
          © 2025 Landfun Technology. All rights reserved.
        </p>
      </div>
    </div>
  </div>
</template>
