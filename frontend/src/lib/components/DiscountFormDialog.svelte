<script lang="ts">
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Select from '$lib/components/ui/select';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Switch } from '$lib/components/ui/switch';
	import { toast } from 'svelte-sonner';
	import {
		createDiscount,
		updateDiscount,
		type Discount,
		type DiscountType,
		type CreateDiscountRequest
	} from '$lib/api/discounts';

	interface Props {
		open: boolean;
		discount?: Discount | null;
		onSaved: () => void;
	}

	let { open = $bindable(false), discount = null, onSaved }: Props = $props();

	let saving = $state(false);

	// Form fields
	let name = $state('');
	let code = $state('');
	let type = $state<DiscountType>('PERCENTAGE');
	let value = $state<number | string>('');
	let minOrderAmount = $state<number | string>('');
	let usageLimit = $state<number | string>('');
	let startsAt = $state('');
	let endsAt = $state('');
	let active = $state(true);

	let isEditing = $derived(!!discount);
	let dialogTitle = $derived(isEditing ? 'Edit Discount' : 'Create Discount');

	// Reset form when dialog opens
	$effect(() => {
		if (open) {
			if (discount) {
				name = discount.name;
				code = discount.code || '';
				type = discount.type;
				value = discount.value;
				minOrderAmount = discount.minOrderAmount ?? '';
				usageLimit = discount.usageLimit ?? '';
				startsAt = discount.startsAt ? toDatetimeLocal(discount.startsAt) : '';
				endsAt = discount.endsAt ? toDatetimeLocal(discount.endsAt) : '';
				active = discount.active;
			} else {
				name = '';
				code = '';
				type = 'PERCENTAGE';
				value = '';
				minOrderAmount = '';
				usageLimit = '';
				startsAt = '';
				endsAt = '';
				active = true;
			}
		}
	});

	function toDatetimeLocal(iso: string): string {
		const d = new Date(iso);
		const offset = d.getTimezoneOffset();
		const local = new Date(d.getTime() - offset * 60000);
		return local.toISOString().slice(0, 16);
	}

	function generateCode() {
		const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
		let result = '';
		for (let i = 0; i < 8; i++) {
			result += chars.charAt(Math.floor(Math.random() * chars.length));
		}
		code = result;
	}

	async function handleSubmit(e: SubmitEvent) {
		e.preventDefault();

		if (!name.trim()) {
			toast.error('Name is required');
			return;
		}

		const numValue = Number(value);
		if (!numValue || numValue <= 0) {
			toast.error('Value must be greater than 0');
			return;
		}

		if (type === 'PERCENTAGE' && numValue > 100) {
			toast.error('Percentage cannot exceed 100');
			return;
		}

		const data: CreateDiscountRequest = {
			name: name.trim(),
			code: code.trim() || undefined,
			type,
			value: numValue,
			minOrderAmount: minOrderAmount !== '' ? Number(minOrderAmount) : null,
			usageLimit: usageLimit !== '' ? Number(usageLimit) : null,
			startsAt: startsAt ? new Date(startsAt).toISOString() : null,
			endsAt: endsAt ? new Date(endsAt).toISOString() : null,
			active
		};

		saving = true;
		try {
			if (discount) {
				await updateDiscount(discount.id, data);
				toast.success('Discount updated');
			} else {
				await createDiscount(data);
				toast.success('Discount created');
			}
			open = false;
			onSaved();
		} catch (err: unknown) {
			const message = err instanceof Error ? err.message : 'Failed to save discount';
			toast.error(message);
		} finally {
			saving = false;
		}
	}
</script>

