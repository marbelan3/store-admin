<script lang="ts">
	import { onMount } from 'svelte';
	import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, type Notification } from '$lib/api/notifications';
	import * as DropdownMenu from '$lib/components/ui/dropdown-menu';
	import { Button } from '$lib/components/ui/button';

	let unreadCount = $state(0);
	let notifications = $state<Notification[]>([]);
	let loaded = $state(false);

	async function loadUnreadCount() {
		try {
			const result = await getUnreadCount();
			unreadCount = result.count;
		} catch {
			// non-critical
		}
	}

	async function loadNotifications() {
		try {
			const result = await getNotifications(0, 10);
			notifications = result.content;
			loaded = true;
		} catch {
			// non-critical
		}
	}

	async function handleMarkAsRead(id: string) {
		try {
			await markAsRead(id);
			notifications = notifications.map((n) =>
				n.id === id ? { ...n, read: true } : n
			);
			unreadCount = Math.max(0, unreadCount - 1);
		} catch {
			// ignore
		}
	}

	async function handleMarkAllRead() {
		try {
			await markAllAsRead();
			notifications = notifications.map((n) => ({ ...n, read: true }));
			unreadCount = 0;
		} catch {
			// ignore
		}
	}

	onMount(() => {
		loadUnreadCount();
		// Poll every 60 seconds
		const interval = setInterval(loadUnreadCount, 60000);
		return () => clearInterval(interval);
	});
</script>

<DropdownMenu.Root onOpenChange={(open) => { if (open && !loaded) loadNotifications(); }}>
	<DropdownMenu.Trigger>
		<button class="relative rounded-md p-2 hover:bg-accent transition-colors" aria-label="Notifications">
			<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
				<path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/>
			</svg>
			{#if unreadCount > 0}
				<span class="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-destructive text-[10px] font-bold text-destructive-foreground">
					{unreadCount > 9 ? '9+' : unreadCount}
				</span>
			{/if}
		</button>
	</DropdownMenu.Trigger>
	<DropdownMenu.Content align="end" class="w-80">
		<div class="flex items-center justify-between px-3 py-2">
			<span class="text-sm font-medium">Notifications</span>
			{#if unreadCount > 0}
				<button
					class="text-xs text-primary hover:underline"
					onclick={handleMarkAllRead}
				>
					Mark all read
				</button>
			{/if}
		</div>
		<DropdownMenu.Separator />
		{#if notifications.length === 0}
			<div class="px-3 py-6 text-center text-sm text-muted-foreground">
				No notifications
			</div>
		{:else}
			<div class="max-h-64 overflow-y-auto">
				{#each notifications as notification}
					<DropdownMenu.Item
						class="flex flex-col items-start gap-1 px-3 py-2 {notification.read ? 'opacity-60' : ''}"
						onclick={() => { if (!notification.read) handleMarkAsRead(notification.id); }}
					>
						<div class="flex w-full items-center gap-2">
							{#if !notification.read}
								<span class="h-2 w-2 rounded-full bg-primary flex-shrink-0"></span>
							{/if}
							<span class="text-sm font-medium truncate">{notification.title}</span>
						</div>
						{#if notification.message}
							<span class="text-xs text-muted-foreground line-clamp-2 pl-4">{notification.message}</span>
						{/if}
						<span class="text-xs text-muted-foreground pl-4">
							{new Date(notification.createdAt).toLocaleString()}
						</span>
					</DropdownMenu.Item>
				{/each}
			</div>
		{/if}
	</DropdownMenu.Content>
</DropdownMenu.Root>
