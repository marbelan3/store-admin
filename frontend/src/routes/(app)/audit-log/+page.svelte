<script lang="ts">
	import { onMount } from 'svelte';
	import { getAuditLogs, type AuditLogEntry } from '$lib/api/audit';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Badge } from '$lib/components/ui/badge';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Table from '$lib/components/ui/table';
	import * as Select from '$lib/components/ui/select';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import ClipboardListIcon from '@lucide/svelte/icons/clipboard-list';

	const PAGE_SIZE = 30;

	let entries = $state<AuditLogEntry[]>([]);
	let totalElements = $state(0);
	let totalPages = $state(0);
	let currentPage = $state(0);
	let isFirst = $state(true);
	let isLast = $state(true);
	let loading = $state(true);

	let entityTypeFilter = $state('');
	let actionFilter = $state('');
	let dateFrom = $state('');
	let dateTo = $state('');

	async function loadLogs() {
		loading = true;
		try {
			const result = await getAuditLogs({
				page: currentPage,
				size: PAGE_SIZE,
				entityType: entityTypeFilter || undefined,
				action: actionFilter || undefined,
				from: dateFrom || undefined,
				to: dateTo || undefined
			});
			entries = result.content;
			totalElements = result.totalElements;
			totalPages = result.totalPages;
			isFirst = result.first;
			isLast = result.last;
		} catch {
			toast.error('Failed to load audit logs');
		} finally {
			loading = false;
		}
	}

	function handleEntityTypeChange(value: string) {
		entityTypeFilter = value === '__all__' ? '' : value;
		currentPage = 0;
		loadLogs();
	}

	function handleActionChange(value: string) {
		actionFilter = value === '__all__' ? '' : value;
		currentPage = 0;
		loadLogs();
	}

	function handleDateFromChange(e: Event) {
		dateFrom = (e.target as HTMLInputElement).value;
		currentPage = 0;
		loadLogs();
	}

	function handleDateToChange(e: Event) {
		dateTo = (e.target as HTMLInputElement).value;
		currentPage = 0;
		loadLogs();
	}

	function clearDateFilters() {
		dateFrom = '';
		dateTo = '';
		currentPage = 0;
		loadLogs();
	}

	function goToPage(p: number) {
		currentPage = p;
		loadLogs();
	}

	function actionVariant(action: string): 'default' | 'secondary' | 'outline' | 'destructive' {
		switch (action) {
			case 'CREATE': return 'default';
			case 'UPDATE': return 'secondary';
			case 'DELETE': return 'destructive';
			default: return 'outline';
		}
	}

	function actionBadgeClass(action: string): string {
		switch (action) {
			case 'CREATE': return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800';
			case 'UPDATE': return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400 border-blue-200 dark:border-blue-800';
			case 'DELETE': return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 border-red-200 dark:border-red-800';
			default: return '';
		}
	}

	function formatChanges(changes: Record<string, unknown> | null): string {
		if (!changes) return '';
		try {
			return JSON.stringify(changes, null, 2);
		} catch {
			return '';
		}
	}

	onMount(loadLogs);
</script>

