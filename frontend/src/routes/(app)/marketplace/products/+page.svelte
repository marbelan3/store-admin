<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/stores/auth.svelte';
	import { getMarketplaceProducts, toggleExclude, updateThreshold } from '$lib/api/marketplace';
	import type { MarketplaceProduct } from '$lib/types/marketplace';
	import type { Page } from '$lib/types/product';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Badge } from '$lib/components/ui/badge';
	import { Switch } from '$lib/components/ui/switch';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Table from '$lib/components/ui/table';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import ListIcon from '@lucide/svelte/icons/list';

	let products = $state<MarketplaceProduct[]>([]);
	let loading = $state(true);
	let pageData = $state<Page<MarketplaceProduct> | null>(null);
	let currentPage = $state(0);

	async function loadProducts(page = 0) {
		loading = true;
		try {
			pageData = await getMarketplaceProducts(page, 20);
			products = pageData.content;
			currentPage = page;
		} catch {
			toast.error('Failed to load marketplace products');
		} finally {
			loading = false;
		}
	}

	async function handleToggleExclude(product: MarketplaceProduct) {
		try {
			const updated = await toggleExclude(product.id);
			const idx = products.findIndex(p => p.id === product.id);
			if (idx >= 0) {
				products[idx] = updated;
				products = [...products];
			}
			toast.success(updated.excluded ? 'Product excluded from sync' : 'Product included in sync');
		} catch (err: any) {
			toast.error(err.message || 'Failed to toggle exclude');
		}
	}

	async function handleUpdateThreshold(product: MarketplaceProduct, value: string) {
		const threshold = parseInt(value);
		if (isNaN(threshold) || threshold < 0) return;
		try {
			const updated = await updateThreshold(product.id, threshold);
			const idx = products.findIndex(p => p.id === product.id);
			if (idx >= 0) {
				products[idx] = updated;
				products = [...products];
			}
			toast.success('Threshold updated');
		} catch (err: any) {
			toast.error(err.message || 'Failed to update threshold');
		}
	}

	function marginColor(margin: number | null): string {
		if (margin == null) return 'text-muted-foreground';
		if (margin >= 20) return 'text-green-600 dark:text-green-400';
		if (margin >= 10) return 'text-amber-600 dark:text-amber-400';
		return 'text-red-600 dark:text-red-400';
	}

	function marginBg(margin: number | null): string {
		if (margin == null) return '';
		if (margin >= 20) return 'bg-green-100 dark:bg-green-900/20';
		if (margin >= 10) return 'bg-amber-100 dark:bg-amber-900/20';
		return 'bg-red-100 dark:bg-red-900/20';
	}

	function syncStatusBadge(status: string): string {
		switch (status) {
			case 'SYNCED': return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
			case 'ERROR': return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
			case 'DELISTED': return 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400';
			default: return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
		}
	}

	function avgSourcePrice(product: MarketplaceProduct): string {
		const prices = product.variantMappings
			.filter(m => m.sourcePrice != null)
			.map(m => m.sourcePrice!);
		if (prices.length === 0) return '—';
		const avg = prices.reduce((a, b) => a + b, 0) / prices.length;
		return `$${avg.toFixed(2)}`;
	}

	function stockSummary(product: MarketplaceProduct): { total: number; color: string } {
		const stocks = product.variantMappings.filter(m => m.stockQuantity != null).map(m => m.stockQuantity!);
		const total = stocks.reduce((a, b) => a + b, 0);
		const threshold = product.lowStockThreshold ?? 5;
		let color = 'text-green-600 dark:text-green-400';
		if (total <= 0) color = 'text-red-600 dark:text-red-400';
		else if (total <= threshold) color = 'text-amber-600 dark:text-amber-400';
		return { total, color };
	}

	onMount(() => loadProducts());
</script>

