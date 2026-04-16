<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/stores/auth.svelte';
	import {
		getDiscounts,
		getDiscount,
		deleteDiscount,
		toggleDiscount,
		type DiscountListItem,
		type Discount
	} from '$lib/api/discounts';
	import { Button } from '$lib/components/ui/button';
	import { Badge } from '$lib/components/ui/badge';
	import { Switch } from '$lib/components/ui/switch';
	import * as Table from '$lib/components/ui/table';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import DiscountFormDialog from '$lib/components/DiscountFormDialog.svelte';

	import TagIcon from '@lucide/svelte/icons/tag';
	import PencilIcon from '@lucide/svelte/icons/pencil';
	import TrashIcon from '@lucide/svelte/icons/trash-2';
	import PlusIcon from '@lucide/svelte/icons/plus';

	const PAGE_SIZE = 20;

	let discounts = $state<DiscountListItem[]>([]);
	let totalElements = $state(0);
	let totalPages = $state(0);
	let currentPage = $state(0);
	let isFirst = $state(true);
	let isLast = $state(true);
	let loading = $state(true);

	// Dialog state
	let formDialogOpen = $state(false);
	let editingDiscount = $state<Discount | null>(null);
	let confirmDeleteOpen = $state(false);
	let deletingId = $state<number | null>(null);
	let togglingIds = $state<Set<number>>(new Set());

	function getStatusInfo(d: DiscountListItem): { label: string; class: string } {
		if (!d.active) {
			return {
				label: 'Disabled',
				class: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 border-red-200 dark:border-red-800'
			};
		}

		const now = new Date();

		if (d.startsAt && new Date(d.startsAt) > now) {
			return {
				label: 'Scheduled',
				class: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400 border-blue-200 dark:border-blue-800'
			};
		}

		if (d.endsAt && new Date(d.endsAt) < now) {
			return {
				label: 'Expired',
				class: 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400 border-gray-200 dark:border-gray-700'
			};
		}

		return {
			label: 'Active',
			class: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800'
		};
	}

	function formatValue(d: DiscountListItem): string {
		if (d.type === 'PERCENTAGE') {
			return `${d.value}%`;
		}
		return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(d.value);
	}

	function formatUsage(d: DiscountListItem): string {
		if (d.usageLimit) {
			return `${d.usageCount} / ${d.usageLimit}`;
		}
		return `${d.usageCount}`;
	}

	function formatDate(iso: string | null): string {
		if (!iso) return '-';
		return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
	}

	async function loadDiscounts() {
		loading = true;
		try {
			const result = await getDiscounts({
				page: currentPage,
				size: PAGE_SIZE,
				sort: 'createdAt,desc'
			});
			discounts = result.content;
			totalElements = result.totalElements;
			totalPages = result.totalPages;
			isFirst = result.first;
			isLast = result.last;
		} catch {
			toast.error('Failed to load discounts');
		} finally {
			loading = false;
		}
	}

	function goToPage(p: number) {
		currentPage = p;
		loadDiscounts();
	}

	function openCreateDialog() {
		editingDiscount = null;
		formDialogOpen = true;
	}

	async function openEditDialog(id: number) {
		try {
			editingDiscount = await getDiscount(id);
			formDialogOpen = true;
		} catch {
			toast.error('Failed to load discount details');
		}
	}

	function openDeleteDialog(id: number) {
		deletingId = id;
		confirmDeleteOpen = true;
	}

	async function handleDelete() {
		if (deletingId === null) return;
		try {
			await deleteDiscount(deletingId);
			toast.success('Discount deleted');
			loadDiscounts();
		} catch {
			toast.error('Failed to delete discount');
		}
	}

	async function handleToggle(d: DiscountListItem, checked: boolean) {
		togglingIds = new Set([...togglingIds, d.id]);
		try {
			await toggleDiscount(d.id, checked);
			d.active = checked;
			discounts = [...discounts];
			toast.success(checked ? 'Discount enabled' : 'Discount disabled');
		} catch {
			toast.error('Failed to toggle discount');
		} finally {
			const next = new Set(togglingIds);
			next.delete(d.id);
			togglingIds = next;
		}
	}

	onMount(() => {
		loadDiscounts();
	});
</script>

