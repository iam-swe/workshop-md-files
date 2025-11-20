import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteTask } from '../api/tasks.api';

export const useDeleteTask = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => deleteTask(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
    },
    onError: (error) => {
      console.error('Failed to delete task:', error);
    },
  });
};
