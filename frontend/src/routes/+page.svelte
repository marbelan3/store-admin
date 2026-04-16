<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';

	onMount(() => {
		const token = localStorage.getItem('access_token');
		if (token) {
			goto('/dashboard');
		} else {
			goto('/login');
		}
	});
</script>
