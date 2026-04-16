<script lang="ts">
	import { goto } from '$app/navigation';
	import { createProduct } from '$lib/api/products';
	import { Button } from '$lib/components/ui/button';
	import ProductForm from '$lib/components/ProductForm.svelte';
	import Breadcrumbs from '$lib/components/Breadcrumbs.svelte';
	import { toast } from 'svelte-sonner';

	async function handleSubmit(data: Record<string, any>) {
		await createProduct(data as any);
		toast.success('Product created');
		goto('/products');
	}
</script>

<div class="mx-auto max-w-2xl space-y-6">
	<Breadcrumbs items={[
		{ label: 'Home', href: '/dashboard' },
		{ label: 'Products', href: '/products' },
		{ label: 'New Product' }
	]} />

	<div>
		<h1 class="text-3xl font-bold tracking-tight">New Product</h1>
	</div>

	<ProductForm onSubmit={handleSubmit} submitLabel="Create Product" submittingLabel="Creating...">
		{#snippet actions()}
			<Button variant="outline" type="button" onclick={() => goto('/products')}>Cancel</Button>
		{/snippet}
	</ProductForm>
</div>
