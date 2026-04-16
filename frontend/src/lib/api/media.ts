import { api } from './client';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// --- Types ---

export interface MediaDto {
	id: string;
	filename: string;
	originalName: string;
	mimeType: string;
	size: number;
	url: string;
	altText: string | null;
	createdAt: string;
}

export interface ProductMediaDto {
	id: string;
	mediaId: string;
	url: string;
	altText: string | null;
	mediaType: string;
	sortOrder: number;
	primary: boolean;
	createdAt: string;
}

// --- Media CRUD ---

export async function uploadMedia(file: File): Promise<MediaDto> {
	const formData = new FormData();
	formData.append('file', file);

	const token =
		typeof window !== 'undefined' ? localStorage.getItem('access_token') : null;
	const headers: Record<string, string> = {};
	if (token) {
		headers['Authorization'] = `Bearer ${token}`;
	}

	const res = await fetch(`${API_BASE}/api/media/upload`, {
		method: 'POST',
		headers,
		body: formData
	});

	if (!res.ok) {
		const error = await res.json().catch(() => ({ message: 'Upload failed' }));
		throw new Error(error.message || error.error || `Upload failed (HTTP ${res.status})`);
	}

	return res.json();
}

export function listMedia(): Promise<MediaDto[]> {
	return api.get<MediaDto[]>('/api/media');
}

export function getMedia(id: string): Promise<MediaDto> {
	return api.get<MediaDto>(`/api/media/${id}`);
}

export function updateMediaAltText(id: string, altText: string): Promise<MediaDto> {
	return api.put<MediaDto>(`/api/media/${id}`, { altText });
}

export function deleteMedia(id: string): Promise<void> {
	return api.delete(`/api/media/${id}`);
}

// --- Product Media ---

export function getProductMedia(productId: string): Promise<ProductMediaDto[]> {
	return api.get<ProductMediaDto[]>(`/api/products/${productId}/media`);
}

export function linkMediaToProduct(
	productId: string,
	mediaId: string
): Promise<ProductMediaDto> {
	return api.post<ProductMediaDto>(`/api/products/${productId}/media`, { mediaId });
}

export function unlinkMediaFromProduct(
	productId: string,
	mediaId: string
): Promise<void> {
	return api.delete(`/api/products/${productId}/media/${mediaId}`);
}

export function reorderProductMedia(
	productId: string,
	mediaIds: string[]
): Promise<void> {
	return api.put(`/api/products/${productId}/media/reorder`, { mediaIds });
}

// --- Helpers ---

export function getMediaUrl(path: string): string {
	if (path.startsWith('http://') || path.startsWith('https://')) {
		return path;
	}
	return `${API_BASE}${path}`;
}
