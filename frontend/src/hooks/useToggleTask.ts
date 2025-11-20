import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toggleTask } from '../api/tasks.api';

export const useToggleTask = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => toggleTask(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
    },
    onError: (error) => {
      console.error('Failed to toggle task:', error);
    },
  });
};
