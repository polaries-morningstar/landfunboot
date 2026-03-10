import { request } from '@/lib/request'
import type { BasePageQuery, PageResult } from './user'

export type { BasePageQuery, PageResult } from './user'

export type MessageTargetType =
    | 'USER'
    | 'DEPT'
    | 'DEPT_WITH_CHILDREN'
    | 'ALL'
    | 'ROLE'
    | 'USER_IDS'

export interface SendMessageReq {
    title: string
    content?: string
    targetType: MessageTargetType
    targetId?: number
    targetIds?: number[]
}

export interface MessageSender {
    id: number
    username: string
    email: string
}

export interface MessageReceiverItem {
    user: MessageSender
}

export interface MessageView {
    id: number
    title: string
    content?: string
    createdTime: string
    targetType?: MessageTargetType | null
    sender: MessageSender
}

export interface MessageListView {
    id: number
    title: string
    content?: string
    createdTime: string
    targetType?: MessageTargetType | null
    sender: MessageSender
    receivers?: MessageReceiverItem[]
}

export interface MyMessageRow {
    id: number
    readAt?: string | null
    message: {
        id: number
        title: string
        content?: string
        createdTime: string
        sender: MessageSender
    }
}

export const messageApi = {
    send: (data: SendMessageReq) => {
        return request<MessageView>('/api/sys/msg/send', {
            method: 'POST',
            body: JSON.stringify(data)
        })
    },
    list: (params: BasePageQuery) => {
        return request<PageResult<MessageListView>>('/api/sys/msg/list', {
            method: 'GET',
            params: params as Record<string, string | number | undefined>
        })
    },
    myList: (params: BasePageQuery) => {
        return request<PageResult<MyMessageRow>>('/api/sys/msg/my', {
            method: 'GET',
            params: params as Record<string, string | number | undefined>
        })
    },
    getDetail: (messageId: number) => {
        return request<MessageView>(`/api/sys/msg/my/${messageId}`, {
            method: 'GET'
        })
    },
    markRead: (messageId: number) => {
        return request<void>(`/api/sys/msg/my/${messageId}/read`, {
            method: 'PATCH'
        })
    }
}
