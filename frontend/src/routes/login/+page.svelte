<script lang="ts">
	const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

	function loginWithGoogle() {
		window.location.href = `${API_BASE}/oauth2/authorization/google`;
	}
</script>

<div class="relative flex min-h-screen items-center justify-center overflow-hidden" style="background: linear-gradient(135deg, oklch(0.45 0.22 265), oklch(0.5 0.2 285), oklch(0.45 0.18 300));">
	<!-- Decorative background blobs -->
	<div class="absolute -top-24 -left-24 h-96 w-96 rounded-full opacity-30" style="background: oklch(0.6 0.2 280); filter: blur(80px);"></div>
	<div class="absolute -bottom-32 -right-32 h-[30rem] w-[30rem] rounded-full opacity-25" style="background: oklch(0.5 0.25 290); filter: blur(100px);"></div>
	<div class="absolute top-1/4 right-1/4 h-64 w-64 rounded-full opacity-20" style="background: oklch(0.65 0.18 250); filter: blur(60px);"></div>
	<div class="absolute bottom-1/4 left-1/3 h-48 w-48 rounded-full opacity-15" style="background: oklch(0.7 0.15 310); filter: blur(50px);"></div>

	<div class="relative z-10 w-full max-w-sm px-4">
		<!-- Logo area -->
		<div class="mb-8 flex flex-col items-center">
			<div class="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl shadow-lg" style="background: linear-gradient(135deg, oklch(0.65 0.2 265), oklch(0.55 0.22 280));">
				<span class="text-xl font-extrabold text-white tracking-tight">G2U</span>
			</div>
			<h1 class="text-3xl font-bold tracking-tight text-white">G2U Store Admin</h1>
			<p class="mt-1 text-white/70">Sign in to manage your store</p>
		</div>

		<!-- Login card -->
		<div class="rounded-2xl bg-white p-8 shadow-2xl dark:bg-card">
			<button
				onclick={loginWithGoogle}
				class="group flex w-full items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm font-medium text-gray-700 shadow-sm transition-all hover:shadow-md hover:border-gray-300 hover:-translate-y-0.5 dark:border-white/10 dark:bg-white/5 dark:text-white dark:hover:bg-white/10"
			>
				<svg class="h-5 w-5" viewBox="0 0 24 24">
					<path
						d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"
						fill="#4285F4"
					/>
					<path
						d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
						fill="#34A853"
					/>
					<path
						d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
						fill="#FBBC05"
					/>
					<path
						d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
						fill="#EA4335"
					/>
				</svg>
				Continue with Google
			</button>
		</div>

		<p class="mt-6 text-center text-xs text-white/50">
			Authorized users only. Contact your admin for access.
		</p>
	</div>
</div>
