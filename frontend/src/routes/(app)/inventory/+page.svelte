<script lang="ts">
	import { onMount } from 'svelte';
	import { getInventory, bulkUpdateInventory, updateInventoryItem, type InventoryItem, type BulkUpdateItem } from '$lib/api/inventory';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Badge } from '$lib/components/ui/badge';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Table from '$lib/components/ui/table';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import InlineEditCell from '$lib/components/InlineEditCell.svelte';
	import SavedViews from '$lib/components/SavedViews.svelte';
	import SearchIcon from '@lucide/svelte/icons/search';
	import WarehouseIcon from '@lucide/svelte/icons/warehouse';

	const PAGE_SIZE = 50;

	let items = $state<InventoryItem[]>([]);
	let totalElements = $state(0);
	let totalPages = $state(0);
	let currentPage = $state(0);
	let isFirst = $state(true);
	let isLast = $state(true);
	let loading = $state(true);
	let saving = $state(false);

	let filter = $state<'all' | 'lowStock' | 'outOfStock'>('all');
	let searchQuery = $state('');
	let pendingChanges = $state<Map<string, number>>(new Map());

	let filteredItems = $derived(() => {
		if (!searchQuery.trim()) return items;
		const q = searchQuery.toLowerCase().trim();
		return items.filter(
			(item) =>
				item.productName.toLowerCase().includes(q) ||
				(item.sku && item.sku.toLowerCase().includes(q))
		);
	});

	async function loadInventory() {
		loading = true;
		try {
			const result = await getInventory({
				page: currentPage,
				size: PAGE_SIZE,
				lowStock: filter === 'lowStock' || undefined,
				outOfStock: filter === 'outOfStock' || undefined
			});
			items = result.content;
			totalElements = result.totalElements;
			totalPages = result.totalPages;
			isFirst = result.first;
			isLast = result.last;
			pendingChanges = new Map();
		} catch {
			toast.error('Failed to load inventory');
		} finally {
			loading = false;
		}
	}

	function handleQuantityChange(item: InventoryItem, value: string) {
		const qty = parseInt(value, 10);
		if (isNaN(qty) || qty < 0) return;
		const key = `${item.type}:${item.id}`;
		if (qty === item.quantity) {
			const next = new Map(pendingChanges);
			next.delete(key);
			pendingChanges = next;
		} else {
			pendingChanges = new Map(pendingChanges).set(key, qty);
		}
	}

	function getDisplayQuantity(item: InventoryItem): number {
		const key = `${item.type}:${item.id}`;
		return pendingChanges.has(key) ? pendingChanges.get(key)! : item.quantity;
	}

	function isChanged(item: InventoryItem): boolean {
		const key = `${item.type}:${item.id}`;
		return pendingChanges.has(key);
	}

	async function saveChanges() {
		if (pendingChanges.size === 0) return;
		saving = true;
		try {
			const updates: BulkUpdateItem[] = [];
			for (const [key, qty] of pendingChanges) {
				const [type, id] = key.split(':');
				updates.push({ id, type: type as 'PRODUCT' | 'VARIANT', quantity: qty });
			}
			await bulkUpdateInventory(updates);
			toast.success(`Updated ${updates.length} items`);
			await loadInventory();
		} catch {
			toast.error('Failed to save changes');
		} finally {
			saving = false;
		}
	}

	function setFilter(f: 'all' | 'lowStock' | 'outOfStock') {
		filter = f;
		currentPage = 0;
		loadInventory();
	}

	function goToPage(p: number) {
		currentPage = p;
		loadInventory();
	}

	async function handleInlineQuantitySave(item: InventoryItem, newValue: string | number) {
		const oldQty = item.quantity;
		const newQty = typeof newValue === 'string' ? parseInt(newValue, 10) : newValue;
		// Optimistic update
		item.quantity = newQty;
		items = [...items];
		try {
			await updateInventoryItem(item.id, item.type, { quantity: newQty });
			// Remove from pending changes if it was there
			const key = `${item.type}:${item.id}`;
			if (pendingChanges.has(key)) {
				const next = new Map(pendingChanges);
				next.delete(key);
				pendingChanges = next;
			}
		} catch (err) {
			// Rollback
			item.quantity = oldQty;
			items = [...items];
			throw err;
		}
	}

	function isLowStock(item: InventoryItem): boolean {
		if (!item.trackInventory || item.lowStockThreshold == null) return false;
		return item.quantity <= item.lowStockThreshold;
	}

	function isOutOfStock(item: InventoryItem): boolean {
		return item.trackInventory && item.quantity === 0;
	}

	let currentInventoryFilters = $derived({
		filter: filter,
		search: searchQuery
	});

	function applyInventoryView(filters: Record<string, any>) {
		filter = (filters.filter as 'all' | 'lowStock' | 'outOfStock') || 'all';
		searchQuery = filters.search || '';
		currentPage = 0;
		loadInventory();
	}

	onMount(loadInventory);
</script>

