import { z } from 'zod';

export const productSchema = z.object({
	name: z.string().min(1, 'Name is required').max(200, 'Name too long'),
	price: z.number().min(0, 'Price must be >= 0').nullable().optional(),
	compareAtPrice: z.number().min(0, 'Must be >= 0').nullable().optional(),
	weight: z.number().min(0, 'Must be >= 0').nullable().optional(),
	quantity: z.number().int().min(0, 'Must be >= 0').nullable().optional(),
	sku: z.string().max(100, 'SKU too long').nullable().optional(),
	barcode: z.string().max(100, 'Barcode too long').nullable().optional(),
	description: z.string().optional(),
	shortDescription: z.string().max(500, 'Short description too long').optional(),
	status: z.enum(['DRAFT', 'ACTIVE', 'ARCHIVED']).optional(),
	trackInventory: z.boolean().optional()
});

export type ProductFormData = z.infer<typeof productSchema>;
export type ProductFormErrors = Partial<Record<keyof ProductFormData, string>>;

export function validateProduct(data: Record<string, unknown>): {
	valid: boolean;
	errors: ProductFormErrors;
} {
	const result = productSchema.safeParse(data);
	if (result.success) return { valid: true, errors: {} };
	const errors: ProductFormErrors = {};
	for (const issue of result.error.issues) {
		const field = issue.path[0] as keyof ProductFormData;
		if (!errors[field]) errors[field] = issue.message;
	}
	return { valid: false, errors };
}

/**
 * Parse backend validation errors (400 responses from GlobalExceptionHandler).
 * Expected format: { message: string, fieldErrors?: Record<string, string> }
 */
export function parseBackendErrors(err: unknown): ProductFormErrors {
	if (err && typeof err === 'object' && 'fieldErrors' in err) {
		const fieldErrors = (err as { fieldErrors: Record<string, string> }).fieldErrors;
		const errors: ProductFormErrors = {};
		for (const [field, message] of Object.entries(fieldErrors)) {
			if (field in productSchema.shape) {
				errors[field as keyof ProductFormData] = message;
			}
		}
		return errors;
	}
	return {};
}
