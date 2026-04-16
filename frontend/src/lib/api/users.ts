import { api } from './client';

export interface UserInfo {
	id: string;
	email: string;
	name: string;
	avatarUrl: string;
	role: string;
	active: boolean;
	tenantId: string;
	createdAt: string;
}

export function getUsers() {
	return api.get<UserInfo[]>('/api/users');
}

export function updateUserRole(id: string, role: string) {
	return api.fetch<UserInfo>(`/api/users/${id}/role`, {
		method: 'PATCH',
		body: JSON.stringify({ role })
	});
}

export function toggleUserActive(id: string) {
	return api.fetch<UserInfo>(`/api/users/${id}/toggle-active`, {
		method: 'PATCH'
	});
}
