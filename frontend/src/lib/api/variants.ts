import { api } from './client';
import type { ProductOption, ProductVariantDetail } from '$lib/types/product';

export function getOptions(productId: string) {
	return api.get<ProductOption[]>(`/api/products/${productId}/options`);
}

export function createOption(productId: string, data: { name: string; values: string[] }) {
	return api.post<ProductOption>(`/api/products/${productId}/options`, data);
}

export function deleteOption(productId: string, optionId: string) {
	return api.delete(`/api/products/${productId}/options/${optionId}`);
}

export function getVariants(productId: string) {
	return api.get<ProductVariantDetail[]>(`/api/products/${productId}/variants`);
}

export function createVariant(
	productId: string,
	data: {
		sku?: string;
		price: number;
		compareAtPrice?: number;
		available?: boolean;
		optionValueIds: string[];
	}
) {
	return api.post<ProductVariantDetail>(`/api/products/${productId}/variants`, data);
}

export function updateVariant(
	productId: string,
	variantId: string,
	data: {
		sku?: string;
		price?: number;
		compareAtPrice?: number;
		available?: boolean;
	}
) {
	return api.put<ProductVariantDetail>(`/api/products/${productId}/variants/${variantId}`, data);
}

export function deleteVariant(productId: string, variantId: string) {
	return api.delete(`/api/products/${productId}/variants/${variantId}`);
}
