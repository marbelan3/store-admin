import { api } from './client';
import type { Page } from '$lib/types/product';

// --- Types ---

export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT';

export interface DiscountListItem {
	id: number;
	name: string;
	code: string;
	type: DiscountType;
	value: number;
	usageLimit: number | null;
	usageCount: number;
	startsAt: string | null;
	endsAt: string | null;
	active: boolean;
	createdAt: string;
}

export interface Discount {
	id: number;
	name: string;
	code: string;
	type: DiscountType;
	value: number;
	minOrderAmount: number | null;
	usageLimit: number | null;
	usageCount: number;
	startsAt: string | null;
	endsAt: string | null;
	active: boolean;
	productIds: number[];
	categoryIds: number[];
	createdAt: string;
	updatedAt: string;
}

export interface CreateDiscountRequest {
	name: string;
	code?: string;
	type: DiscountType;
	value: number;
	minOrderAmount?: number | null;
	usageLimit?: number | null;
	startsAt?: string | null;
	endsAt?: string | null;
	active?: boolean;
	productIds?: number[];
	categoryIds?: number[];
}

export interface UpdateDiscountRequest extends CreateDiscountRequest {}

export interface ValidateDiscountResponse {
	valid: boolean;
	discountAmount: number;
	message?: string;
}

// --- Filters ---

export interface DiscountFilters {
	page?: number;
	size?: number;
	sort?: string;
}

// --- API functions ---

export function getDiscounts(filters: DiscountFilters = {}) {
	const params: Record<string, string> = {};
	if (filters.page !== undefined) params.page = String(filters.page);
	if (filters.size !== undefined) params.size = String(filters.size);
	if (filters.sort) params.sort = filters.sort;
	return api.get<Page<DiscountListItem>>('/api/discounts', params);
}

export function getDiscount(id: number) {
	return api.get<Discount>(`/api/discounts/${id}`);
}

export function createDiscount(data: CreateDiscountRequest) {
	return api.post<Discount>('/api/discounts', data);
}

export function updateDiscount(id: number, data: UpdateDiscountRequest) {
	return api.put<Discount>(`/api/discounts/${id}`, data);
}

export function deleteDiscount(id: number) {
	return api.delete<void>(`/api/discounts/${id}`);
}

export function toggleDiscount(id: number, active: boolean) {
	return api.put<Discount>(`/api/discounts/${id}/toggle`, { active });
}

export function validateDiscountCode(code: string, orderAmount: number) {
	return api.post<ValidateDiscountResponse>(
		`/api/discounts/validate?code=${encodeURIComponent(code)}&orderAmount=${orderAmount}`
	);
}
