import { api } from './client';
import type { Page } from '$lib/types/product';

export interface Notification {
	id: string;
	type: string;
	title: string;
	message: string | null;
	read: boolean;
	createdAt: string;
}

export function getNotifications(page = 0, size = 20) {
	return api.get<Page<Notification>>('/api/notifications', {
		page: String(page),
		size: String(size)
	});
}

export function getUnreadCount() {
	return api.get<{ count: number }>('/api/notifications/unread-count');
}

export function markAsRead(id: string) {
	return api.fetch<void>(`/api/notifications/${id}/read`, { method: 'PATCH' });
}

export function markAllAsRead() {
	return api.post<void>('/api/notifications/read-all');
}
