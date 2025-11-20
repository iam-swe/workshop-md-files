import apiClient from './client';
import { Task, CreateTaskRequest } from '../types/task.types';

const API_TASKS_ENDPOINT = '/api/v1/tasks';

export const getTasks = async (): Promise<Task[]> => {
  try {
    const response = await apiClient.get<Task[]>(API_TASKS_ENDPOINT);
    return response.data;
  } catch (error) {
    console.error('Failed to fetch tasks:', error);
    throw error;
  }
};

export const createTask = async (request: CreateTaskRequest): Promise<Task> => {
  try {
    const response = await apiClient.post<Task>(API_TASKS_ENDPOINT, request);
    return response.data;
  } catch (error) {
    console.error('Failed to create task:', error);
    throw error;
  }
};

export const toggleTask = async (id: string): Promise<Task> => {
  try {
    const response = await apiClient.patch<Task>(`${API_TASKS_ENDPOINT}/${id}/toggle`);
    return response.data;
  } catch (error) {
    console.error('Failed to toggle task:', error);
    throw error;
  }
};

export const deleteTask = async (id: string): Promise<void> => {
  try {
    await apiClient.delete(`${API_TASKS_ENDPOINT}/${id}`);
  } catch (error) {
    console.error('Failed to delete task:', error);
    throw error;
  }
};
