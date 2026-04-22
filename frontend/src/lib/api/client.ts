const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface RequestOptions extends RequestInit {
	params?: Record<string, string>;
}

class ApiClient {
	private baseUrl: string;

	constructor(baseUrl: string) {
		this.baseUrl = baseUrl;
	}

	private getAccessToken(): string | null {
		if (typeof window === 'undefined') return null;
		return localStorage.getItem('access_token');
	}

	private setTokens(accessToken: string, refreshToken: string) {
		localStorage.setItem('access_token', accessToken);
		localStorage.setItem('refresh_token', refreshToken);
	}

	clearTokens() {
		localStorage.removeItem('access_token');
		localStorage.removeItem('refresh_token');
	}

	private async refreshAccessToken(): Promise<boolean> {
		const refreshToken = localStorage.getItem('refresh_token');
		if (!refreshToken) return false;

		try {
			const res = await fetch(`${this.baseUrl}/api/auth/refresh`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ refreshToken })
			});

			if (!res.ok) return false;

			const data = await res.json();
			this.setTokens(data.accessToken, data.refreshToken);
			return true;
		} catch {
			return false;
		}
	}

	async fetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
		const { params, ...init } = options;

		let url = `${this.baseUrl}${path}`;
		if (params) {
			const searchParams = new URLSearchParams(params);
			url += `?${searchParams.toString()}`;
		}

		const headers = new Headers(init.headers);
		const token = this.getAccessToken();
		if (token) {
			headers.set('Authorization', `Bearer ${token}`);
		}
		if (!headers.has('Content-Type') && init.body) {
			headers.set('Content-Type', 'application/json');
		}

		let res = await fetch(url, { ...init, headers });

		if (res.status === 401) {
			const refreshed = await this.refreshAccessToken();
			if (refreshed) {
				headers.set('Authorization', `Bearer ${this.getAccessToken()}`);
				res = await fetch(url, { ...init, headers });
			} else {
				this.clearTokens();
				if (typeof window !== 'undefined') {
					window.location.href = '/login';
				}
				throw new Error('Unauthorized');
			}
		}

		if (!res.ok) {
			const error = await res.json().catch(() => ({ message: res.statusText }));
			const err = new Error(error.message || error.error || `HTTP ${res.status}`) as Error & {
				status: number;
				body: unknown;
			};
			err.status = res.status;
			err.body = error;
			throw err;
		}

		if (res.status === 204 || res.status === 202) return undefined as T;
		const text = await res.text();
		if (!text) return undefined as T;
		return JSON.parse(text);
	}

	get<T>(path: string, params?: Record<string, string>) {
		return this.fetch<T>(path, { params });
	}

	post<T>(path: string, body?: unknown) {
		return this.fetch<T>(path, {
			method: 'POST',
			body: body ? JSON.stringify(body) : undefined
		});
	}

	put<T>(path: string, body?: unknown) {
		return this.fetch<T>(path, {
			method: 'PUT',
			body: body ? JSON.stringify(body) : undefined
		});
	}

	delete<T>(path: string) {
		return this.fetch<T>(path, { method: 'DELETE' });
	}

	handleAuthCallback(accessToken: string, refreshToken: string) {
		this.setTokens(accessToken, refreshToken);
	}
}

export const api = new ApiClient(API_BASE);
