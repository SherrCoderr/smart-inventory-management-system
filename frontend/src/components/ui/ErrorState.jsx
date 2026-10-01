import { AlertCircle } from 'lucide-react';
import { Button } from './Button';

export const ErrorState = ({ message = "An error occurred", onRetry }) => (
  <div className="flex flex-col items-center justify-center p-8 text-center text-red-500">
    <AlertCircle className="h-12 w-12 mb-3 text-red-400" />
    <p className="text-sm mb-4">{message}</p>
    {onRetry && (
      <Button variant="outline" onClick={onRetry}>
        Try Again
      </Button>
    )}
  </div>
);