<Dialog.Root bind:open>
	<Dialog.Content class="sm:max-w-lg max-h-[90vh] overflow-y-auto">
		<Dialog.Header>
			<Dialog.Title>{dialogTitle}</Dialog.Title>
			<Dialog.Description>
				{isEditing ? 'Update the discount details.' : 'Create a new discount or coupon code.'}
			</Dialog.Description>
		</Dialog.Header>

		<form onsubmit={handleSubmit} class="space-y-4">
			<!-- Name -->
			<div class="space-y-1.5">
				<Label for="discount-name">Name *</Label>
				<Input id="discount-name" bind:value={name} placeholder="Summer Sale" required />
			</div>

			<!-- Code -->
			<div class="space-y-1.5">
				<Label for="discount-code">Coupon Code</Label>
				<div class="flex gap-2">
					<Input id="discount-code" bind:value={code} placeholder="SUMMER2026" class="flex-1 font-mono uppercase" />
					<Button type="button" variant="outline" size="sm" onclick={generateCode}>
						Generate
					</Button>
				</div>
				<p class="text-xs text-muted-foreground">Leave empty for auto-generated code.</p>
			</div>

			<!-- Type & Value -->
			<div class="grid grid-cols-2 gap-3">
				<div class="space-y-1.5">
					<Label>Type</Label>
					<Select.Root type="single" value={type} onValueChange={(v) => { if (v) type = v as DiscountType; }}>
						<Select.Trigger class="w-full">
							{type === 'PERCENTAGE' ? 'Percentage' : 'Fixed Amount'}
						</Select.Trigger>
						<Select.Content>
							<Select.Item value="PERCENTAGE">Percentage</Select.Item>
							<Select.Item value="FIXED_AMOUNT">Fixed Amount</Select.Item>
						</Select.Content>
					</Select.Root>
				</div>
				<div class="space-y-1.5">
					<Label for="discount-value">Value *</Label>
					<div class="relative">
						{#if type === 'FIXED_AMOUNT'}
							<span class="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">$</span>
						{/if}
						<Input
							id="discount-value"
							type="number"
							bind:value={value}
							placeholder={type === 'PERCENTAGE' ? '20' : '10.00'}
							min="0"
							max={type === 'PERCENTAGE' ? '100' : undefined}
							step={type === 'PERCENTAGE' ? '1' : '0.01'}
							class={type === 'FIXED_AMOUNT' ? 'pl-7' : ''}
							required
						/>
						{#if type === 'PERCENTAGE'}
							<span class="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">%</span>
						{/if}
					</div>
				</div>
			</div>

			<!-- Min Order Amount -->
			<div class="space-y-1.5">
				<Label for="discount-min-order">Minimum Order Amount</Label>
				<div class="relative">
					<span class="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">$</span>
					<Input
						id="discount-min-order"
						type="number"
						bind:value={minOrderAmount}
						placeholder="0.00"
						min="0"
						step="0.01"
						class="pl-7"
					/>
				</div>
			</div>

			<!-- Usage Limit -->
			<div class="space-y-1.5">
				<Label for="discount-usage-limit">Usage Limit</Label>
				<Input
					id="discount-usage-limit"
					type="number"
					bind:value={usageLimit}
					placeholder="Unlimited"
					min="0"
					step="1"
				/>
				<p class="text-xs text-muted-foreground">Leave empty for unlimited uses.</p>
			</div>

			<!-- Date Range -->
			<div class="grid grid-cols-2 gap-3">
				<div class="space-y-1.5">
					<Label for="discount-starts">Start Date</Label>
					<Input id="discount-starts" type="datetime-local" bind:value={startsAt} />
				</div>
				<div class="space-y-1.5">
					<Label for="discount-ends">End Date</Label>
					<Input id="discount-ends" type="datetime-local" bind:value={endsAt} />
				</div>
			</div>

			<!-- Targeting info -->
			<div class="rounded-md border border-dashed p-3">
				<p class="text-sm text-muted-foreground">
					Applies to all products. Product and category targeting will be available in a future update.
				</p>
			</div>

			<!-- Active toggle -->
			<div class="flex items-center justify-between rounded-md border p-3">
				<div>
					<p class="text-sm font-medium">Active</p>
					<p class="text-xs text-muted-foreground">Enable this discount immediately</p>
				</div>
				<Switch bind:checked={active} />
			</div>

			<Dialog.Footer>
				<Button type="button" variant="outline" onclick={() => (open = false)} disabled={saving}>
					Cancel
				</Button>
				<Button type="submit" disabled={saving}>
					{saving ? 'Saving...' : isEditing ? 'Update' : 'Create'}
				</Button>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>
