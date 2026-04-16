<script lang="ts">
	import { goto } from '$app/navigation';
	import * as Dialog from '$lib/components/ui/dialog';
	import { auth } from '$lib/stores/auth.svelte';

	import LayoutDashboardIcon from '@lucide/svelte/icons/layout-dashboard';
	import PackageIcon from '@lucide/svelte/icons/package';
	import FolderTreeIcon from '@lucide/svelte/icons/folder-tree';
	import WarehouseIcon from '@lucide/svelte/icons/warehouse';
	import UsersIcon from '@lucide/svelte/icons/users';
	import ClipboardListIcon from '@lucide/svelte/icons/clipboard-list';
	import SettingsIcon from '@lucide/svelte/icons/settings';
	import PlusIcon from '@lucide/svelte/icons/plus';
	import SearchIcon from '@lucide/svelte/icons/search';

	interface Props {
		open: boolean;
	}

	let { open = $bindable(false) }: Props = $props();
	let query = $state('');
	let selectedIndex = $state(0);
	let inputRef = $state<HTMLInputElement | null>(null);

	interface Command {
		id: string;
		label: string;
		icon: typeof LayoutDashboardIcon;
		action: () => void;
		keywords: string[];
		adminOnly?: boolean;
	}

	const commands: Command[] = [
		{
			id: 'dashboard',
			label: 'Go to Dashboard',
			icon: LayoutDashboardIcon,
			action: () => goto('/dashboard'),
			keywords: ['dashboard', 'home', 'overview']
		},
		{
			id: 'products',
			label: 'Go to Products',
			icon: PackageIcon,
			action: () => goto('/products'),
			keywords: ['products', 'items', 'catalog']
		},
		{
			id: 'new-product',
			label: 'New Product',
			icon: PlusIcon,
			action: () => goto('/products/new'),
			keywords: ['new', 'create', 'add', 'product']
		},
		{
			id: 'categories',
			label: 'Go to Categories',
			icon: FolderTreeIcon,
			action: () => goto('/categories'),
			keywords: ['categories', 'organize', 'folders']
		},
		{
			id: 'inventory',
			label: 'Go to Inventory',
			icon: WarehouseIcon,
			action: () => goto('/inventory'),
			keywords: ['inventory', 'stock', 'warehouse']
		},
		{
			id: 'users',
			label: 'Go to Users',
			icon: UsersIcon,
			action: () => goto('/users'),
			keywords: ['users', 'team', 'members', 'people'],
			adminOnly: true
		},
		{
			id: 'audit-log',
			label: 'Go to Audit Log',
			icon: ClipboardListIcon,
			action: () => goto('/audit-log'),
			keywords: ['audit', 'log', 'history', 'changes'],
			adminOnly: true
		},
		{
			id: 'settings',
			label: 'Go to Settings',
			icon: SettingsIcon,
			action: () => goto('/settings'),
			keywords: ['settings', 'config', 'preferences'],
			adminOnly: true
		}
	];

	// Dynamic search commands that appear when the user types a query
	let searchCommands = $derived.by((): Command[] => {
		const q = query.trim();
		if (!q) return [];
		const encodedQuery = encodeURIComponent(q);
		return [
			{
				id: 'search-products',
				label: `Search Products for '${q}'...`,
				icon: SearchIcon,
				action: () => goto(`/products?search=${encodedQuery}`),
				keywords: [],
			},
			{
				id: 'search-inventory',
				label: `Search Inventory for '${q}'...`,
				icon: SearchIcon,
				action: () => goto(`/inventory?search=${encodedQuery}`),
				keywords: [],
			},
			{
				id: 'search-users',
				label: `Search Users for '${q}'...`,
				icon: SearchIcon,
				action: () => goto(`/users?search=${encodedQuery}`),
				keywords: [],
				adminOnly: true,
			},
		].filter((cmd) => !cmd.adminOnly || auth.canEdit);
	});

	let filteredCommands = $derived.by(() => {
		const available = commands.filter((cmd) => !cmd.adminOnly || auth.canEdit);
		if (!query.trim()) return available;
		const q = query.toLowerCase().trim();
		const matched = available.filter(
			(cmd) =>
				cmd.label.toLowerCase().includes(q) ||
				cmd.keywords.some((kw) => kw.includes(q))
		);
		return [...searchCommands, ...matched];
	});

	// Reset selected index when filtered commands change
	$effect(() => {
		// Access filteredCommands to track dependency
		filteredCommands;
		selectedIndex = 0;
	});

	function executeCommand(cmd: Command) {
		open = false;
		query = '';
		selectedIndex = 0;
		cmd.action();
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'ArrowDown') {
			e.preventDefault();
			selectedIndex = (selectedIndex + 1) % filteredCommands.length;
		} else if (e.key === 'ArrowUp') {
			e.preventDefault();
			selectedIndex = (selectedIndex - 1 + filteredCommands.length) % filteredCommands.length;
		} else if (e.key === 'Enter' && filteredCommands.length > 0) {
			e.preventDefault();
			executeCommand(filteredCommands[selectedIndex]);
		}
	}

	// Focus input when dialog opens
	$effect(() => {
		if (open) {
			query = '';
			selectedIndex = 0;
			// Use a microtask to wait for the dialog to render
			setTimeout(() => inputRef?.focus(), 50);
		}
	});
</script>

<Dialog.Root bind:open>
	<Dialog.Content class="max-w-md p-0 gap-0 overflow-hidden">
		<Dialog.Header class="sr-only">
			<Dialog.Title>Command Palette</Dialog.Title>
			<Dialog.Description>Search for commands and navigate</Dialog.Description>
		</Dialog.Header>
		<div class="flex items-center gap-2 border-b px-3">
			<SearchIcon class="h-4 w-4 text-muted-foreground shrink-0" />
			<input
				bind:this={inputRef}
				bind:value={query}
				onkeydown={handleKeydown}
				placeholder="Type a command or search..."
				class="flex h-12 w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
			/>
			<kbd class="pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium text-muted-foreground">
				Esc
			</kbd>
		</div>
		<div class="max-h-[300px] overflow-y-auto p-1">
			{#if filteredCommands.length === 0}
				<div class="py-6 text-center text-sm text-muted-foreground">
					No commands found.
				</div>
			{:else}
				{#each filteredCommands as cmd, i}
					{@const IconComponent = cmd.icon}
					<button
						class="flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-sm transition-colors text-left"
						class:bg-accent={i === selectedIndex}
						class:text-accent-foreground={i === selectedIndex}
						onmouseenter={() => (selectedIndex = i)}
						onclick={() => executeCommand(cmd)}
					>
						<IconComponent class="h-4 w-4 shrink-0 text-muted-foreground" />
						<span>{cmd.label}</span>
					</button>
				{/each}
			{/if}
		</div>
		<div class="flex items-center justify-between border-t px-3 py-2 text-xs text-muted-foreground">
			<div class="flex gap-2">
				<span><kbd class="rounded border px-1 font-mono">↑↓</kbd> navigate</span>
				<span><kbd class="rounded border px-1 font-mono">↵</kbd> select</span>
			</div>
			<span><kbd class="rounded border px-1 font-mono">esc</kbd> close</span>
		</div>
	</Dialog.Content>
</Dialog.Root>
