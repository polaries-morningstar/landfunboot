import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type UserInfo, type MenuTreeNode } from '@/api/auth'
import { userApi } from '@/api/user'

export const useAuthStore = defineStore('auth', () => {
    const user = ref<UserInfo | null>(null)
    const permissions = ref<Set<string>>(new Set())
    const isLoading = ref(false)
    const initialized = ref(false)
    const menus = ref<MenuTreeNode[]>([])
    const menusLoaded = ref(false)

    async function fetchUserInfo() {
        if (!localStorage.getItem('token')) {
            clearAuth()
            return false
        }
        try {
            isLoading.value = true
            const res = await authApi.info()
            permissions.value = new Set(res.permissions || [])
            const selfUser = await userApi.getSelf()
            user.value = {
                id: selfUser.id,
                username: selfUser.username,
                email: selfUser.email,
                isSuperuser: selfUser.superuser
            }
            initialized.value = true
            return true
        } catch (e) {
            console.error('Failed to fetch user info:', e)
            clearAuth()
            return false
        } finally {
            isLoading.value = false
        }
    }

    function clearAuth() {
        user.value = null
        permissions.value = new Set()
        initialized.value = false
        menus.value = []
        menusLoaded.value = false
        localStorage.removeItem('token')
    }

    async function loadMenus() {
        if (menusLoaded.value) return
        try {
            const tree = await authApi.menus()
            menus.value = tree || []
            menusLoaded.value = true
        } catch (e) {
            console.error('Failed to load menus:', e)
            menus.value = []
        }
    }

    function hasPermission(permission?: string): boolean {
        if (!permission) return true
        if (user.value?.isSuperuser) return true
        return permissions.value.has('*:*:*') || permissions.value.has(permission)
    }

    const permissionsList = computed(() => Array.from(permissions.value))

    return {
        user: computed(() => user.value),
        permissions: permissionsList,
        isLoading: computed(() => isLoading.value),
        initialized: computed(() => initialized.value),
        menus: computed(() => menus.value),
        menusLoaded: computed(() => menusLoaded.value),
        fetchUserInfo,
        clearAuth,
        hasPermission,
        loadMenus
    }
})
