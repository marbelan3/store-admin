<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { auth } from '$lib/stores/auth.svelte';
	import { getDashboardStats, type DashboardStats } from '$lib/api/dashboard';
	import * as Card from '$lib/components/ui/card';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import * as Table from '$lib/components/ui/table';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import PackageIcon from '@lucide/svelte/icons/package';
	import ClockIcon from '@lucide/svelte/icons/clock';
	import CircleCheckIcon from '@lucide/svelte/icons/circle-check';
	import FolderTreeIcon from '@lucide/svelte/icons/folder-tree';
	import FilePenLineIcon from '@lucide/svelte/icons/file-pen-line';
	import TriangleAlertIcon from '@lucide/svelte/icons/triangle-alert';
	import ImageOffIcon from '@lucide/svelte/icons/image-off';
	import FolderMinusIcon from '@lucide/svelte/icons/folder-minus';
	import ArrowRightIcon from '@lucide/svelte/icons/arrow-right';
	import PlusIcon from '@lucide/svelte/icons/plus';
	import ShoppingCartIcon from '@lucide/svelte/icons/shopping-cart';
	import UsersIcon from '@lucide/svelte/icons/users';
	import ZapIcon from '@lucide/svelte/icons/zap';

	let stats = $state<DashboardStats | null>(null);
	let loading = $state(true);
	let error = $state('');

	onMount(async () => {
		try {
			stats = await getDashboardStats();
		} catch (err: any) {
			error = err.message || 'Failed to load dashboard stats';
		} finally {
			loading = false;
		}
	});

</script>

