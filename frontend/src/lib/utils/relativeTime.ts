/**
 * Formats a date string as a human-readable relative time.
 * Examples: "just now", "5 minutes ago", "2 hours ago", "yesterday", "3 days ago", "Jan 15"
 */
export function relativeTime(dateStr: string): string {
	const date = new Date(dateStr);
	const now = new Date();
	const diffMs = now.getTime() - date.getTime();
	const diffSeconds = Math.floor(diffMs / 1000);
	const diffMinutes = Math.floor(diffSeconds / 60);
	const diffHours = Math.floor(diffMinutes / 60);
	const diffDays = Math.floor(diffHours / 24);

	if (diffSeconds < 60) return 'just now';
	if (diffMinutes < 60) return `${diffMinutes} minute${diffMinutes === 1 ? '' : 's'} ago`;
	if (diffHours < 24) return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;
	if (diffDays === 1) return 'yesterday';
	if (diffDays < 30) return `${diffDays} days ago`;

	return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}
