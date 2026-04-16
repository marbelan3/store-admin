<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/stores/auth.svelte';
	import {
		getConnections,
		createConnection,
		updateConnection,
		deleteConnection,
		testConnection,
		triggerSync,
		getSyncLogs
	} from '$lib/api/marketplace';
	import type { MarketplaceConnection, SyncLog } from '$lib/types/marketplace';
	import type { Page } from '$lib/types/product';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Badge } from '$lib/components/ui/badge';
	import { Switch } from '$lib/components/ui/switch';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Select from '$lib/components/ui/select';
	import * as Card from '$lib/components/ui/card';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import GlobeIcon from '@lucide/svelte/icons/globe';
	import WifiIcon from '@lucide/svelte/icons/wifi';
	import WifiOffIcon from '@lucide/svelte/icons/wifi-off';
	import RefreshCwIcon from '@lucide/svelte/icons/refresh-cw';
	import TrashIcon from '@lucide/svelte/icons/trash-2';
	import PencilIcon from '@lucide/svelte/icons/pencil';

	let connections = $state<MarketplaceConnection[]>([]);
	let loading = $state(true);
	let createDialogOpen = $state(false);
	let editDialogOpen = $state(false);
	let logsDialogOpen = $state(false);

	// Create form
	let newProvider = $state('CJ_DROPSHIPPING');
	let newEmail = $state('');
	let newApiKey = $state('');
	let newWarehouseId = $state('');
	let newShippingMethod = $state('');
	let saving = $state(false);

	// Edit form
	let editId = $state('');
	let editEmail = $state('');
	let editApiKey = $state('');
	let editSyncEnabled = $state(true);
	let editWarehouseId = $state('');
	let editShippingMethod = $state('');
	let editSaving = $state(false);

	// Confirm dialog
	let confirmOpen = $state(false);
	let confirmTitle = $state('');
	let confirmDescription = $state('');
	let confirmAction = $state<() => Promise<void>>(async () => {});

	// Sync logs
	let syncLogs = $state<SyncLog[]>([]);
	let syncLogsConnectionId = $state('');
	let logsLoading = $state(false);

	async function loadConnections() {
		loading = true;
		try {
			connections = await getConnections();
		} catch {
			toast.error('Failed to load connections');
		} finally {
			loading = false;
		}
	}

	async function handleCreate() {
		if (!newEmail.trim() || !newApiKey.trim()) return;
		saving = true;
		try {
			await createConnection({
				provider: newProvider,
				email: newEmail,
				apiKey: newApiKey,
				defaultWarehouseId: newWarehouseId || undefined,
				defaultShippingMethod: newShippingMethod || undefined
			});
			toast.success('Connection created');
			createDialogOpen = false;
			newEmail = '';
			newApiKey = '';
			newWarehouseId = '';
			newShippingMethod = '';
			await loadConnections();
		} catch (err: any) {
			toast.error(err.message || 'Failed to create connection');
		} finally {
			saving = false;
		}
	}

	function openEdit(conn: MarketplaceConnection) {
		editId = conn.id;
		editEmail = conn.email || '';
		editApiKey = '';
		editSyncEnabled = conn.syncEnabled;
		editWarehouseId = conn.defaultWarehouseId || '';
		editShippingMethod = conn.defaultShippingMethod || '';
		editDialogOpen = true;
	}

	async function handleUpdate() {
		editSaving = true;
		try {
			await updateConnection(editId, {
				email: editEmail || undefined,
				apiKey: editApiKey || undefined,
				syncEnabled: editSyncEnabled,
				defaultWarehouseId: editWarehouseId || undefined,
				defaultShippingMethod: editShippingMethod || undefined
			});
			toast.success('Connection updated');
			editDialogOpen = false;
			await loadConnections();
		} catch (err: any) {
			toast.error(err.message || 'Failed to update connection');
		} finally {
			editSaving = false;
		}
	}

	function handleDelete(conn: MarketplaceConnection) {
		confirmTitle = 'Delete connection';
		confirmDescription = `Delete the ${conn.provider} connection? This cannot be undone.`;
		confirmAction = async () => {
			try {
				await deleteConnection(conn.id);
				toast.success('Connection deleted');
				await loadConnections();
			} catch {
				toast.error('Failed to delete connection');
			}
		};
		confirmOpen = true;
	}

	async function handleTest(conn: MarketplaceConnection) {
		try {
			const result = await testConnection(conn.id);
			if (result.status === 'ACTIVE') {
				toast.success('Connection is healthy');
			} else {
				toast.warning(`Connection status: ${result.status}`);
			}
			await loadConnections();
		} catch (err: any) {
			toast.error(err.message || 'Connection test failed');
		}
	}

	async function handleTriggerSync(conn: MarketplaceConnection) {
		try {
			await triggerSync(conn.id);
			toast.success('Sync triggered');
		} catch (err: any) {
			toast.error(err.message || 'Failed to trigger sync');
		}
	}

	async function openLogs(conn: MarketplaceConnection) {
		syncLogsConnectionId = conn.id;
		logsDialogOpen = true;
		logsLoading = true;
		try {
			const page = await getSyncLogs(conn.id, 0, 20);
			syncLogs = page.content;
		} catch {
			toast.error('Failed to load sync logs');
		} finally {
			logsLoading = false;
		}
	}

	function statusColor(status: string): string {
		switch (status) {
			case 'ACTIVE': return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
			case 'ERROR': return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
			case 'TOKEN_EXPIRED': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
			default: return 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400';
		}
	}

	function formatDate(date: string | null): string {
		if (!date) return 'Never';
		return new Date(date).toLocaleString();
	}

	onMount(loadConnections);
