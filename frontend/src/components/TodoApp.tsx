import React from 'react';
import TaskForm from './TaskForm';
import TaskList from './TaskList';
import './TodoApp.css';

const TodoApp: React.FC = () => {
  return (
    <div className="todo-app">
      <header className="app-header">
        <h1>📝 To-Do List</h1>
        <p>Manage your tasks efficiently</p>
      </header>
      <main className="app-main">
        <TaskForm />
        <TaskList />
      </main>
    </div>
  );
};

export default TodoApp;
