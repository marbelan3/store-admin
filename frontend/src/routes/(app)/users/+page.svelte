<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/stores/auth.svelte';
	import { getUsers, updateUserRole, toggleUserActive, type UserInfo } from '$lib/api/users';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Badge } from '$lib/components/ui/badge';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Avatar from '$lib/components/ui/avatar';
	import * as Table from '$lib/components/ui/table';
	import * as Select from '$lib/components/ui/select';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import SearchIcon from '@lucide/svelte/icons/search';
	import UsersIcon from '@lucide/svelte/icons/users';

	let users = $state<UserInfo[]>([]);
	let loading = $state(true);
	let searchQuery = $state('');
	let roleFilter = $state('');
	let sortField = $state('');

	// Confirm dialog state
	let confirmOpen = $state(false);
	let confirmTitle = $state('');
	let confirmDescription = $state('');
	let confirmLabel = $state('Confirm');
	let confirmVariant = $state<'destructive' | 'default'>('destructive');
	let confirmAction = $state<() => Promise<void>>(async () => {});
	let sortDir = $state<'asc' | 'desc'>('asc');

	function roleBadgeClass(role: string): string {
		switch (role) {
			case 'SUPER_ADMIN': return 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400 border-indigo-200 dark:border-indigo-800';
			case 'TENANT_ADMIN': return 'bg-violet-100 text-violet-700 dark:bg-violet-900/30 dark:text-violet-400 border-violet-200 dark:border-violet-800';
			case 'TENANT_VIEWER': return 'bg-gray-100 text-gray-600 dark:bg-gray-800/30 dark:text-gray-400 border-gray-200 dark:border-gray-700';
			default: return '';
		}
	}

	function toggleSort(field: string) {
		if (sortField === field) {
			sortDir = sortDir === 'asc' ? 'desc' : 'asc';
		} else {
			sortField = field;
			sortDir = 'asc';
		}
	}

	function sortIndicator(field: string): string {
		if (sortField !== field) return '';
		return sortDir === 'asc' ? ' \u2191' : ' \u2193';
	}

	let filteredAndSortedUsers = $derived(() => {
		let result = users;

		// Apply search filter
		if (searchQuery.trim()) {
			const q = searchQuery.toLowerCase().trim();
			result = result.filter(
				(u) =>
					(u.name && u.name.toLowerCase().includes(q)) ||
					(u.email && u.email.toLowerCase().includes(q))
			);
		}

		// Apply role filter
		if (roleFilter) {
			result = result.filter((u) => u.role === roleFilter);
		}

		// Apply sorting
		if (!sortField) return result;
		return [...result].sort((a, b) => {
			let aVal: string | number = '';
			let bVal: string | number = '';
			switch (sortField) {
				case 'name': aVal = a.name?.toLowerCase() || ''; bVal = b.name?.toLowerCase() || ''; break;
				case 'email': aVal = a.email?.toLowerCase() || ''; bVal = b.email?.toLowerCase() || ''; break;
				case 'role': aVal = a.role; bVal = b.role; break;
				case 'createdAt': aVal = a.createdAt || ''; bVal = b.createdAt || ''; break;
			}
			if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
			if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
			return 0;
		});
	});

	async function loadUsers() {
		loading = true;
		try {
			users = await getUsers();
		} catch {
			toast.error('Failed to load users');
		} finally {
			loading = false;
		}
	}

	function handleRoleChange(userId: string, newRole: string) {
		const user = users.find((u) => u.id === userId);
		if (!user) return;
		const roleName = newRole.replace(/_/g, ' ');
		confirmTitle = 'Change user role';
		confirmDescription = `Are you sure you want to change "${user.name}"'s role to ${roleName}?`;
		confirmLabel = 'Change Role';
		confirmVariant = 'default';
		confirmAction = async () => {
			try {
				const updated = await updateUserRole(userId, newRole);
				users = users.map((u) => (u.id === userId ? updated : u));
				toast.success('Role updated');
			} catch {
				toast.error('Failed to update role');
			}
		};
		confirmOpen = true;
	}

	function handleRoleFilterChange(value: string) {
		roleFilter = value === '__all__' ? '' : value;
	}

	function handleToggleActive(user: UserInfo) {
		const action = user.active ? 'deactivate' : 'activate';
		confirmTitle = `${user.active ? 'Deactivate' : 'Activate'} user`;
		confirmDescription = `Are you sure you want to ${action} "${user.name}"?${user.active ? ' They will lose access to the admin panel.' : ''}`;
		confirmLabel = user.active ? 'Deactivate' : 'Activate';
		confirmVariant = 'destructive';
		confirmAction = async () => {
			try {
				const updated = await toggleUserActive(user.id);
				users = users.map((u) => (u.id === user.id ? updated : u));
				toast.success(updated.active ? 'User activated' : 'User deactivated');
			} catch {
				toast.error('Failed to update user');
			}
		};
		confirmOpen = true;
	}

	onMount(loadUsers);
</script>

<ConfirmDialog
	bind:open={confirmOpen}
	title={confirmTitle}
	description={confirmDescription}
	confirmLabel={confirmLabel}
	variant={confirmVariant}
	onConfirm={confirmAction}
/>

