import type { Task } from '../hooks/use-task-store';

const baseUrl = process.env.EXPO_PUBLIC_API_URL?.replace(/\/$/, '');
let accessToken: string | null = null;
let unauthorizedHandler: (() => void) | null = null;

export const isTaskApiConfigured = Boolean(baseUrl);

export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
    this.name = 'ApiError';
  }
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  if (!baseUrl) throw new ApiError('EXPO_PUBLIC_API_URL is not configured', 0);

  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}), ...options.headers },
    });
  } catch {
    throw new ApiError('Could not reach the task server', 0);
  }

  if (!response.ok) {
    const rawBody = await response.text();
    let body: { message?: string } = {};
    try {
      body = JSON.parse(rawBody) as { message?: string };
    } catch {
      // A proxy or the wrong service may return a non-JSON response.
    }
    if (response.status === 401) unauthorizedHandler?.();
    throw new ApiError(body.message || `Server request failed (HTTP ${response.status})`, response.status);
  }

  return response.status === 204 ? (undefined as T) : response.json() as Promise<T>;
}

export const taskApi = {
  list: () => request<Task[]>('/tasks'),
  create: (task: Omit<Task, 'id' | 'createdAt' | 'updatedAt'>) => request<Task>('/tasks', { method: 'POST', body: JSON.stringify(task) }),
  update: (id: string, updates: Partial<Task>) => request<Task>(`/tasks/${id}`, { method: 'PATCH', body: JSON.stringify(updates) }),
  updateOccurrence: (id: string, dateKey: string, completed: boolean) => request<Task>(`/tasks/${id}/occurrences/${dateKey}`, { method: 'PATCH', body: JSON.stringify({ completed }) }),
  remove: (id: string) => request<void>(`/tasks/${id}`, { method: 'DELETE' }),
  clear: () => request<{ deletedCount: number }>('/tasks', { method: 'DELETE' }),
};

export type AuthUser = { id: string; name: string; email: string };
export type AuthResponse = { token: string; user: AuthUser };

export const authApi = {
  signUp: (name: string, email: string, password: string) => request<AuthResponse>('/auth/signup', { method: 'POST', body: JSON.stringify({ name, email, password }) }),
  signIn: (email: string, password: string) => request<AuthResponse>('/auth/signin', { method: 'POST', body: JSON.stringify({ email, password }) }),
  me: () => request<{ user: AuthUser }>('/auth/me'),
  updateProfile: (updates: { name: string }) => request<{ user: AuthUser }>('/auth/me', { method: 'PATCH', body: JSON.stringify(updates) }),
  deleteAccount: () => request<void>('/auth/me', { method: 'DELETE' }),
};
