const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export interface ImportResult {
	created: number;
	updated: number;
	errors: { row: number; message: string }[];
}

export async function exportProductsCsv(): Promise<void> {
	const token = localStorage.getItem('access_token');
	const res = await fetch(`${API_BASE}/api/products/export`, {
		headers: token ? { Authorization: `Bearer ${token}` } : {}
	});

	if (!res.ok) throw new Error('Export failed');

	const blob = await res.blob();
	const url = URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url;
	a.download = `products-export-${new Date().toISOString().slice(0, 10)}.csv`;
	a.click();
	URL.revokeObjectURL(url);
}

export async function importProductsCsv(file: File): Promise<ImportResult> {
	const token = localStorage.getItem('access_token');
	const formData = new FormData();
	formData.append('file', file);

	const res = await fetch(`${API_BASE}/api/products/import`, {
		method: 'POST',
		headers: token ? { Authorization: `Bearer ${token}` } : {},
		body: formData
	});

	if (!res.ok) {
		const error = await res.json().catch(() => ({ message: 'Import failed' }));
		throw new Error(error.message || `Import failed (HTTP ${res.status})`);
	}

	return res.json();
}
