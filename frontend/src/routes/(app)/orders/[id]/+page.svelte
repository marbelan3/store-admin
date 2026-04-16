<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { auth } from '$lib/stores/auth.svelte';
	import { getOrder, updateOrderStatus, getOrderHistory } from '$lib/api/orders';
	import type { Order, OrderStatus, OrderStatusHistory } from '$lib/types/order';
	import { Button } from '$lib/components/ui/button';
	import { Badge } from '$lib/components/ui/badge';
	import * as Card from '$lib/components/ui/card';
	import * as Table from '$lib/components/ui/table';
	import * as Dialog from '$lib/components/ui/dialog';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Textarea } from '$lib/components/ui/textarea';
	import { Label } from '$lib/components/ui/label';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';

	import ArrowLeftIcon from '@lucide/svelte/icons/arrow-left';
	import UserIcon from '@lucide/svelte/icons/user';
	import MailIcon from '@lucide/svelte/icons/mail';
	import PhoneIcon from '@lucide/svelte/icons/phone';
	import MapPinIcon from '@lucide/svelte/icons/map-pin';
	import PackageIcon from '@lucide/svelte/icons/package';
	import ClockIcon from '@lucide/svelte/icons/clock';
	import CheckIcon from '@lucide/svelte/icons/check';
	import XIcon from '@lucide/svelte/icons/x';
	import TruckIcon from '@lucide/svelte/icons/truck';
	import CogIcon from '@lucide/svelte/icons/cog';
	import RotateCcwIcon from '@lucide/svelte/icons/rotate-ccw';

	const STATUS_CONFIG: Record<OrderStatus, { label: string; class: string; dotClass: string }> = {
		PENDING: {
			label: 'Pending',
			class: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400 border-amber-200 dark:border-amber-800',
			dotClass: 'bg-amber-500'
		},
		CONFIRMED: {
			label: 'Confirmed',
			class: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400 border-blue-200 dark:border-blue-800',
			dotClass: 'bg-blue-500'
		},
		PROCESSING: {
			label: 'Processing',
			class: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400 border-indigo-200 dark:border-indigo-800',
			dotClass: 'bg-indigo-500'
		},
		SHIPPED: {
			label: 'Shipped',
			class: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400 border-purple-200 dark:border-purple-800',
			dotClass: 'bg-purple-500'
		},
		DELIVERED: {
			label: 'Delivered',
			class: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800',
			dotClass: 'bg-emerald-500'
		},
		CANCELLED: {
			label: 'Cancelled',
			class: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 border-red-200 dark:border-red-800',
			dotClass: 'bg-red-500'
		},
		REFUNDED: {
			label: 'Refunded',
			class: 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400 border-gray-200 dark:border-gray-700',
			dotClass: 'bg-gray-500'
		}
	};

	// Valid status transitions
	const TRANSITIONS: Record<OrderStatus, { status: OrderStatus; label: string; variant: 'default' | 'destructive' | 'outline' }[]> = {
		PENDING: [
			{ status: 'CONFIRMED', label: 'Confirm', variant: 'default' },
			{ status: 'CANCELLED', label: 'Cancel', variant: 'destructive' }
		],
		CONFIRMED: [
			{ status: 'PROCESSING', label: 'Start Processing', variant: 'default' },
			{ status: 'CANCELLED', label: 'Cancel', variant: 'destructive' }
		],
		PROCESSING: [
			{ status: 'SHIPPED', label: 'Mark Shipped', variant: 'default' },
			{ status: 'CANCELLED', label: 'Cancel', variant: 'destructive' }
		],
		SHIPPED: [
			{ status: 'DELIVERED', label: 'Mark Delivered', variant: 'default' }
		],
		DELIVERED: [
			{ status: 'REFUNDED', label: 'Refund', variant: 'destructive' }
		],
		CANCELLED: [],
		REFUNDED: []
	};

	let orderId = $derived($page.params.id as string);
	let order = $state<Order | null>(null);
	let history = $state<OrderStatusHistory[]>([]);
	let loading = $state(true);
	let historyLoading = $state(true);

	// Status change dialog
	let statusDialogOpen = $state(false);
	let targetStatus = $state<OrderStatus | null>(null);
	let targetLabel = $state('');
	let statusNote = $state('');
	let statusUpdating = $state(false);

	function shortId(id: string): string {
		return id.length > 8 ? id.substring(0, 8).toUpperCase() : id;
	}

	function formatCurrency(amount: number, currency: string): string {
		return new Intl.NumberFormat('en-US', { style: 'currency', currency: currency || 'USD' }).format(amount);
	}

	function relativeTime(dateStr: string): string {
		const date = new Date(dateStr);
		const now = new Date();
		const diffMs = now.getTime() - date.getTime();
		const diffMins = Math.floor(diffMs / 60000);
		if (diffMins < 1) return 'just now';
		if (diffMins < 60) return `${diffMins}m ago`;
		const diffHours = Math.floor(diffMins / 60);
		if (diffHours < 24) return `${diffHours}h ago`;
		const diffDays = Math.floor(diffHours / 24);
		if (diffDays < 30) return `${diffDays}d ago`;
		return date.toLocaleDateString();
	}

	async function loadOrder() {
		loading = true;
		try {
			order = await getOrder(orderId);
		} catch (err) {
			toast.error('Failed to load order');
			goto('/orders');
		} finally {
			loading = false;
		}
	}

	async function loadHistory() {
		historyLoading = true;
		try {
			history = await getOrderHistory(orderId);
		} catch {
			// non-critical
		} finally {
			historyLoading = false;
		}
	}

	function openStatusDialog(status: OrderStatus, label: string) {
		targetStatus = status;
		targetLabel = label;
		statusNote = '';
		statusDialogOpen = true;
	}

	async function confirmStatusChange() {
		if (!targetStatus) return;
		statusUpdating = true;
		try {
			order = await updateOrderStatus(orderId, {
				status: targetStatus,
				note: statusNote || undefined
			});
			toast.success(`Order status updated to ${STATUS_CONFIG[targetStatus].label}`);
			statusDialogOpen = false;
			loadHistory();
		} catch (err: any) {
			toast.error(err.message || 'Failed to update status');
		} finally {
			statusUpdating = false;
		}
	}

	onMount(() => {
		loadOrder();
		loadHistory();
	});
