<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/stores/auth.svelte';
	import {
		getConnections,
		getWatchlist,
		removeFromWatchlist,
		getCjProductDetail,
		importProduct
	} from '$lib/api/marketplace';
	import type {
		MarketplaceConnection,
		WatchlistItem,
		CjProductDetail,
		ImportVariantRequest
	} from '$lib/types/marketplace';
	import type { Page } from '$lib/types/product';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Badge } from '$lib/components/ui/badge';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Card from '$lib/components/ui/card';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Select from '$lib/components/ui/select';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import EyeIcon from '@lucide/svelte/icons/eye';
	import TrashIcon from '@lucide/svelte/icons/trash-2';
	import DownloadIcon from '@lucide/svelte/icons/download';
	import PackageIcon from '@lucide/svelte/icons/package';

	let items = $state<WatchlistItem[]>([]);
	let loading = $state(true);
	let pageData = $state<Page<WatchlistItem> | null>(null);
	let currentPage = $state(0);

	// Connections (for import)
	let connections = $state<MarketplaceConnection[]>([]);

	// Import dialog
	let detailDialogOpen = $state(false);
	let detailLoading = $state(false);
	let productDetail = $state<CjProductDetail | null>(null);
	let selectedVariants = $state<Set<string>>(new Set());
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

	// Confirm dialog
	let confirmOpen = $state(false);
	let confirmTitle = $state('');
	let confirmDescription = $state('');
	let confirmAction = $state<() => Promise<void>>(async () => {});

	async function loadWatchlist(page = 0) {
		loading = true;
		try {
			pageData = await getWatchlist(page, 20);
			items = pageData.content;
			currentPage = page;
		} catch {
			toast.error('Failed to load watchlist');
		} finally {
			loading = false;
		}
	}

	function handleRemove(item: WatchlistItem) {
		confirmTitle = 'Remove from watchlist';
		confirmDescription = `Remove "${item.name}" from your watchlist?`;
		confirmAction = async () => {
			try {
				await removeFromWatchlist(item.id);
				toast.success('Removed from watchlist');
				await loadWatchlist(currentPage);
			} catch {
				toast.error('Failed to remove');
			}
		};
		confirmOpen = true;
	}

	function goToImport() {
		goto('/marketplace/import');
	}

	async function openImportDialog(item: WatchlistItem) {
		if (connections.length === 0) {
			toast.error('No marketplace connection found');
			return;
		}
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
			productDetail = await getCjProductDetail(connections[0].id, item.externalProductId);
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
		if (next.has(vid)) next.delete(vid);
		else next.add(vid);
		selectedVariants = next;
	}

	async function handleImport() {
		if (!productDetail || selectedVariants.size === 0 || connections.length === 0) return;
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
				connectionId: connections[0].id,
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

	function formatImportPrice(price: number): string {
		return `$${price.toFixed(2)}`;
	}

	function stockStatusColor(status: string): string {
		switch (status) {
			case 'IN_STOCK': return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
			case 'LOW': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
			case 'OUT_OF_STOCK': return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
			default: return 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400';
		}
	}

	function formatDate(date: string | null): string {
		if (!date) return '—';
		return new Date(date).toLocaleDateString();
	}

	function formatPrice(price: number): string {
		return `$${price.toFixed(2)}`;
	}

	onMount(async () => {
		loadWatchlist();
		try {
			connections = await getConnections();
		} catch {
			// non-critical for page load
		}
	});
</script>

<ConfirmDialog
	bind:open={confirmOpen}
	title={confirmTitle}
	description={confirmDescription}
	confirmLabel="Remove"
	variant="destructive"
	onConfirm={confirmAction}
/>

<div class="space-y-6">
	<PageHeader
		title="Watchlist"
		description="Products you're monitoring before importing"
		breadcrumbs={[
			{ label: 'Home', href: '/dashboard' },
			{ label: 'Marketplace', href: '/marketplace/connections' },
			{ label: 'Watchlist' }
		]}
	>
		<Button variant="outline" onclick={goToImport}>
			<DownloadIcon class="mr-1.5 h-4 w-4" />
			Import Products
		</Button>
	</PageHeader>

	{#if loading}
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
	{:else if items.length === 0}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={EyeIcon}
				title="Watchlist is empty"
				description="Browse the CJ catalog and add products to your watchlist for monitoring."
				actionLabel="Browse Catalog"
				onAction={goToImport}
			/>
		</div>
	{:else}
		<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
			{#each items as item}
				<Card.Root class="overflow-hidden group">
					<div class="aspect-square overflow-hidden bg-muted">
						{#if item.imageUrl}
							<img
								src={item.imageUrl}
								alt={item.name}
								class="h-full w-full object-cover transition-transform group-hover:scale-105"
							/>
						{:else}
							<div class="flex h-full items-center justify-center">
								<PackageIcon class="h-12 w-12 text-muted-foreground/30" />
							</div>
						{/if}
					</div>

					<Card.Content class="pt-3 space-y-2">
						<p class="text-sm font-medium line-clamp-2 leading-tight">{item.name}</p>
						<div class="flex items-center justify-between">
							<span class="text-lg font-semibold text-primary">{formatPrice(item.price)}</span>
							<Badge class={stockStatusColor(item.stockStatus)}>
								{item.stockStatus.replace('_', ' ')}
							</Badge>
						</div>
						<div class="text-xs text-muted-foreground">
							Added {formatDate(item.addedAt)}
							{#if item.lastCheckedAt}
								· Updated {formatDate(item.lastCheckedAt)}
							{/if}
						</div>
						<div class="flex gap-1 pt-1">
							<Button
								variant="default"
								size="sm"
								class="flex-1"
								onclick={() => openImportDialog(item)}
							>
								<DownloadIcon class="mr-1 h-3.5 w-3.5" />
								Import
							</Button>
							<Button
								variant="outline"
								size="sm"
								onclick={() => handleRemove(item)}
								title="Remove from watchlist"
							>
								<TrashIcon class="h-3.5 w-3.5" />
							</Button>
						</div>
					</Card.Content>
				</Card.Root>
			{/each}
		</div>

		<!-- Pagination -->
		{#if pageData && pageData.totalPages > 1}
			<div class="flex items-center justify-center gap-2 pt-4">
				<Button
					variant="outline"
					size="sm"
					disabled={pageData.first}
					onclick={() => loadWatchlist(currentPage - 1)}
				>
					Previous
				</Button>
				<span class="text-sm text-muted-foreground">
					Page {currentPage + 1} of {pageData.totalPages}
				</span>
				<Button
					variant="outline"
					size="sm"
					disabled={pageData.last}
					onclick={() => loadWatchlist(currentPage + 1)}
				>
					Next
				</Button>
			</div>
		{/if}
	{/if}
</div>

<!-- Import Dialog -->
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
						<p class="text-lg font-bold text-primary">{formatImportPrice(productDetail.price)}</p>
						{#if productDetail.categoryName}
							<Badge variant="outline">{productDetail.categoryName}</Badge>
						{/if}
					</div>
				</div>

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
										<p class="text-sm font-medium">{formatImportPrice(variant.price)}</p>
										<p class="text-xs {variant.stock > 0 ? 'text-green-600' : 'text-red-500'}">
											{variant.stock > 0 ? `${variant.stock} in stock` : 'Out of stock'}
										</p>
									</div>
								</label>
							{/each}
						</div>
					</div>
				{/if}

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
							<Label>Target Margin %</Label>
							<Input type="number" bind:value={targetMarginPct} placeholder="30" />
						</div>
					{:else if pricingRule === 'FIXED_MARKUP'}
						<div class="space-y-2">
							<Label>Markup Amount ($)</Label>
							<Input type="number" bind:value={fixedMarkupAmount} placeholder="5.00" />
						</div>
					{:else}
						<div></div>
					{/if}
				</div>

				<div class="grid grid-cols-2 gap-4">
					<div class="space-y-2">
						<Label>Min Margin % (alert)</Label>
						<Input type="number" bind:value={minMarginPct} placeholder="15" />
					</div>
					<div class="space-y-2">
						<Label>Low Stock Threshold</Label>
						<Input type="number" bind:value={lowStockThreshold} placeholder="5" />
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
