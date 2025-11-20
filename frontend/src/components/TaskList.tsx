import React from 'react';
import { useTasks } from '../hooks/useTasks';
import TaskItem from './TaskItem';
import './TaskList.css';

const TaskList: React.FC = () => {
  const { data: tasks, isLoading, isError, error, refetch } = useTasks();

  if (isLoading) {
    return (
      <div className="task-list">
        <div className="loading">
          <div className="spinner"></div>
          <p>Loading tasks...</p>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="task-list">
        <div className="error">
          <p>Failed to load tasks</p>
          <p className="error-details">{error?.message || 'An error occurred'}</p>
          <button onClick={() => refetch()} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!tasks || tasks.length === 0) {
    return (
      <div className="task-list">
        <div className="empty-state">
          <p>No tasks yet. Add one above!</p>
        </div>
      </div>
    );
  }

  return (
    <div className="task-list">
      {tasks.map((task) => (
        <TaskItem key={task.id} task={task} />
      ))}
    </div>
  );
};

export default TaskList;
