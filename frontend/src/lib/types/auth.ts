export interface User {
	id: string;
	email: string;
	name: string;
	avatarUrl: string;
	role: 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'TENANT_VIEWER';
	tenantId: string;
}
