export interface Task {
  id: string;
  description: string;
  completed: boolean;
  createdAt: string;
  completedAt: string | null;
}

export interface CreateTaskRequest {
  description: string;
}

export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  path: string;
}
