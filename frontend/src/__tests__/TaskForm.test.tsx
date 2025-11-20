import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TaskForm from '../components/TaskForm';
import { createTask } from '../api/tasks.api';

jest.mock('../api/tasks.api');

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('TaskForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render input and submit button', () => {
    render(<TaskForm />, { wrapper: createWrapper() });
    
    expect(screen.getByPlaceholderText('Add a new task...')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add task/i })).toBeInTheDocument();
  });

  it('should show validation error for empty input', async () => {
    render(<TaskForm />, { wrapper: createWrapper() });
    
    const submitButton = screen.getByRole('button', { name: /add task/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/description is required/i)).toBeInTheDocument();
    });
  });

  it('should create task and clear input on successful submission', async () => {
    const mockTask = {
      id: '1',
      description: 'Test task',
      completed: false,
      createdAt: new Date().toISOString(),
      completedAt: null,
    };

    (createTask as jest.Mock).mockResolvedValue(mockTask);

    render(<TaskForm />, { wrapper: createWrapper() });
    
    const input = screen.getByPlaceholderText('Add a new task...');
    const submitButton = screen.getByRole('button', { name: /add task/i });

    fireEvent.change(input, { target: { value: 'Test task' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(createTask).toHaveBeenCalledWith({ description: 'Test task' });
    });
  });

  it('should show error message on failed submission', async () => {
    (createTask as jest.Mock).mockRejectedValue(new Error('Network error'));

    render(<TaskForm />, { wrapper: createWrapper() });
    
    const input = screen.getByPlaceholderText('Add a new task...');
    const submitButton = screen.getByRole('button', { name: /add task/i });

    fireEvent.change(input, { target: { value: 'Test task' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/failed to create task/i)).toBeInTheDocument();
    });
  });
});
