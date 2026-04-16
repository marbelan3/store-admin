import { api } from './client';

export interface RecentProduct {
	id: string;
	name: string;
	updatedAt: string;
}

export interface DashboardStats {
	totalProducts: number;
	activeProducts: number;
	draftProducts: number;
	archivedProducts: number;
	totalCategories: number;
	totalTags: number;
	totalUsers: number;
	productsByStatus: Record<string, number>;
	lowStockCount: number;
	noImagesCount: number;
	uncategorizedCount: number;
	recentlyUpdated: RecentProduct[];
}

export function getDashboardStats() {
	return api.get<DashboardStats>('/api/dashboard/stats');
}
