<script lang="ts">
	import type { ProductOption, ProductVariantDetail, OptionValue } from '$lib/types/product';
	import { createVariant, updateVariant, deleteVariant } from '$lib/api/variants';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Badge } from '$lib/components/ui/badge';
	import { Switch } from '$lib/components/ui/switch';
	import * as Card from '$lib/components/ui/card';
	import * as Table from '$lib/components/ui/table';
	import * as Select from '$lib/components/ui/select';
	import * as Dialog from '$lib/components/ui/dialog';
	import { toast } from 'svelte-sonner';
	import PlusIcon from '@lucide/svelte/icons/plus';
	import Trash2Icon from '@lucide/svelte/icons/trash-2';
	import WandIcon from '@lucide/svelte/icons/wand';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';

	interface Props {
		productId: string;
		variants: ProductVariantDetail[];
		options: ProductOption[];
		currency?: string;
		readonly?: boolean;
		onVariantsChange?: (variants: ProductVariantDetail[]) => void;
	}

	let {
		productId,
		variants = $bindable([]),
		options,
		currency = '',
		readonly = false,
		onVariantsChange
	}: Props = $props();

	// Add variant dialog
	let showAddDialog = $state(false);
	let addSelectedValues = $state<Record<string, string>>({});
	let addSku = $state('');
	let addPrice = $state('');
	let addCompareAtPrice = $state('');
	let addAvailable = $state(true);
	let addSaving = $state(false);

	// Inline editing state
	let editingCell = $state<{ variantId: string; field: string } | null>(null);
	let editingValue = $state('');

	// Bulk update
	let showBulkDialog = $state(false);
	let bulkPrice = $state('');
	let bulkAvailable = $state<'unchanged' | 'true' | 'false'>('unchanged');
	let bulkSaving = $state(false);

	// Confirm dialog
	let confirmOpen = $state(false);
	let confirmTitle = $state('');
	let confirmDescription = $state('');
	let confirmAction = $state<() => Promise<void>>(async () => {});

	function getVariantOptionLabel(variant: ProductVariantDetail, option: ProductOption): string {
		const match = variant.optionValues.find((ov) =>
			option.values.some((v) => v.id === ov.id)
		);
		return match?.value ?? '—';
	}

	// --- Add Variant ---
	function resetAddForm() {
		addSelectedValues = {};
		addSku = '';
		addPrice = '';
		addCompareAtPrice = '';
		addAvailable = true;
		showAddDialog = false;
	}

	async function handleAdd() {
		// Validate: one value per option
		for (const opt of options) {
			if (!addSelectedValues[opt.id]) {
				toast.error(`Select a value for "${opt.name}"`);
				return;
			}
		}
		if (!addPrice.trim()) {
			toast.error('Price is required');
			return;
		}

		const optionValueIds = Object.values(addSelectedValues);

		// Check for duplicate combination
		const isDuplicate = variants.some((v) => {
			const existingIds = new Set(v.optionValues.map((ov) => ov.id));
			return optionValueIds.every((id) => existingIds.has(id));
		});
		if (isDuplicate) {
			toast.error('A variant with this combination already exists');
			return;
		}

		addSaving = true;
		try {
			const created = await createVariant(productId, {
				sku: addSku.trim() || undefined,
				price: Number(addPrice),
				compareAtPrice: addCompareAtPrice ? Number(addCompareAtPrice) : undefined,
				available: addAvailable,
				optionValueIds
			});
			variants = [...variants, created];
			onVariantsChange?.(variants);
			toast.success('Variant created');
			resetAddForm();
		} catch (err: any) {
			toast.error(err.message || 'Failed to create variant');
		} finally {
			addSaving = false;
		}
	}

	// --- Auto-generate variants ---
	async function handleAutoGenerate() {
		if (options.length === 0) return;

		// Build all combinations of option values
		let combos: OptionValue[][] = [[]];
		for (const opt of options) {
			const next: OptionValue[][] = [];
			for (const combo of combos) {
				for (const val of opt.values) {
					next.push([...combo, val]);
				}
			}
			combos = next;
		}

		// Filter out combinations that already exist
		const existingCombos = new Set(
			variants.map((v) =>
				v.optionValues
					.map((ov) => ov.id)
					.sort()
					.join(',')
			)
		);

		const newCombos = combos.filter((combo) => {
			const key = combo
				.map((v) => v.id)
				.sort()
				.join(',');
			return !existingCombos.has(key);
		});

		if (newCombos.length === 0) {
			toast.info('All variant combinations already exist');
			return;
		}

		let created = 0;
		let failed = 0;
		for (const combo of newCombos) {
			try {
				const variant = await createVariant(productId, {
					price: 0,
					available: true,
					optionValueIds: combo.map((v) => v.id)
				});
				variants = [...variants, variant];
				created++;
			} catch {
				failed++;
			}
		}
		onVariantsChange?.(variants);

		if (failed > 0) {
			toast.warning(`Created ${created} variants, ${failed} failed`);
		} else {
			toast.success(`Created ${created} variant${created !== 1 ? 's' : ''}`);
		}
	}

	// --- Inline editing ---
	function startEdit(variantId: string, field: string, currentValue: string) {
		if (readonly) return;
		editingCell = { variantId, field };
		editingValue = currentValue;
	}

	function handleEditKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter') {
			saveEdit();
		} else if (e.key === 'Escape') {
			editingCell = null;
		}
	}

	async function saveEdit() {
		if (!editingCell) return;
		const { variantId, field } = editingCell;

		const data: Record<string, any> = {};
		if (field === 'sku') {
			data.sku = editingValue.trim() || undefined;
		} else if (field === 'price') {
			const num = Number(editingValue);
			if (isNaN(num) || num < 0) {
				toast.error('Invalid price');
				return;
			}
			data.price = num;
		} else if (field === 'compareAtPrice') {
			if (editingValue.trim() === '') {
				data.compareAtPrice = null;
			} else {
				const num = Number(editingValue);
				if (isNaN(num) || num < 0) {
					toast.error('Invalid price');
					return;
				}
				data.compareAtPrice = num;
			}
		}

		try {
			const updated = await updateVariant(productId, variantId, data);
			variants = variants.map((v) => (v.id === variantId ? updated : v));
			onVariantsChange?.(variants);
		} catch (err: any) {
			toast.error(err.message || 'Failed to update variant');
		} finally {
			editingCell = null;
		}
	}

	async function handleToggleAvailable(variant: ProductVariantDetail) {
		try {
			const updated = await updateVariant(productId, variant.id, {
				available: !variant.available
			});
			variants = variants.map((v) => (v.id === variant.id ? updated : v));
			onVariantsChange?.(variants);
		} catch (err: any) {
			toast.error(err.message || 'Failed to update variant');
		}
	}

	// --- Delete ---
	function handleDelete(variant: ProductVariantDetail) {
		const label = variant.optionValues.map((ov) => ov.value).join(' / ') || variant.sku || variant.id;
		confirmTitle = 'Delete Variant';
		confirmDescription = `Delete variant "${label}"? This action cannot be undone.`;
		confirmAction = async () => {
			try {
				await deleteVariant(productId, variant.id);
				variants = variants.filter((v) => v.id !== variant.id);
				onVariantsChange?.(variants);
				toast.success('Variant deleted');
			} catch (err: any) {
				toast.error(err.message || 'Failed to delete variant');
			}
		};
		confirmOpen = true;
	}

	// --- Bulk update ---
	async function handleBulkUpdate() {
		if (!bulkPrice.trim() && bulkAvailable === 'unchanged') {
			toast.info('No changes to apply');
			return;
		}

		bulkSaving = true;
		let updated = 0;
		let failed = 0;

		for (const v of variants) {
			const data: Record<string, any> = {};
			if (bulkPrice.trim()) {
				data.price = Number(bulkPrice);
			}
			if (bulkAvailable !== 'unchanged') {
				data.available = bulkAvailable === 'true';
			}
			if (Object.keys(data).length === 0) continue;

			try {
				const result = await updateVariant(productId, v.id, data);
				variants = variants.map((vr) => (vr.id === v.id ? result : vr));
				updated++;
			} catch {
				failed++;
			}
		}
		onVariantsChange?.(variants);

		if (failed > 0) {
			toast.warning(`Updated ${updated} variants, ${failed} failed`);
		} else {
			toast.success(`Updated ${updated} variant${updated !== 1 ? 's' : ''}`);
		}
		bulkPrice = '';
		bulkAvailable = 'unchanged';
		showBulkDialog = false;
		bulkSaving = false;
	}
