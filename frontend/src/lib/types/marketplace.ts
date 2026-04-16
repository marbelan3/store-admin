export type MarketplaceProvider = 'CJ_DROPSHIPPING' | 'ALIEXPRESS';
export type ConnectionStatus = 'ACTIVE' | 'INACTIVE' | 'ERROR' | 'TOKEN_EXPIRED';
export type SyncStatus = 'PENDING' | 'SYNCED' | 'ERROR' | 'DELISTED';
export type PricingRule = 'MARGIN' | 'FIXED_MARKUP' | 'MANUAL';
export type SkuStatus = 'ACTIVE' | 'CHANGED' | 'DISCONTINUED' | 'NOT_FOUND';
export type ExternalStockStatus = 'IN_STOCK' | 'LOW' | 'OUT_OF_STOCK';

export interface MarketplaceConnection {
	id: string;
	provider: MarketplaceProvider;
	email: string | null;
	status: ConnectionStatus;
	syncEnabled: boolean;
	defaultWarehouseId: string | null;
	defaultShippingMethod: string | null;
	lastConnectedAt: string | null;
	createdAt: string;
	updatedAt: string;
}

export interface CreateConnectionRequest {
	provider: string;
	email: string;
	apiKey: string;
	defaultWarehouseId?: string;
	defaultShippingMethod?: string;
}

export interface UpdateConnectionRequest {
	email?: string;
	apiKey?: string;
	syncEnabled?: boolean;
	defaultWarehouseId?: string;
	defaultShippingMethod?: string;
}

export interface VariantMapping {
	id: string;
	variantId: string;
	variantName: string | null;
	cjVariantId: string;
	cjSku: string;
	previousCjSku: string | null;
	warehouseId: string | null;
	warehouseCountry: string | null;
	sourcePrice: number | null;
	shippingEstimate: number | null;
	stockQuantity: number | null;
	stockLastCheckedAt: string | null;
	skuStatus: SkuStatus;
}

export interface MarketplaceProduct {
	id: string;
	productId: string;
	productName: string;
	externalProductId: string;
	syncStatus: SyncStatus;
	pricingRule: PricingRule;
	targetMarginPct: number | null;
	currentMarginPct: number | null;
	minMarginPct: number | null;
	marginAlertTriggered: boolean;
	excluded: boolean;
	lowStockThreshold: number | null;
	stockAlertSent: boolean;
	variantMappings: VariantMapping[];
	createdAt: string;
	updatedAt: string;
}

export interface CjCatalogProduct {
	pid: string;
	name: string;
	image: string;
	categoryName: string;
	price: number;
}

export interface CjVariant {
	vid: string;
	name: string;
	sku: string;
	price: number;
	image: string | null;
	stock: number;
}

export interface CjProductDetail {
	pid: string;
	name: string;
	description: string;
	image: string;
	categoryName: string;
	price: number;
	variants: CjVariant[];
}

export interface ImportProductRequest {
	connectionId: string;
	externalProductId: string;
	variants: ImportVariantRequest[];
	pricingRule?: string;
	targetMarginPct?: number;
	fixedMarkupAmount?: number;
	minMarginPct?: number;
	categoryId?: string;
	lowStockThreshold?: number;
}

export interface ImportVariantRequest {
	cjVariantId: string;
	cjSku: string;
	warehouseId?: string;
	warehouseCountry?: string;
	manualPrice?: number;
}

export interface WatchlistItem {
	id: string;
	externalProductId: string;
	name: string;
	imageUrl: string | null;
	price: number;
	stockStatus: ExternalStockStatus;
	lastCheckedAt: string | null;
	addedAt: string;
}

export interface SyncLog {
	id: string;
	connectionId: string;
	syncType: string;
	status: string;
	itemsChecked: number;
	itemsUpdated: number;
	errorsCount: number;
	errorDetails: string | null;
	startedAt: string;
	completedAt: string | null;
}

export interface MarketplaceAlert {
	marketplaceProductId: string;
	productId: string;
	productName: string;
	alertType: string;
	message: string;
	currentMarginPct: number | null;
	minMarginPct: number | null;
}

export interface PriceHistory {
	id: string;
	priceType: string;
	oldPrice: number | null;
	newPrice: number;
	oldMarginPct: number | null;
	newMarginPct: number | null;
	detectedAt: string;
}

export interface MarketplaceOrderResult {
	orderId: string;
	cjOrderId: string;
	status: string;
	warnings: string[];
}
