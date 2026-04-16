<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { auth } from '$lib/stores/auth.svelte';
	import {
		getCustomer,
		updateCustomer,
		deleteCustomer,
		addAddress,
		updateAddress,
		deleteAddress,
		type Customer,
		type CustomerAddress,
		type UpdateCustomerRequest,
		type CreateAddressRequest
	} from '$lib/api/customers';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Textarea } from '$lib/components/ui/textarea';
	import { Badge } from '$lib/components/ui/badge';
	import * as Card from '$lib/components/ui/card';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Select from '$lib/components/ui/select';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Separator } from '$lib/components/ui/separator';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import ActivityTimeline from '$lib/components/ActivityTimeline.svelte';

	import ArrowLeftIcon from '@lucide/svelte/icons/arrow-left';
	import ClockIcon from '@lucide/svelte/icons/clock';
	import UserIcon from '@lucide/svelte/icons/user';
	import MailIcon from '@lucide/svelte/icons/mail';
	import PhoneIcon from '@lucide/svelte/icons/phone';
	import MapPinIcon from '@lucide/svelte/icons/map-pin';
	import PlusIcon from '@lucide/svelte/icons/plus';
	import PencilIcon from '@lucide/svelte/icons/pencil';
	import TrashIcon from '@lucide/svelte/icons/trash-2';
	import ShoppingBagIcon from '@lucide/svelte/icons/shopping-bag';
	import DollarSignIcon from '@lucide/svelte/icons/dollar-sign';
	import StarIcon from '@lucide/svelte/icons/star';
	import ExternalLinkIcon from '@lucide/svelte/icons/external-link';
	import SaveIcon from '@lucide/svelte/icons/save';

	let customerId = $derived(Number($page.params.id));
	let customer = $state<Customer | null>(null);
	let loading = $state(true);

	// Profile editing
	let profileForm = $state<UpdateCustomerRequest>({
		firstName: '',
		lastName: '',
		email: '',
		phone: '',
		notes: ''
	});
	let saving = $state(false);

	// Delete confirmation
	let deleteDialogOpen = $state(false);
	let deleting = $state(false);

	// Address dialog
	let addressDialogOpen = $state(false);
	let editingAddress = $state<CustomerAddress | null>(null);
	let addressForm = $state<CreateAddressRequest>({
		type: 'SHIPPING',
		line1: '',
		line2: '',
		city: '',
		state: '',
		postalCode: '',
		country: '',
		isDefault: false
	});
	let addressSaving = $state(false);

	// Delete address confirmation
	let deleteAddressDialogOpen = $state(false);
	let addressToDelete = $state<CustomerAddress | null>(null);
	let addressDeleting = $state(false);

	function formatCurrency(amount: number): string {
		return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
	}

	function populateProfileForm(c: Customer) {
		profileForm = {
			firstName: c.firstName,
			lastName: c.lastName,
			email: c.email,
			phone: c.phone || '',
			notes: c.notes || ''
		};
	}

	async function loadCustomer() {
		loading = true;
		try {
			customer = await getCustomer(customerId);
			populateProfileForm(customer);
		} catch (err) {
			toast.error('Failed to load customer');
			goto('/customers');
		} finally {
			loading = false;
		}
	}

	async function handleSaveProfile() {
		if (!profileForm.firstName.trim() || !profileForm.lastName.trim() || !profileForm.email.trim()) {
			toast.error('First name, last name, and email are required');
			return;
		}
		saving = true;
		try {
			customer = await updateCustomer(customerId, {
				firstName: profileForm.firstName.trim(),
				lastName: profileForm.lastName.trim(),
				email: profileForm.email.trim(),
				phone: profileForm.phone?.trim() || undefined,
				notes: profileForm.notes?.trim() || undefined
			});
			populateProfileForm(customer);
			toast.success('Customer updated successfully');
		} catch (err: any) {
			toast.error(err.message || 'Failed to update customer');
		} finally {
			saving = false;
		}
	}

	async function handleDelete() {
		deleting = true;
		try {
			await deleteCustomer(customerId);
			toast.success('Customer deleted');
			goto('/customers');
		} catch (err: any) {
			toast.error(err.message || 'Failed to delete customer');
		} finally {
			deleting = false;
		}
	}

	// ── Address management ─────────────────────────────────────────

	function openAddAddress() {
		editingAddress = null;
		addressForm = {
			type: 'SHIPPING',
			line1: '',
			line2: '',
			city: '',
			state: '',
			postalCode: '',
			country: '',
			isDefault: false
		};
		addressDialogOpen = true;
	}

	function openEditAddress(addr: CustomerAddress) {
		editingAddress = addr;
		addressForm = {
			type: addr.type,
			line1: addr.line1,
			line2: addr.line2 || '',
			city: addr.city,
			state: addr.state,
			postalCode: addr.postalCode,
			country: addr.country,
			isDefault: addr.isDefault
		};
		addressDialogOpen = true;
	}

	async function handleSaveAddress() {
		if (!addressForm.line1.trim() || !addressForm.city.trim() || !addressForm.postalCode.trim() || !addressForm.country.trim()) {
			toast.error('Address line 1, city, postal code, and country are required');
			return;
		}
		addressSaving = true;
		try {
			const data: CreateAddressRequest = {
				type: addressForm.type,
				line1: addressForm.line1.trim(),
				line2: addressForm.line2?.trim() || undefined,
				city: addressForm.city.trim(),
				state: addressForm.state.trim(),
				postalCode: addressForm.postalCode.trim(),
				country: addressForm.country.trim(),
				isDefault: addressForm.isDefault
			};
			if (editingAddress) {
				await updateAddress(customerId, editingAddress.id, data);
				toast.success('Address updated');
			} else {
				await addAddress(customerId, data);
				toast.success('Address added');
			}
			addressDialogOpen = false;
			await loadCustomer();
		} catch (err: any) {
			toast.error(err.message || 'Failed to save address');
		} finally {
			addressSaving = false;
		}
	}

	function confirmDeleteAddress(addr: CustomerAddress) {
		addressToDelete = addr;
		deleteAddressDialogOpen = true;
	}

	async function handleDeleteAddress() {
		if (!addressToDelete) return;
		addressDeleting = true;
		try {
			await deleteAddress(customerId, addressToDelete.id);
			toast.success('Address deleted');
			deleteAddressDialogOpen = false;
			await loadCustomer();
		} catch (err: any) {
			toast.error(err.message || 'Failed to delete address');
		} finally {
			addressDeleting = false;
		}
	}

	function handleAddressTypeChange(value: string) {
		addressForm.type = value as 'SHIPPING' | 'BILLING';
	}

	onMount(() => {
		loadCustomer();
	});
