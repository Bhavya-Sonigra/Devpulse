"use client";

import { Navbar } from "@/components/Navbar";
import { Zap, CreditCard, AlertCircle } from "lucide-react";
import { GithubIcon, SlackIcon } from "@/components/Icons";
import { useAppState } from "@/hooks/useAppState";
import { useRouter } from "next/navigation";

export default function SettingsPage() {
  const { state, updateState, isLoaded } = useAppState();
  const router = useRouter();

  if (!isLoaded) return null;

  const handleDisconnectRepo = () => updateState({ connectedRepo: null });
  const handleReconnectRepo = () => router.push("/setup?force=true&step=1");
  const handleDisconnectSlack = () => updateState({ connectedSlack: null });
  const handleReconnectSlack = () => router.push("/setup?force=true&step=2");

  const togglePlan = () => {
    updateState({ plan: state.plan === "free" ? "pro" : "free" });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <main className="max-w-4xl mx-auto px-6 py-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
          <p className="text-gray-500">Manage your integrations and billing plan.</p>
        </div>

        <div className="space-y-6">
          {/* Integrations Section */}
          <section className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
            <div className="px-6 py-5 border-b border-gray-100">
              <h2 className="text-lg font-semibold text-gray-900">Integrations</h2>
            </div>
            
            <div className="p-6 space-y-6">
              {/* GitHub Settings */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl border border-gray-100 bg-gray-50/50">
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center shrink-0">
                    <GithubIcon className="w-5 h-5 text-gray-900" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">GitHub Repository</h3>
                    {state.connectedRepo ? (
                      <p className="text-sm text-gray-500 mt-1">Connected to <span className="font-medium text-gray-700">{state.connectedRepo}</span></p>
                    ) : (
                      <p className="text-sm text-gray-500 mt-1 flex items-center gap-1.5">
                        <AlertCircle className="w-4 h-4 text-amber-500"/> Not connected
                      </p>
                    )}
                  </div>
                </div>
                <div>
                  {state.connectedRepo ? (
                    <button onClick={handleDisconnectRepo} className="px-4 py-2 text-sm font-medium text-rose-600 bg-rose-50 hover:bg-rose-100 rounded-lg transition-colors">
                      Disconnect
                    </button>
                  ) : (
                    <button onClick={handleReconnectRepo} className="px-4 py-2 text-sm font-medium text-white bg-gray-900 hover:bg-gray-800 rounded-lg transition-colors">
                      Connect
                    </button>
                  )}
                </div>
              </div>

              {/* Slack Settings */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl border border-gray-100 bg-gray-50/50">
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 bg-[#E01E5A]/10 rounded-lg flex items-center justify-center shrink-0">
                    <SlackIcon className="w-5 h-5 text-[#E01E5A]" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">Slack Workspace</h3>
                    {state.connectedSlack ? (
                      <p className="text-sm text-gray-500 mt-1">Posting to <span className="font-medium text-gray-700">{state.connectedSlack}</span></p>
                    ) : (
                      <p className="text-sm text-gray-500 mt-1 flex items-center gap-1.5">
                        <AlertCircle className="w-4 h-4 text-amber-500"/> Not connected
                      </p>
                    )}
                  </div>
                </div>
                <div>
                  {state.connectedSlack ? (
                    <button onClick={handleDisconnectSlack} className="px-4 py-2 text-sm font-medium text-rose-600 bg-rose-50 hover:bg-rose-100 rounded-lg transition-colors">
                      Disconnect
                    </button>
                  ) : (
                    <button onClick={handleReconnectSlack} className="px-4 py-2 text-sm font-medium text-white bg-gray-900 hover:bg-gray-800 rounded-lg transition-colors">
                      Connect
                    </button>
                  )}
                </div>
              </div>
            </div>
          </section>

          {/* Billing Section */}
          <section className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
            <div className="px-6 py-5 border-b border-gray-100">
              <h2 className="text-lg font-semibold text-gray-900">Billing Plan</h2>
            </div>
            <div className="p-6">
              <div className={`relative overflow-hidden rounded-xl border p-6 transition-all ${
                state.plan === 'pro' 
                  ? 'bg-gradient-to-br from-indigo-50 to-purple-50 border-indigo-200' 
                  : 'bg-white border-gray-200'
              }`}>
                {state.plan === 'pro' && (
                  <div className="absolute top-0 right-0 p-4">
                    <span className="inline-flex items-center gap-1.5 py-1 px-3 rounded-full bg-indigo-100 text-indigo-700 text-xs font-bold tracking-wide uppercase">
                      <Zap className="w-3.5 h-3.5" /> Active Plan
                    </span>
                  </div>
                )}
                
                <div className="flex items-start gap-4">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center shrink-0 ${
                    state.plan === 'pro' ? 'bg-indigo-600 text-white shadow-md' : 'bg-gray-100 text-gray-500'
                  }`}>
                    <CreditCard className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-gray-900">
                      {state.plan === 'pro' ? 'DevPulse Pro' : 'Free Plan'}
                    </h3>
                    <p className="text-sm text-gray-500 mt-1 max-w-md">
                      {state.plan === 'pro' 
                        ? 'You have access to advanced metrics, unlimited data retention, and priority support.' 
                        : 'Basic metrics for individual developers. Upgrade to unlock team velocity trends and unlimited history.'}
                    </p>
                    
                    <button 
                      onClick={togglePlan}
                      className={`mt-6 px-5 py-2.5 rounded-xl font-medium transition-all ${
                        state.plan === 'pro'
                          ? 'bg-white text-gray-900 border border-gray-200 hover:bg-gray-50 shadow-sm'
                          : 'bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white shadow-md'
                      }`}
                    >
                      {state.plan === 'pro' ? 'Downgrade to Free' : 'Upgrade to Pro - $19/mo'}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}
