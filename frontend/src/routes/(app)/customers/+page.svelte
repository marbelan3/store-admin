<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { auth } from '$lib/stores/auth.svelte';
	import {
		getCustomers,
		createCustomer,
		type CustomerList,
		type CreateCustomerRequest
	} from '$lib/api/customers';
	import type { Page } from '$lib/types/product';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Textarea } from '$lib/components/ui/textarea';
	import { Badge } from '$lib/components/ui/badge';
	import * as Table from '$lib/components/ui/table';
	import * as Dialog from '$lib/components/ui/dialog';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';

	import ContactIcon from '@lucide/svelte/icons/contact';
	import SearchIcon from '@lucide/svelte/icons/search';
	import PlusIcon from '@lucide/svelte/icons/plus';
	import ShoppingBagIcon from '@lucide/svelte/icons/shopping-bag';
	import DollarSignIcon from '@lucide/svelte/icons/dollar-sign';

	const PAGE_SIZE = 20;

	let customers = $state<CustomerList[]>([]);
	let totalElements = $state(0);
	let totalPages = $state(0);
	let currentPage = $state(0);
	let isFirst = $state(true);
	let isLast = $state(true);
	let loading = $state(true);
	let searchQuery = $state('');
	let searchTimeout: ReturnType<typeof setTimeout>;

	// Create dialog
	let createDialogOpen = $state(false);
	let creating = $state(false);
	let newCustomer = $state<CreateCustomerRequest>({
		firstName: '',
		lastName: '',
		email: '',
		phone: '',
		notes: ''
	});

	function formatCurrency(amount: number): string {
		return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
	}

	function readUrlParams() {
		const params = $page.url.searchParams;
		const urlPage = params.get('page');
		currentPage = urlPage ? Math.max(0, Number(urlPage) - 1) : 0;
		searchQuery = params.get('search') || '';
	}

	function updateUrl() {
		const params = new URLSearchParams();
		if (currentPage > 0) params.set('page', String(currentPage + 1));
		if (searchQuery) params.set('search', searchQuery);
		const qs = params.toString();
		goto(`/customers${qs ? `?${qs}` : ''}`, { replaceState: true, keepFocus: true, noScroll: true });
	}

	async function loadCustomers() {
		loading = true;
		try {
			const result = await getCustomers({
				page: currentPage,
				size: PAGE_SIZE,
				search: searchQuery || undefined,
				sort: 'createdAt,desc'
			});
			customers = result.content;
			totalElements = result.totalElements;
			totalPages = result.totalPages;
			isFirst = result.first;
			isLast = result.last;
		} catch (err) {
			toast.error('Failed to load customers');
		} finally {
			loading = false;
		}
	}

	function handleSearchInput(e: Event) {
		const value = (e.target as HTMLInputElement).value;
		searchQuery = value;
		clearTimeout(searchTimeout);
		searchTimeout = setTimeout(() => {
			currentPage = 0;
			updateUrl();
			loadCustomers();
		}, 300);
	}

	function goToPage(p: number) {
		currentPage = p;
		updateUrl();
		loadCustomers();
	}

	function openCreateDialog() {
		newCustomer = { firstName: '', lastName: '', email: '', phone: '', notes: '' };
		createDialogOpen = true;
	}

	async function handleCreate() {
		if (!newCustomer.firstName.trim() || !newCustomer.lastName.trim() || !newCustomer.email.trim()) {
			toast.error('First name, last name, and email are required');
			return;
		}
		creating = true;
		try {
			const created = await createCustomer({
				firstName: newCustomer.firstName.trim(),
				lastName: newCustomer.lastName.trim(),
				email: newCustomer.email.trim(),
				phone: newCustomer.phone?.trim() || undefined,
				notes: newCustomer.notes?.trim() || undefined
			});
			toast.success('Customer created successfully');
			createDialogOpen = false;
			goto(`/customers/${created.id}`);
		} catch (err: any) {
			toast.error(err.message || 'Failed to create customer');
		} finally {
			creating = false;
		}
	}

	onMount(() => {
		readUrlParams();
		loadCustomers();
	});
</script>

