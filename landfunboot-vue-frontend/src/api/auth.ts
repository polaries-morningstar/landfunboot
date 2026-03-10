import { request } from '@/lib/request'

export interface UserInfo {
    id: number;
    username: string;
    email: string;
    isSuperuser?: boolean;
}

export interface AuthInfoResponse {
    user: UserInfo;
    permissions: string[];
}

export interface MenuTreeNode {
    id: number
    name: string
    path?: string | null
    icon?: string | null
    permission?: string | null
    type?: string
    children?: MenuTreeNode[]
}

export const authApi = {
    info() {
        return request<AuthInfoResponse>('/api/auth/info', {
            method: 'GET'
        })
    },
    menus() {
        return request<MenuTreeNode[]>('/api/auth/menus', {
            method: 'GET'
        })
    }
}
