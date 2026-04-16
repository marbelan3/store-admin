<script lang="ts">
	interface Props {
		value: number;
		suffix?: string;
	}

	let { value, suffix = '%' }: Props = $props();

	let isPositive = $derived(value > 0);
	let isNegative = $derived(value < 0);
	let isNeutral = $derived(value === 0);

	let displayValue = $derived(Math.abs(value).toFixed(1));

	let badgeClass = $derived.by(() => {
		if (isPositive) return 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-400';
		if (isNegative) return 'bg-red-50 text-red-700 dark:bg-red-950/50 dark:text-red-400';
		return 'bg-gray-50 text-gray-600 dark:bg-gray-800 dark:text-gray-400';
	});
</script>

<span class="inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-xs font-medium {badgeClass}">
	{#if isPositive}
		<span class="text-[10px]">&#8593;</span>
	{:else if isNegative}
		<span class="text-[10px]">&#8595;</span>
	{:else}
		<span class="text-[10px]">&#8212;</span>
	{/if}
	{displayValue}{suffix}
</span>