<div class="space-y-6">
	<PageHeader
		title="Marketplace Products"
		description="Manage imported products — margins, sync status, and stock"
		breadcrumbs={[
			{ label: 'Home', href: '/dashboard' },
			{ label: 'Marketplace', href: '/marketplace/connections' },
			{ label: 'Products' }
		]}
	/>

	{#if loading}
		<div class="rounded-md border overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Name</Table.Head>
						<Table.Head>Sync</Table.Head>
						<Table.Head>CJ Price</Table.Head>
						<Table.Head>Margin</Table.Head>
						<Table.Head>Stock</Table.Head>
						<Table.Head>Threshold</Table.Head>
						<Table.Head>Excluded</Table.Head>
						<Table.Head>Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each Array(6) as _}
						<Table.Row>
							{#each Array(8) as __}
								<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							{/each}
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{:else if products.length === 0}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={ListIcon}
				title="No marketplace products"
				description="Import products from CJ Dropshipping to see them here."
				actionLabel="Import Products"
				actionHref="/marketplace/import"
			/>
		</div>
	{:else}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Name</Table.Head>
						<Table.Head>Sync</Table.Head>
						<Table.Head>CJ Price</Table.Head>
						<Table.Head>Margin %</Table.Head>
						<Table.Head>Stock</Table.Head>
						<Table.Head class="w-24">Threshold</Table.Head>
						<Table.Head class="w-20">Excluded</Table.Head>
						<Table.Head class="w-20">Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each products as product}
						{@const stock = stockSummary(product)}
						<Table.Row class="{product.excluded ? 'opacity-50' : ''} hover:bg-muted/50 transition-colors">
							<Table.Cell>
								<div class="flex items-center gap-2">
									<span class="font-medium">{product.productName}</span>
									{#if product.excluded}
										<Badge variant="outline" class="text-xs bg-gray-100 text-gray-500 dark:bg-gray-800/30 dark:text-gray-400">Excluded</Badge>
									{/if}
									{#if product.marginAlertTriggered}
										<Badge variant="outline" class="text-xs bg-red-100 text-red-600 dark:bg-red-900/20 dark:text-red-400">Margin Alert</Badge>
									{/if}
								</div>
								<p class="text-xs text-muted-foreground">CJ: {product.externalProductId}</p>
							</Table.Cell>
							<Table.Cell>
								<Badge class={syncStatusBadge(product.syncStatus)}>{product.syncStatus}</Badge>
							</Table.Cell>
							<Table.Cell>{avgSourcePrice(product)}</Table.Cell>
							<Table.Cell>
								{#if product.currentMarginPct != null}
									<span class="px-2 py-0.5 rounded text-sm font-medium {marginColor(product.currentMarginPct)} {marginBg(product.currentMarginPct)}">
										{product.currentMarginPct.toFixed(1)}%
									</span>
								{:else}
									<span class="text-muted-foreground">—</span>
								{/if}
							</Table.Cell>
							<Table.Cell>
								<span class="font-medium {stock.color}">{stock.total}</span>
							</Table.Cell>
							<Table.Cell>
								{#if auth.canEdit}
									<Input
										type="number"
										value={product.lowStockThreshold ?? ''}
										onchange={(e) => handleUpdateThreshold(product, (e.target as HTMLInputElement).value)}
										class="h-8 w-16 text-center"
										min="0"
									/>
								{:else}
									{product.lowStockThreshold ?? '—'}
								{/if}
							</Table.Cell>
							<Table.Cell>
								{#if auth.canEdit}
									<Switch
										checked={product.excluded}
										onCheckedChange={() => handleToggleExclude(product)}
									/>
								{:else}
									{product.excluded ? 'Yes' : 'No'}
								{/if}
							</Table.Cell>
							<Table.Cell>
								<Button
									variant="ghost"
									size="sm"
									onclick={() => goto(`/marketplace/products/${product.id}`)}
								>
									Details
								</Button>
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		{#if pageData && pageData.totalPages > 1}
			<div class="flex items-center justify-center gap-2 pt-4">
				<Button variant="outline" size="sm" disabled={pageData.first} onclick={() => loadProducts(currentPage - 1)}>Previous</Button>
				<span class="text-sm text-muted-foreground">Page {currentPage + 1} of {pageData.totalPages}</span>
				<Button variant="outline" size="sm" disabled={pageData.last} onclick={() => loadProducts(currentPage + 1)}>Next</Button>
			</div>
		{/if}
	{/if}
</div>