<div class="space-y-6">
	<PageHeader
		title="Discounts"
		description="Manage promotions and coupon codes"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Discounts' }]}
	>
		{#if auth.canEdit}
			<Button onclick={openCreateDialog}>
				<PlusIcon class="h-4 w-4 mr-1.5" />
				Create Discount
			</Button>
		{/if}
	</PageHeader>

	<!-- Table -->
	{#if loading}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Name</Table.Head>
						<Table.Head>Code</Table.Head>
						<Table.Head>Type</Table.Head>
						<Table.Head>Value</Table.Head>
						<Table.Head>Usage</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head>Dates</Table.Head>
						<Table.Head class="w-[100px]">Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each Array(6) as _}
						<Table.Row>
							<Table.Cell><Skeleton class="h-4 w-28" /></Table.Cell>
							<Table.Cell><Skeleton class="h-5 w-20 rounded" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-16" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-12" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-14" /></Table.Cell>
							<Table.Cell><Skeleton class="h-5 w-18 rounded-full" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-32" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-16" /></Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{:else if discounts.length === 0}
		<div class="rounded-lg border border-dashed shadow-sm">
			<EmptyState
				icon={TagIcon}
				title="No discounts yet"
				description="Create your first discount or coupon code to attract customers."
				actionLabel={auth.canEdit ? 'Create Discount' : undefined}
				onAction={auth.canEdit ? openCreateDialog : undefined}
			/>
		</div>
	{:else}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Name</Table.Head>
						<Table.Head>Code</Table.Head>
						<Table.Head>Type</Table.Head>
						<Table.Head>Value</Table.Head>
						<Table.Head>Usage</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head>Dates</Table.Head>
						{#if auth.canEdit}
							<Table.Head class="w-[100px]">Actions</Table.Head>
						{/if}
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each discounts as d (d.id)}
						{@const status = getStatusInfo(d)}
						<Table.Row>
							<Table.Cell class="font-medium">{d.name}</Table.Cell>
							<Table.Cell>
								{#if d.code}
									<Badge variant="outline" class="font-mono text-xs">
										{d.code}
									</Badge>
								{:else}
									<span class="text-muted-foreground text-xs">-</span>
								{/if}
							</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{d.type === 'PERCENTAGE' ? 'Percentage' : 'Fixed'}
							</Table.Cell>
							<Table.Cell class="font-medium">
								{formatValue(d)}
							</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{formatUsage(d)}
							</Table.Cell>
							<Table.Cell>
								<div class="flex items-center gap-2">
									<Badge variant="outline" class={status.class}>
										{status.label}
									</Badge>
									{#if auth.canEdit}
										<Switch
											checked={d.active}
											disabled={togglingIds.has(d.id)}
											onCheckedChange={(checked) => handleToggle(d, checked)}
										/>
									{/if}
								</div>
							</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground whitespace-nowrap">
								{formatDate(d.startsAt)} — {formatDate(d.endsAt)}
							</Table.Cell>
							{#if auth.canEdit}
								<Table.Cell>
									<div class="flex items-center gap-1">
										<Button
											variant="ghost"
											size="icon-sm"
											onclick={() => openEditDialog(d.id)}
											aria-label="Edit discount"
										>
											<PencilIcon class="h-3.5 w-3.5" />
										</Button>
										<Button
											variant="ghost"
											size="icon-sm"
											onclick={() => openDeleteDialog(d.id)}
											aria-label="Delete discount"
											class="text-destructive hover:text-destructive"
										>
											<TrashIcon class="h-3.5 w-3.5" />
										</Button>
									</div>
								</Table.Cell>
							{/if}
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		<!-- Pagination -->
		{#if totalPages > 1}
			<div class="flex items-center justify-between">
				<p class="text-sm text-muted-foreground">
					Showing {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, totalElements)} of {totalElements} discounts
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

<!-- Create/Edit Dialog -->
<DiscountFormDialog
	bind:open={formDialogOpen}
	discount={editingDiscount}
	onSaved={loadDiscounts}
/>

<!-- Delete Confirmation -->
<ConfirmDialog
	bind:open={confirmDeleteOpen}
	title="Delete Discount"
	description="Are you sure you want to delete this discount? This action cannot be undone."
	confirmLabel="Delete"
	variant="destructive"
	onConfirm={handleDelete}
/>
