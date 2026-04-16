<script lang="ts">
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import * as Dialog from '$lib/components/ui/dialog';
	import BookmarkIcon from '@lucide/svelte/icons/bookmark';
	import BookmarkPlusIcon from '@lucide/svelte/icons/bookmark-plus';
	import XIcon from '@lucide/svelte/icons/x';
	import { toast } from 'svelte-sonner';

	interface SavedView {
		name: string;
		filters: Record<string, any>;
	}

	interface Props {
		storageKey: string;
		currentFilters: Record<string, any>;
		defaultFilters?: Record<string, any>;
		onApplyView: (filters: Record<string, any>) => void;
	}

	let { storageKey, currentFilters, defaultFilters, onApplyView }: Props = $props();

	let savedViews = $state<SavedView[]>([]);
	let dialogOpen = $state(false);
	let viewName = $state('');

	// Load saved views from localStorage on init
	function loadViews(): SavedView[] {
		try {
			const raw = localStorage.getItem(storageKey);
			if (raw) return JSON.parse(raw);
		} catch {
			// ignore corrupted data
		}
		return [];
	}

	function persistViews(views: SavedView[]) {
		localStorage.setItem(storageKey, JSON.stringify(views));
		savedViews = views;
	}

	function filtersMatch(a: Record<string, any>, b: Record<string, any>): boolean {
		const keysA = Object.keys(a);
		const keysB = Object.keys(b);
		if (keysA.length !== keysB.length) return false;
		return keysA.every((key) => a[key] === b[key]);
	}

	function isDefaultFilters(filters: Record<string, any>): boolean {
		if (defaultFilters) {
			return filtersMatch(filters, defaultFilters);
		}
		return Object.values(filters).every((v) => v === '' || v === undefined || v === null);
	}

	let activeViewIndex = $derived(
		savedViews.findIndex((v) => filtersMatch(v.filters, currentFilters))
	);

	let isDefault = $derived(isDefaultFilters(currentFilters));

	function handleSave() {
		const name = viewName.trim();
		if (!name) return;

		const duplicate = savedViews.find((v) => v.name.toLowerCase() === name.toLowerCase());
		if (duplicate) {
			toast.error('A view with that name already exists');
			return;
		}

		const updated = [...savedViews, { name, filters: { ...currentFilters } }];
		persistViews(updated);
		viewName = '';
		dialogOpen = false;
		toast.success(`View "${name}" saved`);
	}

	function handleApply(view: SavedView) {
		onApplyView({ ...view.filters });
	}

	function handleDelete(index: number, e: MouseEvent) {
		e.stopPropagation();
		const name = savedViews[index].name;
		const updated = savedViews.filter((_, i) => i !== index);
		persistViews(updated);
		toast.success(`View "${name}" deleted`);
	}

	function handleResetToDefault() {
		if (defaultFilters) {
			onApplyView({ ...defaultFilters });
			return;
		}
		const resetFilters: Record<string, any> = {};
		for (const key of Object.keys(currentFilters)) {
			resetFilters[key] = '';
		}
		onApplyView(resetFilters);
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter') {
			handleSave();
		}
	}

	// Initialize
	savedViews = loadViews();
</script>

{#if savedViews.length > 0 || !isDefault}
	<div class="flex flex-wrap items-center gap-2">
		<BookmarkIcon class="h-4 w-4 text-muted-foreground shrink-0" />

		<!-- Default (reset) pill -->
		<button
			class="inline-flex items-center gap-1 rounded-full border px-3 py-1 text-xs font-medium transition-colors {isDefault
				? 'bg-primary text-primary-foreground border-primary'
				: 'bg-background text-muted-foreground border-border hover:bg-muted hover:text-foreground'}"
			onclick={handleResetToDefault}
		>
			Default
		</button>

		<!-- Saved view pills -->
		{#each savedViews as view, i}
			<button
				class="group inline-flex items-center gap-1 rounded-full border px-3 py-1 text-xs font-medium transition-colors {activeViewIndex === i
					? 'bg-primary text-primary-foreground border-primary'
					: 'bg-background text-muted-foreground border-border hover:bg-muted hover:text-foreground'}"
				onclick={() => handleApply(view)}
			>
				{view.name}
				<span
					role="button"
					tabindex="0"
					class="ml-0.5 inline-flex items-center justify-center rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity {activeViewIndex === i
						? 'hover:bg-primary-foreground/20'
						: 'hover:bg-muted-foreground/20'}"
					onclick={(e) => handleDelete(i, e)}
					onkeydown={(e) => {
						if (e.key === 'Enter' || e.key === ' ') {
							e.preventDefault();
							handleDelete(i, e as unknown as MouseEvent);
						}
					}}
					aria-label="Delete view {view.name}"
				>
					<XIcon class="h-3 w-3" />
				</span>
			</button>
		{/each}

		<!-- Save current view button -->
		{#if !isDefault}
			<Dialog.Root bind:open={dialogOpen}>
				<Dialog.Trigger>
					{#snippet child({ props })}
						<button
							{...props}
							class="inline-flex items-center gap-1 rounded-full border border-dashed border-border px-3 py-1 text-xs font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
						>
							<BookmarkPlusIcon class="h-3 w-3" />
							Save view
						</button>
					{/snippet}
				</Dialog.Trigger>
				<Dialog.Content class="sm:max-w-xs">
					<Dialog.Header>
						<Dialog.Title>Save current view</Dialog.Title>
						<Dialog.Description>
							Give this filter configuration a name to quickly access it later.
						</Dialog.Description>
					</Dialog.Header>
					<div class="flex flex-col gap-3">
						<Input
							placeholder="e.g. Low stock electronics"
							bind:value={viewName}
							onkeydown={handleKeydown}
							autofocus
						/>
						<div class="flex justify-end gap-2">
							<Button variant="outline" size="sm" onclick={() => { dialogOpen = false; }}>
								Cancel
							</Button>
							<Button size="sm" onclick={handleSave} disabled={!viewName.trim()}>
								Save
							</Button>
						</div>
					</div>
				</Dialog.Content>
			</Dialog.Root>
		{/if}
	</div>
{/if}
