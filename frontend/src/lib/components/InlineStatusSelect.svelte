<script lang="ts">
	import { toast } from 'svelte-sonner';
	import { Badge } from '$lib/components/ui/badge';
	import LoaderIcon from '@lucide/svelte/icons/loader';
	import CheckIcon from '@lucide/svelte/icons/check';
	import ChevronDownIcon from '@lucide/svelte/icons/chevron-down';

	interface Props {
		value: string;
		options: { value: string; label: string; class?: string }[];
		onSave: (newValue: string) => Promise<void>;
		disabled?: boolean;
		badgeClass?: (value: string) => string;
	}

	let {
		value,
		options,
		onSave,
		disabled = false,
		badgeClass
	}: Props = $props();

	let open = $state(false);
	let saving = $state(false);
	let showSuccess = $state(false);
	let containerEl = $state<HTMLDivElement>();

	async function selectOption(optionValue: string, e: MouseEvent) {
		e.stopPropagation();
		e.preventDefault();
		open = false;

		if (optionValue === value) return;

		saving = true;
		try {
			await onSave(optionValue);
			showSuccess = true;
			setTimeout(() => {
				showSuccess = false;
			}, 800);
		} catch (err: any) {
			toast.error(err?.message || 'Failed to update status');
		} finally {
			saving = false;
		}
	}

	function toggleDropdown(e: MouseEvent) {
		if (disabled || saving) return;
		e.stopPropagation();
		e.preventDefault();
		open = !open;
	}

	function handleClickOutside(e: MouseEvent) {
		if (containerEl && !containerEl.contains(e.target as Node)) {
			open = false;
		}
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape') {
			open = false;
		}
	}

	$effect(() => {
		if (open) {
			document.addEventListener('click', handleClickOutside, true);
			document.addEventListener('keydown', handleKeydown, true);
			return () => {
				document.removeEventListener('click', handleClickOutside, true);
				document.removeEventListener('keydown', handleKeydown, true);
			};
		}
	});

	function getBadgeClasses(val: string): string {
		return badgeClass ? badgeClass(val) : '';
	}
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<!-- svelte-ignore a11y_no_noninteractive_tabindex -->
<div bind:this={containerEl} class="relative inline-block">
	<div
		class="group/status inline-flex items-center gap-1 rounded-md px-0.5 py-0.5 -mx-0.5 transition-all
			{disabled ? '' : 'cursor-pointer hover:bg-muted/80'}
			{saving ? 'opacity-70' : ''}
			{showSuccess ? 'bg-emerald-50 dark:bg-emerald-950/30' : ''}"
		onclick={toggleDropdown}
		role={disabled ? undefined : 'button'}
		tabindex={disabled ? undefined : 0}
	>
		{#if saving}
			<LoaderIcon class="h-3 w-3 animate-spin text-muted-foreground" />
		{/if}

		<Badge variant="outline" class="{getBadgeClasses(value)} {showSuccess ? 'ring-1 ring-emerald-400' : ''}">
			{value}
		</Badge>

		{#if showSuccess}
			<CheckIcon class="h-3 w-3 text-emerald-600 dark:text-emerald-400" />
		{:else if !disabled && !saving}
			<ChevronDownIcon class="h-3 w-3 text-muted-foreground/0 group-hover/status:text-muted-foreground/70 transition-all" />
		{/if}
	</div>

	{#if open}
		<div class="absolute z-50 mt-1 min-w-[140px] rounded-md border bg-popover p-1 shadow-md">
			{#each options as opt}
				<button
					class="flex w-full items-center gap-2 rounded-sm px-2 py-1.5 text-sm hover:bg-accent hover:text-accent-foreground transition-colors
						{opt.value === value ? 'bg-accent/50 font-medium' : ''}"
					onclick={(e) => selectOption(opt.value, e)}
				>
					<Badge variant="outline" class="{getBadgeClasses(opt.value)} text-xs">
						{opt.label}
					</Badge>
					{#if opt.value === value}
						<CheckIcon class="ml-auto h-3 w-3" />
					{/if}
				</button>
			{/each}
		</div>
	{/if}
</div>
