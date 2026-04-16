<script lang="ts">
	import { Button } from '$lib/components/ui/button';
	import type { Component } from 'svelte';

	interface EmptyStateProps {
		icon: Component<{ class?: string }>;
		title: string;
		description: string;
		actionLabel?: string;
		actionHref?: string;
		onAction?: () => void;
	}

	let { icon: Icon, title, description, actionLabel, actionHref, onAction }: EmptyStateProps = $props();
</script>

<div class="flex flex-col items-center justify-center py-16 px-4">
	<div class="rounded-full bg-muted p-4 mb-4">
		<Icon class="h-8 w-8 text-muted-foreground" />
	</div>
	<h3 class="text-lg font-semibold mb-1">{title}</h3>
	<p class="text-sm text-muted-foreground mb-4 text-center max-w-sm">{description}</p>
	{#if actionLabel && actionHref}
		<Button href={actionHref}>{actionLabel}</Button>
	{:else if actionLabel && onAction}
		<Button onclick={onAction}>{actionLabel}</Button>
	{/if}
</div>