<div class="space-y-6">
	<PageHeader
		title="Dashboard"
		description="Overview of your store performance"
		breadcrumbs={[{ label: 'Home' }]}
	/>

	{#if loading}
		<div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
			{#each Array(4) as _}
				<Card.Root>
					<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
						<Skeleton class="h-4 w-24" />
						<Skeleton class="h-4 w-4 rounded" />
					</Card.Header>
					<Card.Content>
						<Skeleton class="h-8 w-16 mb-1" />
						<Skeleton class="h-3 w-32" />
					</Card.Content>
				</Card.Root>
			{/each}
		</div>
	{:else}
	<div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
		<Card.Root class="shadow-sm hover:shadow-md transition-shadow border-l-4 border-l-indigo-500">
			<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
				<Card.Title class="text-sm font-medium">Total Products</Card.Title>
				<PackageIcon class="h-4 w-4 text-indigo-500" />
			</Card.Header>
			<Card.Content>
				<div class="text-3xl font-extrabold tracking-tight">{stats?.totalProducts ?? 0}</div>
				<p class="text-xs text-muted-foreground mt-1">All products in store</p>
			</Card.Content>
		</Card.Root>

		<Card.Root class="shadow-sm hover:shadow-md transition-shadow border-l-4 border-l-emerald-500">
			<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
				<Card.Title class="text-sm font-medium">Active</Card.Title>
				<CircleCheckIcon class="h-4 w-4 text-emerald-500" />
			</Card.Header>
			<Card.Content>
				<div class="text-3xl font-extrabold tracking-tight">{stats?.activeProducts ?? 0}</div>
				<p class="text-xs text-muted-foreground mt-1">Published in store</p>
			</Card.Content>
		</Card.Root>

		<Card.Root class="shadow-sm hover:shadow-md transition-shadow border-l-4 border-l-violet-500">
			<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
				<Card.Title class="text-sm font-medium">Categories</Card.Title>
				<FolderTreeIcon class="h-4 w-4 text-violet-500" />
			</Card.Header>
			<Card.Content>
				<div class="text-3xl font-extrabold tracking-tight">{stats?.totalCategories ?? 0}</div>
				<p class="text-xs text-muted-foreground mt-1">Product categories</p>
			</Card.Content>
		</Card.Root>

		<Card.Root class="shadow-sm hover:shadow-md transition-shadow border-l-4 border-l-amber-500">
			<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
				<Card.Title class="text-sm font-medium">Draft</Card.Title>
				<FilePenLineIcon class="h-4 w-4 text-amber-500" />
			</Card.Header>
			<Card.Content>
				<div class="text-3xl font-extrabold tracking-tight">{stats?.draftProducts ?? 0}</div>
				<p class="text-xs text-muted-foreground mt-1">Awaiting publish</p>
			</Card.Content>
		</Card.Root>
	</div>
	{/if}

	{#if error}
		<Card.Root class="border-destructive">
			<Card.Content class="pt-6">
				<p class="text-sm text-destructive">{error}</p>
			</Card.Content>
		</Card.Root>
	{/if}

	{#if stats}
		<!-- Quick Actions -->
		<Card.Root class="shadow-sm">
			<Card.Header>
				<div class="flex items-center gap-2">
					<ZapIcon class="h-4 w-4 text-amber-500" />
					<Card.Title>Quick Actions</Card.Title>
				</div>
			</Card.Header>
			<Card.Content>
				<div class="flex flex-wrap gap-3">
					<Button variant="outline" size="sm" onclick={() => goto('/products/new')}>
						<PlusIcon class="h-4 w-4 mr-1.5" />
						Add Product
					</Button>
					<Button variant="outline" size="sm" onclick={() => goto('/orders/new')}>
						<ShoppingCartIcon class="h-4 w-4 mr-1.5" />
						Create Order
					</Button>
					<Button variant="outline" size="sm" onclick={() => goto('/customers/new')}>
						<UsersIcon class="h-4 w-4 mr-1.5" />
						Add Customer
					</Button>
				</div>
			</Card.Content>
		</Card.Root>

		<!-- Alerts Row -->
		<div class="grid gap-4 md:grid-cols-3">
			<a href="/inventory" class="group block rounded-xl no-underline">
				<Card.Root class="shadow-sm hover:shadow-md transition-shadow cursor-pointer {stats.lowStockCount > 0 ? 'border-yellow-500' : ''}">
					<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
						<Card.Title class="text-sm font-medium">Low Stock</Card.Title>
						<TriangleAlertIcon class="h-4 w-4 text-yellow-500" />
					</Card.Header>
					<Card.Content>
						<div class="text-2xl font-bold">{stats.lowStockCount}</div>
						<div class="flex items-center justify-between">
							<p class="text-xs text-muted-foreground">Products below threshold</p>
							<span class="text-xs text-muted-foreground flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
								View <ArrowRightIcon class="h-3 w-3" />
							</span>
						</div>
					</Card.Content>
				</Card.Root>
			</a>

			<a href="/products" class="group block rounded-xl no-underline">
				<Card.Root class="shadow-sm hover:shadow-md transition-shadow cursor-pointer {stats.noImagesCount > 0 ? 'border-orange-500' : ''}">
					<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
						<Card.Title class="text-sm font-medium">No Images</Card.Title>
						<ImageOffIcon class="h-4 w-4 text-orange-500" />
					</Card.Header>
					<Card.Content>
						<div class="text-2xl font-bold">{stats.noImagesCount}</div>
						<div class="flex items-center justify-between">
							<p class="text-xs text-muted-foreground">Products without media</p>
							<span class="text-xs text-muted-foreground flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
								View <ArrowRightIcon class="h-3 w-3" />
							</span>
						</div>
					</Card.Content>
				</Card.Root>
			</a>

			<a href="/products" class="group block rounded-xl no-underline">
				<Card.Root class="shadow-sm hover:shadow-md transition-shadow cursor-pointer {stats.uncategorizedCount > 0 ? 'border-orange-500' : ''}">
					<Card.Header class="flex flex-row items-center justify-between space-y-0 pb-2">
						<Card.Title class="text-sm font-medium">Uncategorized</Card.Title>
						<FolderMinusIcon class="h-4 w-4 text-orange-500" />
					</Card.Header>
					<Card.Content>
						<div class="text-2xl font-bold">{stats.uncategorizedCount}</div>
						<div class="flex items-center justify-between">
							<p class="text-xs text-muted-foreground">Products without categories</p>
							<span class="text-xs text-muted-foreground flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
								View <ArrowRightIcon class="h-3 w-3" />
							</span>
						</div>
					</Card.Content>
				</Card.Root>
			</a>
		</div>

		<div class="grid gap-4 md:grid-cols-2">
			<Card.Root>
				<Card.Header>
					<Card.Title>Products by Status</Card.Title>
				</Card.Header>
				<Card.Content>
					<div class="space-y-3">
						{#each Object.entries(stats.productsByStatus) as [status, count]}
							<div class="flex items-center justify-between">
								<span class="text-sm font-medium">{status}</span>
								<div class="flex items-center gap-2">
									<div class="h-2 rounded-full bg-primary" style="width: {stats.totalProducts > 0 ? Math.max((count / stats.totalProducts) * 200, 4) : 4}px"></div>
									<span class="text-sm text-muted-foreground">{count}</span>
								</div>
							</div>
						{/each}
					</div>
				</Card.Content>
			</Card.Root>

			<Card.Root>
				<Card.Header>
					<Card.Title>Overview</Card.Title>
				</Card.Header>
				<Card.Content>
					<div class="space-y-3">
						<div class="flex items-center justify-between">
							<span class="text-sm">Tags</span>
							<span class="text-sm font-medium">{stats.totalTags}</span>
						</div>
						<div class="flex items-center justify-between">
							<span class="text-sm">Team Members</span>
							<span class="text-sm font-medium">{stats.totalUsers}</span>
						</div>
						<div class="flex items-center justify-between">
							<span class="text-sm">Archived Products</span>
							<span class="text-sm font-medium">{stats.archivedProducts}</span>
						</div>
					</div>
				</Card.Content>
			</Card.Root>
		</div>

		<!-- Recently Updated -->
		<Card.Root class="shadow-sm">
			<Card.Header>
				<Card.Title>Recently Updated Products</Card.Title>
			</Card.Header>
			<Card.Content>
				{#if stats.recentlyUpdated && stats.recentlyUpdated.length > 0}
					<div class="rounded-md border overflow-x-auto">
						<Table.Root>
							<Table.Header>
								<Table.Row>
									<Table.Head>Product</Table.Head>
									<Table.Head>Last Updated</Table.Head>
								</Table.Row>
							</Table.Header>
							<Table.Body>
								{#each stats.recentlyUpdated as product}
									<Table.Row class="cursor-pointer hover:bg-muted/50 transition-colors" onclick={() => goto(`/products/${product.id}`)}>
										<Table.Cell class="font-medium">{product.name}</Table.Cell>
										<Table.Cell class="text-sm text-muted-foreground">
											{new Date(product.updatedAt).toLocaleString()}
										</Table.Cell>
									</Table.Row>
								{/each}
							</Table.Body>
						</Table.Root>
					</div>
				{:else}
					<EmptyState
						icon={ClockIcon}
						title="No recent activity"
						description="Product updates will appear here as you make changes."
					/>
				{/if}
			</Card.Content>
		</Card.Root>
	{/if}
</div>
