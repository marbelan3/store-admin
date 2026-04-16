<script lang="ts">
	interface Props {
		data: number[];
		color?: string;
		width?: number;
		height?: number;
		filled?: boolean;
	}

	let {
		data,
		color = 'currentColor',
		width = 80,
		height = 32,
		filled = false
	}: Props = $props();

	const padding = 2;

	let pathD = $derived.by(() => {
		if (data.length < 2) return '';

		const min = Math.min(...data);
		const max = Math.max(...data);
		const range = max - min || 1;

		const points = data.map((value, i) => ({
			x: padding + (i / (data.length - 1)) * (width - padding * 2),
			y: padding + (1 - (value - min) / range) * (height - padding * 2)
		}));

		let d = `M ${points[0].x},${points[0].y}`;
		for (let i = 1; i < points.length; i++) {
			const prev = points[i - 1];
			const curr = points[i];
			const cpx = (prev.x + curr.x) / 2;
			d += ` C ${cpx},${prev.y} ${cpx},${curr.y} ${curr.x},${curr.y}`;
		}

		return d;
	});

	let fillPathD = $derived.by(() => {
		if (!filled || data.length < 2 || !pathD) return '';

		const points = data.map((value, i) => {
			const min = Math.min(...data);
			const max = Math.max(...data);
			const range = max - min || 1;
			return {
				x: padding + (i / (data.length - 1)) * (width - padding * 2),
				y: padding + (1 - (value - min) / range) * (height - padding * 2)
			};
		});

		const lastPoint = points[points.length - 1];
		const firstPoint = points[0];

		return `${pathD} L ${lastPoint.x},${height} L ${firstPoint.x},${height} Z`;
	});

	let gradientId = $derived(`sparkline-gradient-${Math.random().toString(36).slice(2, 9)}`);
</script>

<svg
	{width}
	{height}
	viewBox="0 0 {width} {height}"
	fill="none"
	xmlns="http://www.w3.org/2000/svg"
	class="overflow-visible"
>
	{#if filled}
		<defs>
			<linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
				<stop offset="0%" stop-color={color} stop-opacity="0.3" />
				<stop offset="100%" stop-color={color} stop-opacity="0.02" />
			</linearGradient>
		</defs>
		{#if fillPathD}
			<path d={fillPathD} fill="url(#{gradientId})" />
		{/if}
	{/if}
	{#if pathD}
		<path
			d={pathD}
			stroke={color}
			stroke-width="1.5"
			stroke-linecap="round"
			stroke-linejoin="round"
			fill="none"
		/>
	{/if}
</svg>
