<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { auth } from '$lib/stores/auth.svelte';
	import { onMount } from 'svelte';
	import * as Avatar from '$lib/components/ui/avatar';
	import * as DropdownMenu from '$lib/components/ui/dropdown-menu';
	import * as Sheet from '$lib/components/ui/sheet';
	import * as Tooltip from '$lib/components/ui/tooltip';
	import { Separator } from '$lib/components/ui/separator';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { toggleMode } from 'mode-watcher';
	import CommandPalette from '$lib/components/CommandPalette.svelte';
	import NotificationBell from '$lib/components/NotificationBell.svelte';

	// Lucide icons
	import LayoutDashboardIcon from '@lucide/svelte/icons/layout-dashboard';
	import PackageIcon from '@lucide/svelte/icons/package';
	import FolderTreeIcon from '@lucide/svelte/icons/folder-tree';
	import ShoppingCartIcon from '@lucide/svelte/icons/shopping-cart';
	import WarehouseIcon from '@lucide/svelte/icons/warehouse';
	import ContactIcon from '@lucide/svelte/icons/contact';
	import TagIcon from '@lucide/svelte/icons/tag';
	import UsersIcon from '@lucide/svelte/icons/users';
	import ClipboardListIcon from '@lucide/svelte/icons/clipboard-list';
	import SettingsIcon from '@lucide/svelte/icons/settings';
	import GlobeIcon from '@lucide/svelte/icons/globe';
	import LinkIcon from '@lucide/svelte/icons/link';
	import DownloadIcon from '@lucide/svelte/icons/download';
	import EyeIcon from '@lucide/svelte/icons/eye';
	import AlertTriangleIcon from '@lucide/svelte/icons/triangle-alert';
	import ListIcon from '@lucide/svelte/icons/list';
	import ActivityIcon from '@lucide/svelte/icons/activity';
	import ChevronsLeftIcon from '@lucide/svelte/icons/chevrons-left';
	import ChevronsRightIcon from '@lucide/svelte/icons/chevrons-right';
	import MenuIcon from '@lucide/svelte/icons/menu';
	import SunIcon from '@lucide/svelte/icons/sun';
	import MoonIcon from '@lucide/svelte/icons/moon';

	let { children } = $props();
	let sidebarOpen = $state(true);
	let mobileSheetOpen = $state(false);
	let commandPaletteOpen = $state(false);

	const iconMap: Record<string, typeof LayoutDashboardIcon> = {
		'layout-dashboard': LayoutDashboardIcon,
		'package': PackageIcon,
		'folder-tree': FolderTreeIcon,
		'shopping-cart': ShoppingCartIcon,
		'warehouse': WarehouseIcon,
		'contact': ContactIcon,
		'tag': TagIcon,
		'globe': GlobeIcon,
		'link': LinkIcon,
		'download': DownloadIcon,
		'eye': EyeIcon,
		'alert-triangle': AlertTriangleIcon,
		'list': ListIcon,
		'activity': ActivityIcon,
		'users': UsersIcon,
		'clipboard-list': ClipboardListIcon,
		'settings': SettingsIcon,
	};

	onMount(async () => {
		await auth.init();
		if (!auth.isAuthenticated) {
			goto('/login');
		}

		// Restore sidebar collapsed state from localStorage
		const saved = localStorage.getItem('sidebar-collapsed');
		if (saved !== null) {
			sidebarOpen = saved !== 'true';
		}
	});

	function toggleSidebar() {
		sidebarOpen = !sidebarOpen;
		localStorage.setItem('sidebar-collapsed', String(!sidebarOpen));
	}

	interface NavItem {
		href: string;
		label: string;
		icon: string;
		adminOnly?: boolean;
		separator?: boolean;
	}

	const navItems: NavItem[] = [
		{ href: '/dashboard', label: 'Dashboard', icon: 'layout-dashboard' },
		{ href: '/products', label: 'Products', icon: 'package' },
		{ href: '/categories', label: 'Categories', icon: 'folder-tree' },
		{ href: '/orders', label: 'Orders', icon: 'shopping-cart' },
		{ href: '/inventory', label: 'Inventory', icon: 'warehouse' },
		{ href: '/customers', label: 'Customers', icon: 'contact' },
		{ href: '/discounts', label: 'Discounts', icon: 'tag' },
		{ href: '/marketplace/connections', label: 'Marketplace', icon: 'globe', separator: true },
		{ href: '/marketplace/products', label: 'MP Products', icon: 'list' },
		{ href: '/marketplace/import', label: 'Import Products', icon: 'download' },
		{ href: '/marketplace/watchlist', label: 'Watchlist', icon: 'eye' },
		{ href: '/marketplace/alerts', label: 'Alerts', icon: 'alert-triangle' },
		{ href: '/marketplace/sync-logs', label: 'Sync Logs', icon: 'activity' },
		{ href: '/users', label: 'Users', icon: 'users', adminOnly: true, separator: true },
		{ href: '/audit-log', label: 'Audit Log', icon: 'clipboard-list', adminOnly: true },
		{ href: '/settings', label: 'Settings', icon: 'settings', adminOnly: true }
	];

	function isActive(href: string) {
		return $page.url.pathname.startsWith(href);
	}

	function handleMobileNav(href: string) {
		mobileSheetOpen = false;
		goto(href);
	}

	function handleGlobalKeydown(e: KeyboardEvent) {
		// Cmd/Ctrl+K -> command palette
		if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
			e.preventDefault();
			commandPaletteOpen = !commandPaletteOpen;
			return;
		}

		// Only handle shortcuts when not focused on input/textarea/select
		const target = e.target as HTMLElement;
		const isInputFocused = target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.tagName === 'SELECT' || target.isContentEditable;

		if (isInputFocused) return;

		if (e.key === '/' ) {
			e.preventDefault();
			// Focus the first search input on the page
			const searchInput = document.querySelector<HTMLInputElement>('input[placeholder*="Search"]');
			if (searchInput) searchInput.focus();
			return;
		}

		if (e.key === 'n' && $page.url.pathname === '/products') {
			e.preventDefault();
			goto('/products/new');
			return;
		}
	}
