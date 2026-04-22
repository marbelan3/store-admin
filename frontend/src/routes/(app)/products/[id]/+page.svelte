<script lang="ts">
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { getProduct, updateProduct } from '$lib/api/products';
	import { getOptions, getVariants } from '$lib/api/variants';
	import { auth } from '$lib/stores/auth.svelte';
	import type { Product, ProductOption, ProductVariantDetail } from '$lib/types/product';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { Badge } from '$lib/components/ui/badge';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import ProductForm from '$lib/components/ProductForm.svelte';
	import ProductOptions from '$lib/components/ProductOptions.svelte';
	import ProductVariants from '$lib/components/ProductVariants.svelte';
	import ActivityTimeline from '$lib/components/ActivityTimeline.svelte';
	import Breadcrumbs from '$lib/components/Breadcrumbs.svelte';
	import { toast } from 'svelte-sonner';
	import ClockIcon from '@lucide/svelte/icons/clock';

	let product = $state<Product | null>(null);
	let productOptions = $state<ProductOption[]>([]);
	let productVariants = $state<ProductVariantDetail[]>([]);
	let loading = $state(true);

	onMount(async () => {
		try {
			const id = $page.params.id!;
			const [prod, opts, vars] = await Promise.all([
				getProduct(id),
				getOptions(id),
				getVariants(id)
			]);
			product = prod;
			productOptions = opts;
			productVariants = vars;
		} catch {
			toast.error('Product not found');
			goto('/products');
		} finally {
			loading = false;
		}
	});

	async function handleSubmit(data: Record<string, any>) {
		if (!product) return;
		await updateProduct(product.id, data as any);
		toast.success('Product updated');
	}

	function handleOptionsChange(opts: ProductOption[]) {
		productOptions = opts;
		// Reload variants when options change as some may have been deleted
		if (product) {
			getVariants(product.id).then((vars) => {
				productVariants = vars;
			}).catch(() => {
				// keep current
			});
		}
	}
</script>

{#if loading}
	<div class="mx-auto max-w-2xl space-y-6">
		<Breadcrumbs items={[
			{ label: 'Home', href: '/dashboard' },
			{ label: 'Products', href: '/products' },
			{ label: 'Loading...' }
		]} />
		<div class="flex items-center justify-between">
			<div>
				<Skeleton class="h-9 w-48 mb-2" />
				<div class="mt-1 flex items-center gap-2">
					<Skeleton class="h-5 w-16 rounded-full" />
					<Skeleton class="h-4 w-32" />
				</div>
			</div>
			<Skeleton class="h-8 w-16" />
		</div>
		<div class="space-y-6">
			{#each Array(4) as _}
				<Card.Root>
					<Card.Header>
						<Skeleton class="h-5 w-40" />
					</Card.Header>
					<Card.Content class="space-y-4">
						<Skeleton class="h-4 w-full" />
						<Skeleton class="h-4 w-3/4" />
						<Skeleton class="h-10 w-full" />
					</Card.Content>
				</Card.Root>
			{/each}
		</div>
	</div>
{:else if product}
	<div class="mx-auto max-w-2xl space-y-6">
		<Breadcrumbs items={[
			{ label: 'Home', href: '/dashboard' },
			{ label: 'Products', href: '/products' },
			{ label: product.name }
		]} />

		<div class="flex items-center justify-between">
			<div>
				<h1 class="text-3xl font-bold tracking-tight">{product.name}</h1>
				<div class="mt-1 flex items-center gap-2">
					<Badge>{product.status}</Badge>
					<span class="text-sm text-muted-foreground"
						>Created {new Date(product.createdAt).toLocaleDateString()}</span
					>
				</div>
			</div>
			<Button variant="outline" onclick={() => goto('/products')}>Back</Button>
		</div>

		{#if auth.canEdit}
			<ProductForm
				{product}
				onSubmit={handleSubmit}
				submitLabel="Save Changes"
				submittingLabel="Saving..."
			>
				{#snippet actions()}
					<Button variant="outline" type="button" onclick={() => goto('/products')}>Cancel</Button>
				{/snippet}
			</ProductForm>
		{:else}
			<!-- Read-only view for TENANT_VIEWER -->
			<Card.Root>
				<Card.Content class="space-y-2 pt-6">
					<p><strong>Description:</strong> {product.description || '---'}</p>
					<p><strong>Price:</strong> {product.price ?? '---'} {product.currency}</p>
					<p><strong>SKU:</strong> {product.sku || '---'}</p>
					<p><strong>Barcode:</strong> {product.barcode || '---'}</p>
					<p>
						<strong>Inventory:</strong>
						{product.trackInventory ? product.quantity : 'Not tracked'}
					</p>
				</Card.Content>
			</Card.Root>
		{/if}

		<!-- Product Options & Variants -->
		<ProductOptions
			productId={product.id}
			bind:options={productOptions}
			readonly={!auth.canEdit}
			onOptionsChange={handleOptionsChange}
		/>

		<ProductVariants
			productId={product.id}
			bind:variants={productVariants}
			options={productOptions}
			currency={product.currency}
			readonly={!auth.canEdit}
		/>

		<!-- Activity Timeline -->
		<Card.Root class="shadow-sm">
			<Card.Header>
				<Card.Title class="flex items-center gap-2">
					<ClockIcon class="h-4 w-4" />
					Activity
				</Card.Title>
			</Card.Header>
			<Card.Content>
				<ActivityTimeline entityType="PRODUCT" entityId={product.id} />
			</Card.Content>
		</Card.Root>
	</div>
{/if}
