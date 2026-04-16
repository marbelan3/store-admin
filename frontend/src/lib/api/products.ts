import { api } from './client';
import type { Product, ProductListItem, Page } from '$lib/types/product';

export interface ProductFilters {
	status?: string;
	search?: string;
	categoryId?: string;
	priceMin?: string;
	priceMax?: string;
	source?: string;
	page?: number;
	size?: number;
	sort?: string;
}

export function getProducts(filters: ProductFilters = {}) {
	const params: Record<string, string> = {};
	if (filters.status) params.status = filters.status;
	if (filters.search) params.search = filters.search;
	if (filters.categoryId) params.categoryId = filters.categoryId;
	if (filters.priceMin) params.priceMin = filters.priceMin;
	if (filters.priceMax) params.priceMax = filters.priceMax;
	if (filters.source) params.source = filters.source;
	if (filters.page !== undefined) params.page = String(filters.page);
	if (filters.size !== undefined) params.size = String(filters.size);
	if (filters.sort) params.sort = filters.sort;
	return api.get<Page<ProductListItem>>('/api/products', params);
}

export function getProduct(id: string) {
	return api.get<Product>(`/api/products/${id}`);
}

export function createProduct(data: Partial<Product>) {
	return api.post<Product>('/api/products', data);
}

export function updateProduct(id: string, data: Partial<Product>) {
	return api.put<Product>(`/api/products/${id}`, data);
}

/** Partial update for inline editing (price, status, etc.) */
export function patchProduct(id: string, data: Partial<Pick<Product, 'price' | 'status' | 'name'>>) {
	return api.put<Product>(`/api/products/${id}`, data);
}

export function deleteProduct(id: string) {
	return api.delete(`/api/products/${id}`);
}

export function bulkDeleteProducts(ids: string[]): Promise<{ affected: number }> {
	return api.post('/api/products/bulk', {
		action: 'DELETE',
		productIds: ids
	});
}

export function bulkUpdateProductStatus(
	ids: string[],
	status: string
): Promise<{ affected: number }> {
	return api.post('/api/products/bulk', {
		action: 'CHANGE_STATUS',
		productIds: ids,
		status
	});
}
