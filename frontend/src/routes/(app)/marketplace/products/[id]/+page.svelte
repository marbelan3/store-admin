<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { auth } from '$lib/stores/auth.svelte';
	import { getMarketplaceProduct, toggleExclude, updateThreshold, getPriceHistory, updatePricing } from '$lib/api/marketplace';
	import type { MarketplaceProduct, VariantMapping, PriceHistory } from '$lib/types/marketplace';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Badge } from '$lib/components/ui/badge';
	import { Switch } from '$lib/components/ui/switch';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Card from '$lib/components/ui/card';
	import * as Select from '$lib/components/ui/select';
	import * as Table from '$lib/components/ui/table';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';

	let product = $state<MarketplaceProduct | null>(null);
	let loading = $state(true);

	// Price history
	let priceHistoryMap = $state<Record<string, PriceHistory[]>>({});
	let expandedVariant = $state<string | null>(null);
	let historyLoading = $state(false);

	// Pricing editor
	let pricingSaving = $state(false);

	async function loadProduct() {
		loading = true;
		try {
			product = await getMarketplaceProduct($page.params.id!);
		} catch {
			toast.error('Failed to load product');
		} finally {
			loading = false;
		}
	}

	async function handleToggleExclude() {
		if (!product) return;
		try {
			product = await toggleExclude(product.id);
			toast.success(product.excluded ? 'Product excluded' : 'Product included');
		} catch (err: any) {
			toast.error(err.message || 'Failed to toggle');
		}
	}

	async function handleUpdateThreshold(value: string) {
		if (!product) return;
		const threshold = parseInt(value);
		if (isNaN(threshold) || threshold < 0) return;
		try {
			product = await updateThreshold(product.id, threshold);
			toast.success('Threshold updated');
		} catch (err: any) {
			toast.error(err.message || 'Failed to update');
		}
	}

	function marginColor(margin: number | null): string {
		if (margin == null) return 'text-muted-foreground';
		if (margin >= 20) return 'text-green-600 dark:text-green-400';
		if (margin >= 10) return 'text-amber-600 dark:text-amber-400';
		return 'text-red-600 dark:text-red-400';
	}

	function stockColor(qty: number | null, threshold: number | null): string {
		if (qty == null) return 'text-muted-foreground';
		const t = threshold ?? 5;
		if (qty <= 0) return 'text-red-600 dark:text-red-400';
		if (qty <= t) return 'text-amber-600 dark:text-amber-400';
		return 'text-green-600 dark:text-green-400';
	}

	function skuStatusBadge(status: string): string {
		switch (status) {
			case 'ACTIVE': return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
			case 'CHANGED': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
			case 'DISCONTINUED': return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
			case 'NOT_FOUND': return 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400';
			default: return '';
		}
	}

	function warehouseBadge(country: string | null): string {
		switch (country) {
			case 'CN': return 'CN';
			case 'US': return 'US';
			case 'DE': return 'DE';
			case 'UK': return 'UK';
			case 'AU': return 'AU';
			case 'TH': return 'TH';
			default: return country || '—';
		}
	}

	async function handlePricingUpdate(field: string, value: string) {
		if (!product) return;
		pricingSaving = true;
		try {
			const data: Record<string, any> = {};
			if (field === 'pricingRule') {
				data.pricingRule = value;
			} else {
				data[field] = parseFloat(value);
			}
			product = await updatePricing(product.id, data);
			toast.success('Pricing updated');
		} catch (err: any) {
			toast.error(err.message || 'Failed to update pricing');
		} finally {
			pricingSaving = false;
		}
	}

	async function togglePriceHistory(mappingId: string) {
		if (expandedVariant === mappingId) {
			expandedVariant = null;
			return;
		}
		expandedVariant = mappingId;
		if (!priceHistoryMap[mappingId]) {
			historyLoading = true;
			try {
				priceHistoryMap[mappingId] = await getPriceHistory(mappingId);
				priceHistoryMap = { ...priceHistoryMap };
			} catch {
				toast.error('Failed to load price history');
			} finally {
				historyLoading = false;
			}
		}
	}

	function miniChartPath(history: PriceHistory[]): string {
		if (history.length < 2) return '';
		const sorted = [...history].reverse();
		const prices = sorted.map(h => h.newPrice);
		const min = Math.min(...prices);
		const max = Math.max(...prices);
		const range = max - min || 1;
		const width = 200;
		const height = 40;
		const points = prices.map((p, i) => {
			const x = (i / (prices.length - 1)) * width;
			const y = height - ((p - min) / range) * height;
			return `${x},${y}`;
		});
		return `M${points.join(' L')}`;
	}

	function formatDate(date: string | null): string {
		if (!date) return '—';
		return new Date(date).toLocaleString();
	}

	onMount(loadProduct);