<!-- Create Customer Dialog -->
<Dialog.Root bind:open={createDialogOpen}>
	<Dialog.Content class="sm:max-w-md">
		<Dialog.Header>
			<Dialog.Title>Add Customer</Dialog.Title>
			<Dialog.Description>Create a new customer record.</Dialog.Description>
		</Dialog.Header>
		<div class="space-y-4 py-2">
			<div class="grid grid-cols-2 gap-4">
				<div class="space-y-2">
					<Label for="firstName">First Name *</Label>
					<Input id="firstName" bind:value={newCustomer.firstName} placeholder="John" />
				</div>
				<div class="space-y-2">
					<Label for="lastName">Last Name *</Label>
					<Input id="lastName" bind:value={newCustomer.lastName} placeholder="Doe" />
				</div>
			</div>
			<div class="space-y-2">
				<Label for="email">Email *</Label>
				<Input id="email" type="email" bind:value={newCustomer.email} placeholder="john@example.com" />
			</div>
			<div class="space-y-2">
				<Label for="phone">Phone</Label>
				<Input id="phone" type="tel" bind:value={newCustomer.phone} placeholder="+1 (555) 000-0000" />
			</div>
			<div class="space-y-2">
				<Label for="notes">Notes</Label>
				<Textarea id="notes" bind:value={newCustomer.notes} placeholder="Optional notes..." rows={3} />
			</div>
		</div>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (createDialogOpen = false)} disabled={creating}>
				Cancel
			</Button>
			<Button onclick={handleCreate} disabled={creating}>
				{creating ? 'Creating...' : 'Create Customer'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<div class="space-y-6">
	<PageHeader
		title="Customers"
		description="Manage your customer base"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Customers' }]}
	>
		{#if auth.canEdit}
			<Button onclick={openCreateDialog}>
				<PlusIcon class="h-4 w-4 mr-2" />
				Add Customer
			</Button>
		{/if}
	</PageHeader>

	<!-- Search -->
	<div class="flex flex-wrap items-center gap-3">
		<div class="relative max-w-xs">
			<SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
			<Input
				placeholder="Search by name or email..."
				value={searchQuery}
				oninput={handleSearchInput}
				class="pl-9 shadow-sm"
			/>
		</div>
		{#if !loading}
			<span class="text-sm text-muted-foreground">{totalElements} customer{totalElements !== 1 ? 's' : ''}</span>
		{/if}
	</div>

	<!-- Table -->
	{#if loading}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Name</Table.Head>
						<Table.Head>Email</Table.Head>
						<Table.Head>Phone</Table.Head>
						<Table.Head>Orders</Table.Head>
						<Table.Head>Total Spent</Table.Head>
						<Table.Head>Joined</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each Array(6) as _}
						<Table.Row>
							<Table.Cell><Skeleton class="h-4 w-32" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-40" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-28" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-8" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-20" /></Table.Cell>
							<Table.Cell><Skeleton class="h-4 w-24" /></Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
	{:else if customers.length === 0}
		<div class="rounded-lg border border-dashed shadow-sm">
			<EmptyState
				icon={ContactIcon}
				title="No customers yet"
				description={searchQuery ? 'Try adjusting your search.' : 'Customers will appear here once added.'}
				actionLabel={auth.canEdit && !searchQuery ? 'Add Customer' : undefined}
				onAction={auth.canEdit && !searchQuery ? openCreateDialog : undefined}
			/>
		</div>
	{:else}
		<div class="rounded-lg border shadow-sm overflow-x-auto">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Name</Table.Head>
						<Table.Head>Email</Table.Head>
						<Table.Head>Phone</Table.Head>
						<Table.Head>Orders</Table.Head>
						<Table.Head>Total Spent</Table.Head>
						<Table.Head>Joined</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each customers as customer}
						<Table.Row
							class="cursor-pointer hover:bg-muted/50 transition-colors"
							onclick={() => goto(`/customers/${customer.id}`)}
						>
							<Table.Cell>
								<p class="font-medium">{customer.firstName} {customer.lastName}</p>
							</Table.Cell>
							<Table.Cell class="text-muted-foreground">{customer.email}</Table.Cell>
							<Table.Cell class="text-muted-foreground">{customer.phone || '—'}</Table.Cell>
							<Table.Cell>
								<div class="flex items-center gap-1.5">
									<ShoppingBagIcon class="h-3.5 w-3.5 text-muted-foreground" />
									<span>{customer.totalOrders}</span>
								</div>
							</Table.Cell>
							<Table.Cell class="font-medium">{formatCurrency(customer.totalSpent)}</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">
								{new Date(customer.createdAt).toLocaleDateString()}
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		<!-- Pagination -->
		{#if totalPages > 1}
			<div class="flex items-center justify-between">
				<p class="text-sm text-muted-foreground">
					Showing {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, totalElements)} of {totalElements} customers
				</p>
				<div class="flex items-center gap-2">
					<Button
						variant="outline"
						size="sm"
						disabled={isFirst}
						onclick={() => goToPage(currentPage - 1)}
					>Previous</Button>
					<span class="text-sm text-muted-foreground">
						Page {currentPage + 1} of {totalPages}
					</span>
					<Button
						variant="outline"
						size="sm"
						disabled={isLast}
						onclick={() => goToPage(currentPage + 1)}
					>Next</Button>
				</div>
			</div>
		{/if}
	{/if}
</div>
