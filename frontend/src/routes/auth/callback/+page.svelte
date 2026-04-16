<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { auth } from '$lib/stores/auth.svelte';
	import { onMount } from 'svelte';

	onMount(async () => {
		const url = new URL($page.url);
		const accessToken = url.searchParams.get('access_token');
		const refreshToken = url.searchParams.get('refresh_token');

		if (accessToken && refreshToken) {
			auth.handleCallback(accessToken, refreshToken);
			await auth.init();
			goto('/dashboard');
		} else {
			goto('/login');
		}
	});
</script>

<div class="flex min-h-screen items-center justify-center">
	<p class="text-muted-foreground">Signing in...</p>
</div>
