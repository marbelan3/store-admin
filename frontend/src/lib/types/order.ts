export type OrderStatus =
	| 'PENDING'
	| 'CONFIRMED'
	| 'PROCESSING'
	| 'SHIPPED'
	| 'DELIVERED'
	| 'CANCELLED'
	| 'REFUNDED';

export interface OrderItem {
	id: number;
	productId: number;
	variantId: number | null;
	productName: string;
	variantInfo: string | null;
	sku: string;
	quantity: number;
	unitPrice: number;
	totalPrice: number;
}

export interface Order {
	id: string;
	customerName: string;
	customerEmail: string;
	customerPhone: string | null;
	status: OrderStatus;
	subtotal: number;
	taxAmount: number;
	shippingAmount: number;
	totalAmount: number;
	currency: string;
	shippingAddress: string | null;
	billingAddress: string | null;
	notes: string | null;
	items: OrderItem[];
	createdAt: string;
	updatedAt: string;
}

export interface OrderStatusHistory {
	id: number;
	fromStatus: OrderStatus | null;
	toStatus: OrderStatus;
	changedBy: string;
	note: string | null;
	createdAt: string;
}

export interface OrderStats {
	totalOrders: number;
	pendingOrders: number;
	confirmedOrders: number;
	processingOrders: number;
	shippedOrders: number;
	deliveredOrders: number;
	cancelledOrders: number;
	refundedOrders: number;
}

export interface CreateOrderRequest {
	customerName: string;
	customerEmail: string;
	customerPhone?: string;
	shippingAddress?: string;
	billingAddress?: string;
	notes?: string;
	items: CreateOrderItemRequest[];
}

export interface CreateOrderItemRequest {
	productId: number;
	variantId?: number;
	quantity: number;
}

export interface UpdateOrderStatusRequest {
	status: OrderStatus;
	note?: string;
}