</script>

<ConfirmDialog
	bind:open={confirmOpen}
	title={confirmTitle}
	description={confirmDescription}
	confirmLabel="Delete"
	variant="destructive"
	onConfirm={confirmAction}
/>

<!-- Add Variant Dialog -->
<Dialog.Root bind:open={showAddDialog}>
	<Dialog.Content class="sm:max-w-lg">
		<Dialog.Header>
			<Dialog.Title>Add Variant</Dialog.Title>
			<Dialog.Description>Select one value for each option and set price details.</Dialog.Description>
		</Dialog.Header>
		<div class="space-y-4 py-4">
			{#each options as opt}
				<div class="space-y-2">
					<Label>{opt.name}</Label>
					<Select.Root
						type="single"
						value={addSelectedValues[opt.id] ?? ''}
						onValueChange={(v) => { addSelectedValues = { ...addSelectedValues, [opt.id]: v }; }}
					>
						<Select.Trigger class="w-full">
							{#if addSelectedValues[opt.id]}
								{opt.values.find((v) => v.id === addSelectedValues[opt.id])?.value ?? 'Select...'}
							{:else}
								Select {opt.name}...
							{/if}
						</Select.Trigger>
						<Select.Content>
							{#each opt.values as val}
								<Select.Item value={val.id}>{val.value}</Select.Item>
							{/each}
						</Select.Content>
					</Select.Root>
				</div>
			{/each}

			<div class="space-y-2">
				<Label>SKU</Label>
				<Input bind:value={addSku} placeholder="Optional SKU" />
			</div>

			<div class="grid grid-cols-2 gap-4">
				<div class="space-y-2">
					<Label>Price{currency ? ` (${currency})` : ''} <span class="text-destructive">*</span></Label>
					<Input type="number" step="0.01" bind:value={addPrice} placeholder="0.00" />
				</div>
				<div class="space-y-2">
					<Label>Compare at Price</Label>
					<Input type="number" step="0.01" bind:value={addCompareAtPrice} placeholder="0.00" />
				</div>
			</div>

			<div class="flex items-center gap-2">
				<Switch checked={addAvailable} onCheckedChange={(v) => (addAvailable = v)} />
				<Label>Available</Label>
			</div>
		</div>
		<Dialog.Footer>
			<Button variant="outline" onclick={resetAddForm} disabled={addSaving}>Cancel</Button>
			<Button onclick={handleAdd} disabled={addSaving}>
				{addSaving ? 'Creating...' : 'Create Variant'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<!-- Bulk Update Dialog -->
<Dialog.Root bind:open={showBulkDialog}>
	<Dialog.Content class="sm:max-w-md">
		<Dialog.Header>
			<Dialog.Title>Bulk Update Variants</Dialog.Title>
			<Dialog.Description>Apply changes to all {variants.length} variants.</Dialog.Description>
		</Dialog.Header>
		<div class="space-y-4 py-4">
			<div class="space-y-2">
				<Label>Set Price{currency ? ` (${currency})` : ''}</Label>
				<Input type="number" step="0.01" bind:value={bulkPrice} placeholder="Leave empty to keep current" />
			</div>
			<div class="space-y-2">
				<Label>Set Availability</Label>
				<Select.Root
					type="single"
					value={bulkAvailable}
					onValueChange={(v) => (bulkAvailable = v as 'unchanged' | 'true' | 'false')}
				>
					<Select.Trigger class="w-full">
						{bulkAvailable === 'unchanged'
							? 'Keep current'
							: bulkAvailable === 'true'
								? 'Available'
								: 'Unavailable'}
					</Select.Trigger>
					<Select.Content>
						<Select.Item value="unchanged">Keep current</Select.Item>
						<Select.Item value="true">Available</Select.Item>
						<Select.Item value="false">Unavailable</Select.Item>
					</Select.Content>
				</Select.Root>
			</div>
		</div>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (showBulkDialog = false)} disabled={bulkSaving}>Cancel</Button>
			<Button onclick={handleBulkUpdate} disabled={bulkSaving}>
				{bulkSaving ? 'Updating...' : 'Update All'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<Card.Root>
	<Card.Header>
		<div class="flex items-center justify-between">
			<div>
				<Card.Title>Variants</Card.Title>
				{#if variants.length > 0}
					<p class="text-sm text-muted-foreground mt-1">{variants.length} variant{variants.length !== 1 ? 's' : ''}</p>
				{/if}
			</div>
			{#if !readonly && options.length > 0}
				<div class="flex gap-2">
					{#if variants.length > 0}
						<Button type="button" variant="outline" size="sm" onclick={() => (showBulkDialog = true)}>
							Bulk Update
						</Button>
					{/if}
					<Button type="button" variant="outline" size="sm" onclick={handleAutoGenerate}>
						<WandIcon class="mr-1 h-4 w-4" />
						Auto-generate
					</Button>
					<Button type="button" variant="outline" size="sm" onclick={() => (showAddDialog = true)}>
						<PlusIcon class="mr-1 h-4 w-4" />
						Add Variant
					</Button>
				</div>
			{/if}
		</div>
	</Card.Header>
	<Card.Content>
		{#if options.length === 0}
			<p class="text-sm text-muted-foreground">
				Add options first (e.g. Size, Color) to create product variants.
			</p>
		{:else if variants.length === 0}
			<p class="text-sm text-muted-foreground">
				No variants yet. Click "Auto-generate" to create all combinations, or add them one by one.
			</p>
		{:else}
			<div class="rounded-md border overflow-x-auto">
				<Table.Root>
					<Table.Header>
						<Table.Row>
							{#each options as opt}
								<Table.Head>{opt.name}</Table.Head>
							{/each}
							<Table.Head>SKU</Table.Head>
							<Table.Head>Price</Table.Head>
							<Table.Head>Compare At</Table.Head>
							<Table.Head>Available</Table.Head>
							{#if !readonly}
								<Table.Head class="w-[60px]"></Table.Head>
							{/if}
						</Table.Row>
					</Table.Header>
					<Table.Body>
						{#each variants as variant}
							<Table.Row>
								{#each options as opt}
									<Table.Cell>
										<Badge variant="outline">{getVariantOptionLabel(variant, opt)}</Badge>
									</Table.Cell>
								{/each}

								<!-- SKU (editable) -->
								<Table.Cell>
									{#if editingCell?.variantId === variant.id && editingCell?.field === 'sku'}
										<Input
											class="h-8 w-28"
											bind:value={editingValue}
											onblur={saveEdit}
											onkeydown={handleEditKeydown}
											autofocus
										/>
									{:else}
										<button
											type="button"
											class="text-left w-full hover:underline cursor-text"
											class:text-muted-foreground={!variant.sku}
											onclick={() => startEdit(variant.id, 'sku', variant.sku || '')}
											disabled={readonly}
										>
											{variant.sku || '—'}
										</button>
									{/if}
								</Table.Cell>

								<!-- Price (editable) -->
								<Table.Cell>
									{#if editingCell?.variantId === variant.id && editingCell?.field === 'price'}
										<Input
											class="h-8 w-24"
											type="number"
											step="0.01"
											bind:value={editingValue}
											onblur={saveEdit}
											onkeydown={handleEditKeydown}
											autofocus
										/>
									{:else}
										<button
											type="button"
											class="text-left w-full hover:underline cursor-text"
											onclick={() => startEdit(variant.id, 'price', String(variant.price))}
											disabled={readonly}
										>
											{variant.price.toFixed(2)}
										</button>
									{/if}
								</Table.Cell>

								<!-- Compare At Price (editable) -->
								<Table.Cell>
									{#if editingCell?.variantId === variant.id && editingCell?.field === 'compareAtPrice'}
										<Input
											class="h-8 w-24"
											type="number"
											step="0.01"
											bind:value={editingValue}
											onblur={saveEdit}
											onkeydown={handleEditKeydown}
											autofocus
										/>
									{:else}
										<button
											type="button"
											class="text-left w-full hover:underline cursor-text"
											class:text-muted-foreground={variant.compareAtPrice == null}
											onclick={() => startEdit(variant.id, 'compareAtPrice', variant.compareAtPrice != null ? String(variant.compareAtPrice) : '')}
											disabled={readonly}
										>
											{variant.compareAtPrice != null ? variant.compareAtPrice.toFixed(2) : '—'}
										</button>
									{/if}
								</Table.Cell>

								<!-- Available toggle -->
								<Table.Cell>
									<Switch
										checked={variant.available}
										onCheckedChange={() => handleToggleAvailable(variant)}
										disabled={readonly}
									/>
								</Table.Cell>

								<!-- Actions -->
								{#if !readonly}
									<Table.Cell>
										<Button
											type="button"
											variant="ghost"
											size="sm"
											onclick={() => handleDelete(variant)}
											class="text-destructive hover:text-destructive"
										>
											<Trash2Icon class="h-4 w-4" />
										</Button>
									</Table.Cell>
								{/if}
							</Table.Row>
						{/each}
					</Table.Body>
				</Table.Root>
			</div>
		{/if}
	</Card.Content>
</Card.Root>
