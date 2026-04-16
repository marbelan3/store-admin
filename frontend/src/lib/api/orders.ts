import { api } from './client';
import type {
	Order,
	OrderStats,
	OrderStatus,
	OrderStatusHistory,
	CreateOrderRequest,
	UpdateOrderStatusRequest
} from '$lib/types/order';
import type { Page } from '$lib/types/product';

export interface OrderFilters {
	status?: string;
	search?: string;
	page?: number;
	size?: number;
	sort?: string;
}

export function getOrders(filters: OrderFilters = {}) {
	const params: Record<string, string> = {};
	if (filters.status) params.status = filters.status;
	if (filters.search) params.search = filters.search;
	if (filters.page !== undefined) params.page = String(filters.page);
	if (filters.size !== undefined) params.size = String(filters.size);
	if (filters.sort) params.sort = filters.sort;
	return api.get<Page<Order>>('/api/orders', params);
}

export function getOrder(id: string) {
	return api.get<Order>(`/api/orders/${id}`);
}

export function createOrder(data: CreateOrderRequest) {
	return api.post<Order>('/api/orders', data);
}

export function updateOrderStatus(id: string, data: UpdateOrderStatusRequest) {
	return api.put<Order>(`/api/orders/${id}/status`, data);
}

export function getOrderHistory(id: string) {
	return api.get<OrderStatusHistory[]>(`/api/orders/${id}/history`);
}

export function getOrderStats() {
	return api.get<OrderStats>('/api/orders/stats');
}

export async function bulkUpdateOrderStatus(ids: string[], status: string): Promise<void> {
	await Promise.all(ids.map((id) => updateOrderStatus(id, { status: status as OrderStatus })));
}
