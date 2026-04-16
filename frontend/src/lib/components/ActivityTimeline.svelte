<script lang="ts">
	import { onMount } from 'svelte';
	import { getAuditLogs, type AuditLogEntry } from '$lib/api/audit';
	import { relativeTime } from '$lib/utils/relativeTime';
	import { Button } from '$lib/components/ui/button';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import PlusCircleIcon from '@lucide/svelte/icons/plus-circle';
	import PencilIcon from '@lucide/svelte/icons/pencil';
	import Trash2Icon from '@lucide/svelte/icons/trash-2';
	import ArrowRightIcon from '@lucide/svelte/icons/arrow-right';
	import ActivityIcon from '@lucide/svelte/icons/activity';
	import ChevronDownIcon from '@lucide/svelte/icons/chevron-down';

	interface Props {
		entityType: string;
		entityId: string;
		maxItems?: number;
	}

	let { entityType, entityId, maxItems = 10 }: Props = $props();

	let entries = $state<AuditLogEntry[]>([]);
	let totalItems = $state(0);
	let loading = $state(true);
	let displayCount = $state(10);
	$effect(() => { displayCount = maxItems; });
	let loadingMore = $state(false);
	let error = $state(false);

	const ACTION_CONFIG: Record<string, { color: string; dotClass: string }> = {
		CREATE: {
			color: 'text-emerald-600 dark:text-emerald-400',
			dotClass: 'bg-emerald-500'
		},
		UPDATE: {
			color: 'text-blue-600 dark:text-blue-400',
			dotClass: 'bg-blue-500'
		},
		DELETE: {
			color: 'text-red-600 dark:text-red-400',
			dotClass: 'bg-red-500'
		},
		STATUS_CHANGE: {
			color: 'text-purple-600 dark:text-purple-400',
			dotClass: 'bg-purple-500'
		}
	};

	const DEFAULT_CONFIG = {
		color: 'text-gray-600 dark:text-gray-400',
		dotClass: 'bg-gray-400'
	};

	function getConfig(action: string) {
		return ACTION_CONFIG[action] || DEFAULT_CONFIG;
	}

	function formatActionDescription(entry: AuditLogEntry): string {
		const entityLabel = entityType.charAt(0) + entityType.slice(1).toLowerCase();
		switch (entry.action) {
			case 'CREATE':
				return `${entityLabel} created`;
			case 'DELETE':
				return `${entityLabel} deleted`;
			case 'STATUS_CHANGE': {
				const to = entry.changes?.status ?? entry.changes?.toStatus;
				if (to) return `Status changed to ${to}`;
				return `Status changed`;
			}
			case 'UPDATE': {
				if (!entry.changes) return `${entityLabel} updated`;
				const keys = Object.keys(entry.changes);
				if (keys.length === 1) {
					const key = keys[0];
					const value = entry.changes[key];
					if (typeof value === 'string' || typeof value === 'number') {
						return `${formatFieldName(key)} updated to ${value}`;
					}
					return `${formatFieldName(key)} updated`;
				}
				if (keys.length <= 3) {
					return `Updated ${keys.map(formatFieldName).join(', ')}`;
				}
				return `${entityLabel} updated (${keys.length} fields)`;
			}
			default:
				return `${entry.action} on ${entityLabel}`;
		}
	}

	function formatFieldName(field: string): string {
		return field
			.replace(/([A-Z])/g, ' $1')
			.replace(/[_-]/g, ' ')
			.trim()
			.toLowerCase();
	}

	function formatChanges(changes: Record<string, unknown> | null): string {
		if (!changes) return '';
		try {
			return JSON.stringify(changes, null, 2);
		} catch {
			return '';
		}
	}

	async function loadEntries() {
		loading = true;
		error = false;
		try {
			const result = await getAuditLogs({
				entityType,
				entityId,
				page: 0,
				size: 50
			});
			entries = result.content;
			totalItems = result.totalElements;
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	}

	function showMore() {
		loadingMore = true;
		displayCount = Math.min(displayCount + maxItems, entries.length);
		loadingMore = false;
	}

	let visibleEntries = $derived(entries.slice(0, displayCount));
	let hasMore = $derived(displayCount < entries.length);

	onMount(loadEntries);
</script>

{#if loading}
	<div class="space-y-4">
		{#each Array(3) as _}
			<div class="flex gap-3">
				<Skeleton class="h-3 w-3 rounded-full mt-1.5 shrink-0" />
				<div class="flex-1 space-y-1.5">
					<Skeleton class="h-3.5 w-48" />
					<Skeleton class="h-3 w-24" />
				</div>
			</div>
		{/each}
	</div>
{:else if error}
	<p class="text-sm text-muted-foreground">Failed to load activity.</p>
{:else if entries.length === 0}
	<div class="flex flex-col items-center py-6 text-center">
		<ActivityIcon class="h-8 w-8 text-muted-foreground/40 mb-2" />
		<p class="text-sm text-muted-foreground">No activity recorded</p>
	</div>
{:else}
	<div class="relative">
		<!-- Vertical line -->
		<div class="absolute left-[5px] top-2 bottom-2 w-px bg-border"></div>

		<div class="space-y-4">
			{#each visibleEntries as entry}
				{@const config = getConfig(entry.action)}
				<div class="relative flex gap-3">
					<!-- Dot -->
					<div class="relative z-10 mt-1.5 h-[11px] w-[11px] shrink-0 rounded-full border-2 border-background {config.dotClass}"></div>

					<!-- Content -->
					<div class="flex-1 min-w-0 pb-1">
						<div class="flex items-start gap-1.5">
							{#if entry.action === 'CREATE'}
								<PlusCircleIcon class="h-3.5 w-3.5 mt-0.5 shrink-0 {config.color}" />
							{:else if entry.action === 'UPDATE'}
								<PencilIcon class="h-3.5 w-3.5 mt-0.5 shrink-0 {config.color}" />
							{:else if entry.action === 'DELETE'}
								<Trash2Icon class="h-3.5 w-3.5 mt-0.5 shrink-0 {config.color}" />
							{:else if entry.action === 'STATUS_CHANGE'}
								<ArrowRightIcon class="h-3.5 w-3.5 mt-0.5 shrink-0 {config.color}" />
							{:else}
								<ActivityIcon class="h-3.5 w-3.5 mt-0.5 shrink-0 {config.color}" />
							{/if}
							<p class="text-sm leading-snug">{formatActionDescription(entry)}</p>
						</div>
						<div class="flex items-center gap-2 mt-1">
							{#if entry.userName}
								<span class="text-xs text-muted-foreground">{entry.userName}</span>
								<span class="text-xs text-muted-foreground">·</span>
							{/if}
							<span class="text-xs text-muted-foreground" title={new Date(entry.createdAt).toLocaleString()}>
								{relativeTime(entry.createdAt)}
							</span>
						</div>
						{#if entry.changes}
							<details class="mt-1.5">
								<summary class="text-xs text-muted-foreground cursor-pointer hover:text-foreground transition-colors">
									View changes
								</summary>
								<pre class="mt-1.5 max-h-28 overflow-auto rounded-md bg-muted/70 border p-2 text-xs font-mono leading-relaxed">{formatChanges(entry.changes)}</pre>
							</details>
						{/if}
					</div>
				</div>
			{/each}
		</div>
	</div>

	{#if hasMore}
		<div class="mt-3 flex justify-center">
			<Button variant="ghost" size="sm" onclick={showMore} disabled={loadingMore}>
				<ChevronDownIcon class="h-3.5 w-3.5 mr-1" />
				Show more
			</Button>
		</div>
	{/if}
{/if}
