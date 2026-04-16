<script lang="ts">
	import { toast } from 'svelte-sonner';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Badge } from '$lib/components/ui/badge';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import {
		uploadMedia,
		getMediaUrl,
		linkMediaToProduct,
		unlinkMediaFromProduct,
		reorderProductMedia,
		updateMediaAltText,
		type ProductMediaDto
	} from '$lib/api/media';
	import UploadIcon from '@lucide/svelte/icons/upload';
	import XIcon from '@lucide/svelte/icons/x';
	import ChevronUpIcon from '@lucide/svelte/icons/chevron-up';
	import ChevronDownIcon from '@lucide/svelte/icons/chevron-down';
	import LoaderIcon from '@lucide/svelte/icons/loader-circle';
	import ImageIcon from '@lucide/svelte/icons/image';
	import GripVerticalIcon from '@lucide/svelte/icons/grip-vertical';

	const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
	const MAX_IMAGES = 20;

	interface Props {
		productId: string;
		images?: ProductMediaDto[];
		readonly?: boolean;
		onImagesChange?: (images: ProductMediaDto[]) => void;
	}

	let {
		productId,
		images = $bindable([]),
		readonly = false,
		onImagesChange
	}: Props = $props();

	// Upload state
	let dragOver = $state(false);
	let uploading = $state(false);
	let uploadQueue = $state<{ file: File; preview: string; progress: boolean }[]>([]);
	let fileInput = $state<HTMLInputElement | null>(null);

	// Delete confirm state
	let confirmOpen = $state(false);
	let deleteTarget = $state<ProductMediaDto | null>(null);

	// Alt text editing
	let editingAltTextId = $state<string | null>(null);
	let altTextValue = $state('');

	// Drag-and-drop reorder state
	let dragReorderIndex = $state<number | null>(null);
	let dragOverIndex = $state<number | null>(null);
	let dragDropPosition = $state<'before' | 'after' | null>(null);

	let canUpload = $derived(!readonly && images.length + uploadQueue.length < MAX_IMAGES);

	function validateFile(file: File): string | null {
		if (!file.type.startsWith('image/')) {
			return `"${file.name}" is not an image file`;
		}
		if (file.size > MAX_FILE_SIZE) {
			return `"${file.name}" exceeds the 10MB limit`;
		}
		return null;
	}

	async function processFiles(files: FileList | File[]) {
		const fileArray = Array.from(files);
		const remaining = MAX_IMAGES - images.length - uploadQueue.length;

		if (fileArray.length > remaining) {
			toast.error(`You can upload at most ${remaining} more image${remaining === 1 ? '' : 's'}`);
			return;
		}

		// Validate all files first
		for (const file of fileArray) {
			const error = validateFile(file);
			if (error) {
				toast.error(error);
				return;
			}
		}

		// Create previews and add to queue
		const newItems: { file: File; preview: string; progress: boolean }[] = [];
		for (const file of fileArray) {
			const preview = await readFileAsDataUrl(file);
			newItems.push({ file, preview, progress: true });
		}
		uploadQueue = [...uploadQueue, ...newItems];
		uploading = true;

		// Upload sequentially
		for (const item of newItems) {
			try {
				const mediaDto = await uploadMedia(item.file);
				const linked = await linkMediaToProduct(productId, mediaDto.id);
				images = [...images, linked];
				onImagesChange?.(images);
			} catch (err: any) {
				toast.error(err.message || `Failed to upload ${item.file.name}`);
			} finally {
				uploadQueue = uploadQueue.filter((q) => q !== item);
			}
		}

		uploading = false;
		if (fileInput) fileInput.value = '';
	}

	function readFileAsDataUrl(file: File): Promise<string> {
		return new Promise((resolve) => {
			const reader = new FileReader();
			reader.onload = () => resolve(reader.result as string);
			reader.readAsDataURL(file);
		});
	}

	function handleDrop(e: DragEvent) {
		e.preventDefault();
		dragOver = false;
		if (readonly) return;
		const files = e.dataTransfer?.files;
		if (files && files.length > 0) {
			processFiles(files);
		}
	}

	function handleDragOver(e: DragEvent) {
		e.preventDefault();
		if (!readonly) dragOver = true;
	}

	function handleDragLeave() {
		dragOver = false;
	}

	function handleFileSelect(e: Event) {
		const input = e.target as HTMLInputElement;
		if (input.files && input.files.length > 0) {
			processFiles(input.files);
		}
	}

	function confirmDelete(img: ProductMediaDto) {
		deleteTarget = img;
		confirmOpen = true;
	}

	async function handleDelete() {
		if (!deleteTarget) return;
		try {
			await unlinkMediaFromProduct(productId, deleteTarget.mediaId);
			images = images.filter((i) => i.id !== deleteTarget!.id);
			onImagesChange?.(images);
			toast.success('Image removed');
		} catch (err: any) {
			toast.error(err.message || 'Failed to remove image');
		}
	}

	async function moveImage(index: number, direction: 'up' | 'down') {
		const newIndex = direction === 'up' ? index - 1 : index + 1;
		if (newIndex < 0 || newIndex >= images.length) return;

		const reordered = [...images];
		const [moved] = reordered.splice(index, 1);
		reordered.splice(newIndex, 0, moved);

		// Update sort orders locally
		images = reordered.map((img, i) => ({ ...img, sortOrder: i, primary: i === 0 }));
		onImagesChange?.(images);

		try {
			await reorderProductMedia(
				productId,
				images.map((img) => img.mediaId)
			);
		} catch (err: any) {
			toast.error('Failed to reorder images');
		}
	}

	function startEditAltText(img: ProductMediaDto) {
		editingAltTextId = img.id;
		altTextValue = img.altText || '';
	}

	async function saveAltText(img: ProductMediaDto) {
		editingAltTextId = null;
		if (altTextValue === (img.altText || '')) return;

		try {
			await updateMediaAltText(img.mediaId, altTextValue);
			images = images.map((i) =>
				i.id === img.id ? { ...i, altText: altTextValue } : i
			);
			onImagesChange?.(images);
		} catch (err: any) {
			toast.error('Failed to update alt text');
		}
	}

	function handleAltTextKeydown(e: KeyboardEvent, img: ProductMediaDto) {
		if (e.key === 'Enter') {
			e.preventDefault();
			saveAltText(img);
		} else if (e.key === 'Escape') {
			editingAltTextId = null;
		}
	}

	function handleMediaDragStart(e: DragEvent, index: number) {
		if (readonly) return;
		dragReorderIndex = index;
		if (e.dataTransfer) {
			e.dataTransfer.effectAllowed = 'move';
			e.dataTransfer.setData('text/plain', String(index));
		}
	}

	function handleMediaDragOver(e: DragEvent, index: number) {
		e.preventDefault();
		if (readonly || dragReorderIndex === null || dragReorderIndex === index) {
			dragOverIndex = null;
			dragDropPosition = null;
			return;
		}
		if (e.dataTransfer) e.dataTransfer.dropEffect = 'move';
		dragOverIndex = index;
		const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
		const midX = rect.left + rect.width / 2;
		dragDropPosition = e.clientX < midX ? 'before' : 'after';
	}

	function handleMediaDragLeave(e: DragEvent) {
		const related = e.relatedTarget as HTMLElement | null;
		const current = e.currentTarget as HTMLElement;
		if (!related || !current.contains(related)) {
			dragOverIndex = null;
			dragDropPosition = null;
		}
	}

	async function handleMediaDrop(e: DragEvent, index: number) {
		e.preventDefault();
		if (readonly || dragReorderIndex === null || dragReorderIndex === index) {
			cleanupMediaDrag();
			return;
		}

		const reordered = [...images];
		const [moved] = reordered.splice(dragReorderIndex, 1);
		let targetIndex = index;
		if (dragReorderIndex < index) {
			targetIndex = dragDropPosition === 'before' ? index - 1 : index;
		} else {
			targetIndex = dragDropPosition === 'before' ? index : index + 1;
		}
		reordered.splice(targetIndex, 0, moved);

		images = reordered.map((img, i) => ({ ...img, sortOrder: i, primary: i === 0 }));
		onImagesChange?.(images);
		cleanupMediaDrag();

		try {
			await reorderProductMedia(
				productId,
				images.map((img) => img.mediaId)
			);
		} catch {
			toast.error('Failed to reorder images');
		}
	}

	function handleMediaDragEnd() {
		cleanupMediaDrag();
	}

	function cleanupMediaDrag() {
		dragReorderIndex = null;
		dragOverIndex = null;
		dragDropPosition = null;
	}
