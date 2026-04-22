<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/stores/auth.svelte';
	import {
		getConnections,
		getCjCategories,
		searchCatalog,
		listByCategory,
		getCjProductDetail,
		importProduct,
		addToWatchlist
	} from '$lib/api/marketplace';
	import type {
		MarketplaceConnection,
		CjCategory,
		CjSubCategory,
		CjLeafCategory,
		CjCatalogProduct,
		CjProductDetail,
		CjVariant,
		ImportVariantRequest
	} from '$lib/types/marketplace';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Badge } from '$lib/components/ui/badge';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Select from '$lib/components/ui/select';
	import * as Card from '$lib/components/ui/card';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import SearchIcon from '@lucide/svelte/icons/search';
	import DownloadIcon from '@lucide/svelte/icons/download';
	import EyeIcon from '@lucide/svelte/icons/eye';
	import PackageIcon from '@lucide/svelte/icons/package';
	import LayoutGridIcon from '@lucide/svelte/icons/layout-grid';
	import ListIcon from '@lucide/svelte/icons/list';
	import XIcon from '@lucide/svelte/icons/x';

	let connections = $state<MarketplaceConnection[]>([]);
	let selectedConnectionId = $state('');
	let searchQuery = $state('');
	let searchResults = $state<CjCatalogProduct[]>([]);
	let searching = $state(false);
	let loadingConnections = $state(true);

	// Categories
	let categories = $state<CjCategory[]>([]);
	let loadingCategories = $state(false);
	let selectedFirstCat = $state('');
	let selectedLeafCatId = $state('');
	let browseMode = $state<'category' | 'search'>('category');

	let availableLeafCategories = $derived.by(() => {
		if (!selectedFirstCat) return [];
		const first = categories.find(c => c.name === selectedFirstCat);
		if (!first) return [];
		const leaves: CjLeafCategory[] = [];
		for (const sub of first.subCategories) {
			for (const leaf of sub.categories) {
				leaves.push(leaf);
			}
		}
		return leaves;
	});

	// Pagination
	const PAGE_SIZE = 20;
	let currentPage = $state(1);
	let hasMore = $state(false);

	// View mode
	let viewMode = $state<'grid' | 'list'>('grid');

	// Batch selection
	let selectedPids = $state<Set<string>>(new Set());
	let batchImporting = $state(false);
	let batchDialogOpen = $state(false);

	// Batch import settings
	let batchPricingRule = $state('MARGIN');
	let batchTargetMargin = $state('30');
	let batchFixedMarkup = $state('');
	let batchMinMargin = $state('15');
	let batchLowStock = $state('5');
	let batchWarehouse = $state('CN');
	let batchProgress = $state(0);
	let batchTotal = $state(0);

	// Product detail / import dialog
	let detailDialogOpen = $state(false);
	let detailLoading = $state(false);
	let productDetail = $state<CjProductDetail | null>(null);
	let selectedVariants = $state<Set<string>>(new Set());

	// Import settings
	let pricingRule = $state('MARGIN');
	let targetMarginPct = $state('30');
	let fixedMarkupAmount = $state('');
	let minMarginPct = $state('15');
	let lowStockThreshold = $state('5');
	let warehouseCountry = $state('CN');
	let importing = $state(false);

	const warehouseOptions = [
		{ value: 'CN', label: 'China' },
		{ value: 'US', label: 'United States' },
		{ value: 'DE', label: 'Germany' },
		{ value: 'UK', label: 'United Kingdom' },
		{ value: 'AU', label: 'Australia' },
		{ value: 'TH', label: 'Thailand' }
	];

	async function loadConnections() {
		loadingConnections = true;
		try {
			connections = await getConnections();
			if (connections.length > 0) {
				selectedConnectionId = connections[0].id;
				loadCategoriesForConnection(connections[0].id);
			}
		} catch {
			toast.error('Failed to load connections');
		} finally {
			loadingConnections = false;
		}
	}

	async function loadCategoriesForConnection(connectionId: string) {
		loadingCategories = true;
		try {
			categories = await getCjCategories(connectionId);
		} catch {
			// Categories are optional — don't block the page
		} finally {
			loadingCategories = false;
		}
	}

	async function handleBrowseCategory(page = 1) {
		if (!selectedConnectionId || !selectedLeafCatId) return;
		searching = true;
		browseMode = 'category';
		if (page === 1) selectedPids = new Set();
		try {
			searchResults = await listByCategory(selectedConnectionId, selectedLeafCatId, page, PAGE_SIZE);
			currentPage = page;
			hasMore = searchResults.length === PAGE_SIZE;
		} catch (err: any) {
			toast.error(err.message || 'Failed to load category products');
		} finally {
			searching = false;
		}
	}

	async function handleSearch(page = 1) {
		if (!selectedConnectionId || !searchQuery.trim()) return;
		searching = true;
		browseMode = 'search';
		if (page === 1) {
			selectedPids = new Set();
			selectedFirstCat = '';
			selectedLeafCatId = '';
		}
		try {
			searchResults = await searchCatalog(selectedConnectionId, searchQuery, page, PAGE_SIZE);
			currentPage = page;
			hasMore = searchResults.length === PAGE_SIZE;
		} catch (err: any) {
			toast.error(err.message || 'Search failed');
		} finally {
			searching = false;
		}
	}

	function handleSearchKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter') handleSearch();
	}

	function toggleSelectProduct(pid: string) {
		const next = new Set(selectedPids);
		if (next.has(pid)) {
			next.delete(pid);
		} else {
			next.add(pid);
		}
		selectedPids = next;
	}

	function toggleSelectAll() {
		if (selectedPids.size === searchResults.length) {
			selectedPids = new Set();
		} else {
			selectedPids = new Set(searchResults.map(p => p.pid));
		}
	}

	async function openProductDetail(product: CjCatalogProduct) {
		detailDialogOpen = true;
		detailLoading = true;
		productDetail = null;
		selectedVariants = new Set();
		pricingRule = 'MARGIN';
		targetMarginPct = '30';
		fixedMarkupAmount = '';
		minMarginPct = '15';
		lowStockThreshold = '5';
		warehouseCountry = 'CN';

		try {
			productDetail = await getCjProductDetail(selectedConnectionId, product.pid);
			// Select all variants by default
			if (productDetail.variants.length > 0) {
				selectedVariants = new Set(productDetail.variants.map(v => v.vid));
			}
		} catch (err: any) {
			toast.error(err.message || 'Failed to load product details');
			detailDialogOpen = false;
		} finally {
			detailLoading = false;
		}
	}

	function toggleVariant(vid: string) {
		const next = new Set(selectedVariants);
		if (next.has(vid)) {
			next.delete(vid);
		} else {
			next.add(vid);
		}
		selectedVariants = next;
	}

	async function handleImport() {
		if (!productDetail || selectedVariants.size === 0) return;
		importing = true;

		const variants: ImportVariantRequest[] = productDetail.variants
			.filter(v => selectedVariants.has(v.vid))
			.map(v => ({
				cjVariantId: v.vid,
				cjSku: v.sku,
				warehouseId: warehouseCountry,
				warehouseCountry: warehouseCountry
			}));

		try {
			await importProduct({
				connectionId: selectedConnectionId,
				externalProductId: productDetail.pid,
				variants,
				pricingRule,
				targetMarginPct: pricingRule === 'MARGIN' ? Number(targetMarginPct) : undefined,
				fixedMarkupAmount: pricingRule === 'FIXED_MARKUP' ? Number(fixedMarkupAmount) : undefined,
				minMarginPct: Number(minMarginPct) || undefined,
				lowStockThreshold: Number(lowStockThreshold) || undefined
			});
			toast.success(`Imported "${productDetail.name}"`);
			detailDialogOpen = false;
		} catch (err: any) {
			toast.error(err.message || 'Import failed');
		} finally {
			importing = false;
		}
	}

	function openBatchDialog() {
		batchPricingRule = 'MARGIN';
		batchTargetMargin = '30';
		batchFixedMarkup = '';
		batchMinMargin = '15';
		batchLowStock = '5';
		batchWarehouse = 'CN';
		batchProgress = 0;
		batchTotal = selectedPids.size;
		batchDialogOpen = true;
	}

	async function handleBatchImport() {
		if (selectedPids.size === 0) return;
		batchImporting = true;
		batchProgress = 0;
		batchTotal = selectedPids.size;
		let imported = 0;
		let failed = 0;

		for (const pid of selectedPids) {
			try {
				const detail = await getCjProductDetail(selectedConnectionId, pid);
				const variants: ImportVariantRequest[] = detail.variants.map(v => ({
					cjVariantId: v.vid,
					cjSku: v.sku,
					warehouseId: batchWarehouse,
					warehouseCountry: batchWarehouse
				}));

				await importProduct({
					connectionId: selectedConnectionId,
					externalProductId: pid,
					variants,
					pricingRule: batchPricingRule,
					targetMarginPct: batchPricingRule === 'MARGIN' ? Number(batchTargetMargin) : undefined,
					fixedMarkupAmount: batchPricingRule === 'FIXED_MARKUP' ? Number(batchFixedMarkup) : undefined,
					minMarginPct: Number(batchMinMargin) || undefined,
					lowStockThreshold: Number(batchLowStock) || undefined
				});
				imported++;
			} catch {
				failed++;
			}
			batchProgress++;
		}

		selectedPids = new Set();
		batchImporting = false;
		batchDialogOpen = false;
		if (failed === 0) {
			toast.success(`Imported ${imported} products`);
		} else {
			toast.warning(`Imported ${imported}, failed ${failed}`);
		}
	}

	async function handleAddToWatchlist(product: CjCatalogProduct) {
		try {
			await addToWatchlist(selectedConnectionId, product.pid);
			toast.success('Added to watchlist');
		} catch (err: any) {
			toast.error(err.message || 'Failed to add to watchlist');
		}
	}

	function formatPrice(price: number): string {
		return `$${price.toFixed(2)}`;
	}

	onMount(loadConnections);