</script>

{#if loading}
	<div class="space-y-6">
		<Skeleton class="h-8 w-64" />
		<Skeleton class="h-4 w-96" />
		<div class="grid gap-4 md:grid-cols-3">
			{#each Array(3) as _}
				<Skeleton class="h-32 rounded-lg" />
			{/each}
		</div>
		<Skeleton class="h-64 rounded-lg" />
	</div>
{:else if product}
	<div class="space-y-6">
		<PageHeader
			title={product.productName}
			description="Marketplace product details and variant mappings"
			breadcrumbs={[
				{ label: 'Home', href: '/dashboard' },
				{ label: 'Marketplace', href: '/marketplace/connections' },
				{ label: 'Products', href: '/marketplace/products' },
				{ label: product.productName }
			]}
		>
			{#if auth.canEdit}
				<Button variant="outline" onclick={() => window.open(`/products/${product?.productId}`, '_blank')}>
					View Product
				</Button>
			{/if}
		</PageHeader>

		<!-- Summary Cards -->
		<div class="grid gap-4 md:grid-cols-4">
			<Card.Root>
				<Card.Content class="pt-4 space-y-2">
					<p class="text-sm text-muted-foreground">Pricing Rule</p>
					{#if auth.canEdit}
						<Select.Root
							type="single"
							value={product.pricingRule}
							onValueChange={(v) => handlePricingUpdate('pricingRule', v)}
						>
							<Select.Trigger class="w-full h-8 text-sm">
								{product.pricingRule === 'MARGIN' ? 'Margin %' : product.pricingRule === 'FIXED_MARKUP' ? 'Fixed Markup' : 'Manual'}
							</Select.Trigger>
							<Select.Content>
								<Select.Item value="MARGIN">Margin %</Select.Item>
								<Select.Item value="FIXED_MARKUP">Fixed Markup</Select.Item>
								<Select.Item value="MANUAL">Manual</Select.Item>
							</Select.Content>
						</Select.Root>
						{#if product.pricingRule === 'MARGIN' && product.targetMarginPct != null}
							<Input
								type="number"
								value={product.targetMarginPct}
								onchange={(e) => handlePricingUpdate('targetMarginPct', (e.target as HTMLInputElement).value)}
								class="h-8 text-sm"
								min="0"
								max="100"
							/>
							<p class="text-xs text-muted-foreground">Target margin %</p>
						{/if}
					{:else}
						<p class="text-lg font-semibold">{product.pricingRule}</p>
						{#if product.targetMarginPct != null}
							<p class="text-xs text-muted-foreground">Target: {product.targetMarginPct}%</p>
						{/if}
					{/if}
				</Card.Content>
			</Card.Root>

			<Card.Root>
				<Card.Content class="pt-4">
					<p class="text-sm text-muted-foreground">Current Margin</p>
					<p class="text-lg font-semibold {marginColor(product.currentMarginPct)}">
						{product.currentMarginPct != null ? `${product.currentMarginPct.toFixed(1)}%` : '—'}
					</p>
					{#if product.minMarginPct != null}
						<p class="text-xs text-muted-foreground">Min alert: {product.minMarginPct}%</p>
					{/if}
				</Card.Content>
			</Card.Root>

			<Card.Root>
				<Card.Content class="pt-4">
					<p class="text-sm text-muted-foreground">Sync Status</p>
					<Badge class="mt-1 {product.syncStatus === 'SYNCED' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-amber-100 text-amber-700'}">
						{product.syncStatus}
					</Badge>
					{#if product.excluded}
						<Badge variant="outline" class="ml-1 text-xs">Excluded</Badge>
					{/if}
				</Card.Content>
			</Card.Root>

			<Card.Root>
				<Card.Content class="pt-4">
					<p class="text-sm text-muted-foreground">Variants</p>
					<p class="text-lg font-semibold">{product.variantMappings.length}</p>
					<p class="text-xs text-muted-foreground">CJ ID: {product.externalProductId}</p>
				</Card.Content>
			</Card.Root>
		</div>

		<!-- Controls -->
		{#if auth.canEdit}
			<Card.Root>
				<Card.Content class="pt-4">
					<div class="flex flex-wrap items-center gap-6">
						<div class="flex items-center gap-2">
							<Label>Excluded from sync</Label>
							<Switch checked={product.excluded} onCheckedChange={handleToggleExclude} />
						</div>
						<div class="flex items-center gap-2">
							<Label>Low stock threshold</Label>
							<Input
								type="number"
								value={product.lowStockThreshold ?? ''}
								onchange={(e) => handleUpdateThreshold((e.target as HTMLInputElement).value)}
								class="h-8 w-20"
								min="0"
							/>
						</div>
					</div>
				</Card.Content>
			</Card.Root>
		{/if}

		<!-- Variant Mappings Table -->
		<div class="space-y-3">
			<h2 class="text-lg font-semibold">Variant Mappings</h2>
			<div class="rounded-lg border shadow-sm overflow-x-auto">
				<Table.Root>
					<Table.Header>
						<Table.Row>
							<Table.Head>Variant</Table.Head>
							<Table.Head>CJ SKU</Table.Head>
							<Table.Head>Warehouse</Table.Head>
							<Table.Head>CJ Price</Table.Head>
							<Table.Head>Shipping</Table.Head>
							<Table.Head>Stock</Table.Head>
							<Table.Head>SKU Status</Table.Head>
							<Table.Head>Last Checked</Table.Head>
						</Table.Row>
					</Table.Header>
					<Table.Body>
						{#each product.variantMappings as mapping}
							<Table.Row class="cursor-pointer hover:bg-muted/50" onclick={() => togglePriceHistory(mapping.id)}>
								<Table.Cell class="font-medium">{mapping.variantName || mapping.variantId}</Table.Cell>
								<Table.Cell>
									<code class="text-xs bg-muted px-1.5 py-0.5 rounded">{mapping.cjSku}</code>
									{#if mapping.previousCjSku}
										<span class="text-xs text-muted-foreground ml-1">(was: {mapping.previousCjSku})</span>
									{/if}
								</Table.Cell>
								<Table.Cell>
									<Badge variant="outline" class="text-xs">
										{warehouseBadge(mapping.warehouseCountry)}
									</Badge>
								</Table.Cell>
								<Table.Cell>
									{mapping.sourcePrice != null ? `$${mapping.sourcePrice.toFixed(2)}` : '—'}
								</Table.Cell>
								<Table.Cell>
									{mapping.shippingEstimate != null ? `$${mapping.shippingEstimate.toFixed(2)}` : '—'}
								</Table.Cell>
								<Table.Cell>
									<span class="font-medium {stockColor(mapping.stockQuantity, product.lowStockThreshold)}">
										{mapping.stockQuantity ?? '—'}
									</span>
								</Table.Cell>
								<Table.Cell>
									<Badge class={skuStatusBadge(mapping.skuStatus)}>{mapping.skuStatus}</Badge>
								</Table.Cell>
								<Table.Cell class="text-xs text-muted-foreground">
									{formatDate(mapping.stockLastCheckedAt)}
								</Table.Cell>
							</Table.Row>
							{#if expandedVariant === mapping.id}
								<Table.Row>
									<Table.Cell colspan={8} class="bg-muted/30 p-4">
										{#if historyLoading}
											<Skeleton class="h-12 w-full" />
										{:else if priceHistoryMap[mapping.id]?.length}
											<div class="flex items-start gap-6">
												<div class="flex-shrink-0">
													<p class="text-xs text-muted-foreground mb-1">Price trend</p>
													<svg width="200" height="40" class="border rounded bg-background">
														<path
															d={miniChartPath(priceHistoryMap[mapping.id])}
															fill="none"
															stroke="currentColor"
															stroke-width="1.5"
															class="text-primary"
														/>
													</svg>
												</div>
												<div class="flex-1">
													<p class="text-xs text-muted-foreground mb-1">Recent changes</p>
													<div class="space-y-1 max-h-32 overflow-auto">
														{#each priceHistoryMap[mapping.id].slice(0, 10) as entry}
															<div class="flex items-center gap-2 text-xs">
																<span class="text-muted-foreground w-32">{formatDate(entry.detectedAt)}</span>
																<span class="text-red-500 line-through">${entry.oldPrice?.toFixed(2) ?? '—'}</span>
																<span>→</span>
																<span class="font-medium">${entry.newPrice.toFixed(2)}</span>
																{#if entry.newMarginPct != null}
																	<span class="text-muted-foreground">({entry.newMarginPct.toFixed(1)}%)</span>
																{/if}
															</div>
														{/each}
													</div>
												</div>
											</div>
										{:else}
											<p class="text-sm text-muted-foreground">No price history yet. Click to close.</p>
										{/if}
									</Table.Cell>
								</Table.Row>
							{/if}
						{/each}
					</Table.Body>
				</Table.Root>
			</div>
		</div>
	</div>
{/if}
