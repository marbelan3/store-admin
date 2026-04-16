import { api } from './client';
import type { Tag } from '$lib/types/tag';

export function getTags() {
	return api.get<Tag[]>('/api/tags');
}

export function createTag(data: { name: string; slug?: string }) {
	return api.post<Tag>('/api/tags', data);
}

export function deleteTag(id: string) {
	return api.delete(`/api/tags/${id}`);
}
