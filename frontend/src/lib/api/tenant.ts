import { api } from './client';

export interface TenantInfo {
	id: string;
	name: string;
	slug: string;
	logoUrl: string | null;
	settings: Record<string, unknown>;
	active: boolean;
}

export function getTenant() {
	return api.get<TenantInfo>('/api/tenant');
}

export function updateTenant(data: { name?: string; logoUrl?: string; settings?: Record<string, unknown> }) {
	return api.put<TenantInfo>('/api/tenant', data);
}