</script>

<ConfirmDialog
	bind:open={confirmOpen}
	title={confirmTitle}
	description={confirmDescription}
	confirmLabel="Delete"
	variant="destructive"
	onConfirm={confirmAction}
/>

<div class="space-y-6">
	<PageHeader
		title="Marketplace"
		description="Manage marketplace connections and integrations"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Marketplace' }]}
	>
		{#if auth.canEdit}
			<Dialog.Root bind:open={createDialogOpen}>
				<Dialog.Trigger>
					{#snippet child({ props })}
						<Button {...props}>Add Connection</Button>
					{/snippet}
				</Dialog.Trigger>
				<Dialog.Content>
					<Dialog.Header>
						<Dialog.Title>New Marketplace Connection</Dialog.Title>
					</Dialog.Header>
					<div class="space-y-4 py-4">
						<div class="space-y-2">
							<Label>Provider</Label>
							<Select.Root
								type="single"
								value={newProvider}
								onValueChange={(v) => (newProvider = v)}
							>
								<Select.Trigger class="w-full">
									{newProvider === 'CJ_DROPSHIPPING' ? 'CJ Dropshipping' : newProvider}
								</Select.Trigger>
								<Select.Content>
									<Select.Item value="CJ_DROPSHIPPING">CJ Dropshipping</Select.Item>
								</Select.Content>
							</Select.Root>
						</div>
						<div class="space-y-2">
							<Label for="email">Email *</Label>
							<Input id="email" type="email" bind:value={newEmail} placeholder="Your CJ account email" />
						</div>
						<div class="space-y-2">
							<Label for="apiKey">API Key *</Label>
							<Input id="apiKey" type="password" bind:value={newApiKey} placeholder="Your CJ API key" />
						</div>
						<div class="space-y-2">
							<Label for="warehouseId">Default Warehouse ID</Label>
							<Input id="warehouseId" bind:value={newWarehouseId} placeholder="e.g. CN, US, DE" />
						</div>
						<div class="space-y-2">
							<Label for="shippingMethod">Default Shipping Method</Label>
							<Input id="shippingMethod" bind:value={newShippingMethod} placeholder="e.g. standard" />
						</div>
					</div>
					<Dialog.Footer>
						<Button variant="outline" onclick={() => (createDialogOpen = false)}>Cancel</Button>
						<Button onclick={handleCreate} disabled={saving || !newEmail.trim() || !newApiKey.trim()}>
							{saving ? 'Creating...' : 'Create'}
						</Button>
					</Dialog.Footer>
				</Dialog.Content>
			</Dialog.Root>
		{/if}
	</PageHeader>

	{#if loading}
		<div class="grid gap-4 md:grid-cols-2">
			{#each Array(2) as _}
				<Card.Root>
					<Card.Header>
						<Skeleton class="h-5 w-32" />
						<Skeleton class="h-4 w-20 mt-1" />
					</Card.Header>
					<Card.Content>
						<Skeleton class="h-4 w-full mb-2" />
						<Skeleton class="h-4 w-3/4" />
					</Card.Content>
				</Card.Root>
			{/each}
		</div>
	{:else if connections.length === 0}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={GlobeIcon}
				title="No connections"
				description="Add a marketplace connection to start importing products."
				actionLabel={auth.canEdit ? 'Add Connection' : undefined}
				onAction={auth.canEdit ? () => (createDialogOpen = true) : undefined}
			/>
		</div>
	{:else}
		<div class="grid gap-4 md:grid-cols-2">
			{#each connections as conn}
				<Card.Root>
					<Card.Header>
						<div class="flex items-center justify-between">
							<div class="flex items-center gap-2">
								<GlobeIcon class="h-5 w-5 text-muted-foreground" />
								<Card.Title class="text-lg">
									{conn.provider === 'CJ_DROPSHIPPING' ? 'CJ Dropshipping' : conn.provider}
								</Card.Title>
							</div>
							<Badge class={statusColor(conn.status)}>{conn.status}</Badge>
						</div>
					</Card.Header>
					<Card.Content class="space-y-3">
						<div class="grid grid-cols-2 gap-2 text-sm">
							<div>
								<span class="text-muted-foreground">Sync:</span>
								<span class="ml-1 font-medium">{conn.syncEnabled ? 'Enabled' : 'Disabled'}</span>
							</div>
							<div>
								<span class="text-muted-foreground">Warehouse:</span>
								<span class="ml-1 font-medium">{conn.defaultWarehouseId || '—'}</span>
							</div>
							<div>
								<span class="text-muted-foreground">Shipping:</span>
								<span class="ml-1 font-medium">{conn.defaultShippingMethod || '—'}</span>
							</div>
							<div>
								<span class="text-muted-foreground">Connected:</span>
								<span class="ml-1 font-medium text-xs">{formatDate(conn.lastConnectedAt)}</span>
							</div>
						</div>

						{#if auth.canEdit}
							<div class="flex flex-wrap gap-2 pt-2 border-t">
								<Button variant="outline" size="sm" onclick={() => handleTest(conn)}>
									<WifiIcon class="mr-1 h-3.5 w-3.5" />
									Test
								</Button>
								<Button variant="outline" size="sm" onclick={() => handleTriggerSync(conn)}>
									<RefreshCwIcon class="mr-1 h-3.5 w-3.5" />
									Sync Now
								</Button>
								<Button variant="outline" size="sm" onclick={() => openLogs(conn)}>
									Logs
								</Button>
								<div class="ml-auto flex gap-1">
									<Button variant="ghost" size="sm" onclick={() => openEdit(conn)}>
										<PencilIcon class="h-3.5 w-3.5" />
									</Button>
									<Button variant="ghost" size="sm" onclick={() => handleDelete(conn)}>
										<TrashIcon class="h-3.5 w-3.5 text-destructive" />
									</Button>
								</div>
							</div>
						{/if}
					</Card.Content>
				</Card.Root>
			{/each}
		</div>
	{/if}
</div>

<!-- Edit Dialog -->
<Dialog.Root bind:open={editDialogOpen}>
	<Dialog.Content>
		<Dialog.Header>
			<Dialog.Title>Edit Connection</Dialog.Title>
		</Dialog.Header>
		<div class="space-y-4 py-4">
			<div class="space-y-2">
				<Label for="editEmail">Email</Label>
				<Input id="editEmail" type="email" bind:value={editEmail} placeholder="CJ account email" />
			</div>
			<div class="space-y-2">
				<Label for="editApiKey">API Key</Label>
				<Input id="editApiKey" type="password" bind:value={editApiKey} placeholder="Leave empty to keep current" />
			</div>
			<div class="flex items-center gap-3">
				<Switch bind:checked={editSyncEnabled} />
				<Label>Sync Enabled</Label>
			</div>
			<div class="space-y-2">
				<Label for="editWarehouse">Default Warehouse ID</Label>
				<Input id="editWarehouse" bind:value={editWarehouseId} placeholder="e.g. CN, US, DE" />
			</div>
			<div class="space-y-2">
				<Label for="editShipping">Default Shipping Method</Label>
				<Input id="editShipping" bind:value={editShippingMethod} placeholder="e.g. standard" />
			</div>
		</div>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (editDialogOpen = false)}>Cancel</Button>
			<Button onclick={handleUpdate} disabled={editSaving}>
				{editSaving ? 'Saving...' : 'Save Changes'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<!-- Sync Logs Dialog -->
<Dialog.Root bind:open={logsDialogOpen}>
	<Dialog.Content class="max-w-2xl">
		<Dialog.Header>
			<Dialog.Title>Sync Logs</Dialog.Title>
		</Dialog.Header>
		<div class="max-h-96 overflow-auto">
			{#if logsLoading}
				<div class="space-y-2">
					{#each Array(5) as _}
						<Skeleton class="h-10 w-full" />
					{/each}
				</div>
			{:else if syncLogs.length === 0}
				<p class="text-sm text-muted-foreground text-center py-8">No sync logs yet.</p>
			{:else}
				<table class="w-full text-sm">
					<thead>
						<tr class="border-b text-left">
							<th class="py-2 pr-3 font-medium">Type</th>
							<th class="py-2 pr-3 font-medium">Status</th>
							<th class="py-2 pr-3 font-medium">Checked</th>
							<th class="py-2 pr-3 font-medium">Updated</th>
							<th class="py-2 pr-3 font-medium">Errors</th>
							<th class="py-2 font-medium">Time</th>
						</tr>
					</thead>
					<tbody>
						{#each syncLogs as log}
							<tr class="border-b">
								<td class="py-2 pr-3">{log.syncType}</td>
								<td class="py-2 pr-3">
									<Badge variant={log.status === 'COMPLETED' ? 'default' : 'destructive'} class="text-xs">
										{log.status}
									</Badge>
								</td>
								<td class="py-2 pr-3">{log.itemsChecked ?? 0}</td>
								<td class="py-2 pr-3">{log.itemsUpdated ?? 0}</td>
								<td class="py-2 pr-3">{log.errorsCount ?? 0}</td>
								<td class="py-2 text-xs text-muted-foreground">{formatDate(log.startedAt)}</td>
							</tr>
						{/each}
					</tbody>
				</table>
			{/if}
		</div>
	</Dialog.Content>
</Dialog.Root>
