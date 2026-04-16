<script lang="ts">
	import { onMount } from 'svelte';
	import { getConnections, getSyncLogs, triggerSync } from '$lib/api/marketplace';
	import type { MarketplaceConnection, SyncLog } from '$lib/types/marketplace';
	import type { Page } from '$lib/types/product';
	import { Button } from '$lib/components/ui/button';
	import { Badge } from '$lib/components/ui/badge';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Select from '$lib/components/ui/select';
	import * as Table from '$lib/components/ui/table';
	import * as Dialog from '$lib/components/ui/dialog';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import ActivityIcon from '@lucide/svelte/icons/activity';
	import RefreshCwIcon from '@lucide/svelte/icons/refresh-cw';

	let connections = $state<MarketplaceConnection[]>([]);
	let selectedConnectionId = $state('');
	let logs = $state<SyncLog[]>([]);
	let loading = $state(true);
	let loadingLogs = $state(false);
	let pageData = $state<Page<SyncLog> | null>(null);
	let currentPage = $state(0);

	// Error detail dialog
	let errorDialogOpen = $state(false);
	let errorDetail = $state('');

	async function loadConnections() {
		loading = true;
		try {
			connections = await getConnections();
			if (connections.length > 0) {
				selectedConnectionId = connections[0].id;
				await loadLogs();
			}
		} catch {
			toast.error('Failed to load connections');
		} finally {
			loading = false;
		}
	}

	async function loadLogs(page = 0) {
		if (!selectedConnectionId) return;
		loadingLogs = true;
		try {
			pageData = await getSyncLogs(selectedConnectionId, page, 20);
			logs = pageData.content;
			currentPage = page;
		} catch {
			toast.error('Failed to load sync logs');
		} finally {
			loadingLogs = false;
		}
	}

	async function handleTriggerSync() {
		if (!selectedConnectionId) return;
		try {
			await triggerSync(selectedConnectionId);
			toast.success('Sync triggered');
			setTimeout(() => loadLogs(0), 2000);
		} catch (err: any) {
			toast.error(err.message || 'Failed to trigger sync');
		}
	}

	function handleConnectionChange(v: string) {
		selectedConnectionId = v;
		loadLogs(0);
	}

	function showErrors(details: string) {
		errorDetail = details;
		errorDialogOpen = true;
	}

	function syncTypeBadge(type: string): string {
		switch (type) {
			case 'PRICE': return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
			case 'STOCK': return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
			case 'SKU_VALIDATION': return 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400';
			case 'CATALOG_HEALTH': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
			case 'ORDER_STATUS': return 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400';
			case 'TRACKING': return 'bg-teal-100 text-teal-700 dark:bg-teal-900/30 dark:text-teal-400';
			default: return 'bg-gray-100 text-gray-600';
		}
	}

	function formatDate(date: string | null): string {
		if (!date) return '—';
		return new Date(date).toLocaleString();
	}

	function duration(start: string, end: string | null): string {
		if (!end) return 'Running...';
		const ms = new Date(end).getTime() - new Date(start).getTime();
		if (ms < 1000) return `${ms}ms`;
		return `${(ms / 1000).toFixed(1)}s`;
	}

	onMount(loadConnections);
</script>

<div class="space-y-6">
	<PageHeader
		title="Sync Logs"
		description="History of marketplace synchronization runs"
		breadcrumbs={[
			{ label: 'Home', href: '/dashboard' },
			{ label: 'Marketplace', href: '/marketplace/connections' },
			{ label: 'Sync Logs' }
		]}
	>
		{#if selectedConnectionId}
			<Button variant="outline" onclick={handleTriggerSync}>
				<RefreshCwIcon class="mr-1.5 h-4 w-4" />
				Trigger Sync
			</Button>
		{/if}
	</PageHeader>

	<!-- Connection Selector -->
	{#if loading}
		<Skeleton class="h-10 w-48" />
	{:else if connections.length > 0}
		<Select.Root
			type="single"
			value={selectedConnectionId}
			onValueChange={handleConnectionChange}
		>
			<Select.Trigger class="w-full sm:w-64">
				{connections.find(c => c.id === selectedConnectionId)?.provider === 'CJ_DROPSHIPPING'
					? 'CJ Dropshipping'
					: connections.find(c => c.id === selectedConnectionId)?.provider || 'Select connection'}
			</Select.Trigger>
			<Select.Content>
				{#each connections as conn}
					<Select.Item value={conn.id}>
						{conn.provider === 'CJ_DROPSHIPPING' ? 'CJ Dropshipping' : conn.provider}
					</Select.Item>
				{/each}
			</Select.Content>
		</Select.Root>
	{/if}

	{#if loadingLogs}
		<div class="rounded-md border overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						{#each Array(7) as _}
							<Table.Head><Skeleton class="h-4 w-16" /></Table.Head>
						{/each}
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each Array(8) as _}
						<Table.Row>
							{#each Array(7) as __}
								<Table.Cell><Skeleton class="h-4 w-16" /></Table.Cell>
							{/each}
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{:else if logs.length === 0}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={ActivityIcon}
				title="No sync logs"
				description="Sync logs will appear after the first synchronization runs."
			/>
		</div>
	{:else}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Type</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head>Checked</Table.Head>
						<Table.Head>Updated</Table.Head>
						<Table.Head>Errors</Table.Head>
						<Table.Head>Duration</Table.Head>
						<Table.Head>Started</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each logs as log}
						<Table.Row>
							<Table.Cell>
								<Badge class={syncTypeBadge(log.syncType)}>{log.syncType}</Badge>
							</Table.Cell>
							<Table.Cell>
								<Badge variant={log.status === 'COMPLETED' ? 'default' : 'destructive'}>
									{log.status}
								</Badge>
							</Table.Cell>
							<Table.Cell>{log.itemsChecked ?? 0}</Table.Cell>
							<Table.Cell>{log.itemsUpdated ?? 0}</Table.Cell>
							<Table.Cell>
								{#if (log.errorsCount ?? 0) > 0}
									<button
										class="text-red-600 hover:underline font-medium"
										onclick={() => showErrors(log.errorDetails || 'No details')}
									>
										{log.errorsCount}
									</button>
								{:else}
									0
								{/if}
							</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{duration(log.startedAt, log.completedAt)}
							</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{formatDate(log.startedAt)}
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		{#if pageData && pageData.totalPages > 1}
			<div class="flex items-center justify-center gap-2 pt-4">
				<Button variant="outline" size="sm" disabled={pageData.first} onclick={() => loadLogs(currentPage - 1)}>Previous</Button>
				<span class="text-sm text-muted-foreground">Page {currentPage + 1} of {pageData.totalPages}</span>
				<Button variant="outline" size="sm" disabled={pageData.last} onclick={() => loadLogs(currentPage + 1)}>Next</Button>
			</div>
		{/if}
	{/if}
</div>

<!-- Error Detail Dialog -->
<Dialog.Root bind:open={errorDialogOpen}>
	<Dialog.Content class="max-w-lg">
		<Dialog.Header>
			<Dialog.Title>Error Details</Dialog.Title>
		</Dialog.Header>
		<div class="max-h-80 overflow-auto">
			<pre class="text-sm bg-muted p-3 rounded whitespace-pre-wrap break-words">{errorDetail}</pre>
		</div>
	</Dialog.Content>
</Dialog.Root>