</script>

<!-- Address Dialog -->
<Dialog.Root bind:open={addressDialogOpen}>
	<Dialog.Content class="sm:max-w-lg">
		<Dialog.Header>
			<Dialog.Title>{editingAddress ? 'Edit Address' : 'Add Address'}</Dialog.Title>
			<Dialog.Description>
				{editingAddress ? 'Update the address details.' : 'Add a new address for this customer.'}
			</Dialog.Description>
		</Dialog.Header>
		<div class="space-y-4 py-2">
			<div class="grid grid-cols-2 gap-4">
				<div class="space-y-2">
					<Label>Type</Label>
					<Select.Root type="single" value={addressForm.type} onValueChange={handleAddressTypeChange}>
						<Select.Trigger class="w-full">
							{addressForm.type === 'SHIPPING' ? 'Shipping' : 'Billing'}
						</Select.Trigger>
						<Select.Content>
							<Select.Item value="SHIPPING">Shipping</Select.Item>
							<Select.Item value="BILLING">Billing</Select.Item>
						</Select.Content>
					</Select.Root>
				</div>
				<div class="flex items-end">
					<label class="flex items-center gap-2 text-sm">
						<input type="checkbox" bind:checked={addressForm.isDefault} class="rounded border-input" />
						Default address
					</label>
				</div>
			</div>
			<div class="space-y-2">
				<Label for="addr-line1">Address Line 1 *</Label>
				<Input id="addr-line1" bind:value={addressForm.line1} placeholder="123 Main St" />
			</div>
			<div class="space-y-2">
				<Label for="addr-line2">Address Line 2</Label>
				<Input id="addr-line2" bind:value={addressForm.line2} placeholder="Apt 4B" />
			</div>
			<div class="grid grid-cols-2 gap-4">
				<div class="space-y-2">
					<Label for="addr-city">City *</Label>
					<Input id="addr-city" bind:value={addressForm.city} placeholder="New York" />
				</div>
				<div class="space-y-2">
					<Label for="addr-state">State</Label>
					<Input id="addr-state" bind:value={addressForm.state} placeholder="NY" />
				</div>
			</div>
			<div class="grid grid-cols-2 gap-4">
				<div class="space-y-2">
					<Label for="addr-zip">Postal Code *</Label>
					<Input id="addr-zip" bind:value={addressForm.postalCode} placeholder="10001" />
				</div>
				<div class="space-y-2">
					<Label for="addr-country">Country *</Label>
					<Input id="addr-country" bind:value={addressForm.country} placeholder="US" />
				</div>
			</div>
		</div>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (addressDialogOpen = false)} disabled={addressSaving}>
				Cancel
			</Button>
			<Button onclick={handleSaveAddress} disabled={addressSaving}>
				{addressSaving ? 'Saving...' : editingAddress ? 'Update Address' : 'Add Address'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<!-- Delete Address Confirmation -->
<Dialog.Root bind:open={deleteAddressDialogOpen}>
	<Dialog.Content class="sm:max-w-sm">
		<Dialog.Header>
			<Dialog.Title>Delete Address</Dialog.Title>
			<Dialog.Description>Are you sure you want to delete this address? This action cannot be undone.</Dialog.Description>
		</Dialog.Header>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (deleteAddressDialogOpen = false)} disabled={addressDeleting}>
				Cancel
			</Button>
			<Button variant="destructive" onclick={handleDeleteAddress} disabled={addressDeleting}>
				{addressDeleting ? 'Deleting...' : 'Delete'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<!-- Delete Customer Confirmation -->
<Dialog.Root bind:open={deleteDialogOpen}>
	<Dialog.Content class="sm:max-w-sm">
		<Dialog.Header>
			<Dialog.Title>Delete Customer</Dialog.Title>
			<Dialog.Description>
				Are you sure you want to delete this customer? All associated data will be permanently removed.
			</Dialog.Description>
		</Dialog.Header>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (deleteDialogOpen = false)} disabled={deleting}>
				Cancel
			</Button>
			<Button variant="destructive" onclick={handleDelete} disabled={deleting}>
				{deleting ? 'Deleting...' : 'Delete Customer'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>

<div class="space-y-6">
	{#if loading}
		<div class="space-y-4">
			<Skeleton class="h-6 w-48" />
			<Skeleton class="h-4 w-32" />
			<div class="grid gap-6 lg:grid-cols-3">
				<div class="lg:col-span-2 space-y-6">
					<Skeleton class="h-64 w-full rounded-lg" />
					<Skeleton class="h-48 w-full rounded-lg" />
				</div>
				<div class="space-y-6">
					<Skeleton class="h-40 w-full rounded-lg" />
					<Skeleton class="h-24 w-full rounded-lg" />
				</div>
			</div>
		</div>
	{:else if customer}
		<PageHeader
			title="{customer.firstName} {customer.lastName}"
			description="Customer since {new Date(customer.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}"
			breadcrumbs={[
				{ label: 'Home', href: '/dashboard' },
				{ label: 'Customers', href: '/customers' },
				{ label: `${customer.firstName} ${customer.lastName}` }
			]}
		>
			<Button variant="outline" onclick={() => goto('/customers')}>
				<ArrowLeftIcon class="h-4 w-4 mr-2" />
				Back
			</Button>
			{#if auth.canEdit}
				<Button variant="destructive" size="sm" onclick={() => (deleteDialogOpen = true)}>
					<TrashIcon class="h-4 w-4 mr-2" />
					Delete
				</Button>
			{/if}
		</PageHeader>

		<div class="grid gap-6 lg:grid-cols-3">
			<!-- Left Column -->
			<div class="lg:col-span-2 space-y-6">
				<!-- Profile Card -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title class="flex items-center gap-2">
							<UserIcon class="h-4 w-4" />
							Profile
						</Card.Title>
					</Card.Header>
					<Card.Content>
						<div class="space-y-4">
							<div class="grid grid-cols-2 gap-4">
								<div class="space-y-2">
									<Label for="edit-firstName">First Name</Label>
									<Input id="edit-firstName" bind:value={profileForm.firstName} disabled={!auth.canEdit} />
								</div>
								<div class="space-y-2">
									<Label for="edit-lastName">Last Name</Label>
									<Input id="edit-lastName" bind:value={profileForm.lastName} disabled={!auth.canEdit} />
								</div>
							</div>
							<div class="grid grid-cols-2 gap-4">
								<div class="space-y-2">
									<Label for="edit-email">Email</Label>
									<Input id="edit-email" type="email" bind:value={profileForm.email} disabled={!auth.canEdit} />
								</div>
								<div class="space-y-2">
									<Label for="edit-phone">Phone</Label>
									<Input id="edit-phone" type="tel" bind:value={profileForm.phone} disabled={!auth.canEdit} />
								</div>
							</div>
							<div class="space-y-2">
								<Label for="edit-notes">Notes</Label>
								<Textarea id="edit-notes" bind:value={profileForm.notes} rows={3} disabled={!auth.canEdit} placeholder="Internal notes about this customer..." />
							</div>
							{#if auth.canEdit}
								<div class="flex justify-end">
									<Button onclick={handleSaveProfile} disabled={saving}>
										<SaveIcon class="h-4 w-4 mr-2" />
										{saving ? 'Saving...' : 'Save Changes'}
									</Button>
								</div>
							{/if}
						</div>
					</Card.Content>
				</Card.Root>

				<!-- Addresses Card -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<div class="flex items-center justify-between">
							<Card.Title class="flex items-center gap-2">
								<MapPinIcon class="h-4 w-4" />
								Addresses
							</Card.Title>
							{#if auth.canEdit}
								<Button size="sm" variant="outline" onclick={openAddAddress}>
									<PlusIcon class="h-4 w-4 mr-1" />
									Add Address
								</Button>
							{/if}
						</div>
					</Card.Header>
					<Card.Content>
						{#if customer.addresses.length === 0}
							<p class="text-sm text-muted-foreground text-center py-6">No addresses on file.</p>
						{:else}
							<div class="space-y-4">
								{#each customer.addresses as addr}
									<div class="rounded-lg border p-4">
										<div class="flex items-start justify-between gap-3">
											<div class="flex-1">
												<div class="flex items-center gap-2 mb-2">
													<Badge variant="outline" class={addr.type === 'SHIPPING'
														? 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-900/20 dark:text-blue-400 dark:border-blue-800'
														: 'bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-900/20 dark:text-purple-400 dark:border-purple-800'
													}>
														{addr.type}
													</Badge>
													{#if addr.isDefault}
														<Badge variant="outline" class="bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-900/20 dark:text-amber-400 dark:border-amber-800">
															<StarIcon class="h-3 w-3 mr-1" />
															Default
														</Badge>
													{/if}
												</div>
												<p class="text-sm">{addr.line1}</p>
												{#if addr.line2}
													<p class="text-sm text-muted-foreground">{addr.line2}</p>
												{/if}
												<p class="text-sm text-muted-foreground">
													{addr.city}{addr.state ? `, ${addr.state}` : ''} {addr.postalCode}
												</p>
												<p class="text-sm text-muted-foreground">{addr.country}</p>
											</div>
											{#if auth.canEdit}
												<div class="flex gap-1 shrink-0">
													<Button size="sm" variant="ghost" onclick={() => openEditAddress(addr)}>
														<PencilIcon class="h-3.5 w-3.5" />
													</Button>
													<Button size="sm" variant="ghost" class="text-destructive hover:text-destructive" onclick={() => confirmDeleteAddress(addr)}>
														<TrashIcon class="h-3.5 w-3.5" />
													</Button>
												</div>
											{/if}
										</div>
									</div>
								{/each}
							</div>
						{/if}
					</Card.Content>
				</Card.Root>
			</div>

			<!-- Right Column -->
			<div class="space-y-6">
				<!-- Stats Card -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title>Customer Stats</Card.Title>
					</Card.Header>
					<Card.Content>
						<div class="space-y-4">
							<div class="flex items-center gap-3">
								<div class="rounded-full bg-blue-100 dark:bg-blue-900/30 p-2.5">
									<ShoppingBagIcon class="h-5 w-5 text-blue-600 dark:text-blue-400" />
								</div>
								<div>
									<p class="text-xs text-muted-foreground">Total Orders</p>
									<p class="text-2xl font-bold">{customer.totalOrders}</p>
								</div>
							</div>
							<Separator />
							<div class="flex items-center gap-3">
								<div class="rounded-full bg-emerald-100 dark:bg-emerald-900/30 p-2.5">
									<DollarSignIcon class="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
								</div>
								<div>
									<p class="text-xs text-muted-foreground">Total Spent</p>
									<p class="text-2xl font-bold">{formatCurrency(customer.totalSpent)}</p>
								</div>
							</div>
						</div>
					</Card.Content>
				</Card.Root>

				<!-- Order History Link -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title class="flex items-center gap-2">
							<ShoppingBagIcon class="h-4 w-4" />
							Order History
						</Card.Title>
					</Card.Header>
					<Card.Content>
						<p class="text-sm text-muted-foreground mb-4">
							View all orders placed by this customer.
						</p>
						<Button
							variant="outline"
							class="w-full"
							onclick={() => goto(`/orders?search=${encodeURIComponent(customer?.email || '')}`)}
						>
							<ExternalLinkIcon class="h-4 w-4 mr-2" />
							View Orders
						</Button>
					</Card.Content>
				</Card.Root>

				<!-- Recent Activity -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title class="flex items-center gap-2">
							<ClockIcon class="h-4 w-4" />
							Recent Activity
						</Card.Title>
					</Card.Header>
					<Card.Content>
						<ActivityTimeline entityType="CUSTOMER" entityId={String(customer.id)} />
					</Card.Content>
				</Card.Root>

				<!-- Metadata -->
				<Card.Root class="shadow-sm">
					<Card.Header>
						<Card.Title>Details</Card.Title>
					</Card.Header>
					<Card.Content>
						<div class="space-y-3 text-sm">
							<div class="flex justify-between">
								<span class="text-muted-foreground">Customer ID</span>
								<span class="font-mono">#{customer.id}</span>
							</div>
							<div class="flex justify-between">
								<span class="text-muted-foreground">Created</span>
								<span>{new Date(customer.createdAt).toLocaleDateString()}</span>
							</div>
							<div class="flex justify-between">
								<span class="text-muted-foreground">Updated</span>
								<span>{new Date(customer.updatedAt).toLocaleDateString()}</span>
							</div>
						</div>
					</Card.Content>
				</Card.Root>
			</div>
		</div>
	{/if}
</div>
