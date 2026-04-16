<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/stores/auth.svelte';
	import {
		getCategoryTree,
		createCategory,
		updateCategory,
		deleteCategory,
		reorderCategories
	} from '$lib/api/categories';
	import type { Category } from '$lib/types/category';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Badge } from '$lib/components/ui/badge';
	import { Switch } from '$lib/components/ui/switch';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Card from '$lib/components/ui/card';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Select from '$lib/components/ui/select';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import SortableList from '$lib/components/SortableList.svelte';
	import FolderTreeIcon from '@lucide/svelte/icons/folder-tree';
	import ArrowUpDownIcon from '@lucide/svelte/icons/arrow-up-down';

	let categories = $state<Category[]>([]);
	let loading = $state(true);
	let createDialogOpen = $state(false);
	let editDialogOpen = $state(false);
	let reorderMode = $state(false);
	let savingOrder = $state(false);

	// Create form state
	let newName = $state('');
	let newDescription = $state('');
	let newSlug = $state('');
	let newParentId = $state<string>('');
	let newSortOrder = $state('0');
	let newMetaTitle = $state('');
	let newMetaDescription = $state('');
	let saving = $state(false);

	// Confirm dialog state
	let confirmOpen = $state(false);
	let confirmTitle = $state('');
	let confirmDescription = $state('');
	let confirmAction = $state<() => Promise<void>>(async () => {});

	// Edit form state
	let editId = $state('');
	let editName = $state('');
	let editDescription = $state('');
	let editSlug = $state('');
	let editParentId = $state<string>('');
	let editSortOrder = $state('0');
	let editActive = $state(true);
	let editMetaTitle = $state('');
	let editMetaDescription = $state('');
	let editSaving = $state(false);

	async function loadCategories() {
		loading = true;
		try {
			categories = await getCategoryTree();
		} catch {
			toast.error('Failed to load categories');
		} finally {
			loading = false;
		}
	}

	function generateSlug(name: string): string {
		return name
			.toLowerCase()
			.trim()
			.replace(/[^\w\s-]/g, '')
			.replace(/[\s_]+/g, '-')
			.replace(/-+/g, '-');
	}

	async function handleCreate() {
		if (!newName.trim()) return;
		saving = true;
		try {
			await createCategory({
				name: newName,
				slug: newSlug.trim() || undefined,
				description: newDescription || undefined,
				parentId: newParentId || undefined,
				sortOrder: Number(newSortOrder) || 0,
				metaTitle: newMetaTitle.trim() || undefined,
				metaDescription: newMetaDescription.trim() || undefined
			});
			toast.success('Category created');
			createDialogOpen = false;
			newName = '';
			newDescription = '';
			newSlug = '';
			newParentId = '';
			newSortOrder = '0';
			newMetaTitle = '';
			newMetaDescription = '';
			await loadCategories();
		} catch (err: any) {
			toast.error(err.message || 'Failed to create category');
		} finally {
			saving = false;
		}
	}

	function openEditDialog(cat: Category) {
		editId = cat.id;
		editName = cat.name;
		editDescription = cat.description || '';
		editSlug = cat.slug;
		editParentId = cat.parentId || '';
		editSortOrder = String(cat.sortOrder);
		editActive = cat.active;
		editMetaTitle = cat.metaTitle || '';
		editMetaDescription = cat.metaDescription || '';
		editDialogOpen = true;
	}

	async function handleUpdate() {
		if (!editName.trim()) return;
		editSaving = true;
		try {
			await updateCategory(editId, {
				name: editName,
				slug: editSlug.trim() || generateSlug(editName),
				description: editDescription || undefined,
				parentId: editParentId || undefined,
				sortOrder: Number(editSortOrder) || 0,
				active: editActive,
				metaTitle: editMetaTitle.trim() || undefined,
				metaDescription: editMetaDescription.trim() || undefined
			});
			toast.success('Category updated');
			editDialogOpen = false;
			await loadCategories();
		} catch (err: any) {
			toast.error(err.message || 'Failed to update category');
		} finally {
			editSaving = false;
		}
	}

	function handleDelete(id: string, name: string) {
		confirmTitle = 'Delete category';
		confirmDescription = `Are you sure you want to delete "${name}"? Child categories will become root-level. This action cannot be undone.`;
		confirmAction = async () => {
			try {
				await deleteCategory(id);
				toast.success('Category deleted');
				await loadCategories();
			} catch {
				toast.error('Failed to delete category');
			}
		};
		confirmOpen = true;
	}

	function flatList(cats: Category[]): Category[] {
		let result: Category[] = [];
		for (const cat of cats) {
			result.push(cat);
			if (cat.children?.length) {
				result = result.concat(flatList(cat.children));
			}
		}
		return result;
	}

	/** Flat list excluding a given category and its children (to prevent circular parent selection). */
	function flatListExcluding(cats: Category[], excludeId: string): Category[] {
		let result: Category[] = [];
		for (const cat of cats) {
			if (cat.id === excludeId) continue;
			result.push(cat);
			if (cat.children?.length) {
				result = result.concat(flatListExcluding(cat.children, excludeId));
			}
		}
		return result;
	}

	async function handleReorder(reordered: any[]) {
		const previous = [...categories];
		categories = reordered as Category[];

		savingOrder = true;
		try {
			await reorderCategories(reordered.map((c: Category) => c.id));
			toast.success('Order saved');
		} catch {
			categories = previous;
			toast.error('Failed to save order');
		} finally {
			savingOrder = false;
		}
	}

	onMount(loadCategories);
