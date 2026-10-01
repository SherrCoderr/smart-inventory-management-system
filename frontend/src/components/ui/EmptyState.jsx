import { PackageX } from 'lucide-react';

export const EmptyState = ({ message = "No data found", icon: Icon = PackageX }) => (
  <div className="flex flex-col items-center justify-center p-8 text-center text-gray-500">
    <Icon className="h-12 w-12 mb-3 text-gray-400" />
    <p className="text-sm">{message}</p>
  </div>
);
