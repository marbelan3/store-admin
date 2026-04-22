<script lang="ts">
	import { onMount } from 'svelte';
	import { getInventory, bulkUpdateInventory, updateInventoryItem, type InventoryItem, type BulkUpdateItem } from '$lib/api/inventory';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Badge } from '$lib/components/ui/badge';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Table from '$lib/components/ui/table';
	import * as Card from '$lib/components/ui/card';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import InlineEditCell from '$lib/components/InlineEditCell.svelte';
	import SavedViews from '$lib/components/SavedViews.svelte';
	import SearchIcon from '@lucide/svelte/icons/search';
	import WarehouseIcon from '@lucide/svelte/icons/warehouse';
	import BoxIcon from '@lucide/svelte/icons/box';
	import TriangleAlertIcon from '@lucide/svelte/icons/triangle-alert';
	import CircleAlertIcon from '@lucide/svelte/icons/circle-alert';
	import CircleCheckIcon from '@lucide/svelte/icons/circle-check';
	import ChevronLeftIcon from '@lucide/svelte/icons/chevron-left';
	import ChevronRightIcon from '@lucide/svelte/icons/chevron-right';
	import SparklesIcon from '@lucide/svelte/icons/sparkles';

	function formatSyncDate(iso: string | null): string {
		if (!iso) return '—';
		const d = new Date(iso);
		const now = new Date();
		const diffMs = now.getTime() - d.getTime();
		const diffMin = Math.floor(diffMs / 60000);
		const diffHr = Math.floor(diffMin / 60);
		const diffDay = Math.floor(diffHr / 24);
		if (diffMin < 1) return 'just now';
		if (diffMin < 60) return `${diffMin}m ago`;
		if (diffHr < 24) return `${diffHr}h ago`;
		if (diffDay < 7) return `${diffDay}d ago`;
		return d.toLocaleDateString('uk-UA', { day: '2-digit', month: '2-digit', year: 'numeric' });
	}

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

	let stats = $derived(() => {
		const list = items;
		let tracked = 0;
		let inStock = 0;
		let low = 0;
		let out = 0;
		for (const it of list) {
			if (!it.trackInventory) continue;
			tracked++;
			if (it.quantity === 0) {
				out++;
			} else if (it.lowStockThreshold != null && it.quantity <= it.lowStockThreshold) {
				low++;
			} else {
				inStock++;
			}
		}
		return { tracked, inStock, low, out };
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
		item.quantity = newQty;
		items = [...items];
		try {
			await updateInventoryItem(item.id, item.type, { quantity: newQty });
			const key = `${item.type}:${item.id}`;
			if (pendingChanges.has(key)) {
				const next = new Map(pendingChanges);
				next.delete(key);
				pendingChanges = next;
			}
		} catch (err) {
			item.quantity = oldQty;
			items = [...items];
			throw err;
		}
	}

	function isLowStock(item: InventoryItem): boolean {
		if (!item.trackInventory || item.lowStockThreshold == null) return false;
		return item.quantity > 0 && item.quantity <= item.lowStockThreshold;
	}

	function isOutOfStock(item: InventoryItem): boolean {
		return item.trackInventory && item.quantity === 0;
	}

	function stockPercent(item: InventoryItem): number {
		if (!item.trackInventory) return 0;
		const threshold = item.lowStockThreshold ?? 10;
		const healthy = Math.max(threshold * 3, 10);
		return Math.max(4, Math.min(100, Math.round((item.quantity / healthy) * 100)));
	}

	function stockBarColor(item: InventoryItem): string {
		if (isOutOfStock(item)) return 'bg-rose-500';
		if (isLowStock(item)) return 'bg-amber-500';
		return 'bg-emerald-500';
	}

	function productInitials(name: string): string {
		return name
			.split(/\s+/)
			.slice(0, 2)
			.map((w) => w.charAt(0).toUpperCase())
			.join('');
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

	<!-- Stats Grid -->
	<div class="grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-4">
		<Card.Root class="relative overflow-hidden border-l-4 border-l-indigo-500 shadow-sm transition-shadow hover:shadow-md">
			<div class="absolute right-0 top-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-indigo-500/5"></div>
			<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
				<Card.Title class="text-xs font-medium uppercase tracking-wide text-muted-foreground">Total Items</Card.Title>
				<div class="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/10">
					<BoxIcon class="h-4 w-4 text-indigo-500" />
				</div>
			</Card.Header>
			<Card.Content>
				<div class="text-2xl font-extrabold tracking-tight lg:text-3xl">{totalElements.toLocaleString()}</div>
				<p class="mt-1 text-xs text-muted-foreground">Across all products & variants</p>
			</Card.Content>
		</Card.Root>

		<Card.Root class="relative overflow-hidden border-l-4 border-l-emerald-500 shadow-sm transition-shadow hover:shadow-md">
			<div class="absolute right-0 top-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-emerald-500/5"></div>
			<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
				<Card.Title class="text-xs font-medium uppercase tracking-wide text-muted-foreground">In Stock</Card.Title>
				<div class="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/10">
					<CircleCheckIcon class="h-4 w-4 text-emerald-500" />
				</div>
			</Card.Header>
			<Card.Content>
				<div class="text-2xl font-extrabold tracking-tight lg:text-3xl">{stats().inStock}</div>
				<p class="mt-1 text-xs text-muted-foreground">On current page · healthy</p>
			</Card.Content>
		</Card.Root>

		<Card.Root class="relative overflow-hidden border-l-4 border-l-amber-500 shadow-sm transition-shadow hover:shadow-md">
			<div class="absolute right-0 top-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-amber-500/5"></div>
			<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
				<Card.Title class="text-xs font-medium uppercase tracking-wide text-muted-foreground">Low Stock</Card.Title>
				<div class="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/10">
					<TriangleAlertIcon class="h-4 w-4 text-amber-500" />
				</div>
			</Card.Header>
			<Card.Content>
				<div class="text-2xl font-extrabold tracking-tight lg:text-3xl">{stats().low}</div>
				<p class="mt-1 text-xs text-muted-foreground">Below threshold</p>
			</Card.Content>
		</Card.Root>

		<Card.Root class="relative overflow-hidden border-l-4 border-l-rose-500 shadow-sm transition-shadow hover:shadow-md">
			<div class="absolute right-0 top-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-rose-500/5"></div>
			<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
				<Card.Title class="text-xs font-medium uppercase tracking-wide text-muted-foreground">Out of Stock</Card.Title>
				<div class="flex h-8 w-8 items-center justify-center rounded-lg bg-rose-500/10">
					<CircleAlertIcon class="h-4 w-4 text-rose-500" />
				</div>
			</Card.Header>
			<Card.Content>
				<div class="text-2xl font-extrabold tracking-tight lg:text-3xl">{stats().out}</div>
				<p class="mt-1 text-xs text-muted-foreground">Needs restocking</p>
			</Card.Content>
		</Card.Root>
	</div>

	<!-- Saved Views -->
	<SavedViews
		storageKey="inventory-views"
		currentFilters={currentInventoryFilters}
		defaultFilters={{ filter: 'all', search: '' }}
		onApplyView={applyInventoryView}
	/>

	<!-- Toolbar: Search + Filters -->
	<Card.Root class="shadow-sm">
		<Card.Content class="flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between">
			<div class="relative w-full sm:max-w-sm">
				<SearchIcon class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
				<Input
					placeholder="Search by name or SKU..."
					value={searchQuery}
					oninput={(e: Event) => { searchQuery = (e.target as HTMLInputElement).value; }}
					class="pl-9 border-muted bg-muted/40 focus-visible:bg-background"
				/>
			</div>

			<div class="flex flex-wrap items-center gap-2">
				<button
					type="button"
					onclick={() => setFilter('all')}
					class="inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition-all {filter === 'all' ? 'border-primary bg-primary text-primary-foreground shadow-sm' : 'border-border bg-background text-muted-foreground hover:border-primary/30 hover:bg-muted hover:text-foreground'}"
				>
					<SparklesIcon class="h-3 w-3" />
					All
				</button>
				<button
					type="button"
					onclick={() => setFilter('lowStock')}
					class="inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition-all {filter === 'lowStock' ? 'border-amber-500 bg-amber-500 text-white shadow-sm' : 'border-border bg-background text-muted-foreground hover:border-amber-500/40 hover:bg-amber-50 hover:text-amber-700 dark:hover:bg-amber-950/30 dark:hover:text-amber-400'}"
				>
					<TriangleAlertIcon class="h-3 w-3" />
					Low Stock
				</button>
				<button
					type="button"
					onclick={() => setFilter('outOfStock')}
					class="inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition-all {filter === 'outOfStock' ? 'border-rose-500 bg-rose-500 text-white shadow-sm' : 'border-border bg-background text-muted-foreground hover:border-rose-500/40 hover:bg-rose-50 hover:text-rose-700 dark:hover:bg-rose-950/30 dark:hover:text-rose-400'}"
				>
					<CircleAlertIcon class="h-3 w-3" />
					Out of Stock
				</button>
			</div>
		</Card.Content>
	</Card.Root>

	{#if loading}
		<Card.Root class="overflow-hidden shadow-sm">
			<div class="w-full overflow-x-auto">
				<Table.Root>
					<Table.Header>
						<Table.Row class="bg-muted/40 hover:bg-muted/40">
							<Table.Head class="w-[40%]">Product</Table.Head>
							<Table.Head class="hidden md:table-cell">SKU</Table.Head>
							<Table.Head>Status</Table.Head>
							<Table.Head class="hidden lg:table-cell">Stock Level</Table.Head>
							<Table.Head class="w-[120px]">Quantity</Table.Head>
							<Table.Head class="hidden xl:table-cell">Threshold</Table.Head>
							<Table.Head class="hidden xl:table-cell">Synced</Table.Head>
						</Table.Row>
					</Table.Header>
					<Table.Body>
						{#each Array(6) as _}
							<Table.Row>
								<Table.Cell>
									<div class="flex items-center gap-3">
										<Skeleton class="h-9 w-9 rounded-lg" />
										<div class="space-y-2">
											<Skeleton class="h-4 w-40" />
											<Skeleton class="h-3 w-24" />
										</div>
									</div>
								</Table.Cell>
								<Table.Cell class="hidden md:table-cell"><Skeleton class="h-4 w-20" /></Table.Cell>
								<Table.Cell><Skeleton class="h-5 w-20 rounded-full" /></Table.Cell>
								<Table.Cell class="hidden lg:table-cell"><Skeleton class="h-2 w-28 rounded-full" /></Table.Cell>
								<Table.Cell><Skeleton class="h-8 w-16" /></Table.Cell>
								<Table.Cell class="hidden xl:table-cell"><Skeleton class="h-4 w-10" /></Table.Cell>
								<Table.Cell class="hidden xl:table-cell"><Skeleton class="h-4 w-20" /></Table.Cell>
							</Table.Row>
						{/each}
					</Table.Body>
				</Table.Root>
			</div>
		</Card.Root>
	{:else if items.length === 0}
		<Card.Root class="border-dashed shadow-sm">
			<EmptyState
				icon={WarehouseIcon}
				title="No inventory items"
				description="Inventory will appear here once you add products with inventory tracking enabled."
			/>
		</Card.Root>
	{:else}
		<Card.Root class="overflow-hidden shadow-sm">
			<div class="w-full overflow-x-auto">
				<Table.Root>
					<Table.Header>
						<Table.Row class="bg-muted/40 hover:bg-muted/40">
							<Table.Head class="w-[40%]">Product</Table.Head>
							<Table.Head class="hidden md:table-cell">SKU</Table.Head>
							<Table.Head>Status</Table.Head>
							<Table.Head class="hidden lg:table-cell">Stock Level</Table.Head>
							<Table.Head class="w-[120px]">Quantity</Table.Head>
							<Table.Head class="hidden xl:table-cell w-[90px]">Threshold</Table.Head>
							<Table.Head class="hidden xl:table-cell w-[110px]">Synced</Table.Head>
						</Table.Row>
					</Table.Header>
					<Table.Body>
						{#each filteredItems() as item (item.type + ':' + item.id)}
							{@const out = isOutOfStock(item)}
							{@const low = isLowStock(item)}
							<Table.Row
								class="group/row border-l-2 transition-colors {out ? 'border-l-rose-400 bg-rose-50/40 hover:bg-rose-50 dark:bg-rose-950/10 dark:hover:bg-rose-950/20' : low ? 'border-l-amber-400 bg-amber-50/40 hover:bg-amber-50 dark:bg-amber-950/10 dark:hover:bg-amber-950/20' : 'border-l-transparent hover:bg-muted/40'}"
							>
								<Table.Cell>
									<div class="flex items-center gap-3 min-w-0">
										<div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500/10 to-violet-500/10 text-[11px] font-bold text-indigo-600 ring-1 ring-inset ring-indigo-500/20 dark:text-indigo-400">
											{productInitials(item.productName)}
										</div>
										<div class="min-w-0">
											<div class="truncate text-sm font-medium">{item.productName}</div>
											<div class="mt-0.5 flex items-center gap-1.5 text-xs text-muted-foreground">
												{#if item.variantName}
													<span class="truncate">{item.variantName}</span>
												{:else}
													<span class="italic opacity-60">base product</span>
												{/if}
												<span class="md:hidden">·</span>
												<span class="md:hidden font-mono">{item.sku ?? '—'}</span>
											</div>
										</div>
									</div>
								</Table.Cell>
								<Table.Cell class="hidden md:table-cell">
									{#if item.sku}
										<span class="rounded bg-muted px-1.5 py-0.5 font-mono text-[11px] text-muted-foreground">{item.sku}</span>
									{:else}
										<span class="text-xs text-muted-foreground">—</span>
									{/if}
								</Table.Cell>
								<Table.Cell>
									{#if out}
										<Badge variant="destructive" class="gap-1">
											<CircleAlertIcon class="h-3 w-3" />
											Out
										</Badge>
									{:else if low}
										<Badge class="gap-1 border-amber-400/60 bg-amber-50 text-amber-700 dark:border-amber-500/40 dark:bg-amber-950/30 dark:text-amber-400">
											<TriangleAlertIcon class="h-3 w-3" />
											Low
										</Badge>
									{:else if item.trackInventory}
										<Badge class="gap-1 border-emerald-400/60 bg-emerald-50 text-emerald-700 dark:border-emerald-500/40 dark:bg-emerald-950/30 dark:text-emerald-400">
											<CircleCheckIcon class="h-3 w-3" />
											In stock
										</Badge>
									{:else}
										<Badge variant="outline" class="text-muted-foreground">Untracked</Badge>
									{/if}
								</Table.Cell>
								<Table.Cell class="hidden lg:table-cell">
									{#if item.trackInventory}
										<div class="flex items-center gap-2">
											<div class="h-1.5 w-24 overflow-hidden rounded-full bg-muted">
												<div class="h-full rounded-full transition-all {stockBarColor(item)}" style="width: {stockPercent(item)}%"></div>
											</div>
											<span class="text-[11px] font-medium tabular-nums text-muted-foreground">{item.quantity}</span>
										</div>
									{:else}
										<span class="text-xs text-muted-foreground">—</span>
									{/if}
								</Table.Cell>
								<Table.Cell>
									{#if item.trackInventory}
										<InlineEditCell
											value={item.quantity}
											type="number"
											min={0}
											onSave={(newValue) => handleInlineQuantitySave(item, newValue)}
										/>
									{:else}
										<span class="text-sm text-muted-foreground">—</span>
									{/if}
								</Table.Cell>
								<Table.Cell class="hidden xl:table-cell text-sm text-muted-foreground tabular-nums">
									{item.lowStockThreshold ?? '—'}
								</Table.Cell>
								<Table.Cell class="hidden xl:table-cell">
									<span class="whitespace-nowrap text-xs text-muted-foreground">{formatSyncDate(item.lastSyncedAt)}</span>
								</Table.Cell>
							</Table.Row>
						{/each}
					</Table.Body>
				</Table.Root>
			</div>
		</Card.Root>

		{#if totalPages > 1}
			<div class="flex flex-col items-center justify-between gap-3 sm:flex-row">
				<p class="text-sm text-muted-foreground">
					Showing page <span class="font-semibold text-foreground">{currentPage + 1}</span>
					of <span class="font-semibold text-foreground">{totalPages}</span>
					<span class="hidden sm:inline">· {totalElements.toLocaleString()} total items</span>
				</p>
				<div class="flex items-center gap-2">
					<Button variant="outline" size="sm" disabled={isFirst} onclick={() => goToPage(currentPage - 1)}>
						<ChevronLeftIcon class="h-4 w-4" />
						Previous
					</Button>
					<Button variant="outline" size="sm" disabled={isLast} onclick={() => goToPage(currentPage + 1)}>
						Next
						<ChevronRightIcon class="h-4 w-4" />
					</Button>
				</div>
			</div>
		{/if}
	{/if}
</div>