<div class="space-y-6">
	<PageHeader
		title="Audit Log"
		description="Track all changes across your store"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Audit Log' }]}
	/>

	<div class="flex flex-wrap items-center gap-3">
		<Select.Root type="single" value={entityTypeFilter || '__all__'} onValueChange={handleEntityTypeChange}>
			<Select.Trigger class="w-[160px]">
				{entityTypeFilter || 'All Entities'}
			</Select.Trigger>
			<Select.Content>
				<Select.Item value="__all__">All Entities</Select.Item>
				<Select.Item value="PRODUCT">Product</Select.Item>
				<Select.Item value="CATEGORY">Category</Select.Item>
				<Select.Item value="TAG">Tag</Select.Item>
				<Select.Item value="USER">User</Select.Item>
				<Select.Item value="TENANT">Tenant</Select.Item>
			</Select.Content>
		</Select.Root>

		<Select.Root type="single" value={actionFilter || '__all__'} onValueChange={handleActionChange}>
			<Select.Trigger class="w-[140px]">
				{actionFilter || 'All Actions'}
			</Select.Trigger>
			<Select.Content>
				<Select.Item value="__all__">All Actions</Select.Item>
				<Select.Item value="CREATE">Create</Select.Item>
				<Select.Item value="UPDATE">Update</Select.Item>
				<Select.Item value="DELETE">Delete</Select.Item>
			</Select.Content>
		</Select.Root>

		<div class="flex items-center gap-2">
			<label class="text-sm text-muted-foreground whitespace-nowrap" for="audit-date-from">From</label>
			<Input
				id="audit-date-from"
				type="date"
				value={dateFrom}
				onchange={handleDateFromChange}
				max={dateTo || undefined}
				class="w-[160px]"
			/>
		</div>
		<div class="flex items-center gap-2">
			<label class="text-sm text-muted-foreground whitespace-nowrap" for="audit-date-to">To</label>
			<Input
				id="audit-date-to"
				type="date"
				value={dateTo}
				onchange={handleDateToChange}
				min={dateFrom || undefined}
				class="w-[160px]"
			/>
		</div>
		{#if dateFrom || dateTo}
			<Button variant="ghost" size="sm" onclick={clearDateFilters}>
				Clear dates
			</Button>
		{/if}
	</div>

	{#if loading}
		<div class="rounded-md border overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Time</Table.Head>
						<Table.Head>User</Table.Head>
						<Table.Head>Action</Table.Head>
						<Table.Head>Entity</Table.Head>
						<Table.Head>Details</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each Array(6) as _}
						<Table.Row>
							<Table.Cell><Skeleton class="h-4 w-32" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-5 w-16 rounded-full" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-28" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{:else if entries.length === 0}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={ClipboardListIcon}
				title="No activity yet"
				description="Actions performed in your store will be logged here."
			/>
		</div>
	{:else}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Time</Table.Head>
						<Table.Head>User</Table.Head>
						<Table.Head>Action</Table.Head>
						<Table.Head>Entity</Table.Head>
						<Table.Head>Details</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each entries as entry}
						<Table.Row class="hover:bg-muted/50 transition-colors">
							<Table.Cell class="text-sm text-muted-foreground whitespace-nowrap">
								{new Date(entry.createdAt).toLocaleString()}
							</Table.Cell>
							<Table.Cell class="text-sm">
								{entry.userName || 'System'}
							</Table.Cell>
							<Table.Cell>
								<Badge variant="outline" class={actionBadgeClass(entry.action)}>{entry.action}</Badge>
							</Table.Cell>
							<Table.Cell class="text-sm">
								<span class="font-medium">{entry.entityType}</span>
								<span class="text-muted-foreground ml-1 text-xs">{entry.entityId.substring(0, 8)}...</span>
							</Table.Cell>
							<Table.Cell class="max-w-xs">
								{#if entry.changes}
									<details class="text-xs">
										<summary class="cursor-pointer text-muted-foreground hover:text-foreground transition-colors">
											View changes
										</summary>
										<pre class="mt-2 max-h-32 overflow-auto rounded-md bg-muted/70 border p-3 text-xs font-mono leading-relaxed">{formatChanges(entry.changes)}</pre>
									</details>
								{:else}
									<span class="text-xs text-muted-foreground">---</span>
								{/if}
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		{#if totalPages > 1}
			<div class="flex items-center justify-between">
				<p class="text-sm text-muted-foreground">
					Showing {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, totalElements)} of {totalElements} entries
				</p>
				<div class="flex items-center gap-2">
					<Button variant="outline" size="sm" disabled={isFirst} onclick={() => goToPage(currentPage - 1)}>
						Previous
					</Button>
					<span class="text-sm text-muted-foreground">Page {currentPage + 1} of {totalPages}</span>
					<Button variant="outline" size="sm" disabled={isLast} onclick={() => goToPage(currentPage + 1)}>
						Next
					</Button>
				</div>
			</div>
		{/if}
	{/if}
</div>
