import { request } from '@/lib/request'


export interface Dept {
    id: number;
    name: string;
    parentId?: number;
    parent?: {
        id: number;
        name: string;
    };
    children?: Dept[];
}

export const deptApi = {
    tree: () => {
        return request<Dept[]>('/api/sys/dept/tree', {
            method: 'GET'
        })
    },
    save: (data: Partial<Dept>) => {
        const path = data.id ? '/api/sys/dept/update' : '/api/sys/dept/create'
        return request<number>(path, {
            method: 'POST',
            body: JSON.stringify(data)
        })
    },
    delete: (id: number) => {
        return request<void>('/api/sys/dept/delete', {
            method: 'POST',
            body: JSON.stringify({ id })
        })
    }
}
