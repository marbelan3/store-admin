<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { auth } from '$lib/stores/auth.svelte';
	import {
		getProducts,
		deleteProduct,
		patchProduct,
		bulkDeleteProducts,
		bulkUpdateProductStatus
	} from '$lib/api/products';
	import { getFlatCategories } from '$lib/api/categories';
	import { exportProductsCsv, importProductsCsv } from '$lib/api/csv';
	import type { ProductListItem, Page } from '$lib/types/product';
	import type { Category } from '$lib/types/category';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Badge } from '$lib/components/ui/badge';
	import * as Table from '$lib/components/ui/table';
	import * as Select from '$lib/components/ui/select';
	import * as DropdownMenu from '$lib/components/ui/dropdown-menu';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import BulkActionBar from '$lib/components/BulkActionBar.svelte';
	import { getMediaUrl } from '$lib/api/media';
	import InlineEditCell from '$lib/components/InlineEditCell.svelte';
	import InlineStatusSelect from '$lib/components/InlineStatusSelect.svelte';
	import SavedViews from '$lib/components/SavedViews.svelte';
	import PackageIcon from '@lucide/svelte/icons/package';
	import GlobeIcon from '@lucide/svelte/icons/globe';
	import SearchIcon from '@lucide/svelte/icons/search';
	import ImageIcon from '@lucide/svelte/icons/image';
	import Trash2Icon from '@lucide/svelte/icons/trash-2';
	import RefreshCwIcon from '@lucide/svelte/icons/refresh-cw';
	import DownloadIcon from '@lucide/svelte/icons/download';
	import ChevronDownIcon from '@lucide/svelte/icons/chevron-down';

	const STATUS_OPTIONS = [
		{ value: 'ACTIVE', label: 'ACTIVE' },
		{ value: 'DRAFT', label: 'DRAFT' },
		{ value: 'ARCHIVED', label: 'ARCHIVED' }
	];

	const PAGE_SIZE = 20;

	let products = $state<ProductListItem[]>([]);
	let totalElements = $state(0);
	let totalPages = $state(0);
	let currentPage = $state(0);
	let isFirst = $state(true);
	let isLast = $state(true);
	let loading = $state(true);
	let statusFilter = $state<string>('');
	let searchQuery = $state('');
	let categoryFilter = $state('');
	let sourceFilter = $state('');
	let sortField = $state('');
	let sortDir = $state<'asc' | 'desc'>('asc');
	let searchTimeout: ReturnType<typeof setTimeout>;

	let categories = $state<Category[]>([]);
	let exporting = $state(false);
	let importInput = $state<HTMLInputElement>();

	// Bulk selection
	let selectedIds = $state(new Set<string>());
	let bulkLoading = $state(false);

	let allOnPageSelected = $derived(
		products.length > 0 && products.every((p) => selectedIds.has(p.id))
	);

	function toggleSelectAll() {
		if (allOnPageSelected) {
			const next = new Set(selectedIds);
			for (const p of products) next.delete(p.id);
			selectedIds = next;
		} else {
			const next = new Set(selectedIds);
			for (const p of products) next.add(p.id);
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

	// Confirm dialog state
	let confirmOpen = $state(false);
	let confirmTitle = $state('');
	let confirmDescription = $state('');
	let confirmLabel = $state('Delete');
	let confirmAction = $state<() => Promise<void>>(async () => {});

	function readUrlParams() {
		const params = $page.url.searchParams;
		const urlPage = params.get('page');
		currentPage = urlPage ? Math.max(0, Number(urlPage) - 1) : 0;
		statusFilter = params.get('status') || '';
		searchQuery = params.get('search') || '';
		categoryFilter = params.get('categoryId') || '';
		sourceFilter = params.get('source') || '';
		const sort = params.get('sort') || '';
		if (sort) {
			const parts = sort.split(',');
			sortField = parts[0];
			sortDir = (parts[1] as 'asc' | 'desc') || 'asc';
		}
	}

	function updateUrl() {
		const params = new URLSearchParams();
		if (currentPage > 0) params.set('page', String(currentPage + 1));
		if (statusFilter) params.set('status', statusFilter);
		if (searchQuery) params.set('search', searchQuery);
		if (categoryFilter) params.set('categoryId', categoryFilter);
		if (sourceFilter) params.set('source', sourceFilter);
		if (sortField) params.set('sort', `${sortField},${sortDir}`);
		const qs = params.toString();
		goto(`/products${qs ? `?${qs}` : ''}`, { replaceState: true, keepFocus: true, noScroll: true });
	}

	async function loadProducts() {
		loading = true;
		try {
			const result = await getProducts({
				page: currentPage,
				size: PAGE_SIZE,
				status: statusFilter || undefined,
				search: searchQuery || undefined,
				categoryId: categoryFilter || undefined,
				source: sourceFilter || undefined,
				sort: sortField ? `${sortField},${sortDir}` : undefined
			});
			products = result.content;
			totalElements = result.totalElements;
			totalPages = result.totalPages;
			isFirst = result.first;
			isLast = result.last;
		} catch (err) {
			toast.error('Failed to load products');
		} finally {
			loading = false;
		}
	}

	function setFilter(status: string) {
		statusFilter = status;
		currentPage = 0;
		updateUrl();
		loadProducts();
	}

	function handleSearchInput(e: Event) {
		const value = (e.target as HTMLInputElement).value;
		searchQuery = value;
		clearTimeout(searchTimeout);
		searchTimeout = setTimeout(() => {
			currentPage = 0;
			updateUrl();
			loadProducts();
		}, 300);
	}

	function handleCategoryChange(value: string) {
		categoryFilter = value === '__all__' ? '' : value;
		currentPage = 0;
		updateUrl();
		loadProducts();
	}

	function toggleSort(field: string) {
		if (sortField === field) {
			sortDir = sortDir === 'asc' ? 'desc' : 'asc';
		} else {
			sortField = field;
			sortDir = 'asc';
		}
		currentPage = 0;
		updateUrl();
		loadProducts();
	}

	function sortIndicator(field: string): string {
		if (sortField !== field) return '';
		return sortDir === 'asc' ? ' \u2191' : ' \u2193';
	}

	function goToPage(p: number) {
		currentPage = p;
		updateUrl();
		loadProducts();
	}

	function handleDelete(id: string, name: string) {
		confirmTitle = 'Delete product';
		confirmDescription = `Are you sure you want to delete "${name}"? This action cannot be undone.`;
		confirmLabel = 'Delete';
		confirmAction = async () => {
			try {
				await deleteProduct(id);
				toast.success('Product deleted');
				await loadProducts();
			} catch {
				toast.error('Failed to delete product');
			}
		};
		confirmOpen = true;
	}

	function handleBulkDelete() {
		const count = selectedIds.size;
		confirmTitle = 'Delete selected products';
		confirmDescription = `Are you sure you want to delete ${count} product${count > 1 ? 's' : ''}? This action cannot be undone.`;
		confirmLabel = 'Delete All';
		confirmAction = async () => {
			bulkLoading = true;
			try {
				const result = await bulkDeleteProducts([...selectedIds]);
				toast.success(`${result.affected} product${result.affected > 1 ? 's' : ''} deleted`);
				clearSelection();
				await loadProducts();
			} catch {
				toast.error('Failed to delete products');
			} finally {
				bulkLoading = false;
			}
		};
		confirmOpen = true;
	}

	async function handleBulkStatusChange(status: string) {
		bulkLoading = true;
		try {
			const result = await bulkUpdateProductStatus([...selectedIds], status);
			toast.success(`${result.affected} product${result.affected > 1 ? 's' : ''} updated to ${status}`);
			clearSelection();
			await loadProducts();
		} catch {
			toast.error('Failed to update product status');
		} finally {
			bulkLoading = false;
		}
	}

	function handleBulkExport() {
		const selected = products.filter((p) => selectedIds.has(p.id));
		if (selected.length === 0) return;

		const headers = ['ID', 'Name', 'SKU', 'Status', 'Price', 'Currency', 'Quantity', 'Created'];
		const rows = selected.map((p) => [
			p.id,
			`"${(p.name || '').replace(/"/g, '""')}"`,
			p.sku || '',
			p.status,
			p.price != null ? String(p.price) : '',
			p.currency || '',
			p.trackInventory ? String(p.quantity) : '',
			p.createdAt ? new Date(p.createdAt).toISOString() : ''
		]);

		const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
		const blob = new Blob([csv], { type: 'text/csv' });
		const url = URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = `products-selected-${new Date().toISOString().slice(0, 10)}.csv`;
		a.click();
		URL.revokeObjectURL(url);
		toast.success(`Exported ${selected.length} product${selected.length > 1 ? 's' : ''}`);
	}

	async function handleInlinePriceUpdate(product: ProductListItem, newValue: string | number) {
		const oldPrice = product.price;
		const newPrice = typeof newValue === 'string' ? parseFloat(newValue) : newValue;
		// Optimistic update
		product.price = newPrice;
		products = [...products];
		try {
			await patchProduct(product.id, { price: newPrice });
		} catch (err) {
			// Rollback on failure
			product.price = oldPrice;
			products = [...products];
			throw err;
		}
	}

	async function handleInlineStatusUpdate(product: ProductListItem, newStatus: string) {
		const oldStatus = product.status;
		// Optimistic update
		product.status = newStatus as ProductListItem['status'];
		products = [...products];
		try {
			await patchProduct(product.id, { status: newStatus as ProductListItem['status'] });
		} catch (err) {
			// Rollback on failure
			product.status = oldStatus;
			products = [...products];
			throw err;
		}
	}

	function statusVariant(status: string): 'default' | 'secondary' | 'outline' | 'destructive' {
		switch (status) {
			case 'ACTIVE': return 'default';
			case 'DRAFT': return 'secondary';
			case 'ARCHIVED': return 'outline';
			default: return 'default';
		}
	}

	function statusBadgeClass(status: string): string {
		switch (status) {
			case 'ACTIVE': return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800';
			case 'DRAFT': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400 border-amber-200 dark:border-amber-800';
			case 'ARCHIVED': return 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400 border-gray-200 dark:border-gray-700';
			default: return '';
		}
	}

	async function handleExport() {
		exporting = true;
		try {
			await exportProductsCsv();
			toast.success('Products exported');
		} catch {
			toast.error('Export failed');
		} finally {
			exporting = false;
		}
	}

	async function handleImport(e: Event) {
		const file = (e.target as HTMLInputElement).files?.[0];
		if (!file) return;
		try {
			const result = await importProductsCsv(file);
			const msg = `Created: ${result.created}, Updated: ${result.updated}`;
			if (result.errors.length > 0) {
				toast.warning(`${msg}. ${result.errors.length} errors.`);
			} else {
				toast.success(msg);
			}
			await loadProducts();
		} catch (err: any) {
			toast.error(err.message || 'Import failed');
		} finally {
			if (importInput) importInput.value = '';
		}
	}

	let currentProductFilters = $derived({
		search: searchQuery,
		status: statusFilter,
		categoryId: categoryFilter,
		source: sourceFilter
	});

	function applyProductView(filters: Record<string, any>) {
		searchQuery = filters.search || '';
		statusFilter = filters.status || '';
		categoryFilter = filters.categoryId || '';
		sourceFilter = filters.source || '';
		currentPage = 0;
		updateUrl();
		loadProducts();
	}

	onMount(async () => {
		readUrlParams();
		try {
			categories = await getFlatCategories();
		} catch {
			// non-critical
		}
		loadProducts();
	});
</script>

<ConfirmDialog
	bind:open={confirmOpen}
	title={confirmTitle}
	description={confirmDescription}
	confirmLabel={confirmLabel}
	variant="destructive"
	onConfirm={confirmAction}
/>

<div class="space-y-6">
	<PageHeader
		title="Products"
		description="Manage your product catalog"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Products' }]}
	>
		{#if auth.canEdit}
			<input bind:this={importInput} type="file" accept=".csv" class="hidden" onchange={handleImport} />
			<Button variant="outline" onclick={() => importInput?.click()}>Import CSV</Button>
		{/if}
		<Button variant="outline" onclick={handleExport} disabled={exporting}>
			{exporting ? 'Exporting...' : 'Export CSV'}
		</Button>
		{#if auth.canEdit}
			<Button onclick={() => goto('/products/new')}>Add Product</Button>
		{/if}
	</PageHeader>

	<!-- Saved Views -->
	<SavedViews
		storageKey="products-views"
		currentFilters={currentProductFilters}
		onApplyView={applyProductView}
	/>

	<!-- Search and Filters -->
	<div class="flex flex-wrap items-center gap-3">
		<div class="relative max-w-xs">
			<SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
			<Input
				placeholder="Search products..."
				value={searchQuery}
				oninput={handleSearchInput}
				class="pl-9 shadow-sm"
			/>
		</div>
		{#if categories.length > 0}
			<Select.Root
				type="single"
				value={categoryFilter || '__all__'}
				onValueChange={handleCategoryChange}
			>
				<Select.Trigger class="w-[180px]">
					{categoryFilter
						? categories.find((c) => c.id === categoryFilter)?.name || 'Category'
						: 'All Categories'}
				</Select.Trigger>
				<Select.Content>
					<Select.Item value="__all__">All Categories</Select.Item>
					{#each categories as cat}
						<Select.Item value={cat.id}>{cat.name}</Select.Item>
					{/each}
				</Select.Content>
			</Select.Root>
		{/if}
		<Select.Root
			type="single"
			value={sourceFilter || '__all__'}
			onValueChange={(v) => { sourceFilter = v === '__all__' ? '' : v; currentPage = 0; updateUrl(); loadProducts(); }}
		>
			<Select.Trigger class="w-[160px]">
				{sourceFilter === 'OWN' ? 'Own Products' : sourceFilter === 'MARKETPLACE' ? 'Marketplace' : 'All Sources'}
			</Select.Trigger>
			<Select.Content>
				<Select.Item value="__all__">All Sources</Select.Item>
				<Select.Item value="OWN">Own Products</Select.Item>
				<Select.Item value="MARKETPLACE">Marketplace</Select.Item>
			</Select.Content>
		</Select.Root>
	</div>

	<div class="flex gap-2">
		<Button
			variant={statusFilter === '' ? 'default' : 'outline'}
			size="sm"
			class="rounded-full {statusFilter === '' ? 'bg-primary text-primary-foreground' : ''}"
			onclick={() => setFilter('')}
		>All</Button>
		<Button
			variant={statusFilter === 'ACTIVE' ? 'default' : 'outline'}
			size="sm"
			class="rounded-full {statusFilter === 'ACTIVE' ? 'bg-primary text-primary-foreground' : ''}"
			onclick={() => setFilter('ACTIVE')}
		>Active</Button>
		<Button
			variant={statusFilter === 'DRAFT' ? 'default' : 'outline'}
			size="sm"
			class="rounded-full {statusFilter === 'DRAFT' ? 'bg-primary text-primary-foreground' : ''}"
			onclick={() => setFilter('DRAFT')}
		>Draft</Button>
		<Button
			variant={statusFilter === 'ARCHIVED' ? 'default' : 'outline'}
			size="sm"
			class="rounded-full {statusFilter === 'ARCHIVED' ? 'bg-primary text-primary-foreground' : ''}"
			onclick={() => setFilter('ARCHIVED')}
		>Archived</Button>
	</div>

	{#if loading}
		<div class="rounded-md border overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head class="w-[60px]"></Table.Head>
						<Table.Head>Name</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head>Price</Table.Head>
						<Table.Head>SKU</Table.Head>
						<Table.Head>Inventory</Table.Head>
						<Table.Head>Created</Table.Head>
						<Table.Head class="w-[100px]">Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each Array(6) as _}
						<Table.Row>
							<Table.Cell><Skeleton class="h-10 w-10 rounded-md" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-32" /></Table.Cell>
							<Table.Cell><Skeleton class="h-5 w-16 rounded-full" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-10" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-7 w-14" /></Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{:else if products.length === 0}
		<div class="rounded-lg border border-dashed shadow-sm">
			<EmptyState
				icon={PackageIcon}
				title="No products yet"
				description={searchQuery ? 'Try a different search term.' : 'Add your first product to start building your catalog.'}
				actionLabel={auth.canEdit && !searchQuery ? 'Add Product' : undefined}
				actionHref={auth.canEdit && !searchQuery ? '/products/new' : undefined}
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
									aria-label="Select all products on this page"
								/>
							</Table.Head>
						{/if}
						<Table.Head class="w-[60px]"></Table.Head>
						<Table.Head>
							<button
								class="flex items-center gap-1 font-medium hover:text-foreground transition-colors"
								onclick={() => toggleSort('name')}
							>
								Name{sortIndicator('name')}
							</button>
						</Table.Head>
						<Table.Head>
							<button
								class="flex items-center gap-1 font-medium hover:text-foreground transition-colors"
								onclick={() => toggleSort('status')}
							>
								Status{sortIndicator('status')}
							</button>
						</Table.Head>
						<Table.Head>
							<button
								class="flex items-center gap-1 font-medium hover:text-foreground transition-colors"
								onclick={() => toggleSort('price')}
							>
								Price{sortIndicator('price')}
							</button>
						</Table.Head>
						<Table.Head>SKU</Table.Head>
						<Table.Head>Inventory</Table.Head>
						<Table.Head>
							<button
								class="flex items-center gap-1 font-medium hover:text-foreground transition-colors"
								onclick={() => toggleSort('createdAt')}
							>
								Created{sortIndicator('createdAt')}
							</button>
						</Table.Head>
						<Table.Head class="w-[100px]">Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each products as product}
						<Table.Row class="cursor-pointer hover:bg-muted/50 transition-colors {selectedIds.has(product.id) ? 'bg-muted/40' : ''}" onclick={() => goto(`/products/${product.id}`)}>
							{#if auth.canEdit}
								<Table.Cell>
									<input
										type="checkbox"
										checked={selectedIds.has(product.id)}
										onchange={() => toggleSelect(product.id)}
										onclick={(e: MouseEvent) => e.stopPropagation()}
										class="h-4 w-4 rounded border-gray-300 accent-primary cursor-pointer"
										aria-label="Select {product.name}"
									/>
								</Table.Cell>
							{/if}
							<Table.Cell>
								{#if product.primaryImageUrl}
									<div class="h-10 w-10 overflow-hidden rounded-md border bg-muted">
										<img
											src={getMediaUrl(product.primaryImageUrl)}
											alt={product.name}
											class="h-full w-full object-cover"
											onerror={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
										/>
									</div>
								{:else}
									<div class="flex h-10 w-10 items-center justify-center rounded-md border bg-muted">
										<ImageIcon class="h-4 w-4 text-muted-foreground/50" />
									</div>
								{/if}
							</Table.Cell>
							<Table.Cell class="font-medium">
								<div class="flex items-center gap-1.5">
									{product.name}
									{#if product.source === 'MARKETPLACE'}
										<Badge variant="outline" class="text-[10px] px-1.5 py-0 bg-blue-50 text-blue-600 border-blue-200 dark:bg-blue-900/20 dark:text-blue-400 dark:border-blue-800">
											<GlobeIcon class="h-2.5 w-2.5 mr-0.5" />CJ
										</Badge>
									{/if}
								</div>
							</Table.Cell>
							<Table.Cell>
								{#if auth.canEdit}
									<InlineStatusSelect
										value={product.status}
										options={STATUS_OPTIONS}
										onSave={(newStatus) => handleInlineStatusUpdate(product, newStatus)}
										badgeClass={statusBadgeClass}
									/>
								{:else}
									<Badge variant="outline" class={statusBadgeClass(product.status)}>{product.status}</Badge>
								{/if}
							</Table.Cell>
							<Table.Cell class="min-w-[120px]">
								{#if auth.canEdit && product.price != null}
									<InlineEditCell
										value={product.price}
										type="currency"
										suffix={product.currency}
										onSave={(newValue) => handleInlinePriceUpdate(product, newValue)}
										min={0}
									/>
								{:else}
									{product.price != null ? `${product.price} ${product.currency}` : '—'}
								{/if}
							</Table.Cell>
							<Table.Cell>{product.sku || '—'}</Table.Cell>
							<Table.Cell>{product.trackInventory ? product.quantity : '—'}</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{new Date(product.createdAt).toLocaleDateString()}
							</Table.Cell>
							<Table.Cell>
								{#if auth.canEdit}
									<Button
										variant="ghost"
										size="sm"
										onclick={(e: MouseEvent) => { e.stopPropagation(); handleDelete(product.id, product.name); }}
									>Delete</Button>
								{/if}
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		<!-- Pagination Controls -->
		{#if totalPages > 1}
			<div class="flex items-center justify-between">
				<p class="text-sm text-muted-foreground">
					Showing {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, totalElements)} of {totalElements} products
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
			<Button
				variant="ghost"
				size="sm"
				class="h-7 text-red-400 hover:text-red-300 hover:bg-red-950/40"
				onclick={handleBulkDelete}
				disabled={bulkLoading}
			>
				<Trash2Icon class="h-4 w-4 mr-1" />
				Delete
			</Button>

			<DropdownMenu.Root>
				<DropdownMenu.Trigger>
					<Button
						variant="ghost"
						size="sm"
						class="h-7 text-zinc-300 hover:text-white hover:bg-zinc-700"
						disabled={bulkLoading}
					>
						<RefreshCwIcon class="h-4 w-4 mr-1" />
						Status
						<ChevronDownIcon class="h-3 w-3 ml-1" />
					</Button>
				</DropdownMenu.Trigger>
				<DropdownMenu.Content>
					<DropdownMenu.Item onclick={() => handleBulkStatusChange('ACTIVE')}>
						<Badge variant="outline" class="mr-2 {statusBadgeClass('ACTIVE')}">ACTIVE</Badge>
						Set Active
					</DropdownMenu.Item>
					<DropdownMenu.Item onclick={() => handleBulkStatusChange('DRAFT')}>
						<Badge variant="outline" class="mr-2 {statusBadgeClass('DRAFT')}">DRAFT</Badge>
						Set Draft
					</DropdownMenu.Item>
					<DropdownMenu.Item onclick={() => handleBulkStatusChange('ARCHIVED')}>
						<Badge variant="outline" class="mr-2 {statusBadgeClass('ARCHIVED')}">ARCHIVED</Badge>
						Set Archived
					</DropdownMenu.Item>
				</DropdownMenu.Content>
			</DropdownMenu.Root>

			<Button
				variant="ghost"
				size="sm"
				class="h-7 text-zinc-300 hover:text-white hover:bg-zinc-700"
				onclick={handleBulkExport}
				disabled={bulkLoading}
			>
				<DownloadIcon class="h-4 w-4 mr-1" />
				Export
			</Button>
		{/snippet}
	</BulkActionBar>
{/if}