</script>

<div class="space-y-6">
	<PageHeader
		title="Import Products"
		description="Search CJ Dropshipping catalog and import products"
		breadcrumbs={[
			{ label: 'Home', href: '/dashboard' },
			{ label: 'Marketplace', href: '/marketplace/connections' },
			{ label: 'Import' }
		]}
	>
		{#if selectedPids.size > 0}
			<Button onclick={openBatchDialog}>
				<DownloadIcon class="mr-1.5 h-4 w-4" />
				Import {selectedPids.size} Selected
			</Button>
		{/if}
	</PageHeader>

	<!-- Search bar -->
	<div class="flex flex-col gap-3 sm:flex-row">
		{#if loadingConnections}
			<Skeleton class="h-10 w-48" />
		{:else if connections.length > 0}
			<Select.Root
				type="single"
				value={selectedConnectionId}
				onValueChange={(v) => (selectedConnectionId = v)}
			>
				<Select.Trigger class="w-full sm:w-48">
					{connections.find(c => c.id === selectedConnectionId)?.provider === 'CJ_DROPSHIPPING'
						? 'CJ Dropshipping'
						: connections.find(c => c.id === selectedConnectionId)?.provider || 'Select...'}
				</Select.Trigger>
				<Select.Content>
					{#each connections as conn}
						<Select.Item value={conn.id}>
							{conn.provider === 'CJ_DROPSHIPPING' ? 'CJ Dropshipping' : conn.provider}
						</Select.Item>
					{/each}
				</Select.Content>
			</Select.Root>
		{/if}

		<div class="relative flex-1">
			<SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
			<Input
				class="pl-9"
				placeholder="Search products on CJ Dropshipping..."
				bind:value={searchQuery}
				onkeydown={handleSearchKeydown}
			/>
		</div>
		<Button onclick={() => handleSearch()} disabled={searching || !selectedConnectionId}>
			{searching ? 'Searching...' : 'Search'}
		</Button>
	</div>

	<!-- Category Browser -->
	{#if categories.length > 0}
		<div class="flex flex-col gap-3 sm:flex-row sm:items-center">
			<Select.Root
				type="single"
				value={selectedFirstCat}
				onValueChange={(v) => { selectedFirstCat = v; selectedLeafCatId = ''; }}
			>
				<Select.Trigger class="w-full sm:w-56">
					{selectedFirstCat || 'Browse by category...'}
				</Select.Trigger>
				<Select.Content class="max-h-64 overflow-y-auto">
					{#each categories as cat}
						<Select.Item value={cat.name}>{cat.name}</Select.Item>
					{/each}
				</Select.Content>
			</Select.Root>

			{#if availableLeafCategories.length > 0}
				<Select.Root
					type="single"
					value={selectedLeafCatId}
					onValueChange={(v) => { selectedLeafCatId = v; handleBrowseCategory(); }}
				>
					<Select.Trigger class="w-full sm:w-64">
						{availableLeafCategories.find(l => l.categoryId === selectedLeafCatId)?.categoryName || 'Select subcategory...'}
					</Select.Trigger>
					<Select.Content class="max-h-64 overflow-y-auto">
						{#each availableLeafCategories as leaf}
							<Select.Item value={leaf.categoryId}>{leaf.categoryName}</Select.Item>
						{/each}
					</Select.Content>
				</Select.Root>
			{/if}

			{#if selectedFirstCat}
				<Button
					variant="ghost"
					size="sm"
					class="text-muted-foreground"
					onclick={() => { selectedFirstCat = ''; selectedLeafCatId = ''; searchResults = []; hasMore = false; }}
				>
					<XIcon class="mr-1 h-3.5 w-3.5" />
					Clear filter
				</Button>
			{/if}
		</div>
	{:else if loadingCategories}
		<div class="flex gap-3">
			<Skeleton class="h-10 w-56" />
			<Skeleton class="h-10 w-64" />
		</div>
	{/if}

	<!-- Results -->
	{#if searching}
		<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
			{#each Array(8) as _}
				<Card.Root>
					<Skeleton class="aspect-square w-full rounded-t-lg" />
					<Card.Content class="pt-3">
						<Skeleton class="h-4 w-full mb-2" />
						<Skeleton class="h-4 w-1/2" />
					</Card.Content>
				</Card.Root>
			{/each}
		</div>
	{:else if searchResults.length === 0 && (searchQuery || selectedLeafCatId)}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={PackageIcon}
				title="No results"
				description="Try different keywords, another category, or check your connection."
			/>
		</div>
	{:else if searchResults.length > 0}
		<div class="flex items-center justify-between">
			<div class="flex items-center gap-2 text-sm text-muted-foreground">
				<button
					class="underline hover:text-foreground"
					onclick={toggleSelectAll}
				>
					{selectedPids.size === searchResults.length ? 'Deselect all' : 'Select all'}
				</button>
				<span>·</span>
				<span>{searchResults.length} results</span>
			</div>
			<div class="flex items-center rounded-lg border p-0.5">
				<button
					class="rounded-md p-1.5 transition-colors {viewMode === 'grid' ? 'bg-muted text-foreground' : 'text-muted-foreground hover:text-foreground'}"
					onclick={() => (viewMode = 'grid')}
					title="Grid view"
				>
					<LayoutGridIcon class="h-4 w-4" />
				</button>
				<button
					class="rounded-md p-1.5 transition-colors {viewMode === 'list' ? 'bg-muted text-foreground' : 'text-muted-foreground hover:text-foreground'}"
					onclick={() => (viewMode = 'list')}
					title="List view"
				>
					<ListIcon class="h-4 w-4" />
				</button>
			</div>
		</div>

		{#if viewMode === 'grid'}
			<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
				{#each searchResults as product}
					<Card.Root class="overflow-hidden group relative">
						<!-- Checkbox -->
						<div class="absolute top-2 left-2 z-10">
							<input
								type="checkbox"
								checked={selectedPids.has(product.pid)}
								onchange={() => toggleSelectProduct(product.pid)}
								class="h-4 w-4 rounded border-gray-300 accent-primary cursor-pointer"
							/>
						</div>

						<!-- Product image -->
						<div class="aspect-square overflow-hidden bg-muted">
							{#if product.image}
								<img
									src={product.image}
									alt={product.name}
									class="h-full w-full object-cover transition-transform group-hover:scale-105"
								/>
							{:else}
								<div class="flex h-full items-center justify-center">
									<PackageIcon class="h-12 w-12 text-muted-foreground/30" />
								</div>
							{/if}
						</div>

						<Card.Content class="pt-3 space-y-2">
							<p class="text-sm font-medium line-clamp-2 leading-tight">{product.name}</p>
							<div class="flex items-center justify-between">
								<span class="text-lg font-semibold text-primary">{formatPrice(product.price)}</span>
								{#if product.categoryName}
									<Badge variant="outline" class="text-xs">{product.categoryName}</Badge>
								{/if}
							</div>
							<div class="flex gap-1 pt-1">
								<Button
									variant="default"
									size="sm"
									class="flex-1"
									onclick={() => openProductDetail(product)}
								>
									<DownloadIcon class="mr-1 h-3.5 w-3.5" />
									Import
								</Button>
								<Button
									variant="outline"
									size="sm"
									onclick={() => handleAddToWatchlist(product)}
									title="Add to watchlist"
								>
									<EyeIcon class="h-3.5 w-3.5" />
								</Button>
							</div>
						</Card.Content>
					</Card.Root>
				{/each}
			</div>
		{:else}
			<div class="space-y-2">
				{#each searchResults as product}
					<div class="flex items-center gap-4 rounded-lg border p-3 hover:bg-muted/50 transition-colors group">
						<input
							type="checkbox"
							checked={selectedPids.has(product.pid)}
							onchange={() => toggleSelectProduct(product.pid)}
							class="h-4 w-4 rounded border-gray-300 accent-primary cursor-pointer flex-shrink-0"
						/>
						<div class="h-14 w-14 flex-shrink-0 overflow-hidden rounded-md bg-muted">
							{#if product.image}
								<img
									src={product.image}
									alt={product.name}
									class="h-full w-full object-cover"
								/>
							{:else}
								<div class="flex h-full items-center justify-center">
									<PackageIcon class="h-6 w-6 text-muted-foreground/30" />
								</div>
							{/if}
						</div>
						<div class="flex-1 min-w-0">
							<p class="text-sm font-medium truncate">{product.name}</p>
							{#if product.categoryName}
								<Badge variant="outline" class="text-xs mt-1">{product.categoryName}</Badge>
							{/if}
						</div>
						<span class="text-lg font-semibold text-primary flex-shrink-0">{formatPrice(product.price)}</span>
						<div class="flex gap-1 flex-shrink-0">
							<Button
								variant="default"
								size="sm"
								onclick={() => openProductDetail(product)}
							>
								<DownloadIcon class="mr-1 h-3.5 w-3.5" />
								Import
							</Button>
							<Button
								variant="outline"
								size="sm"
								onclick={() => handleAddToWatchlist(product)}
								title="Add to watchlist"
							>
								<EyeIcon class="h-3.5 w-3.5" />
							</Button>
						</div>
					</div>
				{/each}
			</div>
		{/if}

		<!-- Pagination -->
		{#if currentPage > 1 || hasMore}
			{@const goToPage = browseMode === 'category' ? handleBrowseCategory : handleSearch}
			<div class="flex items-center justify-center gap-2 pt-4">
				<Button
					variant="outline"
					size="sm"
					disabled={currentPage <= 1 || searching}
					onclick={() => goToPage(currentPage - 1)}
				>
					Previous
				</Button>
				<span class="text-sm text-muted-foreground">Page {currentPage}</span>
				<Button
					variant="outline"
					size="sm"
					disabled={!hasMore || searching}
					onclick={() => goToPage(currentPage + 1)}
				>
					Next
				</Button>
			</div>
		{/if}
	{/if}
</div>

<!-- Product Detail / Import Dialog -->
<Dialog.Root bind:open={detailDialogOpen}>
	<Dialog.Content class="max-w-5xl max-h-[90vh] overflow-y-auto">
		<Dialog.Header>
			<Dialog.Title>Import Product</Dialog.Title>
		</Dialog.Header>

		{#if detailLoading}
			<div class="space-y-4 py-4">
				<Skeleton class="h-48 w-full" />
				<Skeleton class="h-6 w-3/4" />
				<Skeleton class="h-4 w-1/2" />
			</div>
		{:else if productDetail}
			<div class="space-y-6 py-4">
				<!-- Product Info -->
				<div class="flex gap-4">
					{#if productDetail.image}
						<img
							src={productDetail.image}
							alt={productDetail.name}
							class="h-32 w-32 rounded-lg object-cover flex-shrink-0"
						/>
					{/if}
					<div class="space-y-1">
						<h3 class="font-semibold">{productDetail.name}</h3>
						<p class="text-lg font-bold text-primary">{formatPrice(productDetail.price)}</p>
						{#if productDetail.categoryName}
							<Badge variant="outline">{productDetail.categoryName}</Badge>
						{/if}
					</div>
				</div>

				<!-- Variant Selection -->
				{#if productDetail.variants.length > 0}
					<div class="space-y-3">
						<Label class="text-base font-medium">Variants ({selectedVariants.size}/{productDetail.variants.length})</Label>
						<div class="max-h-48 overflow-auto space-y-2 rounded-lg border p-3">
							{#each productDetail.variants as variant}
								<label class="flex items-center gap-3 cursor-pointer hover:bg-muted/50 rounded p-1.5 -mx-1.5">
									<input
										type="checkbox"
										checked={selectedVariants.has(variant.vid)}
										onchange={() => toggleVariant(variant.vid)}
										class="h-4 w-4 rounded border-gray-300 accent-primary cursor-pointer"
									/>
									{#if variant.image}
										<img src={variant.image} alt="" class="h-8 w-8 rounded object-cover" />
									{/if}
									<div class="flex-1 min-w-0">
										<p class="text-sm font-medium truncate">{variant.name || variant.sku}</p>
										<p class="text-xs text-muted-foreground">SKU: {variant.sku}</p>
									</div>
									<div class="text-right flex-shrink-0">
										<p class="text-sm font-medium">{formatPrice(variant.price)}</p>
										<p class="text-xs {variant.stock > 0 ? 'text-green-600' : 'text-red-500'}">
											{variant.stock > 0 ? `${variant.stock} in stock` : 'Out of stock'}
										</p>
									</div>
								</label>
							{/each}
						</div>
					</div>
				{/if}

				<!-- Warehouse Selection -->
				<div class="space-y-2">
					<Label>Warehouse Country</Label>
					<div class="flex flex-wrap gap-2">
						{#each warehouseOptions as opt}
							<button
								class="px-3 py-1.5 rounded-full text-sm border transition-colors {warehouseCountry === opt.value
									? 'bg-primary text-primary-foreground border-primary'
									: 'hover:bg-muted'}"
								onclick={() => (warehouseCountry = opt.value)}
							>
								{opt.label}
							</button>
						{/each}
					</div>
				</div>

				<!-- Pricing Rule -->
				<div class="grid grid-cols-2 gap-4">
					<div class="space-y-2">
						<Label>Pricing Rule</Label>
						<Select.Root
							type="single"
							value={pricingRule}
							onValueChange={(v) => (pricingRule = v)}
						>
							<Select.Trigger class="w-full">
								{pricingRule === 'MARGIN' ? 'Margin %' : pricingRule === 'FIXED_MARKUP' ? 'Fixed Markup' : 'Manual'}
							</Select.Trigger>
							<Select.Content>
								<Select.Item value="MARGIN">Margin %</Select.Item>
								<Select.Item value="FIXED_MARKUP">Fixed Markup</Select.Item>
								<Select.Item value="MANUAL">Manual</Select.Item>
							</Select.Content>
						</Select.Root>
					</div>

					{#if pricingRule === 'MARGIN'}
						<div class="space-y-2">
							<Label for="targetMargin">Target Margin %</Label>
							<Input id="targetMargin" type="number" bind:value={targetMarginPct} placeholder="30" />
						</div>
					{:else if pricingRule === 'FIXED_MARKUP'}
						<div class="space-y-2">
							<Label for="fixedMarkup">Markup Amount ($)</Label>
							<Input id="fixedMarkup" type="number" bind:value={fixedMarkupAmount} placeholder="5.00" />
						</div>
					{:else}
						<div></div>
					{/if}
				</div>

				<div class="grid grid-cols-2 gap-4">
					<div class="space-y-2">
						<Label for="minMargin">Min Margin % (alert)</Label>
						<Input id="minMargin" type="number" bind:value={minMarginPct} placeholder="15" />
					</div>
					<div class="space-y-2">
						<Label for="lowStock">Low Stock Threshold</Label>
						<Input id="lowStock" type="number" bind:value={lowStockThreshold} placeholder="5" />
					</div>
				</div>
			</div>

			<Dialog.Footer>
				<Button variant="outline" onclick={() => (detailDialogOpen = false)}>Cancel</Button>
				<Button onclick={handleImport} disabled={importing || selectedVariants.size === 0}>
					{importing ? 'Importing...' : `Import ${selectedVariants.size} Variant${selectedVariants.size === 1 ? '' : 's'}`}
				</Button>
			</Dialog.Footer>
		{/if}
	</Dialog.Content>
</Dialog.Root>

<!-- Batch Import Dialog -->
<Dialog.Root bind:open={batchDialogOpen}>
	<Dialog.Content class="max-w-lg">
		<Dialog.Header>
			<Dialog.Title>Batch Import — {selectedPids.size} Products</Dialog.Title>
		</Dialog.Header>
		<div class="space-y-5 py-4">
			<p class="text-sm text-muted-foreground">
				Configure global settings for all selected products. All variants will be imported.
			</p>

			<!-- Warehouse -->
			<div class="space-y-2">
				<Label>Warehouse Country</Label>
				<div class="flex flex-wrap gap-2">
					{#each warehouseOptions as opt}
						<button
							class="px-3 py-1.5 rounded-full text-sm border transition-colors {batchWarehouse === opt.value
								? 'bg-primary text-primary-foreground border-primary'
								: 'hover:bg-muted'}"
							onclick={() => (batchWarehouse = opt.value)}
						>
							{opt.label}
						</button>
					{/each}
				</div>
			</div>

			<!-- Pricing Rule -->
			<div class="grid grid-cols-2 gap-4">
				<div class="space-y-2">
					<Label>Pricing Rule</Label>
					<Select.Root
						type="single"
						value={batchPricingRule}
						onValueChange={(v) => (batchPricingRule = v)}
					>
						<Select.Trigger class="w-full">
							{batchPricingRule === 'MARGIN' ? 'Margin %' : batchPricingRule === 'FIXED_MARKUP' ? 'Fixed Markup' : 'Manual'}
						</Select.Trigger>
						<Select.Content>
							<Select.Item value="MARGIN">Margin %</Select.Item>
							<Select.Item value="FIXED_MARKUP">Fixed Markup</Select.Item>
							<Select.Item value="MANUAL">Manual</Select.Item>
						</Select.Content>
					</Select.Root>
				</div>

				{#if batchPricingRule === 'MARGIN'}
					<div class="space-y-2">
						<Label>Target Margin %</Label>
						<Input type="number" bind:value={batchTargetMargin} placeholder="30" />
					</div>
				{:else if batchPricingRule === 'FIXED_MARKUP'}
					<div class="space-y-2">
						<Label>Markup Amount ($)</Label>
						<Input type="number" bind:value={batchFixedMarkup} placeholder="5.00" />
					</div>
				{:else}
					<div></div>
				{/if}
			</div>

			<div class="grid grid-cols-2 gap-4">
				<div class="space-y-2">
					<Label>Min Margin % (alert)</Label>
					<Input type="number" bind:value={batchMinMargin} placeholder="15" />
				</div>
				<div class="space-y-2">
					<Label>Low Stock Threshold</Label>
					<Input type="number" bind:value={batchLowStock} placeholder="5" />
				</div>
			</div>

			{#if batchImporting}
				<div class="space-y-2">
					<div class="flex justify-between text-sm">
						<span>Importing...</span>
						<span>{batchProgress}/{batchTotal}</span>
					</div>
					<div class="w-full bg-muted rounded-full h-2">
						<div
							class="bg-primary h-2 rounded-full transition-all"
							style="width: {batchTotal > 0 ? (batchProgress / batchTotal) * 100 : 0}%"
						></div>
					</div>
				</div>
			{/if}
		</div>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (batchDialogOpen = false)} disabled={batchImporting}>Cancel</Button>
			<Button onclick={handleBatchImport} disabled={batchImporting}>
				{batchImporting ? `Importing ${batchProgress}/${batchTotal}...` : `Import ${selectedPids.size} Products`}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>
