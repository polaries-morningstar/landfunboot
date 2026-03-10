import { request } from '@/lib/request'

export interface Menu {
    id: number;
    name: string;
    parentId?: number;
    parent?: {
        id: number;
        name: string;
    };
    permission?: string;
    path?: string;
    icon?: string;
    type?: 'DIR' | 'MENU' | 'BUTTON';
    children?: Menu[];
}

export const menuApi = {
    tree: () => {
        return request<Menu[]>('/api/sys/menu/tree', {
            method: 'GET'
        })
    },
    save: (data: Partial<Menu>) => {
        const path = data.id ? '/api/sys/menu/update' : '/api/sys/menu/create'
        return request<number>(path, {
            method: 'POST',
            body: JSON.stringify(data)
        })
    },
    delete: (id: number) => {
        return request<void>('/api/sys/menu/delete', {
            method: 'POST',
            body: JSON.stringify({ id })
        })
    }
}
