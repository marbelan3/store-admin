import { api } from './client';
import type { Page } from '$lib/types/product';

// ── Types ──────────────────────────────────────────────────────────

export interface CustomerList {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	phone: string | null;
	totalOrders: number;
	totalSpent: number;
	createdAt: string;
}

export interface CustomerAddress {
	id: number;
	type: 'SHIPPING' | 'BILLING';
	line1: string;
	line2: string | null;
	city: string;
	state: string;
	postalCode: string;
	country: string;
	isDefault: boolean;
}

export interface Customer {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	phone: string | null;
	notes: string | null;
	totalOrders: number;
	totalSpent: number;
	addresses: CustomerAddress[];
	createdAt: string;
	updatedAt: string;
}

export interface CreateCustomerRequest {
	firstName: string;
	lastName: string;
	email: string;
	phone?: string;
	notes?: string;
}

export interface UpdateCustomerRequest {
	firstName: string;
	lastName: string;
	email: string;
	phone?: string;
	notes?: string;
}

export interface CreateAddressRequest {
	type: 'SHIPPING' | 'BILLING';
	line1: string;
	line2?: string;
	city: string;
	state: string;
	postalCode: string;
	country: string;
	isDefault?: boolean;
}

export interface UpdateAddressRequest extends CreateAddressRequest {}

// ── Filters ────────────────────────────────────────────────────────

export interface CustomerFilters {
	search?: string;
	page?: number;
	size?: number;
	sort?: string;
}

// ── API functions ──────────────────────────────────────────────────

export function getCustomers(filters: CustomerFilters = {}) {
	const params: Record<string, string> = {};
	if (filters.search) params.search = filters.search;
	if (filters.page !== undefined) params.page = String(filters.page);
	if (filters.size !== undefined) params.size = String(filters.size);
	if (filters.sort) params.sort = filters.sort;
	return api.get<Page<CustomerList>>('/api/customers', params);
}

export function getCustomer(id: number) {
	return api.get<Customer>(`/api/customers/${id}`);
}

export function createCustomer(data: CreateCustomerRequest) {
	return api.post<Customer>('/api/customers', data);
}

export function updateCustomer(id: number, data: UpdateCustomerRequest) {
	return api.put<Customer>(`/api/customers/${id}`, data);
}

export function deleteCustomer(id: number) {
	return api.delete<void>(`/api/customers/${id}`);
}

// ── Address API ────────────────────────────────────────────────────

export function addAddress(customerId: number, data: CreateAddressRequest) {
	return api.post<CustomerAddress>(`/api/customers/${customerId}/addresses`, data);
}

export function updateAddress(customerId: number, addressId: number, data: UpdateAddressRequest) {
	return api.put<CustomerAddress>(`/api/customers/${customerId}/addresses/${addressId}`, data);
}

export function deleteAddress(customerId: number, addressId: number) {
	return api.delete<void>(`/api/customers/${customerId}/addresses/${addressId}`);
}
