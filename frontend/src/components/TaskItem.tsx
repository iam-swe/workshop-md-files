import React from 'react';
import { Task } from '../types/task.types';
import { useToggleTask } from '../hooks/useToggleTask';
import { useDeleteTask } from '../hooks/useDeleteTask';
import './TaskItem.css';

interface TaskItemProps {
  task: Task;
}

const TaskItem: React.FC<TaskItemProps> = ({ task }) => {
  const toggleTaskMutation = useToggleTask();
  const deleteTaskMutation = useDeleteTask();

  const handleToggle = () => {
    toggleTaskMutation.mutate(task.id);
  };

  const handleDelete = () => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      deleteTaskMutation.mutate(task.id);
    }
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    
    return date.toLocaleDateString();
  };

  return (
    <div className={`task-item ${task.completed ? 'completed' : ''}`}>
      <input
        type="checkbox"
        checked={task.completed}
        onChange={handleToggle}
        disabled={toggleTaskMutation.isPending}
        aria-label={`Mark "${task.description}" as ${task.completed ? 'incomplete' : 'complete'}`}
      />
      <div className="task-content">
        <p className="task-description">{task.description}</p>
        <p className="task-date">
          Created {formatDate(task.createdAt)}
          {task.completedAt && ` • Completed ${formatDate(task.completedAt)}`}
        </p>
      </div>
      <button
        onClick={handleDelete}
        disabled={deleteTaskMutation.isPending}
        className="delete-button"
        aria-label={`Delete task "${task.description}"`}
      >
        🗑️
      </button>
    </div>
  );
};

export default TaskItem;
