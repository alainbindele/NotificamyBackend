import React, { useState } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { 
  Bell, 
  User, 
  Settings, 
  LogOut, 
  Plus,
  Calendar,
  BarChart3,
  MessageSquare
} from 'lucide-react';
import { UserProfile } from './UserProfile';
import { NotificationManager } from './NotificationManager';
import { CreateNotification } from './CreateNotification';

type TabType = 'notifications' | 'profile' | 'create';

export const Dashboard: React.FC = () => {
  const { user, logout } = useAuth0();
  const [activeTab, setActiveTab] = useState<TabType>('notifications');

  const handleLogout = () => {
    logout({
      logoutParams: {
        returnTo: window.location.origin + '/welcome'
      }
    });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center">
              <Bell className="h-8 w-8 text-indigo-600" />
              <span className="ml-2 text-2xl font-bold text-gray-900">NotifyMe</span>
            </div>
            
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <img
                  src={user?.picture || `https://ui-avatars.com/api/?name=${user?.email}&background=6366f1&color=fff`}
                  alt="Profile"
                  className="w-8 h-8 rounded-full"
                />
                <span className="text-sm font-medium text-gray-700">
                  {user?.name || user?.email}
                </span>
              </div>
              
              <button
                onClick={handleLogout}
                className="flex items-center space-x-1 text-gray-600 hover:text-gray-900 transition-colors"
              >
                <LogOut className="h-4 w-4" />
                <span className="text-sm">Logout</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col lg:flex-row gap-8">
          {/* Sidebar */}
          <div className="lg:w-64 flex-shrink-0">
            <nav className="bg-white rounded-lg shadow-sm p-4">
              <div className="space-y-2">
                <button
                  onClick={() => setActiveTab('notifications')}
                  className={`w-full flex items-center space-x-3 px-3 py-2 rounded-lg text-left transition-colors ${
                    activeTab === 'notifications'
                      ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <Bell className="h-5 w-5" />
                  <span>My Notifications</span>
                </button>
                
                <button
                  onClick={() => setActiveTab('create')}
                  className={`w-full flex items-center space-x-3 px-3 py-2 rounded-lg text-left transition-colors ${
                    activeTab === 'create'
                      ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <Plus className="h-5 w-5" />
                  <span>Create New</span>
                </button>
                
                <button
                  onClick={() => setActiveTab('profile')}
                  className={`w-full flex items-center space-x-3 px-3 py-2 rounded-lg text-left transition-colors ${
                    activeTab === 'profile'
                      ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <User className="h-5 w-5" />
                  <span>Profile & Settings</span>
                </button>
              </div>
            </nav>
          </div>

          {/* Main Content */}
          <div className="flex-1">
            {activeTab === 'notifications' && <NotificationManager />}
            {activeTab === 'create' && <CreateNotification />}
            {activeTab === 'profile' && <UserProfile />}
          </div>
        </div>
      </div>
    </div>
  );
};