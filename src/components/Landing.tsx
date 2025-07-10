import React from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { Bell, Zap, Shield, Clock } from 'lucide-react';

export const Landing: React.FC = () => {
  const { loginWithRedirect } = useAuth0();

  const handleLogin = () => {
    loginWithRedirect({
      appState: {
        returnTo: '/'
      }
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div className="flex items-center">
              <Bell className="h-8 w-8 text-indigo-600" />
              <span className="ml-2 text-2xl font-bold text-gray-900">NotifyMe</span>
            </div>
            <button
              onClick={handleLogin}
              className="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700 transition-colors"
            >
              Sign In
            </button>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="text-center">
          <h1 className="text-4xl md:text-6xl font-bold text-gray-900 mb-6">
            Smart Notifications
            <span className="text-indigo-600 block">Made Simple</span>
          </h1>
          <p className="text-xl text-gray-600 mb-8 max-w-3xl mx-auto">
            Create intelligent, scheduled notifications using natural language. 
            Let AI understand your needs and deliver timely reminders across multiple channels.
          </p>
          <button
            onClick={handleLogin}
            className="bg-indigo-600 text-white px-8 py-4 rounded-lg text-lg font-semibold hover:bg-indigo-700 transition-colors shadow-lg"
          >
            Get Started Free
          </button>
        </div>

        {/* Features */}
        <div className="mt-20 grid md:grid-cols-3 gap-8">
          <div className="text-center p-6">
            <div className="bg-indigo-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
              <Zap className="h-8 w-8 text-indigo-600" />
            </div>
            <h3 className="text-xl font-semibold mb-2">AI-Powered</h3>
            <p className="text-gray-600">
              Simply describe what you want to be notified about in natural language. 
              Our AI understands and creates the perfect schedule.
            </p>
          </div>

          <div className="text-center p-6">
            <div className="bg-purple-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
              <Clock className="h-8 w-8 text-purple-600" />
            </div>
            <h3 className="text-xl font-semibold mb-2">Smart Scheduling</h3>
            <p className="text-gray-600">
              From one-time reminders to complex recurring patterns, 
              NotifyMe handles all your scheduling needs automatically.
            </p>
          </div>

          <div className="text-center p-6">
            <div className="bg-green-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
              <Shield className="h-8 w-8 text-green-600" />
            </div>
            <h3 className="text-xl font-semibold mb-2">Multi-Channel</h3>
            <p className="text-gray-600">
              Receive notifications via email, Discord, Slack, or WhatsApp. 
              Choose the channels that work best for you.
            </p>
          </div>
        </div>

        {/* Examples */}
        <div className="mt-20 bg-white rounded-2xl shadow-xl p-8">
          <h2 className="text-3xl font-bold text-center mb-8">Just Tell Us What You Need</h2>
          <div className="grid md:grid-cols-2 gap-6">
            <div className="bg-gray-50 p-6 rounded-lg">
              <h4 className="font-semibold text-indigo-600 mb-2">Simple Reminders</h4>
              <p className="text-gray-700 italic">"Remind me to call mom tomorrow at 3pm"</p>
            </div>
            <div className="bg-gray-50 p-6 rounded-lg">
              <h4 className="font-semibold text-indigo-600 mb-2">Recurring Tasks</h4>
              <p className="text-gray-700 italic">"Notify me every Monday at 9am about the team meeting"</p>
            </div>
            <div className="bg-gray-50 p-6 rounded-lg">
              <h4 className="font-semibold text-indigo-600 mb-2">Conditional Alerts</h4>
              <p className="text-gray-700 italic">"Tell me when Bitcoin drops below $50,000"</p>
            </div>
            <div className="bg-gray-50 p-6 rounded-lg">
              <h4 className="font-semibold text-indigo-600 mb-2">Content Updates</h4>
              <p className="text-gray-700 italic">"Send me daily tech news every morning at 8am"</p>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-12 mt-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="flex items-center justify-center mb-4">
            <Bell className="h-6 w-6 text-indigo-400" />
            <span className="ml-2 text-xl font-bold">NotifyMe</span>
          </div>
          <p className="text-gray-400">
            Smart notifications powered by AI. Never miss what matters.
          </p>
        </div>
      </footer>
    </div>
  );
};