<div class="space-y-6">
	<PageHeader
		title="Inventory"
		description="Track stock levels and manage inventory"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Inventory' }]}
	>
		{#if pendingChanges.size > 0}
			<Button onclick={saveChanges} disabled={saving} class="relative">
				{saving ? 'Saving...' : `Save ${pendingChanges.size} changes`}
				<span class="absolute -top-2 -right-2 flex h-5 w-5 items-center justify-center rounded-full bg-destructive text-[10px] font-bold text-destructive-foreground">{pendingChanges.size}</span>
			</Button>
		{/if}
	</PageHeader>

	<!-- Saved Views -->
	<SavedViews
		storageKey="inventory-views"
		currentFilters={currentInventoryFilters}
		defaultFilters={{ filter: 'all', search: '' }}
		onApplyView={applyInventoryView}
	/>

	<!-- Search -->
	<div class="flex flex-wrap items-center gap-3">
		<div class="relative max-w-xs">
			<SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
			<Input
				placeholder="Search by name or SKU..."
				value={searchQuery}
				oninput={(e: Event) => { searchQuery = (e.target as HTMLInputElement).value; }}
				class="pl-9 shadow-sm"
			/>
		</div>
	</div>

	<div class="flex gap-2">
		<Button variant={filter === 'all' ? 'default' : 'outline'} size="sm" class="rounded-full {filter === 'all' ? 'bg-primary text-primary-foreground' : ''}" onclick={() => setFilter('all')}>
			All
		</Button>
		<Button variant={filter === 'lowStock' ? 'default' : 'outline'} size="sm" class="rounded-full {filter === 'lowStock' ? 'bg-primary text-primary-foreground' : ''}" onclick={() => setFilter('lowStock')}>
			Low Stock
		</Button>
		<Button variant={filter === 'outOfStock' ? 'default' : 'outline'} size="sm" class="rounded-full {filter === 'outOfStock' ? 'bg-primary text-primary-foreground' : ''}" onclick={() => setFilter('outOfStock')}>
			Out of Stock
		</Button>
	</div>

	{#if loading}
		<div class="rounded-md border overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Product</Table.Head>
						<Table.Head>Variant</Table.Head>
						<Table.Head>SKU</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head class="w-[120px]">Quantity</Table.Head>
						<Table.Head>Threshold</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each Array(6) as _}
						<Table.Row>
							<Table.Cell><Skeleton class="h-4 w-32" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-5 w-20 rounded-full" /></Table.Cell>
							<Table.Cell><Skeleton class="h-8 w-24" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-10" /></Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{:else if items.length === 0}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={WarehouseIcon}
				title="No inventory items"
				description="Inventory will appear here once you add products with inventory tracking enabled."
			/>
		</div>
	{:else}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Product</Table.Head>
						<Table.Head>Variant</Table.Head>
						<Table.Head>SKU</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head class="w-[120px]">Quantity</Table.Head>
						<Table.Head>Threshold</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each filteredItems() as item}
						<Table.Row
							class="hover:bg-muted/50 transition-colors {isOutOfStock(item) ? 'bg-red-50 dark:bg-red-950/20 border-l-2 border-l-red-400' : isLowStock(item) ? 'bg-amber-50 dark:bg-amber-950/20 border-l-2 border-l-amber-400' : ''}"
						>
							<Table.Cell class="font-medium">{item.productName}</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{item.variantName || '—'}
							</Table.Cell>
							<Table.Cell class="text-sm">{item.sku || '—'}</Table.Cell>
							<Table.Cell>
								{#if isOutOfStock(item)}
									<Badge variant="destructive">Out of Stock</Badge>
								{:else if isLowStock(item)}
									<Badge variant="outline" class="border-yellow-500 text-yellow-700">Low Stock</Badge>
								{:else if item.trackInventory}
									<Badge variant="secondary">In Stock</Badge>
								{:else}
									<Badge variant="outline">Not Tracked</Badge>
								{/if}
							</Table.Cell>
							<Table.Cell class="min-w-[100px]">
								{#if item.trackInventory}
									<InlineEditCell
										value={getDisplayQuantity(item)}
										type="number"
										min={0}
										onSave={(newValue) => handleInlineQuantitySave(item, newValue)}
									/>
								{:else}
									<span class="text-sm text-muted-foreground">—</span>
								{/if}
							</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{item.lowStockThreshold ?? '—'}
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		{#if totalPages > 1}
			<div class="flex items-center justify-between">
				<p class="text-sm text-muted-foreground">
					Page {currentPage + 1} of {totalPages}
				</p>
				<div class="flex items-center gap-2">
					<Button variant="outline" size="sm" disabled={isFirst} onclick={() => goToPage(currentPage - 1)}>
						Previous
					</Button>
					<Button variant="outline" size="sm" disabled={isLast} onclick={() => goToPage(currentPage + 1)}>
						Next
					</Button>
				</div>
			</div>
		{/if}
	{/if}
</div>
