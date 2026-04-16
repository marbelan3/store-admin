<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { getAlerts } from '$lib/api/marketplace';
	import type { MarketplaceAlert } from '$lib/types/marketplace';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Card from '$lib/components/ui/card';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import AlertTriangleIcon from '@lucide/svelte/icons/triangle-alert';

	let alerts = $state<MarketplaceAlert[]>([]);
	let loading = $state(true);

	let marginCount = $derived(alerts.filter(a => a.alertType === 'MARGIN_VIOLATION').length);
	let skuCount = $derived(alerts.filter(a => a.alertType === 'SKU_CHANGED').length);
	let oosCount = $derived(alerts.filter(a => a.alertType === 'OUT_OF_STOCK' || a.alertType === 'LOW_STOCK').length);
	let delistedCount = $derived(alerts.filter(a => a.alertType === 'DELISTED').length);

	async function loadAlerts() {
		loading = true;
		try {
			alerts = await getAlerts();
		} catch {
			toast.error('Failed to load alerts');
		} finally {
			loading = false;
		}
	}

	function alertTypeBadge(type: string): { class: string; label: string } {
		switch (type) {
			case 'MARGIN_VIOLATION':
				return { class: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400', label: 'Margin' };
			case 'SKU_CHANGED':
				return { class: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400', label: 'SKU Changed' };
			case 'OUT_OF_STOCK':
				return { class: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400', label: 'Out of Stock' };
			case 'DELISTED':
				return { class: 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400', label: 'Delisted' };
			case 'LOW_STOCK':
				return { class: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400', label: 'Low Stock' };
			default:
				return { class: 'bg-blue-100 text-blue-600', label: type };
		}
	}

	onMount(loadAlerts);
</script>

<div class="space-y-6">
	<PageHeader
		title="Marketplace Alerts"
		description="Margin violations, SKU changes, and stock events"
		breadcrumbs={[
			{ label: 'Home', href: '/dashboard' },
			{ label: 'Marketplace', href: '/marketplace/connections' },
			{ label: 'Alerts' }
		]}
	>
		<Button variant="outline" onclick={loadAlerts}>Refresh</Button>
	</PageHeader>

	{#if loading}
		<div class="space-y-3">
			{#each Array(5) as _}
				<Skeleton class="h-20 w-full rounded-lg" />
			{/each}
		</div>
	{:else if alerts.length === 0}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={AlertTriangleIcon}
				title="No alerts"
				description="All marketplace products are healthy. Alerts will appear when margins drop, SKUs change, or products go out of stock."
			/>
		</div>
	{:else}
		<!-- Summary counts -->
		<div class="flex flex-wrap gap-3">
			{#if marginCount > 0}
				<Badge class="bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 text-sm px-3 py-1">
					{marginCount} Margin Alert{marginCount !== 1 ? 's' : ''}
				</Badge>
			{/if}
			{#if skuCount > 0}
				<Badge class="bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400 text-sm px-3 py-1">
					{skuCount} SKU Change{skuCount !== 1 ? 's' : ''}
				</Badge>
			{/if}
			{#if oosCount > 0}
				<Badge class="bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400 text-sm px-3 py-1">
					{oosCount} Stock Alert{oosCount !== 1 ? 's' : ''}
				</Badge>
			{/if}
			{#if delistedCount > 0}
				<Badge class="bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400 text-sm px-3 py-1">
					{delistedCount} Delisted
				</Badge>
			{/if}
		</div>

		<div class="space-y-3">
			{#each alerts as alert}
				{@const badge = alertTypeBadge(alert.alertType)}
				<Card.Root class="hover:shadow-md transition-shadow">
					<Card.Content class="pt-4">
						<div class="flex items-start justify-between gap-4">
							<div class="flex-1 space-y-1">
								<div class="flex items-center gap-2">
									<Badge class={badge.class}>{badge.label}</Badge>
									<span class="font-medium">{alert.productName}</span>
								</div>
								<p class="text-sm text-muted-foreground">{alert.message}</p>
								{#if alert.currentMarginPct != null}
									<p class="text-xs">
										Current margin: <span class="font-medium {alert.currentMarginPct < (alert.minMarginPct ?? 0) ? 'text-red-600' : 'text-green-600'}">{alert.currentMarginPct.toFixed(1)}%</span>
										{#if alert.minMarginPct != null}
											<span class="text-muted-foreground"> (min: {alert.minMarginPct}%)</span>
										{/if}
									</p>
								{/if}
							</div>
							<Button
								variant="outline"
								size="sm"
								onclick={() => goto(`/marketplace/products/${alert.marketplaceProductId}`)}
							>
								View
							</Button>
						</div>
					</Card.Content>
				</Card.Root>
			{/each}
		</div>
	{/if}
</div>
