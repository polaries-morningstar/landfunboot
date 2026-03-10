import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'

export function useAuth() {
    const store = useAuthStore()
    const { user, permissions, isLoading, initialized } = storeToRefs(store)
    return {
        user,
        permissions,
        isLoading,
        initialized,
        fetchUserInfo: store.fetchUserInfo,
        clearAuth: store.clearAuth,
        hasPermission: store.hasPermission
    }
}
