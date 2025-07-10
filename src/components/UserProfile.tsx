import React, { useState, useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { 
  User, 
  Mail, 
  Calendar, 
  MessageSquare, 
  Phone, 
  Trash2,
  Save,
  AlertTriangle,
  BarChart3
} from 'lucide-react';
import { LoadingSpinner } from './LoadingSpinner';
import { useApi } from '../hooks/useApi';

interface UserProfileData {
  id: number;
  email: string;
  displayName: string;
  createdAt: string;
  discordWebhook: string;
  slackWebhook: string;
  phone: string;
}

interface UserStatistics {
  userId: number;
  email: string;
  displayName: string;
  memberSince: string;
  daysSinceRegistration: number;
  configuredChannels: number;
  totalQueries: number;
  activeQueries: number;
  cronQueries: number;
  specificQueries: number;
  checkQueries: number;
}

export const UserProfile: React.FC = () => {
  const { logout } = useAuth0();
  const { apiCall } = useApi();
  
  const [profile, setProfile] = useState<UserProfileData | null>(null);
  const [statistics, setStatistics] = useState<UserStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  
  // Form states
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [discordWebhook, setDiscordWebhook] = useState('');
  const [slackWebhook, setSlackWebhook] = useState('');
  const [phone, setPhone] = useState('');

  useEffect(() => {
    loadUserData();
  }, []);

  const loadUserData = async () => {
    try {
      setLoading(true);
      
      // Carica profilo e statistiche in parallelo
      const [profileResponse, statsResponse] = await Promise.all([
        apiCall('/api/v1/user/profile'),
        apiCall('/api/v1/user/statistics')
      ]);

      if (profileResponse.success) {
        const profileData = profileResponse.data;
        setProfile(profileData);
        setDisplayName(profileData.displayName || '');
        setEmail(profileData.email || '');
        setDiscordWebhook(profileData.discordWebhook || '');
        setSlackWebhook(profileData.slackWebhook || '');
        setPhone(profileData.phone || '');
      }

      if (statsResponse.success) {
        setStatistics(statsResponse.data);
      }
    } catch (error) {
      console.error('Error loading user data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateProfile = async () => {
    try {
      setSaving(true);
      
      const updateData: any = {};
      if (displayName !== (profile?.displayName || '')) {
        updateData.displayName = displayName;
      }
      if (email !== profile?.email) {
        updateData.email = email;
      }

      if (Object.keys(updateData).length > 0) {
        const response = await apiCall('/api/v1/user/profile', {
          method: 'PUT',
          body: JSON.stringify(updateData)
        });

        if (response.success) {
          setProfile(response.data);
          alert('Profile updated successfully!');
        } else {
          alert('Error updating profile: ' + response.error);
        }
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      alert('Error updating profile');
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateChannels = async () => {
    try {
      setSaving(true);
      
      const channelData: any = {};
      if (discordWebhook) channelData.discord = discordWebhook;
      if (slackWebhook) channelData.slack = slackWebhook;
      if (phone) channelData.whatsapp = phone;

      const response = await apiCall('/api/v1/user/notification-channels', {
        method: 'PUT',
        body: JSON.stringify(channelData)
      });

      if (response.success) {
        setProfile(response.data);
        alert('Notification channels updated successfully!');
      } else {
        alert('Error updating channels: ' + response.error);
      }
    } catch (error) {
      console.error('Error updating channels:', error);
      alert('Error updating notification channels');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteAccount = async () => {
    try {
      setSaving(true);
      
      const response = await apiCall('/api/v1/user/account', {
        method: 'DELETE'
      });

      if (response.success) {
        alert('Account deleted successfully. You will be logged out.');
        logout({
          logoutParams: {
            returnTo: window.location.origin + '/welcome'
          }
        });
      } else {
        alert('Error deleting account: ' + response.error);
      }
    } catch (error) {
      console.error('Error deleting account:', error);
      alert('Error deleting account');
    } finally {
      setSaving(false);
      setShowDeleteConfirm(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Statistics Cards */}
      {statistics && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="bg-white p-6 rounded-lg shadow-sm">
            <div className="flex items-center">
              <Calendar className="h-8 w-8 text-indigo-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Member Since</p>
                <p className="text-2xl font-bold text-gray-900">{statistics.daysSinceRegistration} days</p>
              </div>
            </div>
          </div>
          
          <div className="bg-white p-6 rounded-lg shadow-sm">
            <div className="flex items-center">
              <MessageSquare className="h-8 w-8 text-green-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Channels</p>
                <p className="text-2xl font-bold text-gray-900">{statistics.configuredChannels}</p>
              </div>
            </div>
          </div>
          
          <div className="bg-white p-6 rounded-lg shadow-sm">
            <div className="flex items-center">
              <BarChart3 className="h-8 w-8 text-blue-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Total Queries</p>
                <p className="text-2xl font-bold text-gray-900">{statistics.totalQueries}</p>
              </div>
            </div>
          </div>
          
          <div className="bg-white p-6 rounded-lg shadow-sm">
            <div className="flex items-center">
              <User className="h-8 w-8 text-purple-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Active</p>
                <p className="text-2xl font-bold text-gray-900">{statistics.activeQueries}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Profile Settings */}
      <div className="bg-white rounded-lg shadow-sm">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Profile Settings</h2>
        </div>
        <div className="p-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Display Name
              </label>
              <input
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                placeholder="Your display name"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Email Address
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                placeholder="your@email.com"
              />
            </div>
          </div>
          
          <button
            onClick={handleUpdateProfile}
            disabled={saving}
            className="flex items-center space-x-2 bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors"
          >
            <Save className="h-4 w-4" />
            <span>{saving ? 'Saving...' : 'Update Profile'}</span>
          </button>
        </div>
      </div>

      {/* Notification Channels */}
      <div className="bg-white rounded-lg shadow-sm">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Notification Channels</h2>
          <p className="text-sm text-gray-600 mt-1">Configure where you want to receive notifications</p>
        </div>
        <div className="p-6 space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Discord Webhook URL
            </label>
            <input
              type="url"
              value={discordWebhook}
              onChange={(e) => setDiscordWebhook(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="https://discord.com/api/webhooks/..."
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Slack Webhook URL
            </label>
            <input
              type="url"
              value={slackWebhook}
              onChange={(e) => setSlackWebhook(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="https://hooks.slack.com/services/..."
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              WhatsApp Phone Number
            </label>
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="+393123456789"
            />
          </div>
          
          <button
            onClick={handleUpdateChannels}
            disabled={saving}
            className="flex items-center space-x-2 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 disabled:opacity-50 transition-colors"
          >
            <MessageSquare className="h-4 w-4" />
            <span>{saving ? 'Saving...' : 'Update Channels'}</span>
          </button>
        </div>
      </div>

      {/* Danger Zone */}
      <div className="bg-white rounded-lg shadow-sm border border-red-200">
        <div className="px-6 py-4 border-b border-red-200">
          <h2 className="text-lg font-semibold text-red-900">Danger Zone</h2>
          <p className="text-sm text-red-600 mt-1">Irreversible actions</p>
        </div>
        <div className="p-6">
          {!showDeleteConfirm ? (
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className="flex items-center space-x-2 bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors"
            >
              <Trash2 className="h-4 w-4" />
              <span>Delete Account</span>
            </button>
          ) : (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <div className="flex items-start space-x-3">
                <AlertTriangle className="h-5 w-5 text-red-600 mt-0.5" />
                <div className="flex-1">
                  <h3 className="text-sm font-medium text-red-900">
                    Are you absolutely sure?
                  </h3>
                  <p className="text-sm text-red-700 mt-1">
                    This action cannot be undone. This will permanently delete your account 
                    and remove all your data from our servers.
                  </p>
                  <div className="mt-4 flex space-x-3">
                    <button
                      onClick={handleDeleteAccount}
                      disabled={saving}
                      className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors text-sm"
                    >
                      {saving ? 'Deleting...' : 'Yes, delete my account'}
                    </button>
                    <button
                      onClick={() => setShowDeleteConfirm(false)}
                      className="bg-gray-200 text-gray-900 px-4 py-2 rounded-lg hover:bg-gray-300 transition-colors text-sm"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};