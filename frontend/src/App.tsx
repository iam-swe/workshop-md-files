import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TodoApp from './components/TodoApp';
import './App.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: true,
      retry: 1,
      staleTime: 30000,
    },
  },
});

const App = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="App">
        <TodoApp />
      </div>
    </QueryClientProvider>
  );
};

export default App;