<script lang="ts">
	import type { Snippet } from 'svelte';
	import Breadcrumbs from '$lib/components/Breadcrumbs.svelte';

	interface BreadcrumbItem {
		label: string;
		href?: string;
	}

	interface Props {
		title: string;
		description?: string;
		breadcrumbs: BreadcrumbItem[];
		children?: Snippet;
	}

	let { title, description, breadcrumbs, children }: Props = $props();
</script>

<div class="mb-6 space-y-4">
	<Breadcrumbs items={breadcrumbs} />

	<div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
		<div>
			<h1 class="text-2xl font-semibold tracking-tight">{title}</h1>
			{#if description}
				<p class="text-sm text-muted-foreground">{description}</p>
			{/if}
		</div>
		{#if children}
			<div class="flex flex-wrap items-center gap-2">
				{@render children()}
			</div>
		{/if}
	</div>
</div>
