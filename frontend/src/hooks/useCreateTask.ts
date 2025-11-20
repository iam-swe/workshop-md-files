import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createTask } from '../api/tasks.api';
import { CreateTaskRequest } from '../types/task.types';

export const useCreateTask = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateTaskRequest) => createTask(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
    },
    onError: (error) => {
      console.error('Failed to create task:', error);
    },
  });
};
