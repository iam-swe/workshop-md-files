import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TaskList from '../components/TaskList';
import { getTasks } from '../api/tasks.api';

jest.mock('../api/tasks.api');

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('TaskList', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state initially', () => {
    (getTasks as jest.Mock).mockImplementation(() => new Promise(() => {}));
    
    render(<TaskList />, { wrapper: createWrapper() });
    
    expect(screen.getByText(/loading tasks/i)).toBeInTheDocument();
  });

  it('should show empty state when no tasks', async () => {
    (getTasks as jest.Mock).mockResolvedValue([]);
    
    render(<TaskList />, { wrapper: createWrapper() });
    
    expect(await screen.findByText(/no tasks yet/i)).toBeInTheDocument();
  });

  it('should render tasks when data is loaded', async () => {
    const mockTasks = [
      {
        id: '1',
        description: 'Task 1',
        completed: false,
        createdAt: new Date().toISOString(),
        completedAt: null,
      },
      {
        id: '2',
        description: 'Task 2',
        completed: true,
        createdAt: new Date().toISOString(),
        completedAt: new Date().toISOString(),
      },
    ];

    (getTasks as jest.Mock).mockResolvedValue(mockTasks);
    
    render(<TaskList />, { wrapper: createWrapper() });
    
    expect(await screen.findByText('Task 1')).toBeInTheDocument();
    expect(screen.getByText('Task 2')).toBeInTheDocument();
  });

  it('should show error state on fetch failure', async () => {
    (getTasks as jest.Mock).mockRejectedValue(new Error('Network error'));
    
    render(<TaskList />, { wrapper: createWrapper() });
    
    expect(await screen.findByText(/failed to load tasks/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });
});
