<script lang="ts">
	import { onMount } from 'svelte';
	import type { Product } from '$lib/types/product';
	import type { Category } from '$lib/types/category';
	import type { Tag } from '$lib/types/tag';
	import { getFlatCategories } from '$lib/api/categories';
	import { getTags } from '$lib/api/tags';
	import { validateProduct, parseBackendErrors } from '$lib/validation/product';
	import type { ProductFormErrors } from '$lib/validation/product';
	import { uploadMedia, getMediaUrl, getProductMedia, type ProductMediaDto } from '$lib/api/media';
	import MediaUploader from '$lib/components/MediaUploader.svelte';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Textarea } from '$lib/components/ui/textarea';
	import * as Select from '$lib/components/ui/select';
	import * as Card from '$lib/components/ui/card';
	import { toast } from 'svelte-sonner';
	import { createUnsavedChanges } from '$lib/hooks/useUnsavedChanges.svelte';

	import type { Snippet } from 'svelte';

	interface Props {
		product?: Product;
		onSubmit: (data: Record<string, any>) => Promise<void>;
		submitLabel: string;
		submittingLabel: string;
		actions?: Snippet;
	}

	let { product, onSubmit, submitLabel, submittingLabel, actions }: Props = $props();

	let currencyLabel = $derived(product?.currency ?? '');

	// Unsaved changes tracking
	const unsaved = createUnsavedChanges();
	let formTouched = $state(false);

	$effect(() => {
		unsaved.setDirty(formTouched);
	});

	// General fields
	let name = $state('');
	let description = $state('');
	let shortDescription = $state('');
	let status = $state('DRAFT');
	let price = $state('');
	let compareAtPrice = $state('');
	let sku = $state('');
	let barcode = $state('');
	let trackInventory = $state(false);
	let quantity = $state('0');
	let saving = $state(false);
	let errors = $state<ProductFormErrors>({});

	function clearError(field: keyof ProductFormErrors) {
		if (errors[field]) {
			errors = { ...errors, [field]: undefined };
		}
	}

	// Categories & Tags
	let allCategories = $state<Category[]>([]);
	let allTags = $state<Tag[]>([]);
	let selectedCategoryIds = $state<Set<string>>(new Set());
	let selectedTagIds = $state<Set<string>>(new Set());

	// Media (for create mode — inline basic upload)
	let mediaItems = $state<{ url: string; altText: string; sortOrder: number }[]>([]);
	let uploading = $state(false);
	let dragOver = $state(false);
	let fileInput = $state<HTMLInputElement | null>(null);

	// Media (for edit mode — MediaUploader with product media API)
	let productMediaImages = $state<ProductMediaDto[]>([]);
	let isEditMode = $derived(!!product);
	let productMediaLoading = $state(false);

	// Variants (edit mode)
	let variants = $state<{
		id?: string;
		name: string;
		sku: string;
		price: string;
		quantity: string;
		active: boolean;
	}[]>([]);

	function initFromProduct(p: Product) {
		name = p.name;
		description = p.description || '';
		shortDescription = p.shortDescription || '';
		status = p.status;
		price = p.price != null ? String(p.price) : '';
		compareAtPrice = p.compareAtPrice != null ? String(p.compareAtPrice) : '';
		sku = p.sku || '';
		barcode = p.barcode || '';
		trackInventory = p.trackInventory;
		quantity = String(p.quantity || 0);
		selectedCategoryIds = new Set(p.categories.map((c) => c.id));
		selectedTagIds = new Set(p.tags.map((t) => t.id));
		mediaItems = p.media.map((m) => ({
			url: m.url,
			altText: m.altText || '',
			sortOrder: m.sortOrder
		}));
		variants = p.variants.map((v) => ({
			id: v.id,
			name: v.name,
			sku: v.sku || '',
			price: String(v.price),
			quantity: String(v.quantity || 0),
			active: v.active
		}));
	}

	onMount(async () => {
		try {
			const [cats, tags] = await Promise.all([getFlatCategories(), getTags()]);
			allCategories = cats;
			allTags = tags;
		} catch {
			toast.error('Failed to load categories or tags');
		}

		if (product) {
			initFromProduct(product);
			// Load product media via dedicated API for edit mode
			productMediaLoading = true;
			try {
				productMediaImages = await getProductMedia(product.id);
			} catch {
				// Fall back to product.media data already loaded by initFromProduct
			} finally {
				productMediaLoading = false;
			}
		}
	});

	function toggleCategory(id: string) {
		const next = new Set(selectedCategoryIds);
		if (next.has(id)) {
			next.delete(id);
		} else {
			next.add(id);
		}
		selectedCategoryIds = next;
	}

	function toggleTag(id: string) {
		const next = new Set(selectedTagIds);
		if (next.has(id)) {
			next.delete(id);
		} else {
			next.add(id);
		}
		selectedTagIds = next;
	}

	function addMediaRow() {
		mediaItems = [...mediaItems, { url: '', altText: '', sortOrder: mediaItems.length }];
	}

	function removeMediaRow(index: number) {
		mediaItems = mediaItems.filter((_, i) => i !== index);
	}

	async function handleFileUpload(files: FileList | null) {
		if (!files || files.length === 0) return;
		uploading = true;
		try {
			for (const file of Array.from(files)) {
				const result = await uploadMedia(file);
				mediaItems = [
					...mediaItems,
					{
						url: result.url,
						altText: '',
						sortOrder: mediaItems.length
					}
				];
			}
		} catch (err: any) {
			toast.error(err.message || 'File upload failed');
		} finally {
			uploading = false;
			if (fileInput) fileInput.value = '';
		}
	}

	function handleDrop(e: DragEvent) {
		e.preventDefault();
		dragOver = false;
		handleFileUpload(e.dataTransfer?.files ?? null);
	}

	function handleDragOver(e: DragEvent) {
		e.preventDefault();
		dragOver = true;
	}

	function handleDragLeave() {
		dragOver = false;
	}

	function addVariant() {
		variants = [...variants, { name: '', sku: '', price: '', quantity: '0', active: true }];
	}

	function removeVariant(index: number) {
		variants = variants.filter((_, i) => i !== index);
	}

	async function handleSubmit(e: SubmitEvent) {
		e.preventDefault();

		const formData = {
			name,
			description,
			shortDescription,
			status,
			price: price ? Number(price) : null,
			compareAtPrice: compareAtPrice ? Number(compareAtPrice) : null,
			sku: sku || null,
			barcode: barcode || null,
			trackInventory,
			quantity: trackInventory ? Number(quantity) : 0
		};

		const validation = validateProduct(formData);
		if (!validation.valid) {
			errors = validation.errors;
			return;
		}
		errors = {};

		saving = true;
		try {
			const data: Record<string, any> = {
				name,
				description,
				shortDescription,
				status,
				price: price ? Number(price) : undefined,
				compareAtPrice: compareAtPrice ? Number(compareAtPrice) : undefined,
				sku: sku || undefined,
				barcode: barcode || undefined,
				trackInventory,
				quantity: trackInventory ? Number(quantity) : 0,
				categoryIds: [...selectedCategoryIds],
				tagIds: [...selectedTagIds],
				media: mediaItems
					.filter((m) => m.url.trim())
					.map((m, i) => ({
						url: m.url,
						altText: m.altText || undefined,
						mediaType: 'IMAGE',
						sortOrder: i,
						primary: i === 0
					})),
				variants: variants.map((v, i) => ({
					name: v.name,
					sku: v.sku || undefined,
					price: v.price ? Number(v.price) : undefined,
					quantity: Number(v.quantity || 0),
					sortOrder: i,
					active: v.active
				}))
			};
			await onSubmit(data);
			formTouched = false;
		} catch (err: any) {
			if (err.status === 400 && err.body) {
				const backendErrors = parseBackendErrors(err.body);
				if (Object.keys(backendErrors).length > 0) {
					errors = backendErrors;
					return;
				}
			}
			toast.error(err.message || 'Operation failed');
		} finally {
			saving = false;
		}
	}
