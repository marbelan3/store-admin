export interface Category {
	id: string;
	name: string;
	slug: string;
	description: string | null;
	imageUrl: string | null;
	path: string | null;
	sortOrder: number;
	active: boolean;
	parentId: string | null;
	metaTitle: string | null;
	metaDescription: string | null;
	createdAt: string;
	children: Category[];
}
