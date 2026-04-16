import { api } from './client';
import type {
	MarketplaceConnection,
	CreateConnectionRequest,
	UpdateConnectionRequest,
	CjCatalogProduct,
	CjProductDetail,
	MarketplaceProduct,
	ImportProductRequest,
	WatchlistItem,
	SyncLog,
	MarketplaceAlert,
	MarketplaceOrderResult,
	PriceHistory
} from '$lib/types/marketplace';
import type { Page } from '$lib/types/product';

// --- Connections ---

export function getConnections() {
	return api.get<MarketplaceConnection[]>('/api/marketplace/connections');
}

export function getConnection(id: string) {
	return api.get<MarketplaceConnection>(`/api/marketplace/connections/${id}`);
}

export function createConnection(data: CreateConnectionRequest) {
	return api.post<MarketplaceConnection>('/api/marketplace/connections', data);
}

export function updateConnection(id: string, data: UpdateConnectionRequest) {
	return api.put<MarketplaceConnection>(`/api/marketplace/connections/${id}`, data);
}

export function deleteConnection(id: string) {
	return api.delete(`/api/marketplace/connections/${id}`);
}

export function testConnection(id: string) {
	return api.post<MarketplaceConnection>(`/api/marketplace/connections/${id}/test`);
}

// --- CJ Catalog ---

export function searchCatalog(connectionId: string, q: string, page = 1, pageSize = 20) {
	return api.get<CjCatalogProduct[]>('/api/marketplace/cj/catalog', {
		connectionId,
		q,
		page: String(page),
		pageSize: String(pageSize)
	});
}

export function getCjProductDetail(connectionId: string, pid: string) {
	return api.get<CjProductDetail>(`/api/marketplace/cj/products/${pid}`, { connectionId });
}

// --- Import ---

export function importProduct(data: ImportProductRequest) {
	return api.post<MarketplaceProduct>('/api/marketplace/import', data);
}

export function getMarketplaceProducts(page = 0, size = 20) {
	return api.get<Page<MarketplaceProduct>>('/api/marketplace/products', {
		page: String(page),
		size: String(size)
	});
}

export function getMarketplaceProduct(id: string) {
	return api.get<MarketplaceProduct>(`/api/marketplace/products/${id}`);
}

export function updatePricing(id: string, data: {
	pricingRule?: string;
	targetMarginPct?: number;
	fixedMarkupAmount?: number;
	minMarginPct?: number;
}) {
	return api.fetch<MarketplaceProduct>(`/api/marketplace/products/${id}/pricing`, {
		method: 'PATCH',
		body: JSON.stringify(data)
	});
}

export function toggleExclude(id: string) {
	return api.fetch<MarketplaceProduct>(`/api/marketplace/products/${id}/exclude`, { method: 'PATCH' });
}

export function updateThreshold(id: string, threshold: number) {
	return api.fetch<MarketplaceProduct>(`/api/marketplace/products/${id}/threshold`, {
		method: 'PATCH',
		body: JSON.stringify({ threshold })
	});
}

// --- Watchlist ---

export function getWatchlist(page = 0, size = 20) {
	return api.get<Page<WatchlistItem>>('/api/marketplace/watchlist', {
		page: String(page),
		size: String(size)
	});
}

export function addToWatchlist(connectionId: string, externalProductId: string) {
	return api.post<WatchlistItem>('/api/marketplace/watchlist', { connectionId, externalProductId });
}

export function removeFromWatchlist(id: string) {
	return api.delete(`/api/marketplace/watchlist/${id}`);
}

// --- Price History ---

export function getPriceHistory(mappingId: string) {
	return api.get<PriceHistory[]>(`/api/marketplace/variants/${mappingId}/price-history`);
}

// --- Sync ---

export function getSyncLogs(connectionId: string, page = 0, size = 20) {
	return api.get<Page<SyncLog>>(`/api/marketplace/sync-logs/${connectionId}`, {
		page: String(page),
		size: String(size)
	});
}

export function triggerSync(connectionId: string) {
	return api.post(`/api/marketplace/sync/trigger/${connectionId}`);
}

export function getAlerts() {
	return api.get<MarketplaceAlert[]>('/api/marketplace/alerts');
}

// --- Orders ---

export function placeMarketplaceOrder(orderId: string, data: {
	shippingCountry: string;
	shippingProvince: string;
	shippingCity: string;
	shippingAddress: string;
	shippingZip: string;
	shippingCustomerName: string;
	shippingPhone: string;
}) {
	return api.post<MarketplaceOrderResult>(`/api/marketplace/orders/${orderId}/place`, data);
}
