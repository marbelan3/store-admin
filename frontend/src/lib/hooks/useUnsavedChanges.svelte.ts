import { beforeNavigate } from '$app/navigation';
import { onMount, onDestroy } from 'svelte';

/**
 * Tracks dirty state and warns user before navigating away from unsaved changes.
 *
 * Usage:
 * ```ts
 * const unsaved = createUnsavedChanges();
 * // Call unsaved.setDirty(true) when form changes
 * // Call unsaved.setDirty(false) after save
 * ```
 */
export function createUnsavedChanges() {
	let dirty = $state(false);

	function handleBeforeUnload(e: BeforeUnloadEvent) {
		if (dirty) {
			e.preventDefault();
		}
	}

	onMount(() => {
		window.addEventListener('beforeunload', handleBeforeUnload);
	});

	onDestroy(() => {
		window.removeEventListener('beforeunload', handleBeforeUnload);
	});

	beforeNavigate((navigation) => {
		if (dirty) {
			if (!confirm('You have unsaved changes. Are you sure you want to leave?')) {
				navigation.cancel();
			}
		}
	});

	return {
		get dirty() {
			return dirty;
		},
		setDirty(value: boolean) {
			dirty = value;
		}
	};
}
