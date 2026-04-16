<script lang="ts">
	import type { Snippet } from 'svelte';
	import { fly } from 'svelte/transition';
	import { Button } from '$lib/components/ui/button';
	import XIcon from '@lucide/svelte/icons/x';

	interface Props {
		selectedCount: number;
		onClearSelection: () => void;
		children: Snippet;
	}

	let { selectedCount, onClearSelection, children }: Props = $props();
</script>

{#if selectedCount > 0}
	<div
		class="fixed bottom-6 left-1/2 z-50 -translate-x-1/2"
		transition:fly={{ y: 80, duration: 200 }}
	>
		<div
			class="flex items-center gap-3 rounded-lg border border-border/40 bg-zinc-900 px-4 py-2.5 text-white shadow-2xl dark:bg-zinc-800"
		>
			<span class="text-sm font-medium whitespace-nowrap">
				{selectedCount} selected
			</span>

			<div class="h-5 w-px bg-zinc-600"></div>

			<div class="flex items-center gap-2">
				{@render children()}
			</div>

			<div class="h-5 w-px bg-zinc-600"></div>

			<Button
				variant="ghost"
				size="sm"
				class="h-7 px-2 text-zinc-400 hover:text-white hover:bg-zinc-700"
				onclick={onClearSelection}
			>
				<XIcon class="h-4 w-4 mr-1" />
				Clear
			</Button>
		</div>
	</div>
{/if}