</script>

<ConfirmDialog
	bind:open={confirmOpen}
	title={confirmTitle}
	description={confirmDescription}
	confirmLabel="Delete"
	variant="destructive"
	onConfirm={confirmAction}
/>

<div class="space-y-6">
	<PageHeader
		title="Categories"
		description="Organize products into categories"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Categories' }]}
	>
		{#if auth.canEdit}
			{#if categories.length > 1}
				<Button
					variant={reorderMode ? 'default' : 'outline'}
					size="sm"
					onclick={() => (reorderMode = !reorderMode)}
					disabled={savingOrder}
				>
					<ArrowUpDownIcon class="mr-1.5 h-4 w-4" />
					{reorderMode ? 'Done' : 'Reorder'}
				</Button>
			{/if}
			<Dialog.Root bind:open={createDialogOpen}>
				<Dialog.Trigger>
					{#snippet child({ props })}
						<Button {...props}>Add Category</Button>
					{/snippet}
				</Dialog.Trigger>
				<Dialog.Content>
					<Dialog.Header>
						<Dialog.Title>New Category</Dialog.Title>
					</Dialog.Header>
					<div class="space-y-4 py-4">
						<div class="space-y-2">
							<Label for="catName">Name *</Label>
							<Input id="catName" bind:value={newName} placeholder="Category name" />
						</div>
						<div class="space-y-2">
							<Label for="catSlug">Slug</Label>
							<Input
								id="catSlug"
								bind:value={newSlug}
								placeholder={newName ? generateSlug(newName) : 'auto-generated'}
							/>
							<p class="text-xs text-muted-foreground">Leave empty to auto-generate from name.</p>
						</div>
						<div class="space-y-2">
							<Label for="catDesc">Description</Label>
							<Input id="catDesc" bind:value={newDescription} placeholder="Optional description" />
						</div>
						<div class="space-y-2">
							<Label>Parent Category</Label>
							<Select.Root
								type="single"
								value={newParentId}
								onValueChange={(v) => (newParentId = v)}
							>
								<Select.Trigger class="w-full">
									{#if newParentId}
										{flatList(categories).find((c) => c.id === newParentId)?.name || 'Select...'}
									{:else}
										None (root)
									{/if}
								</Select.Trigger>
								<Select.Content>
									<Select.Item value="">None (root)</Select.Item>
									{#each flatList(categories) as cat}
										<Select.Item value={cat.id}>{cat.path || cat.name}</Select.Item>
									{/each}
								</Select.Content>
							</Select.Root>
						</div>
						<div class="space-y-2">
							<Label for="catSort">Sort Order</Label>
							<Input
								id="catSort"
								type="number"
								bind:value={newSortOrder}
								placeholder="0"
							/>
						</div>
						<div class="space-y-2">
							<Label for="catMetaTitle">Meta Title</Label>
							<Input id="catMetaTitle" bind:value={newMetaTitle} placeholder="SEO title (optional)" />
						</div>
						<div class="space-y-2">
							<Label for="catMetaDesc">Meta Description</Label>
							<Input id="catMetaDesc" bind:value={newMetaDescription} placeholder="SEO description (optional)" />
						</div>
					</div>
					<Dialog.Footer>
						<Button variant="outline" onclick={() => (createDialogOpen = false)}>Cancel</Button>
						<Button onclick={handleCreate} disabled={saving || !newName.trim()}>
							{saving ? 'Creating...' : 'Create'}
						</Button>
					</Dialog.Footer>
				</Dialog.Content>
			</Dialog.Root>
		{/if}
	</PageHeader>

	{#if loading}
		<div class="space-y-2">
			{#each Array(5) as _}
				<div class="flex items-center justify-between rounded-md border bg-card p-3">
					<div class="flex items-center gap-3">
						<div>
							<Skeleton class="h-4 w-32 mb-1" />
							<Skeleton class="h-3 w-20" />
						</div>
					</div>
					<div class="flex items-center gap-1">
						<Skeleton class="h-7 w-12" />
						<Skeleton class="h-7 w-14" />
					</div>
				</div>
			{/each}
		</div>
	{:else if categories.length === 0}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={FolderTreeIcon}
				title="No categories yet"
				description="Create categories to organize your products."
				actionLabel={auth.canEdit ? 'Add Category' : undefined}
				onAction={auth.canEdit ? () => (createDialogOpen = true) : undefined}
			/>
		</div>
	{:else if reorderMode}
		<div class="space-y-1">
			<p class="text-sm text-muted-foreground mb-3">
				Drag and drop to reorder root-level categories. Order is saved automatically.
				{#if savingOrder}
					<span class="text-primary font-medium ml-1">Saving...</span>
				{/if}
			</p>
			<SortableList items={categories} onReorder={handleReorder}>
				{#snippet children({ item, index })}
					{@const cat = item as Category}
					<div class="flex items-center justify-between p-3">
						<div class="flex items-center gap-3">
							<span class="text-xs text-muted-foreground font-mono w-6 text-center">{index + 1}</span>
							<div>
								<p class="font-medium">{cat.name}</p>
								<p class="text-xs text-muted-foreground">{cat.slug}</p>
							</div>
							{#if !cat.active}
								<Badge variant="outline" class="bg-gray-100 text-gray-500 dark:bg-gray-800/30 dark:text-gray-400 border-gray-200 dark:border-gray-700">Inactive</Badge>
							{/if}
						</div>
						{#if cat.children?.length}
							<span class="text-xs text-muted-foreground">{cat.children.length} subcategories</span>
						{/if}
					</div>
				{/snippet}
			</SortableList>
		</div>
	{:else}
		<div class="space-y-2">
			{#each categories as category}
				{@render categoryItem(category, 0)}
			{/each}
		</div>
	{/if}
</div>

<!-- Edit Dialog -->
<Dialog.Root bind:open={editDialogOpen}>
	<Dialog.Content>
		<Dialog.Header>
			<Dialog.Title>Edit Category</Dialog.Title>
		</Dialog.Header>
		<div class="space-y-4 py-4">
			<div class="space-y-2">
				<Label for="editName">Name *</Label>
				<Input id="editName" bind:value={editName} placeholder="Category name" />
			</div>
			<div class="space-y-2">
				<Label for="editSlug">Slug</Label>
				<Input
					id="editSlug"
					bind:value={editSlug}
					placeholder={editName ? generateSlug(editName) : 'slug'}
				/>
				<p class="text-xs text-muted-foreground">Leave empty to auto-generate from name.</p>
			</div>
			<div class="space-y-2">
				<Label for="editDesc">Description</Label>
				<Input id="editDesc" bind:value={editDescription} placeholder="Optional description" />
			</div>
			<div class="space-y-2">
				<Label>Parent Category</Label>
				<Select.Root
					type="single"
					value={editParentId}
					onValueChange={(v) => (editParentId = v)}
				>
					<Select.Trigger class="w-full">
						{#if editParentId}
							{flatListExcluding(categories, editId).find((c) => c.id === editParentId)?.name ||
								'Select...'}
						{:else}
							None (root)
						{/if}
					</Select.Trigger>
					<Select.Content>
						<Select.Item value="">None (root)</Select.Item>
						{#each flatListExcluding(categories, editId) as cat}
							<Select.Item value={cat.id}>{cat.path || cat.name}</Select.Item>
						{/each}
					</Select.Content>
				</Select.Root>
			</div>
			<div class="space-y-2">
				<Label for="editSort">Sort Order</Label>
				<Input id="editSort" type="number" bind:value={editSortOrder} placeholder="0" />
			</div>
			<div class="space-y-2">
				<Label for="editMetaTitle">Meta Title</Label>
				<Input id="editMetaTitle" bind:value={editMetaTitle} placeholder="SEO title (optional)" />
			</div>
			<div class="space-y-2">
				<Label for="editMetaDesc">Meta Description</Label>
				<Input id="editMetaDesc" bind:value={editMetaDescription} placeholder="SEO description (optional)" />
			</div>
			<div class="flex items-center gap-3">
				<Switch bind:checked={editActive} />
				<Label>Active</Label>
			</div>
		</div>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (editDialogOpen = false)}>Cancel</Button>
			<Button onclick={handleUpdate} disabled={editSaving || !editName.trim()}>
				{editSaving ? 'Saving...' : 'Save Changes'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

{#snippet categoryItem(cat: Category, depth: number)}
	<div
		class="flex items-center justify-between rounded-md border bg-card p-3 shadow-sm hover:shadow-md transition-all {depth > 0 ? 'border-l-2 border-l-muted-foreground/20' : ''}"
		style="margin-left: {depth * 24}px"
	>
		<div class="flex items-center gap-3">
			<div>
				{#if auth.canEdit}
					<button
						type="button"
						class="font-medium hover:underline text-left"
						onclick={() => openEditDialog(cat)}
					>
						{cat.name}
					</button>
				{:else}
					<p class="font-medium">{cat.name}</p>
				{/if}
				<p class="text-xs text-muted-foreground">{cat.slug}</p>
			</div>
			{#if !cat.active}
				<Badge variant="outline" class="bg-gray-100 text-gray-500 dark:bg-gray-800/30 dark:text-gray-400 border-gray-200 dark:border-gray-700">Inactive</Badge>
			{/if}
		</div>
		{#if auth.canEdit}
			<div class="flex items-center gap-1">
				<Button variant="ghost" size="sm" onclick={() => openEditDialog(cat)}>Edit</Button>
				<Button variant="ghost" size="sm" onclick={() => handleDelete(cat.id, cat.name)}
					>Delete</Button
				>
			</div>
		{/if}
	</div>
	{#if cat.children?.length}
		{#each cat.children as child}
			{@render categoryItem(child, depth + 1)}
		{/each}
	{/if}
{/snippet}
