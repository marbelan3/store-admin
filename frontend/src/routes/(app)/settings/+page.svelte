<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/stores/auth.svelte';
	import { getTenant, updateTenant, type TenantInfo } from '$lib/api/tenant';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Card from '$lib/components/ui/card';
	import * as Select from '$lib/components/ui/select';
	import { toast } from 'svelte-sonner';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import { createUnsavedChanges } from '$lib/hooks/useUnsavedChanges.svelte';

	const currencies = [
		{ value: 'UAH', label: 'UAH - Ukrainian Hryvnia' },
		{ value: 'USD', label: 'USD - US Dollar' },
		{ value: 'EUR', label: 'EUR - Euro' },
		{ value: 'GBP', label: 'GBP - British Pound' },
		{ value: 'PLN', label: 'PLN - Polish Zloty' },
		{ value: 'CZK', label: 'CZK - Czech Koruna' },
		{ value: 'CAD', label: 'CAD - Canadian Dollar' },
		{ value: 'AUD', label: 'AUD - Australian Dollar' },
		{ value: 'JPY', label: 'JPY - Japanese Yen' },
		{ value: 'CNY', label: 'CNY - Chinese Yuan' },
		{ value: 'INR', label: 'INR - Indian Rupee' },
		{ value: 'BRL', label: 'BRL - Brazilian Real' },
		{ value: 'KRW', label: 'KRW - South Korean Won' },
		{ value: 'CHF', label: 'CHF - Swiss Franc' },
		{ value: 'SEK', label: 'SEK - Swedish Krona' },
		{ value: 'TRY', label: 'TRY - Turkish Lira' },
	];

	const timezones = [
		{ value: 'Europe/Kyiv', label: 'Europe/Kyiv (UTC+2)' },
		{ value: 'Europe/Warsaw', label: 'Europe/Warsaw (UTC+1)' },
		{ value: 'Europe/Berlin', label: 'Europe/Berlin (UTC+1)' },
		{ value: 'Europe/London', label: 'Europe/London (UTC+0)' },
		{ value: 'Europe/Paris', label: 'Europe/Paris (UTC+1)' },
		{ value: 'Europe/Prague', label: 'Europe/Prague (UTC+1)' },
		{ value: 'Europe/Istanbul', label: 'Europe/Istanbul (UTC+3)' },
		{ value: 'America/New_York', label: 'America/New York (UTC-5)' },
		{ value: 'America/Chicago', label: 'America/Chicago (UTC-6)' },
		{ value: 'America/Denver', label: 'America/Denver (UTC-7)' },
		{ value: 'America/Los_Angeles', label: 'America/Los Angeles (UTC-8)' },
		{ value: 'America/Sao_Paulo', label: 'America/Sao Paulo (UTC-3)' },
		{ value: 'Asia/Tokyo', label: 'Asia/Tokyo (UTC+9)' },
		{ value: 'Asia/Shanghai', label: 'Asia/Shanghai (UTC+8)' },
		{ value: 'Asia/Dubai', label: 'Asia/Dubai (UTC+4)' },
		{ value: 'Asia/Kolkata', label: 'Asia/Kolkata (UTC+5:30)' },
		{ value: 'Asia/Seoul', label: 'Asia/Seoul (UTC+9)' },
		{ value: 'Australia/Sydney', label: 'Australia/Sydney (UTC+11)' },
		{ value: 'Pacific/Auckland', label: 'Pacific/Auckland (UTC+12)' },
	];

	let tenant = $state<TenantInfo | null>(null);
	let loading = $state(true);
	let saving = $state(false);

	let name = $state('');
	let logoUrl = $state('');
	let currency = $state('UAH');
	let timezone = $state('Europe/Kyiv');

	// Track original values for dirty detection
	let originalName = $state('');
	let originalLogoUrl = $state('');
	let originalCurrency = $state('UAH');
	let originalTimezone = $state('Europe/Kyiv');

	const unsaved = createUnsavedChanges();

	let isDirty = $derived(
		name !== originalName ||
		logoUrl !== originalLogoUrl ||
		currency !== originalCurrency ||
		timezone !== originalTimezone
	);

	$effect(() => {
		unsaved.setDirty(isDirty);
	});

	onMount(async () => {
		try {
			tenant = await getTenant();
			name = tenant.name;
			logoUrl = tenant.logoUrl || '';
			currency = (tenant.settings?.currency as string) || 'UAH';
			timezone = (tenant.settings?.timezone as string) || 'Europe/Kyiv';
			originalName = name;
			originalLogoUrl = logoUrl;
			originalCurrency = currency;
			originalTimezone = timezone;
		} catch {
			toast.error('Failed to load settings');
		} finally {
			loading = false;
		}
	});

	async function handleSave(e: SubmitEvent) {
		e.preventDefault();
		saving = true;
		try {
			tenant = await updateTenant({
				name,
				logoUrl: logoUrl || undefined,
				settings: { currency, timezone }
			});
			toast.success('Settings saved');
			// Update originals after successful save
			originalName = name;
			originalLogoUrl = logoUrl;
			originalCurrency = currency;
			originalTimezone = timezone;
		} catch (err: any) {
			toast.error(err.message || 'Failed to save settings');
		} finally {
			saving = false;
		}
	}
