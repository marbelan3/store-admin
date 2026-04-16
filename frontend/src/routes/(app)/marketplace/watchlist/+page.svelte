<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/stores/auth.svelte';
	import { getWatchlist, removeFromWatchlist } from '$lib/api/marketplace';
	import type { WatchlistItem } from '$lib/types/marketplace';
	import type { Page } from '$lib/types/product';
	import { Button } from '$lib/components/ui/button';
	import { Badge } from '$lib/components/ui/badge';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Card from '$lib/components/ui/card';
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

	onMount(() => loadWatchlist());
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
								onclick={goToImport}
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
