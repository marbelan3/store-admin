<script lang="ts">
	import { toast } from 'svelte-sonner';
	import PencilIcon from '@lucide/svelte/icons/pencil';
	import LoaderIcon from '@lucide/svelte/icons/loader';
	import CheckIcon from '@lucide/svelte/icons/check';

	interface Props {
		value: string | number;
		type?: 'text' | 'number' | 'currency';
		onSave: (newValue: string | number) => Promise<void>;
		disabled?: boolean;
		placeholder?: string;
		suffix?: string;
		min?: number;
	}

	let {
		value,
		type = 'text',
		onSave,
		disabled = false,
		placeholder = '',
		suffix = '',
		min
	}: Props = $props();

	let editing = $state(false);
	let saving = $state(false);
	let editValue = $state('');
	let showSuccess = $state(false);
	let inputEl = $state<HTMLInputElement>();

	function displayValue(): string {
		if (value == null || value === '') return placeholder || '—';
		if (type === 'currency') {
			const num = typeof value === 'string' ? parseFloat(value) : value;
			return isNaN(num) ? String(value) : `$${num.toFixed(2)}`;
		}
		return String(value);
	}

	function startEdit(e: MouseEvent) {
		if (disabled || saving) return;
		e.stopPropagation();
		e.preventDefault();

		if (type === 'currency') {
			const num = typeof value === 'string' ? parseFloat(value) : value;
			editValue = isNaN(num as number) ? String(value) : String(num);
		} else {
			editValue = String(value ?? '');
		}
		editing = true;

		// Auto-focus and select after Svelte renders the input
		requestAnimationFrame(() => {
			if (inputEl) {
				inputEl.focus();
				inputEl.select();
			}
		});
	}

	async function save() {
		if (saving) return;

		let newValue: string | number;
		if (type === 'number' || type === 'currency') {
			const parsed = parseFloat(editValue);
			if (isNaN(parsed)) {
				cancel();
				return;
			}
			if (min !== undefined && parsed < min) {
				cancel();
				return;
			}
			newValue = parsed;
		} else {
			newValue = editValue;
		}

		// No change — just close
		if (newValue === value) {
			editing = false;
			return;
		}

		saving = true;
		editing = false;

		try {
			await onSave(newValue);
			// Brief green flash to confirm success
			showSuccess = true;
			setTimeout(() => {
				showSuccess = false;
			}, 800);
		} catch (err: any) {
			toast.error(err?.message || 'Failed to save');
		} finally {
			saving = false;
		}
	}

	function cancel() {
		editing = false;
		editValue = '';
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter') {
			e.preventDefault();
			save();
		} else if (e.key === 'Escape') {
			e.preventDefault();
			cancel();
		} else if (e.key === 'Tab') {
			// Allow tab to save and move to next cell
			save();
		}
	}

	function handleBlur() {
		if (editing && !saving) {
			save();
		}
	}
</script>

{#if editing}
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div
		class="inline-flex items-center"
		onclick={(e) => e.stopPropagation()}
	>
		<input
			bind:this={inputEl}
			type={type === 'currency' || type === 'number' ? 'number' : 'text'}
			step={type === 'currency' ? '0.01' : type === 'number' ? '1' : undefined}
			min={min !== undefined ? String(min) : undefined}
			class="h-8 w-full min-w-[80px] max-w-[120px] rounded-md border border-primary bg-background px-2 text-sm
				ring-2 ring-primary/30 focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
			value={editValue}
			oninput={(e) => { editValue = (e.target as HTMLInputElement).value; }}
			onkeydown={handleKeydown}
			onblur={handleBlur}
			{placeholder}
		/>
	</div>
{:else}
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<!-- svelte-ignore a11y_no_noninteractive_tabindex -->
	<div
		class="group/cell inline-flex items-center gap-1.5 rounded-md px-1.5 py-0.5 -mx-1.5 -my-0.5 transition-all
			{disabled ? '' : 'cursor-pointer hover:bg-muted/80'}
			{saving ? 'opacity-70' : ''}
			{showSuccess ? 'bg-emerald-50 dark:bg-emerald-950/30' : ''}"
		onclick={startEdit}
		role={disabled ? undefined : 'button'}
		tabindex={disabled ? undefined : 0}
	>
		{#if saving}
			<LoaderIcon class="h-3 w-3 animate-spin text-muted-foreground" />
		{/if}

		<span class="text-sm {showSuccess ? 'text-emerald-700 dark:text-emerald-400' : ''} transition-colors">
			{displayValue()}{suffix ? ` ${suffix}` : ''}
		</span>

		{#if showSuccess}
			<CheckIcon class="h-3 w-3 text-emerald-600 dark:text-emerald-400" />
		{:else if !disabled && !saving}
			<PencilIcon class="h-3 w-3 text-muted-foreground/0 group-hover/cell:text-muted-foreground/70 transition-all" />
		{/if}
	</div>
{/if}
