import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import { useAuthStore } from '@/stores/auth'
import type { MenuTreeNode } from '@/api/auth'

/** path (without leading slash) -> lazy component for dynamic routes */
const pathToComponent: Record<string, () => Promise<unknown>> = {
    'system/user': () => import('@/views/system/user/index.vue'),
    'system/role': () => import('@/views/system/role/index.vue'),
    'system/dept': () => import('@/views/system/dept/index.vue'),
    'system/menu': () => import('@/views/system/menu/index.vue'),
    'monitor': () => import('@/views/monitor/index.vue'),
    'monitor/online': () => import('@/views/monitor/online.vue'),
    'messages': () => import('@/views/messages/index.vue'),
    'messages/all': () => import('@/views/messages/all.vue')
}

function flattenMenuRoutes(nodes: MenuTreeNode[]): { path: string; permission?: string }[] {
    const out: { path: string; permission?: string }[] = []
    function walk(list: MenuTreeNode[]) {
        for (const n of list) {
            const path = n.path ? n.path.replace(/^\//, '') : ''
            if (n.type === 'MENU' && path) {
                out.push({ path, permission: n.permission ?? undefined })
            }
            if (n.children?.length) walk(n.children)
        }
    }
    walk(nodes)
    return out
}

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: Login
        },
        {
            path: '/',
            name: 'MainLayout',
            component: () => import('@/components/layout/MainLayout.vue'),
            redirect: '/welcome',
            children: [
                {
                    path: 'welcome',
                    name: 'Welcome',
                    component: () => import('@/views/Welcome.vue')
                }
            ]
        }
    ]
})

router.beforeEach(async (to, _from, next) => {
    const token = localStorage.getItem('token')
    const store = useAuthStore()

    if (!token && to.name !== 'login') {
        return next({ name: 'login' })
    }

    if (token && !store.initialized && to.name !== 'login') {
        const success = await store.fetchUserInfo()
        if (!success) return next({ name: 'login' })
    }

    if (token && store.initialized && !store.menusLoaded && to.name !== 'login') {
        await store.loadMenus()
        const items = flattenMenuRoutes(store.menus ?? [])
        for (const { path, permission } of items) {
            const load = pathToComponent[path]
            if (load) {
                router.addRoute('MainLayout', {
                    path,
                    name: path.replace(/\//g, '_').replace(/^_/, '') || 'Dynamic',
                    component: load,
                    meta: { permission }
                })
            } else {
                console.warn('No component for menu path:', path)
            }
        }
        return next({ ...to, replace: true })
    }

    if (to.meta.permission) {
        if (!store.hasPermission(to.meta.permission as string)) {
            console.warn(`Access Denied to ${to.path} (Requires: ${to.meta.permission})`)
            return next({ name: 'Welcome' })
        }
    }

    if (token && to.name === 'login') {
        return next({ name: 'Welcome' })
    }

    next()
})

export default router
