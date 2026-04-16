export interface Product {
	id: string;
	name: string;
	slug: string;
	description: string;
	shortDescription: string;
	status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
	price: number;
	compareAtPrice: number | null;
	currency: string;
	sku: string;
	barcode: string;
	trackInventory: boolean;
	quantity: number;
	weight: number | null;
	weightUnit: string;
	metaTitle: string;
	metaDescription: string;
	attributes: Record<string, unknown>;
	publishedAt: string | null;
	createdAt: string;
	updatedAt: string;
	variants: ProductVariant[];
	media: ProductMedia[];
	categories: CategoryRef[];
	tags: TagRef[];
}

export interface ProductVariant {
	id: string;
	name: string;
	sku: string;
	price: number;
	compareAtPrice: number | null;
	costPrice: number | null;
	quantity: number;
	lowStockThreshold: number | null;
	options: Record<string, unknown>;
	sortOrder: number;
	active: boolean;
}

export interface ProductMedia {
	id: string;
	url: string;
	altText: string;
	mediaType: string;
	sortOrder: number;
	primary: boolean;
}

export interface CategoryRef {
	id: string;
	name: string;
	slug: string;
}

export interface TagRef {
	id: string;
	name: string;
	slug: string;
}

/** Lightweight product type returned by the list endpoint (no collections). */
export interface ProductListItem {
	id: string;
	name: string;
	slug: string;
	status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
	price: number;
	compareAtPrice: number | null;
	currency: string;
	sku: string;
	trackInventory: boolean;
	quantity: number;
	publishedAt: string | null;
	createdAt: string;
	updatedAt: string;
	primaryImageUrl: string | null;
	source: 'OWN' | 'MARKETPLACE';
}

export interface Page<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	size: number;
	number: number;
	first: boolean;
	last: boolean;
}

/** Option value (e.g. "Red", "XL") */
export interface OptionValue {
	id: string;
	value: string;
	displayOrder: number;
}

/** Product option (e.g. "Color", "Size") with its values */
export interface ProductOption {
	id: string;
	name: string;
	displayOrder: number;
	values: OptionValue[];
}

/** A specific variant of a product with its own SKU/price/availability */
export interface ProductVariantDetail {
	id: string;
	sku: string;
	price: number;
	compareAtPrice: number | null;
	available: boolean;
	optionValues: OptionValue[];
	createdAt: string;
	updatedAt: string;
}
