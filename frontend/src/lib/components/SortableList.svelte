<script lang="ts">
	import GripVerticalIcon from '@lucide/svelte/icons/grip-vertical';

	interface Props {
		items: any[];
		onReorder: (items: any[]) => void;
		disabled?: boolean;
		children: import('svelte').Snippet<[{ item: any; index: number }]>;
	}

	let { items, onReorder, disabled = false, children }: Props = $props();

	let dragIndex = $state<number | null>(null);
	let overIndex = $state<number | null>(null);
	let dropPosition = $state<'before' | 'after' | null>(null);

	function handleDragStart(e: DragEvent, index: number) {
		if (disabled) return;
		dragIndex = index;
		if (e.dataTransfer) {
			e.dataTransfer.effectAllowed = 'move';
			e.dataTransfer.setData('text/plain', String(index));
		}
	}

	function handleDragOver(e: DragEvent, index: number) {
		e.preventDefault();
		if (disabled || dragIndex === null || dragIndex === index) {
			overIndex = null;
			dropPosition = null;
			return;
		}

		if (e.dataTransfer) {
			e.dataTransfer.dropEffect = 'move';
		}

		const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
		const midY = rect.top + rect.height / 2;
		overIndex = index;
		dropPosition = e.clientY < midY ? 'before' : 'after';
	}

	function handleDragLeave(e: DragEvent) {
		const relatedTarget = e.relatedTarget as HTMLElement | null;
		const currentTarget = e.currentTarget as HTMLElement;
		if (!relatedTarget || !currentTarget.contains(relatedTarget)) {
			overIndex = null;
			dropPosition = null;
		}
	}

	function handleDrop(e: DragEvent, index: number) {
		e.preventDefault();
		if (disabled || dragIndex === null || dragIndex === index) {
			cleanup();
			return;
		}

		const reordered = [...items];
		const [moved] = reordered.splice(dragIndex, 1);

		let targetIndex = index;
		if (dragIndex < index) {
			targetIndex = dropPosition === 'before' ? index - 1 : index;
		} else {
			targetIndex = dropPosition === 'before' ? index : index + 1;
		}

		reordered.splice(targetIndex, 0, moved);
		onReorder(reordered);
		cleanup();
	}

	function handleDragEnd() {
		cleanup();
	}

	function cleanup() {
		dragIndex = null;
		overIndex = null;
		dropPosition = null;
	}
</script>

<div class="sortable-list space-y-1" role="list">
	{#each items as item, index (item.id ?? index)}
		<div
			class="sortable-item relative"
			class:sortable-item--dragging={dragIndex === index}
			class:sortable-item--indicator-before={overIndex === index && dropPosition === 'before'}
			class:sortable-item--indicator-after={overIndex === index && dropPosition === 'after'}
			role="listitem"
		>
			<!-- svelte-ignore a11y_no_static_element_interactions -->
			<div
				class="flex items-center gap-2 rounded-md border bg-card transition-all
					{dragIndex === index ? 'opacity-50 border-dashed border-primary/50' : 'shadow-sm'}
					{disabled ? '' : 'hover:shadow-md'}"
				draggable={!disabled}
				ondragstart={(e) => handleDragStart(e, index)}
				ondragover={(e) => handleDragOver(e, index)}
				ondragleave={handleDragLeave}
				ondrop={(e) => handleDrop(e, index)}
				ondragend={handleDragEnd}
			>
				{#if !disabled}
					<div
						class="flex-shrink-0 cursor-grab active:cursor-grabbing p-2 text-muted-foreground/50 hover:text-muted-foreground transition-colors"
						aria-label="Drag to reorder"
					>
						<GripVerticalIcon class="h-5 w-5" />
					</div>
				{/if}
				<div class="flex-1 min-w-0">
					{@render children({ item, index })}
				</div>
			</div>
		</div>
	{/each}
</div>

<style>
	.sortable-item--indicator-before::before {
		content: '';
		position: absolute;
		top: -3px;
		left: 0;
		right: 0;
		height: 3px;
		background-color: var(--color-primary, #6366f1);
		border-radius: 2px;
		z-index: 10;
	}

	.sortable-item--indicator-after::after {
		content: '';
		position: absolute;
		bottom: -3px;
		left: 0;
		right: 0;
		height: 3px;
		background-color: var(--color-primary, #6366f1);
		border-radius: 2px;
		z-index: 10;
	}
</style>