<div class="space-y-6">
	<PageHeader
		title="Users"
		description="Manage team members and permissions"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Users' }]}
	/>

	<!-- Search and Filters -->
	<div class="flex flex-wrap items-center gap-3">
		<div class="relative max-w-xs">
			<SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
			<Input
				placeholder="Search by name or email..."
				value={searchQuery}
				oninput={(e: Event) => { searchQuery = (e.target as HTMLInputElement).value; }}
				class="pl-9 shadow-sm"
			/>
		</div>
		<Select.Root type="single" value={roleFilter || '__all__'} onValueChange={handleRoleFilterChange}>
			<Select.Trigger class="w-[180px]">
				{roleFilter ? roleFilter.replace(/_/g, ' ') : 'All Roles'}
			</Select.Trigger>
			<Select.Content>
				<Select.Item value="__all__">All Roles</Select.Item>
				<Select.Item value="SUPER_ADMIN">Super Admin</Select.Item>
				<Select.Item value="TENANT_ADMIN">Tenant Admin</Select.Item>
				<Select.Item value="TENANT_VIEWER">Tenant Viewer</Select.Item>
			</Select.Content>
		</Select.Root>
	</div>

	{#if loading}
		<div class="rounded-md border overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>User</Table.Head>
						<Table.Head>Email</Table.Head>
						<Table.Head>Role</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head>Joined</Table.Head>
						<Table.Head class="w-[100px]">Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each Array(6) as _}
						<Table.Row>
							<Table.Cell>
								<div class="flex items-center gap-3">
									<Skeleton class="h-8 w-8 rounded-full" />
									<Skeleton class="h-4 w-24" />
								</div>
							</Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-36" /></Table.Cell>
							<Table.Cell><Skeleton class="h-7 w-[160px]" /></Table.Cell>
							<Table.Cell><Skeleton class="h-5 w-16 rounded-full" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-7 w-20" /></Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{:else if users.length === 0}
		<div class="rounded-lg border border-dashed">
			<EmptyState
				icon={UsersIcon}
				title="No team members"
				description="Invite your first team member to get started."
			/>
		</div>
	{:else}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>
							<button class="flex items-center gap-1 font-medium hover:text-foreground transition-colors" onclick={() => toggleSort('name')}>
								User{sortIndicator('name')}
							</button>
						</Table.Head>
						<Table.Head>
							<button class="flex items-center gap-1 font-medium hover:text-foreground transition-colors" onclick={() => toggleSort('email')}>
								Email{sortIndicator('email')}
							</button>
						</Table.Head>
						<Table.Head>
							<button class="flex items-center gap-1 font-medium hover:text-foreground transition-colors" onclick={() => toggleSort('role')}>
								Role{sortIndicator('role')}
							</button>
						</Table.Head>
						<Table.Head>Status</Table.Head>
						<Table.Head>
							<button class="flex items-center gap-1 font-medium hover:text-foreground transition-colors" onclick={() => toggleSort('createdAt')}>
								Joined{sortIndicator('createdAt')}
							</button>
						</Table.Head>
						<Table.Head class="w-[100px]">Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each filteredAndSortedUsers() as user}
						<Table.Row class="hover:bg-muted/50 transition-colors">
							<Table.Cell>
								<div class="flex items-center gap-3">
									<Avatar.Root class="h-8 w-8">
										<Avatar.Image src={user.avatarUrl} alt={user.name} />
										<Avatar.Fallback>{user.name?.charAt(0) || '?'}</Avatar.Fallback>
									</Avatar.Root>
									<span class="font-medium">{user.name}</span>
								</div>
							</Table.Cell>
							<Table.Cell>{user.email}</Table.Cell>
							<Table.Cell>
								{#if auth.isSuperAdmin || auth.isTenantAdmin}
									<Select.Root
										type="single"
										value={user.role}
										onValueChange={(v) => handleRoleChange(user.id, v)}
									>
										<Select.Trigger class="w-[160px]">
											{user.role.replace(/_/g, ' ')}
										</Select.Trigger>
										<Select.Content>
											<Select.Item value="TENANT_ADMIN">Tenant Admin</Select.Item>
											<Select.Item value="TENANT_VIEWER">Tenant Viewer</Select.Item>
										</Select.Content>
									</Select.Root>
								{:else}
									<Badge variant="outline" class={roleBadgeClass(user.role)}>{user.role.replace(/_/g, ' ')}</Badge>
								{/if}
							</Table.Cell>
							<Table.Cell>
								<div class="flex items-center gap-2">
									<span class="h-2 w-2 rounded-full {user.active ? 'bg-emerald-500' : 'bg-gray-400'}"></span>
									<span class="text-sm {user.active ? 'text-emerald-700 dark:text-emerald-400' : 'text-muted-foreground'}">{user.active ? 'Active' : 'Inactive'}</span>
								</div>
							</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{new Date(user.createdAt).toLocaleDateString()}
							</Table.Cell>
							<Table.Cell>
								{#if auth.canEdit && user.id !== auth.user?.id}
									<Button
										variant="ghost"
										size="sm"
										onclick={() => handleToggleActive(user)}
									>
										{user.active ? 'Deactivate' : 'Activate'}
									</Button>
								{/if}
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{/if}
</div>