</script>

<div class="mx-auto max-w-2xl space-y-6">
	<PageHeader
		title="Settings"
		description="Configure your store settings"
		breadcrumbs={[{ label: 'Home', href: '/dashboard' }, { label: 'Settings' }]}
	/>

	{#if loading}
		<div class="space-y-6">
			<Card.Root>
				<Card.Header>
					<Skeleton class="h-5 w-40" />
				</Card.Header>
				<Card.Content class="space-y-4">
					{#each Array(3) as _}
						<div class="space-y-2">
							<Skeleton class="h-4 w-24" />
							<Skeleton class="h-10 w-full" />
						</div>
					{/each}
				</Card.Content>
			</Card.Root>
			<Card.Root>
				<Card.Header>
					<Skeleton class="h-5 w-32" />
				</Card.Header>
				<Card.Content class="space-y-4">
					{#each Array(2) as _}
						<div class="space-y-2">
							<Skeleton class="h-4 w-24" />
							<Skeleton class="h-10 w-full" />
						</div>
					{/each}
				</Card.Content>
			</Card.Root>
		</div>
	{:else if tenant}
		<form onsubmit={handleSave} class="space-y-6">
			<Card.Root>
				<Card.Header>
					<Card.Title>Store Information</Card.Title>
				</Card.Header>
				<Card.Content class="space-y-4">
					<div class="space-y-2">
						<Label for="storeName">Store Name</Label>
						<Input id="storeName" bind:value={name} disabled={!auth.canEdit} />
					</div>
					<div class="space-y-2">
						<Label for="slug">Slug</Label>
						<Input id="slug" value={tenant.slug} disabled />
						<p class="text-xs text-muted-foreground">Slug cannot be changed.</p>
					</div>
					<div class="space-y-2">
						<Label for="logoUrl">Logo URL</Label>
						<Input id="logoUrl" bind:value={logoUrl} placeholder="https://..." disabled={!auth.canEdit} />
					</div>
				</Card.Content>
			</Card.Root>

			<Card.Root>
				<Card.Header>
					<Card.Title>Preferences</Card.Title>
				</Card.Header>
				<Card.Content class="space-y-4">
					<div class="space-y-2">
						<Label>Currency</Label>
						<Select.Root type="single" value={currency} onValueChange={(v) => (currency = v)} disabled={!auth.canEdit}>
							<Select.Trigger class="w-full">
								{currencies.find((c) => c.value === currency)?.label || currency}
							</Select.Trigger>
							<Select.Content>
								{#each currencies as curr}
									<Select.Item value={curr.value}>{curr.label}</Select.Item>
								{/each}
							</Select.Content>
						</Select.Root>
					</div>
					<div class="space-y-2">
						<Label>Timezone</Label>
						<Select.Root type="single" value={timezone} onValueChange={(v) => (timezone = v)} disabled={!auth.canEdit}>
							<Select.Trigger class="w-full">
								{timezones.find((t) => t.value === timezone)?.label || timezone}
							</Select.Trigger>
							<Select.Content>
								{#each timezones as tz}
									<Select.Item value={tz.value}>{tz.label}</Select.Item>
								{/each}
							</Select.Content>
						</Select.Root>
					</div>
				</Card.Content>
			</Card.Root>

			{#if auth.canEdit}
				<div class="flex justify-end">
					<Button type="submit" disabled={saving}>
						{saving ? 'Saving...' : 'Save Settings'}
					</Button>
				</div>
			{/if}
		</form>
	{/if}
</div>