</script>

<!-- Status Change Confirmation Dialog -->
<Dialog.Root bind:open={statusDialogOpen}>
	<Dialog.Content class="sm:max-w-md">
		<Dialog.Header>
			<Dialog.Title>Change Order Status</Dialog.Title>
			<Dialog.Description>
				Change status to <strong>{targetLabel}</strong>. Optionally add a note.
			</Dialog.Description>
		</Dialog.Header>
		<div class="space-y-4 py-2">
			<div class="space-y-2">
				<Label for="status-note">Note (optional)</Label>
				<Textarea
					id="status-note"
					bind:value={statusNote}
					placeholder="Add a note about this status change..."
					rows={3}
				/>
			</div>
		</div>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (statusDialogOpen = false)} disabled={statusUpdating}>
				Cancel
			</Button>
			<Button
				variant={targetStatus === 'CANCELLED' || targetStatus === 'REFUNDED' ? 'destructive' : 'default'}
				onclick={confirmStatusChange}
				disabled={statusUpdating}
			>
				{statusUpdating ? 'Updating...' : targetLabel}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<div class="space-y-6">
	{#if loading}
		<div class="space-y-4">
			<Skeleton class="h-6 w-48" />
			<Skeleton class="h-4 w-32" />
			<div class="grid gap-6 lg:grid-cols-3">
				<div class="lg:col-span-2 space-y-6">
					<Skeleton class="h-64 w-full rounded-lg" />
					<Skeleton class="h-40 w-full rounded-lg" />
				</div>
				<div class="space-y-6">
					<Skeleton class="h-48 w-full rounded-lg" />
					<Skeleton class="h-32 w-full rounded-lg" />
				</div>
			</div>
		</div>
	{:else if order}
		<PageHeader
			title="Order #{shortId(order.id)}"
			description="Placed on {new Date(order.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}"
			breadcrumbs={[
				{ label: 'Home', href: '/dashboard' },
				{ label: 'Orders', href: '/orders' },
				{ label: `Order #${shortId(order.id)}` }
			]}
		>
			<Button variant="outline" onclick={() => goto('/orders')}>
				<ArrowLeftIcon class="h-4 w-4 mr-2" />
				Back
			</Button>
		</PageHeader>

		<div class="grid gap-6 lg:grid-cols-3">
			<!-- Left Column (wider) -->
			<div class="lg:col-span-2 space-y-6">
				<!-- Order Items -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title class="flex items-center gap-2">
							<PackageIcon class="h-4 w-4" />
							Order Items
						</Card.Title>
					</Card.Header>
					<Card.Content>
						<div class="rounded-md border overflow-x-auto">
							<Table.Root>
								<Table.Header>
									<Table.Row>
										<Table.Head>Product</Table.Head>
										<Table.Head>Variant</Table.Head>
										<Table.Head>SKU</Table.Head>
										<Table.Head class="text-right">Qty</Table.Head>
										<Table.Head class="text-right">Unit Price</Table.Head>
										<Table.Head class="text-right">Total</Table.Head>
									</Table.Row>
								</Table.Header>
								<Table.Body>
									{#each order.items as item}
										<Table.Row>
											<Table.Cell class="font-medium">{item.productName}</Table.Cell>
											<Table.Cell class="text-muted-foreground">{item.variantInfo || '—'}</Table.Cell>
											<Table.Cell class="font-mono text-xs text-muted-foreground">{item.sku || '—'}</Table.Cell>
											<Table.Cell class="text-right">{item.quantity}</Table.Cell>
											<Table.Cell class="text-right">{formatCurrency(item.unitPrice, order.currency)}</Table.Cell>
											<Table.Cell class="text-right font-medium">{formatCurrency(item.totalPrice, order.currency)}</Table.Cell>
										</Table.Row>
									{/each}
								</Table.Body>
							</Table.Root>
						</div>
					</Card.Content>
				</Card.Root>

				<!-- Customer Info -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title class="flex items-center gap-2">
							<UserIcon class="h-4 w-4" />
							Customer Information
						</Card.Title>
					</Card.Header>
					<Card.Content>
						<div class="grid gap-4 sm:grid-cols-3">
							<div class="flex items-start gap-3">
								<UserIcon class="h-4 w-4 mt-0.5 text-muted-foreground" />
								<div>
									<p class="text-xs text-muted-foreground">Name</p>
									<p class="text-sm font-medium">{order.customerName}</p>
								</div>
							</div>
							<div class="flex items-start gap-3">
								<MailIcon class="h-4 w-4 mt-0.5 text-muted-foreground" />
								<div>
									<p class="text-xs text-muted-foreground">Email</p>
									<p class="text-sm font-medium">{order.customerEmail}</p>
								</div>
							</div>
							<div class="flex items-start gap-3">
								<PhoneIcon class="h-4 w-4 mt-0.5 text-muted-foreground" />
								<div>
									<p class="text-xs text-muted-foreground">Phone</p>
									<p class="text-sm font-medium">{order.customerPhone || '—'}</p>
								</div>
							</div>
						</div>
					</Card.Content>
				</Card.Root>

				<!-- Addresses -->
				{#if order.shippingAddress || order.billingAddress}
					<Card.Root class="shadow-sm">
						<Card.Header>
							<Card.Title class="flex items-center gap-2">
								<MapPinIcon class="h-4 w-4" />
								Addresses
							</Card.Title>
						</Card.Header>
						<Card.Content>
							<div class="grid gap-6 sm:grid-cols-2">
								{#if order.shippingAddress}
									<div>
										<p class="text-xs font-medium text-muted-foreground mb-1">Shipping Address</p>
										<p class="text-sm whitespace-pre-line">{order.shippingAddress}</p>
									</div>
								{/if}
								{#if order.billingAddress}
									<div>
										<p class="text-xs font-medium text-muted-foreground mb-1">Billing Address</p>
										<p class="text-sm whitespace-pre-line">{order.billingAddress}</p>
									</div>
								{/if}
							</div>
						</Card.Content>
					</Card.Root>
				{/if}

				<!-- Notes -->
				{#if order.notes}
					<Card.Root class="shadow-sm">
						<Card.Header>
							<Card.Title>Notes</Card.Title>
						</Card.Header>
						<Card.Content>
							<p class="text-sm whitespace-pre-line">{order.notes}</p>
						</Card.Content>
					</Card.Root>
				{/if}
			</div>

			<!-- Right Column (narrower) -->
			<div class="space-y-6">
				<!-- Order Summary -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title>Order Summary</Card.Title>
					</Card.Header>
					<Card.Content>
						<div class="space-y-3">
							<div class="flex items-center justify-between text-sm">
								<span class="text-muted-foreground">Subtotal</span>
								<span>{formatCurrency(order.subtotal, order.currency)}</span>
							</div>
							<div class="flex items-center justify-between text-sm">
								<span class="text-muted-foreground">Tax</span>
								<span>{formatCurrency(order.taxAmount, order.currency)}</span>
							</div>
							<div class="flex items-center justify-between text-sm">
								<span class="text-muted-foreground">Shipping</span>
								<span>{formatCurrency(order.shippingAmount, order.currency)}</span>
							</div>
							<div class="border-t pt-3">
								<div class="flex items-center justify-between">
									<span class="font-semibold">Total</span>
									<span class="text-lg font-bold">{formatCurrency(order.totalAmount, order.currency)}</span>
								</div>
							</div>
						</div>
					</Card.Content>
				</Card.Root>

				<!-- Status & Actions -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title>Status</Card.Title>
					</Card.Header>
					<Card.Content>
						<div class="space-y-4">
							<Badge variant="outline" class="text-sm px-3 py-1 {STATUS_CONFIG[order.status]?.class || ''}">
								{STATUS_CONFIG[order.status]?.label || order.status}
							</Badge>

							{#if auth.canEdit && TRANSITIONS[order.status]?.length}
								<div class="flex flex-wrap gap-2">
									{#each TRANSITIONS[order.status] as transition}
										<Button
											variant={transition.variant}
											size="sm"
											onclick={() => openStatusDialog(transition.status, transition.label)}
										>
											{transition.label}
										</Button>
									{/each}
								</div>
							{/if}
						</div>
					</Card.Content>
				</Card.Root>

				<!-- Timeline -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title class="flex items-center gap-2">
							<ClockIcon class="h-4 w-4" />
							Activity Timeline
						</Card.Title>
					</Card.Header>
					<Card.Content>
						{#if historyLoading}
							<div class="space-y-4">
								{#each Array(3) as _}
									<div class="flex gap-3">
										<Skeleton class="h-3 w-3 rounded-full mt-1 shrink-0" />
										<div class="flex-1 space-y-1">
											<Skeleton class="h-3 w-32" />
											<Skeleton class="h-3 w-20" />
										</div>
									</div>
								{/each}
							</div>
						{:else if history.length === 0}
							<p class="text-sm text-muted-foreground">No status changes recorded yet.</p>
						{:else}
							<div class="relative">
								<!-- Vertical line -->
								<div class="absolute left-[5px] top-2 bottom-2 w-px bg-border"></div>

								<div class="space-y-4">
									{#each history as entry, i}
										{@const statusConf = STATUS_CONFIG[entry.toStatus]}
										<div class="relative flex gap-3 pl-0">
											<!-- Dot -->
											<div class="relative z-10 mt-1.5 h-[11px] w-[11px] shrink-0 rounded-full border-2 border-background {statusConf?.dotClass || 'bg-gray-400'}"></div>

											<!-- Content -->
											<div class="flex-1 min-w-0 pb-1">
												<div class="flex items-center gap-1.5 flex-wrap">
													{#if entry.fromStatus}
														<Badge variant="outline" class="text-xs px-1.5 py-0 {STATUS_CONFIG[entry.fromStatus]?.class || ''}">
															{STATUS_CONFIG[entry.fromStatus]?.label || entry.fromStatus}
														</Badge>
														<span class="text-xs text-muted-foreground">→</span>
													{/if}
													<Badge variant="outline" class="text-xs px-1.5 py-0 {statusConf?.class || ''}">
														{statusConf?.label || entry.toStatus}
													</Badge>
												</div>
												{#if entry.note}
													<p class="text-xs text-muted-foreground mt-1">{entry.note}</p>
												{/if}
												<div class="flex items-center gap-2 mt-1">
													<span class="text-xs text-muted-foreground">{entry.changedBy}</span>
													<span class="text-xs text-muted-foreground">·</span>
													<span class="text-xs text-muted-foreground" title={new Date(entry.createdAt).toLocaleString()}>
														{relativeTime(entry.createdAt)}
													</span>
												</div>
											</div>
										</div>
									{/each}
								</div>
							</div>
						{/if}
					</Card.Content>
				</Card.Root>
			</div>
		</div>
	{/if}
</div>