</script>

<form onsubmit={handleSubmit} oninput={() => { formTouched = true; }} onchange={() => { formTouched = true; }} class="space-y-6">
	<!-- General Information -->
	<Card.Root>
		<Card.Header>
			<Card.Title>General Information</Card.Title>
		</Card.Header>
		<Card.Content class="space-y-4">
			<div class="space-y-2">
				<Label for="name">Product Name <span class="text-destructive">*</span></Label>
				<Input id="name" bind:value={name} placeholder="Enter product name" oninput={() => clearError('name')} />
				{#if errors.name}<p class="text-sm text-destructive">{errors.name}</p>{/if}
			</div>
			<div class="space-y-2">
				<Label for="shortDescription">Short Description</Label>
				<Input id="shortDescription" bind:value={shortDescription} placeholder="Brief summary" oninput={() => clearError('shortDescription')} />
				{#if errors.shortDescription}<p class="text-sm text-destructive">{errors.shortDescription}</p>{/if}
			</div>
			<div class="space-y-2">
				<Label for="description">Description</Label>
				<Textarea
					id="description"
					bind:value={description}
					placeholder="Full product description"
					rows={4}
					oninput={() => clearError('description')}
				/>
				{#if errors.description}<p class="text-sm text-destructive">{errors.description}</p>{/if}
			</div>
			<div class="space-y-2">
				<Label>Status</Label>
				<Select.Root type="single" value={status} onValueChange={(v) => (status = v)}>
					<Select.Trigger class="w-full">
						{status}
					</Select.Trigger>
					<Select.Content>
						<Select.Item value="DRAFT">Draft</Select.Item>
						<Select.Item value="ACTIVE">Active</Select.Item>
						<Select.Item value="ARCHIVED">Archived</Select.Item>
					</Select.Content>
				</Select.Root>
			</div>
		</Card.Content>
	</Card.Root>

	<!-- Pricing -->
	<Card.Root>
		<Card.Header>
			<Card.Title>Pricing</Card.Title>
		</Card.Header>
		<Card.Content class="space-y-4">
			<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
				<div class="space-y-2">
					<Label for="price">Price{currencyLabel ? ` (${currencyLabel})` : ''}</Label>
					<Input id="price" type="number" step="0.01" bind:value={price} placeholder="0.00" oninput={() => clearError('price')} />
					{#if errors.price}<p class="text-sm text-destructive">{errors.price}</p>{/if}
				</div>
				<div class="space-y-2">
					<Label for="compareAtPrice">Compare at Price</Label>
					<Input
						id="compareAtPrice"
						type="number"
						step="0.01"
						bind:value={compareAtPrice}
						placeholder="0.00"
						oninput={() => clearError('compareAtPrice')}
					/>
					{#if errors.compareAtPrice}<p class="text-sm text-destructive">{errors.compareAtPrice}</p>{/if}
				</div>
			</div>
		</Card.Content>
	</Card.Root>

	<!-- Inventory & Identification -->
	<Card.Root>
		<Card.Header>
			<Card.Title>Inventory & Identification</Card.Title>
		</Card.Header>
		<Card.Content class="space-y-4">
			<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
				<div class="space-y-2">
					<Label for="sku">SKU</Label>
					<Input id="sku" bind:value={sku} placeholder="SKU-001" oninput={() => clearError('sku')} />
					{#if errors.sku}<p class="text-sm text-destructive">{errors.sku}</p>{/if}
				</div>
				<div class="space-y-2">
					<Label for="barcode">Barcode</Label>
					<Input id="barcode" bind:value={barcode} placeholder="EAN / UPC" oninput={() => clearError('barcode')} />
					{#if errors.barcode}<p class="text-sm text-destructive">{errors.barcode}</p>{/if}
				</div>
			</div>
			<div class="flex items-center gap-2">
				<input
					type="checkbox"
					id="trackInventory"
					bind:checked={trackInventory}
					class="rounded"
				/>
				<Label for="trackInventory">Track inventory</Label>
			</div>
			{#if trackInventory}
				<div class="space-y-2">
					<Label for="quantity">Quantity</Label>
					<Input id="quantity" type="number" bind:value={quantity} oninput={() => clearError('quantity')} />
					{#if errors.quantity}<p class="text-sm text-destructive">{errors.quantity}</p>{/if}
				</div>
			{/if}
		</Card.Content>
	</Card.Root>

	<!-- Categories -->
	<Card.Root>
		<Card.Header>
			<Card.Title>Categories</Card.Title>
		</Card.Header>
		<Card.Content>
			{#if allCategories.length === 0}
				<p class="text-sm text-muted-foreground">No categories available.</p>
			{:else}
				<div class="grid grid-cols-1 gap-2 sm:grid-cols-2 md:grid-cols-3">
					{#each allCategories as cat}
						<label
							class="flex cursor-pointer items-center gap-2 rounded-md border p-2 transition-colors hover:bg-accent"
							class:bg-accent={selectedCategoryIds.has(cat.id)}
							class:border-primary={selectedCategoryIds.has(cat.id)}
						>
							<input
								type="checkbox"
								checked={selectedCategoryIds.has(cat.id)}
								onchange={() => toggleCategory(cat.id)}
								class="rounded"
							/>
							<span class="text-sm">
								{#if cat.path}
									{cat.path}
								{:else}
									{cat.name}
								{/if}
							</span>
						</label>
					{/each}
				</div>
			{/if}
		</Card.Content>
	</Card.Root>

	<!-- Tags -->
	<Card.Root>
		<Card.Header>
			<Card.Title>Tags</Card.Title>
		</Card.Header>
		<Card.Content>
			{#if allTags.length === 0}
				<p class="text-sm text-muted-foreground">No tags available.</p>
			{:else}
				<div class="flex flex-wrap gap-2">
					{#each allTags as tag}
						<button
							type="button"
							onclick={() => toggleTag(tag.id)}
							class="inline-flex items-center rounded-full border px-3 py-1 text-sm transition-colors"
							class:bg-primary={selectedTagIds.has(tag.id)}
							class:text-primary-foreground={selectedTagIds.has(tag.id)}
							class:border-primary={selectedTagIds.has(tag.id)}
							class:hover:bg-accent={!selectedTagIds.has(tag.id)}
						>
							{tag.name}
						</button>
					{/each}
				</div>
			{/if}
		</Card.Content>
	</Card.Root>

	<!-- Media -->
	<Card.Root>
		<Card.Header>
			<Card.Title>Media</Card.Title>
		</Card.Header>
		<Card.Content>
			{#if isEditMode}
				<!-- Edit mode: full MediaUploader with product media API -->
				{#if productMediaLoading}
					<div class="flex items-center justify-center py-8">
						<p class="text-sm text-muted-foreground">Loading images...</p>
					</div>
				{:else}
					<MediaUploader
						productId={product!.id}
						bind:images={productMediaImages}
					/>
				{/if}
			{:else}
				<!-- Create mode: basic upload (media linked after product creation) -->
				<div class="space-y-4">
					<div class="flex gap-2">
						<Button type="button" variant="outline" size="sm" onclick={() => fileInput?.click()} disabled={uploading}>
							{uploading ? 'Uploading...' : 'Upload Image'}
						</Button>
						<Button type="button" variant="outline" size="sm" onclick={addMediaRow}>
							Add URL
						</Button>
					</div>

					<input
						bind:this={fileInput}
						type="file"
						accept="image/jpeg,image/png,image/gif,image/webp"
						multiple
						class="hidden"
						onchange={(e) => handleFileUpload((e.target as HTMLInputElement).files)}
					/>

					<!-- svelte-ignore a11y_no_static_element_interactions -->
					<div
						class="rounded-lg border-2 border-dashed p-6 text-center transition-colors {dragOver ? 'border-primary bg-primary/5' : 'border-muted-foreground/25'}"
						ondrop={handleDrop}
						ondragover={handleDragOver}
						ondragleave={handleDragLeave}
						role="region"
						aria-label="Drop zone for image uploads"
					>
						{#if uploading}
							<p class="text-sm text-muted-foreground">Uploading...</p>
						{:else}
							<p class="text-sm text-muted-foreground">
								Drag and drop images here, or click "Upload Image"
							</p>
							<p class="text-xs text-muted-foreground mt-1">
								JPEG, PNG, GIF, WebP — Max 10MB
							</p>
						{/if}
					</div>

					{#if mediaItems.length === 0}
						<p class="text-sm text-muted-foreground">No media added yet.</p>
					{:else}
						<div class="space-y-3">
							{#each mediaItems as item, i}
								<div class="flex items-start gap-3">
									{#if item.url}
										<div class="h-16 w-16 flex-shrink-0 overflow-hidden rounded-md border bg-muted">
											<img
												src={item.url.startsWith('/api/') ? getMediaUrl(item.url) : item.url}
												alt={item.altText || 'Preview'}
												class="h-full w-full object-cover"
												onerror={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
											/>
										</div>
									{/if}
									<div class="flex-1 space-y-2">
										<Input bind:value={item.url} placeholder="Image URL" />
										<Input bind:value={item.altText} placeholder="Alt text (optional)" />
									</div>
									<Button
										type="button"
										variant="ghost"
										size="sm"
										onclick={() => removeMediaRow(i)}
										class="mt-1 text-destructive hover:text-destructive"
									>
										Remove
									</Button>
								</div>
							{/each}
						</div>
					{/if}
				</div>
			{/if}
		</Card.Content>
	</Card.Root>

	<!-- Variants -->
	<Card.Root>
		<Card.Header>
			<div class="flex items-center justify-between">
				<Card.Title>Variants</Card.Title>
				<Button type="button" variant="outline" size="sm" onclick={addVariant}>
					Add Variant
				</Button>
			</div>
		</Card.Header>
		<Card.Content>
			{#if variants.length === 0}
				<p class="text-sm text-muted-foreground">No variants. Click "Add Variant" to add product variants.</p>
			{:else}
				<div class="space-y-4">
					{#each variants as variant, i}
						<div class="rounded-md border p-3 space-y-3">
							<div class="flex items-center justify-between">
								<span class="text-sm font-medium">Variant {i + 1}</span>
								<Button
									type="button"
									variant="ghost"
									size="sm"
									onclick={() => removeVariant(i)}
									class="text-destructive hover:text-destructive"
								>
									Remove
								</Button>
							</div>
							<div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
								<div class="space-y-1">
									<Label>Name *</Label>
									<Input bind:value={variant.name} placeholder="Variant name" />
								</div>
								<div class="space-y-1">
									<Label>SKU</Label>
									<Input bind:value={variant.sku} placeholder="SKU" />
								</div>
								<div class="space-y-1">
									<Label>Price{currencyLabel ? ` (${currencyLabel})` : ''}</Label>
									<Input type="number" step="0.01" bind:value={variant.price} placeholder="0.00" />
								</div>
								<div class="space-y-1">
									<Label>Quantity</Label>
									<Input type="number" bind:value={variant.quantity} placeholder="0" />
								</div>
							</div>
							<div class="flex items-center gap-2">
								<input type="checkbox" bind:checked={variant.active} class="rounded" />
								<Label>Active</Label>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</Card.Content>
	</Card.Root>

	<div class="flex justify-end gap-3">
		{#if actions}{@render actions()}{/if}
		<Button type="submit" disabled={saving}>
			{saving ? submittingLabel : submitLabel}
		</Button>
	</div>
</form>
