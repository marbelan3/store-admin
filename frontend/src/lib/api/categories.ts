import { api } from './client';
import type { Category } from '$lib/types/category';

export function getCategoryTree() {
	return api.get<Category[]>('/api/categories');
}

export function getFlatCategories() {
	return api.get<Category[]>('/api/categories', { flat: 'true' });
}

export function getCategory(id: string) {
	return api.get<Category>(`/api/categories/${id}`);
}

export function createCategory(data: {
	name: string;
	slug?: string;
	description?: string;
	imageUrl?: string;
	sortOrder?: number;
	parentId?: string;
	metaTitle?: string;
	metaDescription?: string;
}) {
	return api.post<Category>('/api/categories', data);
}

export function updateCategory(
	id: string,
	data: {
		name?: string;
		slug?: string;
		description?: string;
		imageUrl?: string;
		sortOrder?: number;
		active?: boolean;
		parentId?: string;
		metaTitle?: string;
		metaDescription?: string;
	}
) {
	return api.put<Category>(`/api/categories/${id}`, data);
}

export function deleteCategory(id: string) {
	return api.delete(`/api/categories/${id}`);
}

export function reorderCategories(categoryIds: string[]) {
	return api.put('/api/categories/reorder', { categoryIds });
}
