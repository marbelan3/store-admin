import { api } from './client';
import type { Page } from '$lib/types/product';

export interface InventoryItem {
	id: string;
	type: 'PRODUCT' | 'VARIANT';
	productName: string;
	variantName: string | null;
	sku: string | null;
	quantity: number;
	lowStockThreshold: number | null;
	trackInventory: boolean;
	lastSyncedAt: string | null;
}

export interface InventoryFilters {
	lowStock?: boolean;
	outOfStock?: boolean;
	page?: number;
	size?: number;
}

export function getInventory(filters: InventoryFilters = {}) {
	const params: Record<string, string> = {};
	if (filters.lowStock) params.lowStock = 'true';
	if (filters.outOfStock) params.outOfStock = 'true';
	if (filters.page !== undefined) params.page = String(filters.page);
	if (filters.size !== undefined) params.size = String(filters.size);
	return api.get<Page<InventoryItem>>('/api/inventory', params);
}

export interface BulkUpdateItem {
	id: string;
	type: 'PRODUCT' | 'VARIANT';
	quantity: number;
}

export function bulkUpdateInventory(items: BulkUpdateItem[]) {
	return api.post<InventoryItem[]>('/api/inventory/bulk', items);
}

/** Update a single inventory item (quantity and/or threshold). Uses the bulk endpoint under the hood. */
export async function updateInventoryItem(
	id: string,
	type: 'PRODUCT' | 'VARIANT',
	data: { quantity?: number; lowStockThreshold?: number }
): Promise<void> {
	const updates: BulkUpdateItem[] = [];
	if (data.quantity !== undefined) {
		updates.push({ id, type, quantity: data.quantity });
	}
	if (updates.length > 0) {
		await api.post<InventoryItem[]>('/api/inventory/bulk', updates);
	}
}