</script>

<ConfirmDialog
	bind:open={confirmOpen}
	title="Remove image"
	description="Are you sure you want to remove this image from the product?"
	confirmLabel="Remove"
	variant="destructive"
	onConfirm={handleDelete}
/>

<div class="space-y-4">
	<!-- Drop zone -->
	{#if canUpload}
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<div
			class="group relative rounded-lg border-2 border-dashed p-8 text-center transition-all cursor-pointer
				{dragOver
					? 'border-primary bg-primary/5 scale-[1.01]'
					: 'border-muted-foreground/25 hover:border-muted-foreground/50 hover:bg-muted/30'}"
			ondrop={handleDrop}
			ondragover={handleDragOver}
			ondragleave={handleDragLeave}
			onclick={() => !readonly && fileInput?.click()}
			role="button"
			tabindex="0"
			aria-label="Upload images"
			onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fileInput?.click(); } }}
		>
			<div class="flex flex-col items-center gap-2">
				<div class="rounded-full bg-muted p-3 transition-colors group-hover:bg-muted-foreground/10">
					<UploadIcon class="h-6 w-6 text-muted-foreground" />
				</div>
				<div>
					<p class="text-sm font-medium text-foreground">
						Drag and drop images here, or click to browse
					</p>
					<p class="mt-1 text-xs text-muted-foreground">
						JPEG, PNG, GIF, WebP — Max 10MB per file — {images.length}/{MAX_IMAGES} images
					</p>
				</div>
			</div>
		</div>

		<input
			bind:this={fileInput}
			type="file"
			accept="image/*"
			multiple
			class="hidden"
			onchange={handleFileSelect}
		/>
	{:else if !readonly}
		<div class="rounded-lg border border-dashed p-4 text-center">
			<p class="text-sm text-muted-foreground">
				Maximum of {MAX_IMAGES} images reached
			</p>
		</div>
	{/if}

	<!-- Upload queue (in-progress) -->
	{#if uploadQueue.length > 0}
		<div class="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
			{#each uploadQueue as item}
				<div class="relative aspect-square overflow-hidden rounded-lg border bg-muted">
					<img
						src={item.preview}
						alt="Uploading"
						class="h-full w-full object-cover opacity-50"
					/>
					<div class="absolute inset-0 flex items-center justify-center">
						<LoaderIcon class="h-6 w-6 animate-spin text-primary" />
					</div>
				</div>
			{/each}
		</div>
	{/if}

	<!-- Image grid -->
	{#if images.length === 0 && uploadQueue.length === 0}
		<div class="flex flex-col items-center justify-center rounded-lg border border-dashed py-12 text-center">
			<ImageIcon class="mb-3 h-10 w-10 text-muted-foreground/50" />
			<p class="text-sm text-muted-foreground">No images uploaded yet</p>
		</div>
	{:else if images.length > 0}
		<div class="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
			{#each images as img, index}
				<div
					class="group/card relative rounded-lg border bg-card shadow-sm transition-shadow hover:shadow-md
						{dragReorderIndex === index ? 'opacity-50 border-dashed border-primary/50' : ''}
						{dragOverIndex === index && dragDropPosition === 'before' ? 'ring-l-2 ring-primary' : ''}
						{dragOverIndex === index && dragDropPosition === 'after' ? 'ring-r-2 ring-primary' : ''}"
					draggable={!readonly && images.length > 1}
					ondragstart={(e) => handleMediaDragStart(e, index)}
					ondragover={(e) => handleMediaDragOver(e, index)}
					ondragleave={handleMediaDragLeave}
					ondrop={(e) => handleMediaDrop(e, index)}
					ondragend={handleMediaDragEnd}
					role="listitem"
				>
					{#if dragOverIndex === index && dragDropPosition === 'before'}
						<div class="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-l-lg z-10"></div>
					{/if}
					{#if dragOverIndex === index && dragDropPosition === 'after'}
						<div class="absolute right-0 top-0 bottom-0 w-1 bg-primary rounded-r-lg z-10"></div>
					{/if}
					<!-- Thumbnail -->
					<div class="relative aspect-square overflow-hidden rounded-t-lg bg-muted {!readonly && images.length > 1 ? 'cursor-grab active:cursor-grabbing' : ''}">
						<img
							src={getMediaUrl(img.url)}
							alt={img.altText || 'Product image'}
							class="h-full w-full object-cover"
							onerror={(e) => {
								(e.target as HTMLImageElement).style.display = 'none';
							}}
						/>

						<!-- Primary badge -->
						{#if index === 0}
							<Badge class="absolute left-2 top-2 bg-primary text-primary-foreground text-xs">
								Primary
							</Badge>
						{/if}

						<!-- Delete button -->
						{#if !readonly}
							<button
								type="button"
								class="absolute right-1.5 top-1.5 rounded-full bg-background/80 p-1 opacity-0 shadow-sm backdrop-blur-sm transition-opacity hover:bg-destructive hover:text-destructive-foreground group-hover/card:opacity-100"
								onclick={() => confirmDelete(img)}
								aria-label="Remove image"
							>
								<XIcon class="h-3.5 w-3.5" />
							</button>
						{/if}

						<!-- Sort order badge -->
						<div class="absolute bottom-1.5 right-1.5 rounded-full bg-background/80 px-2 py-0.5 text-xs font-medium text-muted-foreground backdrop-blur-sm">
							{index + 1}
						</div>
					</div>

					<!-- Card footer -->
					<div class="space-y-1.5 p-2">
						<!-- Alt text -->
						{#if editingAltTextId === img.id}
							<Input
								bind:value={altTextValue}
								placeholder="Alt text"
								class="h-7 text-xs"
								onblur={() => saveAltText(img)}
								onkeydown={(e) => handleAltTextKeydown(e, img)}
							/>
						{:else}
							<button
								type="button"
								class="w-full truncate text-left text-xs text-muted-foreground hover:text-foreground transition-colors {readonly ? 'cursor-default' : 'cursor-text'}"
								onclick={() => !readonly && startEditAltText(img)}
								title={img.altText || 'Click to add alt text'}
							>
								{img.altText || 'Add alt text...'}
							</button>
						{/if}

						<!-- Reorder buttons -->
						{#if !readonly && images.length > 1}
							<div class="flex items-center justify-center gap-1">
								<Button
									type="button"
									variant="ghost"
									size="sm"
									class="h-6 w-6 p-0"
									disabled={index === 0}
									onclick={() => moveImage(index, 'up')}
									aria-label="Move up"
								>
									<ChevronUpIcon class="h-3.5 w-3.5" />
								</Button>
								<GripVerticalIcon class="h-3.5 w-3.5 text-muted-foreground/50" />
								<Button
									type="button"
									variant="ghost"
									size="sm"
									class="h-6 w-6 p-0"
									disabled={index === images.length - 1}
									onclick={() => moveImage(index, 'down')}
									aria-label="Move down"
								>
									<ChevronDownIcon class="h-3.5 w-3.5" />
								</Button>
							</div>
						{/if}
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>
