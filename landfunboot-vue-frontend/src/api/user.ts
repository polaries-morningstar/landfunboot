import { request } from '@/lib/request'

export interface User {
    id: number;
    username: string;
    email: string;
    dept?: {
        id: number;
        name: string;
    };
    createdTime: string;
    active: boolean;
    superuser: boolean;
    role?: {
        id: number;
        name: string;
    };
}

export interface UserPayload {
    id?: number;
    username: string;
    email: string;
    active?: boolean;
    deptId?: number;
    roleId?: number | null;
    password?: string;
}

export interface PageResult<T> {
    total: number;
    rows: T[];
}

export interface BasePageQuery {
    page?: number;
    size?: number;
    sort?: string;
}

export const userApi = {
    page: (query: BasePageQuery) => {
        return request<PageResult<User>>('/api/sys/user', {
            method: 'GET',
            params: query as any
        })
    },
    get: (id: number) => {
        return request<User>(`/api/sys/user/${id}`, {
            method: 'GET'
        })
    },
    getSelf: () => {
        return request<User>('/api/sys/user/self', {
            method: 'GET'
        })
    },
    create: (data: UserPayload & { password: string }) => {
        return request<User>('/api/sys/user', {
            method: 'POST',
            body: JSON.stringify(data)
        })
    },
    update: (data: UserPayload & { id: number }) => {
        return request<User>('/api/sys/user', {
            method: 'PUT',
            body: JSON.stringify(data)
        })
    },
    save: (data: UserPayload) => {
        if (data.id) {
            return request<User>('/api/sys/user', {
                method: 'PUT',
                body: JSON.stringify(data)
            })
        } else {
            return request<User>('/api/sys/user', {
                method: 'POST',
                body: JSON.stringify(data)
            })
        }
    },
    changePassword: (data: { id: number; password: string }) => {
        return request<void>('/api/sys/user/password', {
            method: 'POST',
            body: JSON.stringify(data)
        })
    },
    updateSelf: (data: { username: string }) => {
        return request<User>('/api/sys/user/self', {
            method: 'PUT',
            body: JSON.stringify(data)
        })
    },
    changeSelfPassword: (data: { password: string }) => {
        return request<void>('/api/sys/user/password/self', {
            method: 'POST',
            body: JSON.stringify(data)
        })
    },
    delete: (id: number) => {
        return request<void>(`/api/sys/user/${id}`, {
            method: 'DELETE'
        })
    },
    onlineUsers: () => {
        return request<any[]>('/api/sys/user/online', {
            method: 'GET'
        })
    },
    kickout: (userId: number) => {
        return request<void>(`/api/sys/user/online/${userId}`, {
            method: 'DELETE'
        })
    }
}