</script>

<svelte:window onkeydown={handleGlobalKeydown} />

<CommandPalette bind:open={commandPaletteOpen} />

{#if auth.loading}
	<div class="flex min-h-screen">
		<aside class="hidden md:flex w-64 flex-col gradient-sidebar">
			<div class="flex h-14 items-center gap-2 border-b border-white/10 px-4">
				<Skeleton class="h-6 w-28" />
			</div>
			<nav class="flex-1 space-y-1 p-2">
				{#each Array(5) as _}
					<div class="flex items-center gap-3 px-3 py-2">
						<Skeleton class="h-4 w-4" />
						<Skeleton class="h-4 w-24" />
					</div>
				{/each}
			</nav>
		</aside>
		<main class="flex-1 p-6">
			<Skeleton class="h-8 w-48 mb-4" />
			<Skeleton class="h-4 w-64 mb-8" />
			<div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
				{#each Array(4) as _}
					<Skeleton class="h-32 w-full rounded-lg" />
				{/each}
			</div>
		</main>
	</div>
{:else if auth.isAuthenticated}
	<div class="flex min-h-screen">
		<!-- Mobile top bar -->
		<div class="fixed inset-x-0 top-0 z-40 flex h-14 items-center gap-2 bg-card px-4 shadow-sm md:hidden">
			<!-- Gradient accent line at top -->
			<div class="absolute inset-x-0 top-0 h-0.5" style="background: linear-gradient(90deg, oklch(0.55 0.22 265), oklch(0.6 0.2 290), oklch(0.55 0.22 265));"></div>
			<button
				class="rounded-md p-1.5 hover:bg-accent"
				onclick={() => (mobileSheetOpen = true)}
				aria-label="Open navigation"
			>
				<MenuIcon class="h-5 w-5" />
			</button>
			<span class="text-lg font-bold text-primary">G2U Admin</span>
			<div class="ml-auto flex items-center gap-1">
				{#if auth.canEdit}
					<NotificationBell />
				{/if}
			</div>
		</div>

		<!-- Mobile sidebar Sheet -->
		<Sheet.Root bind:open={mobileSheetOpen}>
			<Sheet.Content side="left" class="w-64 p-0 gradient-sidebar border-r-0">
				<Sheet.Header class="border-b border-white/10 px-4 py-3">
					<Sheet.Title class="text-white font-bold">G2U Admin</Sheet.Title>
				</Sheet.Header>
				<nav class="flex-1 space-y-1 p-2">
					{#each navItems as item}
						{#if !item.adminOnly || auth.canEdit}
							{@const IconComponent = iconMap[item.icon]}
							{#if item.separator}
								<div class="my-2 border-t border-white/10"></div>
							{/if}
							<button
								class="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors text-left {isActive(item.href) ? 'bg-white/15 text-white' : 'text-white/70 hover:text-white hover:bg-white/10'}"
								onclick={() => handleMobileNav(item.href)}
							>
								{#if IconComponent}
									<IconComponent class="h-4 w-4 shrink-0" />
								{/if}
								<span>{item.label}</span>
							</button>
						{/if}
					{/each}
				</nav>
				<div class="border-t border-white/10"></div>
				<div class="p-3">
					<button
						class="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors text-white/70 hover:text-white hover:bg-white/10"
						onclick={() => toggleMode()}
					>
						<SunIcon class="h-4 w-4 shrink-0 dark:hidden" />
						<MoonIcon class="h-4 w-4 shrink-0 hidden dark:block" />
						<span>Toggle Theme</span>
					</button>
					{#if auth.user}
						<div class="flex w-full items-center gap-3 rounded-md px-2 py-2 text-sm">
							<Avatar.Root class="h-8 w-8 ring-2 ring-white/20">
								<Avatar.Image src={auth.user.avatarUrl} alt={auth.user.name} />
								<Avatar.Fallback class="bg-white/20 text-white">{auth.user.name?.charAt(0) || '?'}</Avatar.Fallback>
							</Avatar.Root>
							<div class="flex-1 text-left">
								<p class="text-sm font-medium leading-none text-white">{auth.user.name}</p>
								<p class="text-xs text-white/50">{auth.user.role.replace('_', ' ')}</p>
							</div>
						</div>
						<button
							class="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors text-red-300 hover:text-red-200 hover:bg-white/10"
							onclick={() => auth.logout()}
						>
							Sign out
						</button>
					{/if}
				</div>
			</Sheet.Content>
		</Sheet.Root>

		<!-- Desktop Sidebar -->
		<aside
			class="hidden md:flex flex-col gradient-sidebar transition-all duration-200"
			class:w-64={sidebarOpen}
			class:w-16={!sidebarOpen}
		>
			<div class="flex h-14 items-center gap-2 border-b border-white/10 px-4">
				{#if sidebarOpen}
					<span class="text-lg font-bold text-white">G2U Admin</span>
				{/if}
				<div class="ml-auto flex items-center gap-1">
					{#if sidebarOpen && auth.canEdit}
						<div class="text-white/80">
							<NotificationBell />
						</div>
					{/if}
				</div>
				<button
					class="rounded-md p-1.5 text-white/70 hover:text-white hover:bg-white/10 transition-colors"
					onclick={toggleSidebar}
					aria-label={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
				>
					{#if sidebarOpen}
						<ChevronsLeftIcon class="h-4 w-4" />
					{:else}
						<ChevronsRightIcon class="h-4 w-4" />
					{/if}
				</button>
			</div>

			<nav class="flex-1 space-y-1 p-2">
				<Tooltip.Provider delayDuration={0}>
					{#each navItems as item}
						{#if !item.adminOnly || auth.canEdit}
							{@const IconComponent = iconMap[item.icon]}
							{#if item.separator}
								<div class="my-2 border-t border-white/10"></div>
							{/if}
							{#if !sidebarOpen}
								<Tooltip.Root>
									<Tooltip.Trigger>
										{#snippet child({ props })}
											<a
												{...props}
												href={item.href}
												class="flex items-center justify-center rounded-md px-3 py-2 text-sm font-medium transition-colors {isActive(item.href) ? 'bg-white/15 text-white' : 'text-white/70 hover:text-white hover:bg-white/10'}"
											>
												{#if IconComponent}
													<IconComponent class="h-4 w-4 shrink-0" />
												{/if}
											</a>
										{/snippet}
									</Tooltip.Trigger>
									<Tooltip.Content side="right" sideOffset={8}>
										{item.label}
									</Tooltip.Content>
								</Tooltip.Root>
							{:else}
								<a
									href={item.href}
									class="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors {isActive(item.href) ? 'bg-white/15 text-white' : 'text-white/70 hover:text-white hover:bg-white/10'}"
								>
									{#if IconComponent}
										<IconComponent class="h-4 w-4 shrink-0" />
									{/if}
									<span>{item.label}</span>
								</a>
							{/if}
						{/if}
					{/each}
				</Tooltip.Provider>
			</nav>

			<div class="border-t border-white/10"></div>
			<div class="p-3">
				{#if auth.user}
					<DropdownMenu.Root>
						<DropdownMenu.Trigger>
							<button class="flex w-full items-center gap-3 rounded-md px-2 py-2 text-sm text-white/80 hover:text-white hover:bg-white/10 transition-colors">
								<Avatar.Root class="h-8 w-8 ring-2 ring-white/20">
									<Avatar.Image src={auth.user.avatarUrl} alt={auth.user.name} />
									<Avatar.Fallback class="bg-white/20 text-white">{auth.user.name?.charAt(0) || '?'}</Avatar.Fallback>
								</Avatar.Root>
								{#if sidebarOpen}
									<div class="flex-1 text-left">
										<p class="text-sm font-medium leading-none text-white">{auth.user.name}</p>
										<p class="text-xs text-white/50">{auth.user.role.replace('_', ' ')}</p>
									</div>
								{/if}
							</button>
						</DropdownMenu.Trigger>
						<DropdownMenu.Content align="end" class="w-48">
							<DropdownMenu.Label>{auth.user.email}</DropdownMenu.Label>
							<DropdownMenu.Separator />
							<DropdownMenu.Item onclick={() => toggleMode()}>
								<SunIcon class="mr-2 h-4 w-4 dark:hidden" />
								<MoonIcon class="mr-2 h-4 w-4 hidden dark:block" />
								Toggle Theme
							</DropdownMenu.Item>
							<DropdownMenu.Separator />
							<DropdownMenu.Item onclick={() => auth.logout()}>
								Sign out
							</DropdownMenu.Item>
						</DropdownMenu.Content>
					</DropdownMenu.Root>
				{/if}
			</div>
		</aside>

		<!-- Main content -->
		<main class="flex-1 overflow-auto pt-14 md:pt-0">
			<div class="p-4 md:p-6">
				{@render children()}
			</div>
		</main>
	</div>
{/if}
