import { apiClient } from './client';

export interface SearchResult {
  id: string;
  name: string;
  parentId: string | null;
  isDir: boolean;
  size: number;
  createTime: string;
  path: string;
}

export interface SearchResponse {
  items: SearchResult[];
  total: number;
  page: number;
  pageSize: number;
}

export interface SearchParams {
  q: string;
  type?: 'file' | 'folder';
  sort?: 'name' | 'date' | 'size';
  order?: 'asc' | 'desc';
  page?: number;
  pageSize?: number;
}

export async function searchFiles(params: SearchParams): Promise<SearchResponse> {
  const query = new URLSearchParams();
  query.set('q', params.q);
  if (params.type) query.set('type', params.type);
  if (params.sort) query.set('sort', params.sort);
  if (params.order) query.set('order', params.order);
  if (params.page) query.set('page', String(params.page));
  if (params.pageSize) query.set('pageSize', String(params.pageSize));

  return apiClient<SearchResponse>(`/search?${query.toString()}`);
}
