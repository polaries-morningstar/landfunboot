import { request } from '@/lib/request'
import type { BasePageQuery, PageResult } from './user'

export interface Role {
    id: number;
    code: string;
    name: string;
    description: string;
    dataScope: 'ALL' | 'DEPT_SAME' | 'DEPT_RECURSIVE' | 'SELF' | 'DEPT_CUSTOM';
    deptIds?: number[];
    menuIds?: number[];
    menus?: { id: number }[];
}

export const roleApi = {
    page: (query: BasePageQuery) => {
        return request<PageResult<Role>>('/api/sys/role', {
            method: 'GET',
            params: query as any
        })
    },
    all: () => {
        return request<Role[]>('/api/sys/role/all', {
            method: 'GET'
        })
    },
    get: (id: number) => {
        return request<Role>(`/api/sys/role/${id}`, {
            method: 'GET'
        })
    },
    save: (data: Partial<Role>) => {
        if (data.id) {
            return request<number>('/api/sys/role', {
                method: 'PUT',
                body: JSON.stringify(data)
            })
        } else {
            return request<number>('/api/sys/role', {
                method: 'POST',
                body: JSON.stringify(data)
            })
        }
    },
    delete: (id: number) => {
        return request<void>(`/api/sys/role/${id}`, {
            method: 'DELETE'
        })
    }
}
