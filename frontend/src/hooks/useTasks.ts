import { useQuery } from '@tanstack/react-query';
import { getTasks } from '../api/tasks.api';

export const useTasks = () => {
  return useQuery({
    queryKey: ['tasks'],
    queryFn: getTasks,
    staleTime: 30000,
    refetchOnWindowFocus: true,
  });
};
