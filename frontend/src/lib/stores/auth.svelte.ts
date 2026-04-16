import { api } from '$lib/api/client';
import type { User } from '$lib/types/auth';

class AuthStore {
	user = $state<User | null>(null);
	loading = $state(true);
	error = $state<string | null>(null);

	get isAuthenticated() {
		return this.user !== null;
	}

	get isSuperAdmin() {
		return this.user?.role === 'SUPER_ADMIN';
	}

	get isTenantAdmin() {
		return this.user?.role === 'TENANT_ADMIN';
	}

	get canEdit() {
		return this.user?.role === 'SUPER_ADMIN' || this.user?.role === 'TENANT_ADMIN';
	}

	async init() {
		this.loading = true;
		try {
			const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') : null;
			if (!token) {
				this.user = null;
				return;
			}
			this.user = await api.get<User>('/api/auth/me');
			this.error = null;
		} catch {
			this.user = null;
		} finally {
			this.loading = false;
		}
	}

	handleCallback(accessToken: string, refreshToken: string) {
		api.handleAuthCallback(accessToken, refreshToken);
	}

	async logout() {
		try {
			await api.post('/api/auth/logout');
		} catch {
			// Best-effort: even if the server call fails, clear local state
		}
		api.clearTokens();
		this.user = null;
		if (typeof window !== 'undefined') {
			window.location.href = '/login';
		}
	}
}

export const auth = new AuthStore();
