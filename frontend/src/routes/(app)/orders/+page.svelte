<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { auth } from '$lib/stores/auth.svelte';
	import { getOrders, getOrderStats, bulkUpdateOrderStatus } from '$lib/api/orders';
	import type { Order, OrderStats, OrderStatus } from '$lib/types/order';
	import type { Page } from '$lib/types/product';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Badge } from '$lib/components/ui/badge';
	import * as Table from '$lib/components/ui/table';
	import * as Card from '$lib/components/ui/card';
	import * as DropdownMenu from '$lib/components/ui/dropdown-menu';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import BulkActionBar from '$lib/components/BulkActionBar.svelte';

	import SavedViews from '$lib/components/SavedViews.svelte';
	import ShoppingCartIcon from '@lucide/svelte/icons/shopping-cart';
	import SearchIcon from '@lucide/svelte/icons/search';
	import PackageIcon from '@lucide/svelte/icons/package';
	import ClockIcon from '@lucide/svelte/icons/clock';
	import CheckCircleIcon from '@lucide/svelte/icons/circle-check';
	import TruckIcon from '@lucide/svelte/icons/truck';
	import CircleXIcon from '@lucide/svelte/icons/circle-x';
	import RotateCcwIcon from '@lucide/svelte/icons/rotate-ccw';
	import CogIcon from '@lucide/svelte/icons/cog';
	import RefreshCwIcon from '@lucide/svelte/icons/refresh-cw';
	import ChevronDownIcon from '@lucide/svelte/icons/chevron-down';

	const PAGE_SIZE = 20;

	let orders = $state<Order[]>([]);
	let totalElements = $state(0);
	let totalPages = $state(0);
	let currentPage = $state(0);
	let isFirst = $state(true);
	let isLast = $state(true);
	let loading = $state(true);
	let statusFilter = $state<string>('');
	let searchQuery = $state('');
	let searchTimeout: ReturnType<typeof setTimeout>;

	let stats = $state<OrderStats | null>(null);
	let statsLoading = $state(true);

	// Bulk selection
	let selectedIds = $state(new Set<string>());
	let bulkLoading = $state(false);

	let allOnPageSelected = $derived(
		orders.length > 0 && orders.every((o) => selectedIds.has(o.id))
	);

	function toggleSelectAll() {
		if (allOnPageSelected) {
			const next = new Set(selectedIds);
			for (const o of orders) next.delete(o.id);
			selectedIds = next;
		} else {
			const next = new Set(selectedIds);
			for (const o of orders) next.add(o.id);
			selectedIds = next;
		}
	}

	function toggleSelect(id: string) {
		const next = new Set(selectedIds);
		if (next.has(id)) {
			next.delete(id);
		} else {
			next.add(id);
		}
		selectedIds = next;
	}

	function clearSelection() {
		selectedIds = new Set();
	}

	async function handleBulkStatusChange(status: string) {
		bulkLoading = true;
		try {
			await bulkUpdateOrderStatus([...selectedIds], status);
			toast.success(`${selectedIds.size} order${selectedIds.size > 1 ? 's' : ''} updated to ${status}`);
			clearSelection();
			await loadOrders();
			await loadStats();
		} catch {
			toast.error('Failed to update order status');
		} finally {
			bulkLoading = false;
		}
	}

	const STATUS_CONFIG: Record<OrderStatus, { label: string; class: string }> = {
		PENDING: {
			label: 'Pending',
			class: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400 border-amber-200 dark:border-amber-800'
		},
		CONFIRMED: {
			label: 'Confirmed',
			class: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400 border-blue-200 dark:border-blue-800'
		},
		PROCESSING: {
			label: 'Processing',
			class: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400 border-indigo-200 dark:border-indigo-800'
		},
		SHIPPED: {
			label: 'Shipped',
			class: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400 border-purple-200 dark:border-purple-800'
		},
		DELIVERED: {
			label: 'Delivered',
			class: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800'
		},
		CANCELLED: {
			label: 'Cancelled',
			class: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 border-red-200 dark:border-red-800'
		},
		REFUNDED: {
			label: 'Refunded',
			class: 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400 border-gray-200 dark:border-gray-700'
		}
	};

	const STATUS_FILTERS: { value: string; label: string }[] = [
		{ value: '', label: 'All' },
		{ value: 'PENDING', label: 'Pending' },
		{ value: 'CONFIRMED', label: 'Confirmed' },
		{ value: 'PROCESSING', label: 'Processing' },
		{ value: 'SHIPPED', label: 'Shipped' },
		{ value: 'DELIVERED', label: 'Delivered' },
		{ value: 'CANCELLED', label: 'Cancelled' }
	];

	function shortId(id: string): string {
		return id.length > 8 ? id.substring(0, 8).toUpperCase() : id;
	}

	function formatCurrency(amount: number, currency: string): string {
		return new Intl.NumberFormat('en-US', { style: 'currency', currency: currency || 'USD' }).format(amount);
	}

	function readUrlParams() {
		const params = $page.url.searchParams;
		const urlPage = params.get('page');
		currentPage = urlPage ? Math.max(0, Number(urlPage) - 1) : 0;
		statusFilter = params.get('status') || '';
		searchQuery = params.get('search') || '';
	}

	function updateUrl() {
		const params = new URLSearchParams();
		if (currentPage > 0) params.set('page', String(currentPage + 1));
		if (statusFilter) params.set('status', statusFilter);
		if (searchQuery) params.set('search', searchQuery);
		const qs = params.toString();
		goto(`/orders${qs ? `?${qs}` : ''}`, { replaceState: true, keepFocus: true, noScroll: true });
	}

	async function loadOrders() {
		loading = true;
		try {
			const result = await getOrders({
				page: currentPage,
				size: PAGE_SIZE,
				status: statusFilter || undefined,
				search: searchQuery || undefined,
				sort: 'createdAt,desc'
			});
			orders = result.content;
			totalElements = result.totalElements;
			totalPages = result.totalPages;
			isFirst = result.first;
			isLast = result.last;
		} catch (err) {
			toast.error('Failed to load orders');
		} finally {
			loading = false;
		}
	}

	async function loadStats() {
		statsLoading = true;
		try {
			stats = await getOrderStats();
		} catch {
			// non-critical
		} finally {
			statsLoading = false;
		}
	}

	function setFilter(status: string) {
		statusFilter = status;
		currentPage = 0;
		updateUrl();
		loadOrders();
	}

	function handleSearchInput(e: Event) {
		const value = (e.target as HTMLInputElement).value;
		searchQuery = value;
		clearTimeout(searchTimeout);
		searchTimeout = setTimeout(() => {
			currentPage = 0;
			updateUrl();
			loadOrders();
		}, 300);
	}

	function goToPage(p: number) {
		currentPage = p;
		updateUrl();
		loadOrders();
	}

	let currentOrderFilters = $derived({
		status: statusFilter,
		search: searchQuery
	});

	function applyOrderView(filters: Record<string, any>) {
		statusFilter = filters.status || '';
		searchQuery = filters.search || '';
		currentPage = 0;
		updateUrl();
		loadOrders();
	}

	onMount(() => {
		readUrlParams();
		loadOrders();
		loadStats();
	});
