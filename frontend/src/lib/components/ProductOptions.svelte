<script lang="ts">
	import type { ProductOption } from '$lib/types/product';
	import { createOption, deleteOption } from '$lib/api/variants';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Badge } from '$lib/components/ui/badge';
	import * as Card from '$lib/components/ui/card';
	import { toast } from 'svelte-sonner';
	import PlusIcon from '@lucide/svelte/icons/plus';
	import XIcon from '@lucide/svelte/icons/x';
	import Trash2Icon from '@lucide/svelte/icons/trash-2';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';

	interface Props {
		productId: string;
		options: ProductOption[];
		readonly?: boolean;
		onOptionsChange?: (options: ProductOption[]) => void;
	}

	let { productId, options = $bindable([]), readonly = false, onOptionsChange }: Props = $props();

	let showForm = $state(false);
	let optionName = $state('');
	let valueInput = $state('');
	let values = $state<string[]>([]);
	let saving = $state(false);

	// Confirm dialog
	let confirmOpen = $state(false);
	let confirmTitle = $state('');
	let confirmDescription = $state('');
	let confirmAction = $state<() => Promise<void>>(async () => {});

	function addValue() {
		const trimmed = valueInput.trim();
		if (trimmed && !values.includes(trimmed)) {
			values = [...values, trimmed];
		}
		valueInput = '';
	}

	function handleValueKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter') {
			e.preventDefault();
			addValue();
		} else if (e.key === ',' || e.key === 'Tab') {
			e.preventDefault();
			addValue();
		}
	}

	function removeValue(index: number) {
		values = values.filter((_, i) => i !== index);
	}

	function resetForm() {
		optionName = '';
		valueInput = '';
		values = [];
		showForm = false;
	}

	async function handleCreate() {
		if (!optionName.trim()) {
			toast.error('Option name is required');
			return;
		}
		if (values.length === 0) {
			toast.error('Add at least one value');
			return;
		}

		saving = true;
		try {
			const created = await createOption(productId, {
				name: optionName.trim(),
				values
			});
			options = [...options, created];
			onOptionsChange?.(options);
			toast.success(`Option "${created.name}" created`);
			resetForm();
		} catch (err: any) {
			toast.error(err.message || 'Failed to create option');
		} finally {
			saving = false;
		}
	}

	function handleDelete(opt: ProductOption) {
		confirmTitle = 'Delete Option';
		confirmDescription = `Delete option "${opt.name}" and all its values? This will also remove associated variants.`;
		confirmAction = async () => {
			try {
				await deleteOption(productId, opt.id);
				options = options.filter((o) => o.id !== opt.id);
				onOptionsChange?.(options);
				toast.success(`Option "${opt.name}" deleted`);
			} catch (err: any) {
				toast.error(err.message || 'Failed to delete option');
			}
		};
		confirmOpen = true;
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

<Card.Root>
	<Card.Header>
		<div class="flex items-center justify-between">
			<Card.Title>Options</Card.Title>
			{#if !readonly && !showForm}
				<Button type="button" variant="outline" size="sm" onclick={() => (showForm = true)}>
					<PlusIcon class="mr-1 h-4 w-4" />
					Add Option
				</Button>
			{/if}
		</div>
	</Card.Header>
	<Card.Content class="space-y-4">
		{#if options.length === 0 && !showForm}
			<p class="text-sm text-muted-foreground">
				No options defined. Add options like Size, Color, or Material to create product variants.
			</p>
		{/if}

		{#each options as opt}
			<div class="flex items-start justify-between rounded-md border p-3">
				<div class="space-y-1.5">
					<p class="text-sm font-medium">{opt.name}</p>
					<div class="flex flex-wrap gap-1.5">
						{#each opt.values as val}
							<Badge variant="secondary">{val.value}</Badge>
						{/each}
					</div>
				</div>
				{#if !readonly}
					<Button
						type="button"
						variant="ghost"
						size="sm"
						onclick={() => handleDelete(opt)}
						class="text-destructive hover:text-destructive"
					>
						<Trash2Icon class="h-4 w-4" />
					</Button>
				{/if}
			</div>
		{/each}

		{#if showForm}
			<div class="rounded-md border p-4 space-y-4 bg-muted/30">
				<div class="space-y-2">
					<Label>Option Name</Label>
					<Input
						bind:value={optionName}
						placeholder="e.g. Size, Color, Material"
					/>
				</div>

				<div class="space-y-2">
					<Label>Values</Label>
					<div class="flex gap-2">
						<Input
							bind:value={valueInput}
							placeholder="Type a value and press Enter"
							onkeydown={handleValueKeydown}
						/>
						<Button type="button" variant="outline" size="sm" onclick={addValue}>
							Add
						</Button>
					</div>
					{#if values.length > 0}
						<div class="flex flex-wrap gap-1.5 mt-2">
							{#each values as val, i}
								<Badge variant="secondary" class="gap-1 pr-1">
									{val}
									<button
										type="button"
										class="ml-0.5 rounded-full p-0.5 hover:bg-muted-foreground/20"
										onclick={() => removeValue(i)}
									>
										<XIcon class="h-3 w-3" />
									</button>
								</Badge>
							{/each}
						</div>
					{/if}
				</div>

				<div class="flex gap-2 justify-end">
					<Button type="button" variant="outline" size="sm" onclick={resetForm} disabled={saving}>
						Cancel
					</Button>
					<Button type="button" size="sm" onclick={handleCreate} disabled={saving}>
						{saving ? 'Creating...' : 'Create Option'}
					</Button>
				</div>
			</div>
		{/if}
	</Card.Content>
</Card.Root>
