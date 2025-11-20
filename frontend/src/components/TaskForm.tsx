import React from 'react';
import { useForm } from 'react-hook-form';
import { useCreateTask } from '../hooks/useCreateTask';
import { CreateTaskRequest } from '../types/task.types';
import './TaskForm.css';

const TaskForm: React.FC = () => {
  const { register, handleSubmit, formState: { errors }, reset } = useForm<CreateTaskRequest>();
  const createTaskMutation = useCreateTask();

  const onSubmit = async (data: CreateTaskRequest) => {
    try {
      await createTaskMutation.mutateAsync(data);
      reset();
    } catch (error) {
      console.error('Error creating task:', error);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="task-form">
      <div className="form-group">
        <input
          type="text"
          placeholder="Add a new task..."
          {...register('description', {
            required: 'Description is required',
            maxLength: {
              value: 1000,
              message: 'Description must be less than 1000 characters'
            },
            validate: (value) => value.trim().length > 0 || 'Description cannot be empty'
          })}
          className={errors.description ? 'error' : ''}
          disabled={createTaskMutation.isPending}
        />
        <button 
          type="submit" 
          disabled={createTaskMutation.isPending}
          className="submit-button"
        >
          {createTaskMutation.isPending ? 'Adding...' : 'Add Task'}
        </button>
      </div>
      {errors.description && (
        <p className="error-message">{errors.description.message}</p>
      )}
      {createTaskMutation.isError && (
        <p className="error-message">Failed to create task. Please try again.</p>
      )}
    </form>
  );
};

export default TaskForm;