</script>

<div class="space-y-6">
	<PageHeader
		title="Orders"
		description="Manage customer orders"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Orders' }]}
	>
		{#if auth.canEdit}
			<Button onclick={() => goto('/orders/new')}>Create Order</Button>
		{/if}
	</PageHeader>

	<!-- Stats Bar -->
	{#if statsLoading}
		<div class="grid gap-3 grid-cols-2 md:grid-cols-4 lg:grid-cols-7">
			{#each Array(7) as _}
				<Card.Root>
					<Card.Content class="p-3">
						<Skeleton class="h-3 w-16 mb-1" />
						<Skeleton class="h-6 w-10" />
					</Card.Content>
				</Card.Root>
			{/each}
		</div>
	{:else if stats}
		<div class="grid gap-3 grid-cols-2 md:grid-cols-4 lg:grid-cols-7">
			<Card.Root class="shadow-sm border-l-4 border-l-slate-400">
				<Card.Content class="p-3">
					<p class="text-xs text-muted-foreground">Total</p>
					<p class="text-xl font-bold">{stats.totalOrders}</p>
				</Card.Content>
			</Card.Root>
			<Card.Root class="shadow-sm border-l-4 border-l-amber-400">
				<Card.Content class="p-3">
					<p class="text-xs text-muted-foreground">Pending</p>
					<p class="text-xl font-bold">{stats.pendingOrders}</p>
				</Card.Content>
			</Card.Root>
			<Card.Root class="shadow-sm border-l-4 border-l-blue-400">
				<Card.Content class="p-3">
					<p class="text-xs text-muted-foreground">Confirmed</p>
					<p class="text-xl font-bold">{stats.confirmedOrders}</p>
				</Card.Content>
			</Card.Root>
			<Card.Root class="shadow-sm border-l-4 border-l-indigo-400">
				<Card.Content class="p-3">
					<p class="text-xs text-muted-foreground">Processing</p>
					<p class="text-xl font-bold">{stats.processingOrders}</p>
				</Card.Content>
			</Card.Root>
			<Card.Root class="shadow-sm border-l-4 border-l-purple-400">
				<Card.Content class="p-3">
					<p class="text-xs text-muted-foreground">Shipped</p>
					<p class="text-xl font-bold">{stats.shippedOrders}</p>
				</Card.Content>
			</Card.Root>
			<Card.Root class="shadow-sm border-l-4 border-l-emerald-400">
				<Card.Content class="p-3">
					<p class="text-xs text-muted-foreground">Delivered</p>
					<p class="text-xl font-bold">{stats.deliveredOrders}</p>
				</Card.Content>
			</Card.Root>
			<Card.Root class="shadow-sm border-l-4 border-l-red-400">
				<Card.Content class="p-3">
					<p class="text-xs text-muted-foreground">Cancelled</p>
					<p class="text-xl font-bold">{stats.cancelledOrders}</p>
				</Card.Content>
			</Card.Root>
		</div>
	{/if}

	<!-- Saved Views -->
	<SavedViews
		storageKey="orders-views"
		currentFilters={currentOrderFilters}
		onApplyView={applyOrderView}
	/>

	<!-- Search and Status Filter Pills -->
	<div class="flex flex-wrap items-center gap-3">
		<div class="relative max-w-xs">
			<SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
			<Input
				placeholder="Search orders..."
				value={searchQuery}
				oninput={handleSearchInput}
				class="pl-9 shadow-sm"
			/>
		</div>
	</div>

	<div class="flex flex-wrap gap-2">
		{#each STATUS_FILTERS as filter}
			<Button
				variant={statusFilter === filter.value ? 'default' : 'outline'}
				size="sm"
				class="rounded-full {statusFilter === filter.value ? 'bg-primary text-primary-foreground' : ''}"
				onclick={() => setFilter(filter.value)}
			>{filter.label}</Button>
		{/each}
	</div>

	<!-- Table -->
	{#if loading}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Order #</Table.Head>
						<Table.Head>Customer</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head>Items</Table.Head>
						<Table.Head>Total</Table.Head>
						<Table.Head>Date</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each Array(6) as _}
						<Table.Row>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-32" /></Table.Cell>
							<Table.Cell><Skeleton class="h-5 w-20 rounded-full" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-8" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-24" /></Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{:else if orders.length === 0}
		<div class="rounded-lg border border-dashed shadow-sm">
			<EmptyState
				icon={ShoppingCartIcon}
				title="No orders yet"
				description={searchQuery || statusFilter ? 'Try adjusting your filters.' : 'Orders will appear here when customers place them.'}
				actionLabel={auth.canEdit && !searchQuery && !statusFilter ? 'Create Order' : undefined}
				actionHref={auth.canEdit && !searchQuery && !statusFilter ? '/orders/new' : undefined}
			/>
		</div>
	{:else}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						{#if auth.canEdit}
							<Table.Head class="w-[44px]">
								<input
									type="checkbox"
									checked={allOnPageSelected}
									onchange={toggleSelectAll}
									class="h-4 w-4 rounded border-gray-300 accent-primary cursor-pointer"
									aria-label="Select all orders on this page"
								/>
							</Table.Head>
						{/if}
						<Table.Head>Order #</Table.Head>
						<Table.Head>Customer</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head>Items</Table.Head>
						<Table.Head>Total</Table.Head>
						<Table.Head>Date</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each orders as order}
						<Table.Row
							class="cursor-pointer hover:bg-muted/50 transition-colors {selectedIds.has(order.id) ? 'bg-muted/40' : ''}"
							onclick={() => goto(`/orders/${order.id}`)}
						>
							{#if auth.canEdit}
								<Table.Cell>
									<input
										type="checkbox"
										checked={selectedIds.has(order.id)}
										onchange={() => toggleSelect(order.id)}
										onclick={(e: MouseEvent) => e.stopPropagation()}
										class="h-4 w-4 rounded border-gray-300 accent-primary cursor-pointer"
										aria-label="Select order #{shortId(order.id)}"
									/>
								</Table.Cell>
							{/if}
							<Table.Cell class="font-mono text-sm font-medium">#{shortId(order.id)}</Table.Cell>
							<Table.Cell>
								<div>
									<p class="font-medium">{order.customerName}</p>
									<p class="text-xs text-muted-foreground">{order.customerEmail}</p>
								</div>
							</Table.Cell>
							<Table.Cell>
								<Badge variant="outline" class={STATUS_CONFIG[order.status]?.class || ''}>
									{STATUS_CONFIG[order.status]?.label || order.status}
								</Badge>
							</Table.Cell>
							<Table.Cell class="text-muted-foreground">
								{order.items?.length ?? 0}
							</Table.Cell>
							<Table.Cell class="font-medium">
								{formatCurrency(order.totalAmount, order.currency)}
							</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{new Date(order.createdAt).toLocaleDateString()}
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		<!-- Pagination -->
		{#if totalPages > 1}
			<div class="flex items-center justify-between">
				<p class="text-sm text-muted-foreground">
					Showing {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, totalElements)} of {totalElements} orders
				</p>
				<div class="flex items-center gap-2">
					<Button
						variant="outline"
						size="sm"
						disabled={isFirst}
						onclick={() => goToPage(currentPage - 1)}
					>Previous</Button>
					<span class="text-sm text-muted-foreground">
						Page {currentPage + 1} of {totalPages}
					</span>
					<Button
						variant="outline"
						size="sm"
						disabled={isLast}
						onclick={() => goToPage(currentPage + 1)}
					>Next</Button>
				</div>
			</div>
		{/if}
	{/if}
</div>

<!-- Bulk Action Bar -->
{#if auth.canEdit}
	<BulkActionBar selectedCount={selectedIds.size} onClearSelection={clearSelection}>
		{#snippet children()}
			<DropdownMenu.Root>
				<DropdownMenu.Trigger>
					<Button
						variant="ghost"
						size="sm"
						class="h-7 text-zinc-300 hover:text-white hover:bg-zinc-700"
						disabled={bulkLoading}
					>
						<RefreshCwIcon class="h-4 w-4 mr-1" />
						Change Status
						<ChevronDownIcon class="h-3 w-3 ml-1" />
					</Button>
				</DropdownMenu.Trigger>
				<DropdownMenu.Content>
					{#each ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'] as status}
						<DropdownMenu.Item onclick={() => handleBulkStatusChange(status)}>
							<Badge variant="outline" class="mr-2 {STATUS_CONFIG[status as OrderStatus]?.class || ''}">{status}</Badge>
							{STATUS_CONFIG[status as OrderStatus]?.label || status}
						</DropdownMenu.Item>
					{/each}
				</DropdownMenu.Content>
			</DropdownMenu.Root>
		{/snippet}
	</BulkActionBar>
{/if}
