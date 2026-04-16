<script lang="ts">
	import { auth } from '$lib/stores/auth.svelte';
	import type { Snippet } from 'svelte';

	interface Props {
		roles?: string[];
		requireEdit?: boolean;
		children: Snippet;
		fallback?: Snippet;
	}

	let { roles, requireEdit = false, children, fallback }: Props = $props();

	function hasAccess(): boolean {
		if (!auth.user) return false;
		if (requireEdit && !auth.canEdit) return false;
		if (roles && roles.length > 0 && !roles.includes(auth.user.role)) return false;
		return true;
	}
</script>

{#if hasAccess()}
	{@render children()}
{:else if fallback}
	{@render fallback()}
{/if}
