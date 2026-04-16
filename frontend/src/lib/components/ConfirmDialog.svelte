<script lang="ts">
	import * as Dialog from '$lib/components/ui/dialog';
	import { Button } from '$lib/components/ui/button';

	interface Props {
		open: boolean;
		title?: string;
		description?: string;
		confirmLabel?: string;
		variant?: 'destructive' | 'default';
		onConfirm: () => Promise<void> | void;
		onCancel?: () => void;
	}

	let {
		open = $bindable(false),
		title = 'Are you sure?',
		description = 'This action cannot be undone.',
		confirmLabel = 'Delete',
		variant = 'destructive',
		onConfirm,
		onCancel
	}: Props = $props();

	let loading = $state(false);

	async function handleConfirm() {
		loading = true;
		try {
			await onConfirm();
			open = false;
		} finally {
			loading = false;
		}
	}

	function handleCancel() {
		open = false;
		onCancel?.();
	}
</script>

<Dialog.Root bind:open>
	<Dialog.Content class="sm:max-w-md" showCloseButton={false}>
		<Dialog.Header>
			<Dialog.Title>{title}</Dialog.Title>
			<Dialog.Description>{description}</Dialog.Description>
		</Dialog.Header>
		<Dialog.Footer>
			<Button variant="outline" onclick={handleCancel} disabled={loading}>
				Cancel
			</Button>
			<Button
				variant={variant}
				onclick={handleConfirm}
				disabled={loading}
			>
				{loading ? 'Please wait...' : confirmLabel}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>
